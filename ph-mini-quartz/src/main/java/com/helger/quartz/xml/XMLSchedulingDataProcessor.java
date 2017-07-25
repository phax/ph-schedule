/**
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 * Copyright (C) 2016-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.quartz.xml;

import static com.helger.quartz.CalendarIntervalScheduleBuilder.calendarIntervalSchedule;
import static com.helger.quartz.CronScheduleBuilder.cronSchedule;
import static com.helger.quartz.JobBuilder.newJob;
import static com.helger.quartz.SimpleScheduleBuilder.simpleSchedule;
import static com.helger.quartz.TriggerBuilder.newTrigger;
import static com.helger.quartz.TriggerKey.triggerKey;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.quartz.CalendarIntervalScheduleBuilder;
import com.helger.quartz.CronScheduleBuilder;
import com.helger.quartz.EIntervalUnit;
import com.helger.quartz.IJob;
import com.helger.quartz.IJobDetail;
import com.helger.quartz.IScheduleBuilder;
import com.helger.quartz.IScheduler;
import com.helger.quartz.ITrigger;
import com.helger.quartz.JobKey;
import com.helger.quartz.JobPersistenceException;
import com.helger.quartz.ObjectAlreadyExistsException;
import com.helger.quartz.SchedulerException;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.TriggerKey;
import com.helger.quartz.impl.matchers.GroupMatcher;
import com.helger.quartz.spi.IClassLoadHelper;
import com.helger.quartz.spi.IMutableTrigger;

/**
 * Parses an XML file that declares Jobs and their schedules (Triggers), and
 * processes the related data. The xml document must conform to the format
 * defined in "job_scheduling_data_2_0.xsd" The same instance can be used again
 * and again, however a single instance is not thread-safe.
 *
 * @author James House
 * @author Past contributions from
 *         <a href="mailto:bonhamcm@thirdeyeconsulting.com">Chris Bonham</a>
 * @author Past contributions from pl47ypus
 * @since Quartz 1.8
 */
public class XMLSchedulingDataProcessor
{
  public static final String QUARTZ_NS = "http://www.quartz-scheduler.org/xml/JobSchedulingData";
  public static final String QUARTZ_SCHEMA_WEB_URL = "http://www.quartz-scheduler.org/xml/job_scheduling_data_2_0.xsd";
  public static final String QUARTZ_XSD_PATH_IN_JAR = "quartz/job_scheduling_data_2_0.xsd";
  public static final String QUARTZ_XML_DEFAULT_FILE_NAME = "quartz_data.xml";
  public static final String QUARTZ_SYSTEM_ID_JAR_PREFIX = "jar:";

  // pre-processing commands
  protected ICommonsList <String> m_aJobGroupsToDelete = new CommonsArrayList <> ();
  protected ICommonsList <String> m_aTriggerGroupsToDelete = new CommonsArrayList <> ();
  protected ICommonsList <JobKey> m_aJobsToDelete = new CommonsArrayList <> ();
  protected ICommonsList <TriggerKey> m_aTriggersToDelete = new CommonsArrayList <> ();

  // scheduling commands
  protected ICommonsList <IJobDetail> m_aLoadedJobs = new CommonsArrayList <> ();
  protected ICommonsList <IMutableTrigger> m_aLoadedTriggers = new CommonsArrayList <> ();

  // directives
  private boolean m_bOverWriteExistingData = true;
  private boolean m_bIgnoreDuplicates = false;

  protected ICommonsList <Exception> m_aValidationExceptions = new CommonsArrayList <> ();

  protected IClassLoadHelper m_aClassLoadHelper;
  protected ICommonsList <String> m_aJobGroupsToNeverDelete = new CommonsArrayList <> ();
  protected ICommonsList <String> m_aTriggerGroupsToNeverDelete = new CommonsArrayList <> ();

  private DocumentBuilder m_aDocBuilder;
  private XPath m_aXPath;

  private final Logger m_aLog = LoggerFactory.getLogger (getClass ());

  /**
   * Constructor for JobSchedulingDataLoader.
   *
   * @param clh
   *        class-loader helper to share with digester.
   * @throws ParserConfigurationException
   *         if the XML parser cannot be configured as needed.
   */
  public XMLSchedulingDataProcessor (@Nonnull final IClassLoadHelper clh) throws ParserConfigurationException
  {
    m_aClassLoadHelper = clh;
    initDocumentParser ();
  }

  /**
   * Initializes the XML parser.
   *
   * @throws ParserConfigurationException
   */
  protected void initDocumentParser () throws ParserConfigurationException
  {
    final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance ();
    docBuilderFactory.setNamespaceAware (true);
    docBuilderFactory.setValidating (true);
    docBuilderFactory.setAttribute ("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                                    "http://www.w3.org/2001/XMLSchema");
    docBuilderFactory.setAttribute ("http://java.sun.com/xml/jaxp/properties/schemaSource", resolveSchemaSource ());

    m_aDocBuilder = docBuilderFactory.newDocumentBuilder ();
    m_aDocBuilder.setErrorHandler (new ErrorHandler ()
    {

      /**
       * ErrorHandler interface. Receive notification of a warning.
       *
       * @param e
       *        The error information encapsulated in a SAX parse exception.
       * @exception SAXException
       *            Any SAX exception, possibly wrapping another exception.
       */
      public void warning (@Nonnull final SAXParseException e) throws SAXException
      {
        addValidationException (e);
      }

      /**
       * ErrorHandler interface. Receive notification of a recoverable error.
       *
       * @param e
       *        The error information encapsulated in a SAX parse exception.
       * @exception SAXException
       *            Any SAX exception, possibly wrapping another exception.
       */
      public void error (@Nonnull final SAXParseException e) throws SAXException
      {
        addValidationException (e);
      }

      /**
       * ErrorHandler interface. Receive notification of a non-recoverable
       * error.
       *
       * @param e
       *        The error information encapsulated in a SAX parse exception.
       * @exception SAXException
       *            Any SAX exception, possibly wrapping another exception.
       */
      public void fatalError (@Nonnull final SAXParseException e) throws SAXException
      {
        addValidationException (e);
      }
    });

    final NamespaceContext nsContext = new NamespaceContext ()
    {
      public String getNamespaceURI (final String prefix)
      {
        if (prefix == null)
          throw new IllegalArgumentException ("Null prefix");
        if (XMLConstants.XML_NS_PREFIX.equals (prefix))
          return XMLConstants.XML_NS_URI;
        if (XMLConstants.XMLNS_ATTRIBUTE.equals (prefix))
          return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

        if ("q".equals (prefix))
          return QUARTZ_NS;

        return XMLConstants.NULL_NS_URI;
      }

      public Iterator <?> getPrefixes (final String namespaceURI)
      {
        // This method isn't necessary for XPath processing.
        throw new UnsupportedOperationException ();
      }

      public String getPrefix (final String namespaceURI)
      {
        // This method isn't necessary for XPath processing.
        throw new UnsupportedOperationException ();
      }

    };

    m_aXPath = XPathFactory.newInstance ().newXPath ();
    m_aXPath.setNamespaceContext (nsContext);
  }

  protected Object resolveSchemaSource ()
  {
    InputSource inputSource;
    InputStream is = null;
    try
    {
      is = m_aClassLoadHelper.getResourceAsStream (QUARTZ_XSD_PATH_IN_JAR);
    }
    finally
    {
      if (is != null)
      {
        inputSource = new InputSource (is);
        inputSource.setSystemId (QUARTZ_SCHEMA_WEB_URL);
        if (m_aLog.isDebugEnabled ())
          m_aLog.debug ("Utilizing schema packaged in local quartz distribution jar.");
      }
      else
      {
        m_aLog.info ("Unable to load local schema packaged in quartz distribution jar. Utilizing schema online at " +
                     QUARTZ_SCHEMA_WEB_URL);
        return QUARTZ_SCHEMA_WEB_URL;
      }
    }
    return inputSource;
  }

  /**
   * Whether the existing scheduling data (with same identifiers) will be
   * overwritten. If false, and <code>IgnoreDuplicates</code> is not false, and
   * jobs or triggers with the same names already exist as those in the file, an
   * error will occur.
   *
   * @see #isIgnoreDuplicates()
   */
  public boolean isOverWriteExistingData ()
  {
    return m_bOverWriteExistingData;
  }

  /**
   * Whether the existing scheduling data (with same identifiers) will be
   * overwritten. If false, and <code>IgnoreDuplicates</code> is not false, and
   * jobs or triggers with the same names already exist as those in the file, an
   * error will occur.
   *
   * @see #setIgnoreDuplicates(boolean)
   */
  protected void setOverWriteExistingData (final boolean overWriteExistingData)
  {
    m_bOverWriteExistingData = overWriteExistingData;
  }

  /**
   * If true (and <code>OverWriteExistingData</code> is false) then any
   * job/triggers encountered in this file that have names that already exist in
   * the scheduler will be ignored, and no error will be produced.
   *
   * @see #isOverWriteExistingData()
   */
  public boolean isIgnoreDuplicates ()
  {
    return m_bIgnoreDuplicates;
  }

  /**
   * If true (and <code>OverWriteExistingData</code> is false) then any
   * job/triggers encountered in this file that have names that already exist in
   * the scheduler will be ignored, and no error will be produced.
   *
   * @see #setOverWriteExistingData(boolean)
   */
  public void setIgnoreDuplicates (final boolean ignoreDuplicates)
  {
    m_bIgnoreDuplicates = ignoreDuplicates;
  }

  /**
   * Add the given group to the list of job groups that will never be deleted by
   * this processor, even if a pre-processing-command to delete the group is
   * encountered.
   */
  public void addJobGroupToNeverDelete (final String group)
  {
    if (group != null)
      m_aJobGroupsToNeverDelete.add (group);
  }

  /**
   * Remove the given group to the list of job groups that will never be deleted
   * by this processor, even if a pre-processing-command to delete the group is
   * encountered.
   */
  public boolean removeJobGroupToNeverDelete (final String group)
  {
    return group != null && m_aJobGroupsToNeverDelete.remove (group);
  }

  /**
   * Get the (unmodifiable) list of job groups that will never be deleted by
   * this processor, even if a pre-processing-command to delete the group is
   * encountered.
   */
  public List <String> getJobGroupsToNeverDelete ()
  {
    return Collections.unmodifiableList (m_aJobGroupsToDelete);
  }

  /**
   * Add the given group to the list of trigger groups that will never be
   * deleted by this processor, even if a pre-processing-command to delete the
   * group is encountered.
   */
  public void addTriggerGroupToNeverDelete (final String group)
  {
    if (group != null)
      m_aTriggerGroupsToNeverDelete.add (group);
  }

  /**
   * Remove the given group to the list of trigger groups that will never be
   * deleted by this processor, even if a pre-processing-command to delete the
   * group is encountered.
   */
  public boolean removeTriggerGroupToNeverDelete (final String group)
  {
    if (group != null)
      return m_aTriggerGroupsToNeverDelete.remove (group);
    return false;
  }

  /**
   * Get the (unmodifiable) list of trigger groups that will never be deleted by
   * this processor, even if a pre-processing-command to delete the group is
   * encountered.
   */
  public List <String> getTriggerGroupsToNeverDelete ()
  {
    return Collections.unmodifiableList (m_aTriggerGroupsToDelete);
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Interface.
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Process the xml file in the default location (a file named
   * "quartz_jobs.xml" in the current working directory).
   */
  protected void processFile () throws Exception
  {
    processFile (QUARTZ_XML_DEFAULT_FILE_NAME);
  }

  /**
   * Process the xml file named <code>fileName</code>.
   *
   * @param fileName
   *        meta data file name.
   */
  protected void processFile (final String fileName) throws Exception
  {
    processFile (fileName, getSystemIdForFileName (fileName));
  }

  /**
   * For the given <code>fileName</code>, attempt to expand it to its full path
   * for use as a system id.
   *
   * @see #getURL(String)
   * @see #processFile()
   * @see #processFile(String)
   * @see #processFileAndScheduleJobs(IScheduler, boolean)
   * @see #processFileAndScheduleJobs(String, com.helger.quartz.IScheduler)
   */
  protected String getSystemIdForFileName (final String fileName)
  {
    final File file = new File (fileName); // files in filesystem
    if (file.exists ())
      return file.toURI ().toString ();

    final URL url = getURL (fileName);
    if (url == null)
      return fileName;

    try
    {
      url.openStream ().close ();
      return url.toString ();
    }
    catch (final IOException ignore)
    {
      return fileName;
    }
  }

  /**
   * Returns an <code>URL</code> from the fileName as a resource.
   *
   * @param fileName
   *        file name.
   * @return an <code>URL</code> from the fileName as a resource.
   */
  protected URL getURL (final String fileName)
  {
    return m_aClassLoadHelper.getResource (fileName);
  }

  protected void prepForProcessing ()
  {
    clearValidationExceptions ();

    setOverWriteExistingData (true);
    setIgnoreDuplicates (false);

    m_aJobGroupsToDelete.clear ();
    m_aJobsToDelete.clear ();
    m_aTriggerGroupsToDelete.clear ();
    m_aTriggersToDelete.clear ();

    m_aLoadedJobs.clear ();
    m_aLoadedTriggers.clear ();
  }

  /**
   * Process the xmlfile named <code>fileName</code> with the given system ID.
   *
   * @param fileName
   *        meta data file name.
   * @param systemId
   *        system ID.
   */
  protected void processFile (final String fileName,
                              final String systemId) throws ValidationException,
                                                     SAXException,
                                                     IOException,
                                                     ClassNotFoundException,
                                                     ParseException,
                                                     XPathException
  {

    prepForProcessing ();

    m_aLog.info ("Parsing XML file: " + fileName + " with systemId: " + systemId);
    final InputSource is = new InputSource (getInputStream (fileName));
    is.setSystemId (systemId);

    process (is);

    maybeThrowValidationException ();
  }

  /**
   * Process the xmlfile named <code>fileName</code> with the given system ID.
   *
   * @param stream
   *        an input stream containing the xml content.
   * @param systemId
   *        system ID.
   */
  public void processStreamAndScheduleJobs (final InputStream stream,
                                            final String systemId,
                                            final IScheduler sched) throws ValidationException,
                                                                    SAXException,
                                                                    XPathException,
                                                                    IOException,
                                                                    SchedulerException,
                                                                    ClassNotFoundException,
                                                                    ParseException
  {

    prepForProcessing ();

    m_aLog.info ("Parsing XML from stream with systemId: " + systemId);

    final InputSource is = new InputSource (stream);
    is.setSystemId (systemId);

    process (is);
    executePreProcessCommands (sched);
    scheduleJobs (sched);

    maybeThrowValidationException ();
  }

  protected void process (final InputSource is) throws SAXException,
                                                IOException,
                                                ParseException,
                                                XPathException,
                                                ClassNotFoundException
  {

    // load the document
    final Document document = m_aDocBuilder.parse (is);

    //
    // Extract pre-processing commands
    //

    final NodeList deleteJobGroupNodes = (NodeList) m_aXPath.evaluate ("/q:job-scheduling-data/q:pre-processing-commands/q:delete-jobs-in-group",
                                                                       document,
                                                                       XPathConstants.NODESET);

    if (m_aLog.isDebugEnabled ())
      m_aLog.debug ("Found " + deleteJobGroupNodes.getLength () + " delete job group commands.");

    for (int i = 0; i < deleteJobGroupNodes.getLength (); i++)
    {
      final Node node = deleteJobGroupNodes.item (i);
      String t = node.getTextContent ();
      if (t == null || (t = t.trim ()).length () == 0)
        continue;
      m_aJobGroupsToDelete.add (t);
    }

    final NodeList deleteTriggerGroupNodes = (NodeList) m_aXPath.evaluate ("/q:job-scheduling-data/q:pre-processing-commands/q:delete-triggers-in-group",
                                                                           document,
                                                                           XPathConstants.NODESET);

    if (m_aLog.isDebugEnabled ())
      m_aLog.debug ("Found " + deleteTriggerGroupNodes.getLength () + " delete trigger group commands.");

    for (int i = 0; i < deleteTriggerGroupNodes.getLength (); i++)
    {
      final Node node = deleteTriggerGroupNodes.item (i);
      String t = node.getTextContent ();
      if (t == null || (t = t.trim ()).length () == 0)
        continue;
      m_aTriggerGroupsToDelete.add (t);
    }

    final NodeList deleteJobNodes = (NodeList) m_aXPath.evaluate ("/q:job-scheduling-data/q:pre-processing-commands/q:delete-job",
                                                                  document,
                                                                  XPathConstants.NODESET);

    if (m_aLog.isDebugEnabled ())
      m_aLog.debug ("Found " + deleteJobNodes.getLength () + " delete job commands.");

    for (int i = 0; i < deleteJobNodes.getLength (); i++)
    {
      final Node node = deleteJobNodes.item (i);

      final String name = getTrimmedToNullString (m_aXPath, "q:name", node);
      final String group = getTrimmedToNullString (m_aXPath, "q:group", node);

      if (name == null)
        throw new ParseException ("Encountered a 'delete-job' command without a name specified.", -1);
      m_aJobsToDelete.add (new JobKey (name, group));
    }

    final NodeList deleteTriggerNodes = (NodeList) m_aXPath.evaluate ("/q:job-scheduling-data/q:pre-processing-commands/q:delete-trigger",
                                                                      document,
                                                                      XPathConstants.NODESET);

    if (m_aLog.isDebugEnabled ())
      m_aLog.debug ("Found " + deleteTriggerNodes.getLength () + " delete trigger commands.");

    for (int i = 0; i < deleteTriggerNodes.getLength (); i++)
    {
      final Node node = deleteTriggerNodes.item (i);

      final String name = getTrimmedToNullString (m_aXPath, "q:name", node);
      final String group = getTrimmedToNullString (m_aXPath, "q:group", node);

      if (name == null)
        throw new ParseException ("Encountered a 'delete-trigger' command without a name specified.", -1);
      m_aTriggersToDelete.add (new TriggerKey (name, group));
    }

    //
    // Extract directives
    //

    final Boolean overWrite = getBoolean (m_aXPath,
                                          "/q:job-scheduling-data/q:processing-directives/q:overwrite-existing-data",
                                          document);
    if (overWrite == null)
    {
      if (m_aLog.isDebugEnabled ())
        m_aLog.debug ("Directive 'overwrite-existing-data' not specified, defaulting to " + isOverWriteExistingData ());
    }
    else
    {
      if (m_aLog.isDebugEnabled ())
        m_aLog.debug ("Directive 'overwrite-existing-data' specified as: " + overWrite);
      setOverWriteExistingData (overWrite);
    }

    final Boolean ignoreDupes = getBoolean (m_aXPath,
                                            "/q:job-scheduling-data/q:processing-directives/q:ignore-duplicates",
                                            document);
    if (ignoreDupes == null)
    {
      if (m_aLog.isDebugEnabled ())
        m_aLog.debug ("Directive 'ignore-duplicates' not specified, defaulting to " + isIgnoreDuplicates ());
    }
    else
    {
      if (m_aLog.isDebugEnabled ())
        m_aLog.debug ("Directive 'ignore-duplicates' specified as: " + ignoreDupes);
      setIgnoreDuplicates (ignoreDupes);
    }

    //
    // Extract Job definitions...
    //

    final NodeList jobNodes = (NodeList) m_aXPath.evaluate ("/q:job-scheduling-data/q:schedule/q:job",
                                                            document,
                                                            XPathConstants.NODESET);

    if (m_aLog.isDebugEnabled ())
      m_aLog.debug ("Found " + jobNodes.getLength () + " job definitions.");

    for (int i = 0; i < jobNodes.getLength (); i++)
    {
      final Node jobDetailNode = jobNodes.item (i);
      String t = null;

      final String jobName = getTrimmedToNullString (m_aXPath, "q:name", jobDetailNode);
      final String jobGroup = getTrimmedToNullString (m_aXPath, "q:group", jobDetailNode);
      final String jobDescription = getTrimmedToNullString (m_aXPath, "q:description", jobDetailNode);
      final String jobClassName = getTrimmedToNullString (m_aXPath, "q:job-class", jobDetailNode);
      t = getTrimmedToNullString (m_aXPath, "q:durability", jobDetailNode);
      final boolean jobDurability = (t != null) && t.equals ("true");
      t = getTrimmedToNullString (m_aXPath, "q:recover", jobDetailNode);
      final boolean jobRecoveryRequested = (t != null) && t.equals ("true");

      final Class <? extends IJob> jobClass = m_aClassLoadHelper.loadClass (jobClassName, IJob.class);

      final IJobDetail jobDetail = newJob (jobClass).withIdentity (jobName, jobGroup)
                                                    .withDescription (jobDescription)
                                                    .storeDurably (jobDurability)
                                                    .requestRecovery (jobRecoveryRequested)
                                                    .build ();

      final NodeList jobDataEntries = (NodeList) m_aXPath.evaluate ("q:job-data-map/q:entry",
                                                                    jobDetailNode,
                                                                    XPathConstants.NODESET);

      for (int k = 0; k < jobDataEntries.getLength (); k++)
      {
        final Node entryNode = jobDataEntries.item (k);
        final String key = getTrimmedToNullString (m_aXPath, "q:key", entryNode);
        final String value = getTrimmedToNullString (m_aXPath, "q:value", entryNode);
        jobDetail.getJobDataMap ().put (key, value);
      }

      if (m_aLog.isDebugEnabled ())
        m_aLog.debug ("Parsed job definition: " + jobDetail);

      addJobToSchedule (jobDetail);
    }

    //
    // Extract Trigger definitions...
    //

    final NodeList triggerEntries = (NodeList) m_aXPath.evaluate ("/q:job-scheduling-data/q:schedule/q:trigger/*",
                                                                  document,
                                                                  XPathConstants.NODESET);

    if (m_aLog.isDebugEnabled ())
      m_aLog.debug ("Found " + triggerEntries.getLength () + " trigger definitions.");

    for (int j = 0; j < triggerEntries.getLength (); j++)
    {
      final Node triggerNode = triggerEntries.item (j);
      final String triggerName = getTrimmedToNullString (m_aXPath, "q:name", triggerNode);
      final String triggerGroup = getTrimmedToNullString (m_aXPath, "q:group", triggerNode);
      final String triggerDescription = getTrimmedToNullString (m_aXPath, "q:description", triggerNode);
      final String triggerMisfireInstructionConst = getTrimmedToNullString (m_aXPath,
                                                                            "q:misfire-instruction",
                                                                            triggerNode);
      final String triggerPriorityString = getTrimmedToNullString (m_aXPath, "q:priority", triggerNode);
      final String triggerCalendarRef = getTrimmedToNullString (m_aXPath, "q:calendar-name", triggerNode);
      final String triggerJobName = getTrimmedToNullString (m_aXPath, "q:job-name", triggerNode);
      final String triggerJobGroup = getTrimmedToNullString (m_aXPath, "q:job-group", triggerNode);

      int triggerPriority = ITrigger.DEFAULT_PRIORITY;
      if (triggerPriorityString != null)
        triggerPriority = Integer.valueOf (triggerPriorityString);

      final String startTimeString = getTrimmedToNullString (m_aXPath, "q:start-time", triggerNode);
      final String startTimeFutureSecsString = getTrimmedToNullString (m_aXPath,
                                                                       "q:start-time-seconds-in-future",
                                                                       triggerNode);
      final String endTimeString = getTrimmedToNullString (m_aXPath, "q:end-time", triggerNode);

      // QTZ-273 : use of DatatypeConverter.parseDateTime() instead of
      // SimpleDateFormat
      Date triggerStartTime;
      if (startTimeFutureSecsString != null)
        triggerStartTime = new Date (System.currentTimeMillis () + (Long.valueOf (startTimeFutureSecsString) * 1000L));
      else
        triggerStartTime = (startTimeString == null || startTimeString.length () == 0 ? new Date ()
                                                                                      : DatatypeConverter.parseDateTime (startTimeString)
                                                                                                         .getTime ());
      final Date triggerEndTime = endTimeString == null || endTimeString.length () == 0 ? null
                                                                                        : DatatypeConverter.parseDateTime (endTimeString)
                                                                                                           .getTime ();

      final TriggerKey triggerKey = triggerKey (triggerName, triggerGroup);

      IScheduleBuilder <?> sched;

      if (triggerNode.getNodeName ().equals ("simple"))
      {
        final String repeatCountString = getTrimmedToNullString (m_aXPath, "q:repeat-count", triggerNode);
        final String repeatIntervalString = getTrimmedToNullString (m_aXPath, "q:repeat-interval", triggerNode);

        final int repeatCount = repeatCountString == null ? 0 : Integer.parseInt (repeatCountString);
        final long repeatInterval = repeatIntervalString == null ? 0 : Long.parseLong (repeatIntervalString);

        sched = simpleSchedule ().withIntervalInMilliseconds (repeatInterval).withRepeatCount (repeatCount);

        if (triggerMisfireInstructionConst != null && triggerMisfireInstructionConst.length () != 0)
        {
          if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_FIRE_NOW"))
            ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionFireNow ();
          else
            if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT"))
              ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionNextWithExistingCount ();
            else
              if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT"))
                ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionNextWithRemainingCount ();
              else
                if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT"))
                  ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionNowWithExistingCount ();
                else
                  if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT"))
                    ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionNowWithRemainingCount ();
                  else
                    if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_SMART_POLICY"))
                    {
                      // do nothing.... (smart policy is default)
                    }
                    else
                      throw new ParseException ("Unexpected/Unhandlable Misfire Instruction encountered '" +
                                                triggerMisfireInstructionConst +
                                                "', for trigger: " +
                                                triggerKey,
                                                -1);
        }
      }
      else
        if (triggerNode.getNodeName ().equals ("cron"))
        {
          final String cronExpression = getTrimmedToNullString (m_aXPath, "q:cron-expression", triggerNode);
          final String timezoneString = getTrimmedToNullString (m_aXPath, "q:time-zone", triggerNode);

          final TimeZone tz = timezoneString == null ? null : TimeZone.getTimeZone (timezoneString);

          sched = cronSchedule (cronExpression).inTimeZone (tz);

          if (triggerMisfireInstructionConst != null && triggerMisfireInstructionConst.length () != 0)
          {
            if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_DO_NOTHING"))
              ((CronScheduleBuilder) sched).withMisfireHandlingInstructionDoNothing ();
            else
              if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_FIRE_ONCE_NOW"))
                ((CronScheduleBuilder) sched).withMisfireHandlingInstructionFireAndProceed ();
              else
                if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_SMART_POLICY"))
                {
                  // do nothing.... (smart policy is default)
                }
                else
                  throw new ParseException ("Unexpected/Unhandlable Misfire Instruction encountered '" +
                                            triggerMisfireInstructionConst +
                                            "', for trigger: " +
                                            triggerKey,
                                            -1);
          }
        }
        else
          if (triggerNode.getNodeName ().equals ("calendar-interval"))
          {
            final String repeatIntervalString = getTrimmedToNullString (m_aXPath, "q:repeat-interval", triggerNode);
            final String repeatUnitString = getTrimmedToNullString (m_aXPath, "q:repeat-interval-unit", triggerNode);

            final int repeatInterval = Integer.parseInt (repeatIntervalString);

            final EIntervalUnit repeatUnit = EIntervalUnit.valueOf (repeatUnitString);

            sched = calendarIntervalSchedule ().withInterval (repeatInterval, repeatUnit);

            if (triggerMisfireInstructionConst != null && triggerMisfireInstructionConst.length () != 0)
            {
              if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_DO_NOTHING"))
                ((CalendarIntervalScheduleBuilder) sched).withMisfireHandlingInstructionDoNothing ();
              else
                if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_FIRE_ONCE_NOW"))
                  ((CalendarIntervalScheduleBuilder) sched).withMisfireHandlingInstructionFireAndProceed ();
                else
                  if (triggerMisfireInstructionConst.equals ("MISFIRE_INSTRUCTION_SMART_POLICY"))
                  {
                    // do nothing.... (smart policy is default)
                  }
                  else
                    throw new ParseException ("Unexpected/Unhandlable Misfire Instruction encountered '" +
                                              triggerMisfireInstructionConst +
                                              "', for trigger: " +
                                              triggerKey,
                                              -1);
            }
          }
          else
          {
            throw new ParseException ("Unknown trigger type: " + triggerNode.getNodeName (), -1);
          }

      final IMutableTrigger trigger = (IMutableTrigger) newTrigger ().withIdentity (triggerName, triggerGroup)
                                                                     .withDescription (triggerDescription)
                                                                     .forJob (triggerJobName, triggerJobGroup)
                                                                     .startAt (triggerStartTime)
                                                                     .endAt (triggerEndTime)
                                                                     .withPriority (triggerPriority)
                                                                     .modifiedByCalendar (triggerCalendarRef)
                                                                     .withSchedule (sched)
                                                                     .build ();

      final NodeList jobDataEntries = (NodeList) m_aXPath.evaluate ("q:job-data-map/q:entry",
                                                                    triggerNode,
                                                                    XPathConstants.NODESET);

      for (int k = 0; k < jobDataEntries.getLength (); k++)
      {
        final Node entryNode = jobDataEntries.item (k);
        final String key = getTrimmedToNullString (m_aXPath, "q:key", entryNode);
        final String value = getTrimmedToNullString (m_aXPath, "q:value", entryNode);
        trigger.getJobDataMap ().put (key, value);
      }

      if (m_aLog.isDebugEnabled ())
        m_aLog.debug ("Parsed trigger definition: " + trigger);

      addTriggerToSchedule (trigger);
    }
  }

  protected String getTrimmedToNullString (final XPath xpathToElement,
                                           final String elementName,
                                           final Node parentNode) throws XPathExpressionException
  {
    String str = (String) xpathToElement.evaluate (elementName, parentNode, XPathConstants.STRING);

    if (str != null)
      str = str.trim ();

    if (str != null && str.length () == 0)
      str = null;

    return str;
  }

  protected Boolean getBoolean (final XPath xpathToElement,
                                final String elementName,
                                final Document document) throws XPathExpressionException
  {

    final Node directive = (Node) xpathToElement.evaluate (elementName, document, XPathConstants.NODE);

    if (directive == null || directive.getTextContent () == null)
      return null;

    final String val = directive.getTextContent ();
    if (val.equalsIgnoreCase ("true") || val.equalsIgnoreCase ("yes") || val.equalsIgnoreCase ("y"))
      return Boolean.TRUE;

    return Boolean.FALSE;
  }

  /**
   * Process the xml file in the default location, and schedule all of the jobs
   * defined within it.
   * <p>
   * Note that we will set overWriteExistingJobs after the default xml is
   * parsed.
   */
  public void processFileAndScheduleJobs (final IScheduler sched, final boolean overWriteExistingJobs) throws Exception
  {
    final String fileName = QUARTZ_XML_DEFAULT_FILE_NAME;
    processFile (fileName, getSystemIdForFileName (fileName));
    // The overWriteExistingJobs flag was set by processFile() ->
    // prepForProcessing(), then by xml parsing, and then now
    // we need to reset it again here by this method parameter to override it.
    setOverWriteExistingData (overWriteExistingJobs);
    executePreProcessCommands (sched);
    scheduleJobs (sched);
  }

  /**
   * Process the xml file in the given location, and schedule all of the jobs
   * defined within it.
   *
   * @param fileName
   *        meta data file name.
   */
  public void processFileAndScheduleJobs (final String fileName, final IScheduler sched) throws Exception
  {
    processFileAndScheduleJobs (fileName, getSystemIdForFileName (fileName), sched);
  }

  /**
   * Process the xml file in the given location, and schedule all of the jobs
   * defined within it.
   *
   * @param fileName
   *        meta data file name.
   */
  public void processFileAndScheduleJobs (final String fileName,
                                          final String systemId,
                                          final IScheduler sched) throws Exception
  {
    processFile (fileName, systemId);
    executePreProcessCommands (sched);
    scheduleJobs (sched);
  }

  /**
   * Returns a <code>List</code> of jobs loaded from the xml file.
   *
   * @return a <code>List</code> of jobs.
   */
  protected List <IJobDetail> getLoadedJobs ()
  {
    return Collections.unmodifiableList (m_aLoadedJobs);
  }

  /**
   * Returns a <code>List</code> of triggers loaded from the xml file.
   *
   * @return a <code>List</code> of triggers.
   */
  protected List <IMutableTrigger> getLoadedTriggers ()
  {
    return Collections.unmodifiableList (m_aLoadedTriggers);
  }

  /**
   * Returns an <code>InputStream</code> from the fileName as a resource.
   *
   * @param fileName
   *        file name.
   * @return an <code>InputStream</code> from the fileName as a resource.
   */
  protected InputStream getInputStream (final String fileName)
  {
    return m_aClassLoadHelper.getResourceAsStream (fileName);
  }

  protected void addJobToSchedule (final IJobDetail job)
  {
    m_aLoadedJobs.add (job);
  }

  protected void addTriggerToSchedule (final IMutableTrigger trigger)
  {
    m_aLoadedTriggers.add (trigger);
  }

  private Map <JobKey, List <IMutableTrigger>> buildTriggersByFQJobNameMap (final List <IMutableTrigger> triggers)
  {

    final Map <JobKey, List <IMutableTrigger>> triggersByFQJobName = new HashMap <> ();

    for (final IMutableTrigger trigger : triggers)
    {
      List <IMutableTrigger> triggersOfJob = triggersByFQJobName.get (trigger.getJobKey ());
      if (triggersOfJob == null)
      {
        triggersOfJob = new CommonsArrayList <> ();
        triggersByFQJobName.put (trigger.getJobKey (), triggersOfJob);
      }
      triggersOfJob.add (trigger);
    }

    return triggersByFQJobName;
  }

  protected void executePreProcessCommands (final IScheduler scheduler) throws SchedulerException
  {

    for (final String group : m_aJobGroupsToDelete)
    {
      if (group.equals ("*"))
      {
        m_aLog.info ("Deleting all jobs in ALL groups.");
        for (final String groupName : scheduler.getJobGroupNames ())
        {
          if (!m_aJobGroupsToNeverDelete.contains (groupName))
          {
            for (final JobKey key : scheduler.getJobKeys (GroupMatcher.jobGroupEquals (groupName)))
            {
              scheduler.deleteJob (key);
            }
          }
        }
      }
      else
      {
        if (!m_aJobGroupsToNeverDelete.contains (group))
        {
          m_aLog.info ("Deleting all jobs in group: {}", group);
          for (final JobKey key : scheduler.getJobKeys (GroupMatcher.jobGroupEquals (group)))
          {
            scheduler.deleteJob (key);
          }
        }
      }
    }

    for (final String group : m_aTriggerGroupsToDelete)
    {
      if (group.equals ("*"))
      {
        m_aLog.info ("Deleting all triggers in ALL groups.");
        for (final String groupName : scheduler.getTriggerGroupNames ())
        {
          if (!m_aTriggerGroupsToNeverDelete.contains (groupName))
          {
            for (final TriggerKey key : scheduler.getTriggerKeys (GroupMatcher.triggerGroupEquals (groupName)))
            {
              scheduler.unscheduleJob (key);
            }
          }
        }
      }
      else
      {
        if (!m_aTriggerGroupsToNeverDelete.contains (group))
        {
          m_aLog.info ("Deleting all triggers in group: {}", group);
          for (final TriggerKey key : scheduler.getTriggerKeys (GroupMatcher.triggerGroupEquals (group)))
          {
            scheduler.unscheduleJob (key);
          }
        }
      }
    }

    for (final JobKey key : m_aJobsToDelete)
    {
      if (!m_aJobGroupsToNeverDelete.contains (key.getGroup ()))
      {
        m_aLog.info ("Deleting job: {}", key);
        scheduler.deleteJob (key);
      }
    }

    for (final TriggerKey key : m_aTriggersToDelete)
    {
      if (!m_aTriggerGroupsToNeverDelete.contains (key.getGroup ()))
      {
        m_aLog.info ("Deleting trigger: {}", key);
        scheduler.unscheduleJob (key);
      }
    }
  }

  /**
   * Schedules the given sets of jobs and triggers.
   *
   * @param sched
   *        job scheduler.
   * @exception SchedulerException
   *            if the Job or Trigger cannot be added to the Scheduler, or there
   *            is an internal Scheduler error.
   */
  protected void scheduleJobs (final IScheduler sched) throws SchedulerException
  {
    final ICommonsList <IJobDetail> jobs = new CommonsArrayList <> (getLoadedJobs ());
    final ICommonsList <IMutableTrigger> triggers = new CommonsArrayList <> (getLoadedTriggers ());

    m_aLog.info ("Adding " + jobs.size () + " jobs, " + triggers.size () + " triggers.");

    final Map <JobKey, List <IMutableTrigger>> triggersByFQJobName = buildTriggersByFQJobNameMap (triggers);

    // add each job, and it's associated triggers
    final Iterator <IJobDetail> itr = jobs.iterator ();
    while (itr.hasNext ())
    {
      final IJobDetail detail = itr.next ();
      itr.remove (); // remove jobs as we handle them...

      IJobDetail dupeJ = null;
      try
      {
        // The existing job could have been deleted, and Quartz API doesn't
        // allow us to query this without
        // loading the job class, so use try/catch to handle it.
        dupeJ = sched.getJobDetail (detail.getKey ());
      }
      catch (final JobPersistenceException e)
      {
        if (e.getCause () instanceof ClassNotFoundException && isOverWriteExistingData ())
        {
          // We are going to replace jobDetail anyway, so just delete it first.
          m_aLog.info ("Removing job: " + detail.getKey ());
          sched.deleteJob (detail.getKey ());
        }
        else
        {
          throw e;
        }
      }

      if ((dupeJ != null))
      {
        if (!isOverWriteExistingData () && isIgnoreDuplicates ())
        {
          m_aLog.info ("Not overwriting existing job: " + dupeJ.getKey ());
          continue; // just ignore the entry
        }
        if (!isOverWriteExistingData () && !isIgnoreDuplicates ())
        {
          throw new ObjectAlreadyExistsException (detail);
        }
      }

      if (dupeJ != null)
      {
        m_aLog.info ("Replacing job: " + detail.getKey ());
      }
      else
      {
        m_aLog.info ("Adding job: " + detail.getKey ());
      }

      final List <IMutableTrigger> triggersOfJob = triggersByFQJobName.get (detail.getKey ());

      if (!detail.isDurable () && (triggersOfJob == null || triggersOfJob.size () == 0))
      {
        if (dupeJ == null)
        {
          throw new SchedulerException ("A new job defined without any triggers must be durable: " + detail.getKey ());
        }

        if ((dupeJ.isDurable () && (sched.getTriggersOfJob (detail.getKey ()).size () == 0)))
        {
          throw new SchedulerException ("Can't change existing durable job without triggers to non-durable: " +
                                        detail.getKey ());
        }
      }

      if (dupeJ != null || detail.isDurable ())
      {
        if (triggersOfJob != null && triggersOfJob.size () > 0)
          sched.addJob (detail, true, true); // add the job regardless is
                                             // durable or not b/c we have
                                             // trigger to add
        else
          sched.addJob (detail, true, false); // add the job only if a
                                              // replacement or durable, else
                                              // exception will throw!
      }
      else
      {
        boolean addJobWithFirstSchedule = true;

        // Add triggers related to the job...
        for (final IMutableTrigger trigger : triggersOfJob)
        {
          triggers.remove (trigger); // remove triggers as we handle them...

          if (trigger.getStartTime () == null)
            trigger.setStartTime (new Date ());

          final ITrigger dupeT = sched.getTrigger (trigger.getKey ());
          if (dupeT != null)
          {
            if (isOverWriteExistingData ())
            {
              if (m_aLog.isDebugEnabled ())
              {
                m_aLog.debug ("Rescheduling job: " +
                              trigger.getJobKey () +
                              " with updated trigger: " +
                              trigger.getKey ());
              }
            }
            else
              if (isIgnoreDuplicates ())
              {
                m_aLog.info ("Not overwriting existing trigger: " + dupeT.getKey ());
                continue; // just ignore the trigger (and possibly job)
              }
              else
              {
                throw new ObjectAlreadyExistsException (trigger);
              }

            if (!dupeT.getJobKey ().equals (trigger.getJobKey ()))
            {
              m_aLog.warn ("Possibly duplicately named ({}) triggers in jobs xml file! ", trigger.getKey ());
            }

            sched.rescheduleJob (trigger.getKey (), trigger);
          }
          else
          {
            if (m_aLog.isDebugEnabled ())
            {
              m_aLog.debug ("Scheduling job: " + trigger.getJobKey () + " with trigger: " + trigger.getKey ());
            }

            try
            {
              if (addJobWithFirstSchedule)
              {
                sched.scheduleJob (detail, trigger); // add the job if it's not
                                                     // in yet...
                addJobWithFirstSchedule = false;
              }
              else
              {
                sched.scheduleJob (trigger);
              }
            }
            catch (final ObjectAlreadyExistsException e)
            {
              if (m_aLog.isDebugEnabled ())
              {
                m_aLog.debug ("Adding trigger: " +
                              trigger.getKey () +
                              " for job: " +
                              detail.getKey () +
                              " failed because the trigger already existed.  " +
                              "This is likely due to a race condition between multiple instances " +
                              "in the cluster.  Will try to reschedule instead.");
              }

              // Let's try one more time as reschedule.
              sched.rescheduleJob (trigger.getKey (), trigger);
            }
          }
        }
      }
    }

    // add triggers that weren't associated with a new job... (those we already
    // handled were removed above)
    for (final IMutableTrigger trigger : triggers)
    {
      if (trigger.getStartTime () == null)
        trigger.setStartTime (new Date ());

      final ITrigger dupeT = sched.getTrigger (trigger.getKey ());
      if (dupeT != null)
      {
        if (isOverWriteExistingData ())
        {
          if (m_aLog.isDebugEnabled ())
          {
            m_aLog.debug ("Rescheduling job: " + trigger.getJobKey () + " with updated trigger: " + trigger.getKey ());
          }
        }
        else
          if (isIgnoreDuplicates ())
          {
            m_aLog.info ("Not overwriting existing trigger: " + dupeT.getKey ());
            continue; // just ignore the trigger
          }
          else
          {
            throw new ObjectAlreadyExistsException (trigger);
          }

        if (!dupeT.getJobKey ().equals (trigger.getJobKey ()))
        {
          m_aLog.warn ("Possibly duplicately named ({}) triggers in jobs xml file! ", trigger.getKey ());
        }

        sched.rescheduleJob (trigger.getKey (), trigger);
      }
      else
      {
        if (m_aLog.isDebugEnabled ())
        {
          m_aLog.debug ("Scheduling job: " + trigger.getJobKey () + " with trigger: " + trigger.getKey ());
        }

        try
        {
          sched.scheduleJob (trigger);
        }
        catch (final ObjectAlreadyExistsException e)
        {
          if (m_aLog.isDebugEnabled ())
          {
            m_aLog.debug ("Adding trigger: " +
                          trigger.getKey () +
                          " for job: " +
                          trigger.getJobKey () +
                          " failed because the trigger already existed.  " +
                          "This is likely due to a race condition between multiple instances " +
                          "in the cluster.  Will try to reschedule instead.");
          }

          // Let's rescheduleJob one more time.
          sched.rescheduleJob (trigger.getKey (), trigger);
        }
      }
    }
  }

  /**
   * Adds a detected validation exception.
   *
   * @param e
   *        SAX exception.
   */
  protected void addValidationException (@Nonnull final SAXException e)
  {
    m_aValidationExceptions.add (e);
  }

  /**
   * Resets the the number of detected validation exceptions.
   */
  protected void clearValidationExceptions ()
  {
    m_aValidationExceptions.clear ();
  }

  /**
   * Throws a ValidationException if the number of validationExceptions detected
   * is greater than zero.
   *
   * @exception ValidationException
   *            DTD validation exception.
   */
  protected void maybeThrowValidationException () throws ValidationException
  {
    if (m_aValidationExceptions.isNotEmpty ())
    {
      throw new ValidationException ("Encountered " +
                                     m_aValidationExceptions.size () +
                                     " validation exceptions.",
                                     m_aValidationExceptions);
    }
  }
}
