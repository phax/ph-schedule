# ph-schedule

Java scheduling library based on Quartz with scope support (see [ph-commons](https://github.com/phax/ph-commons))

# News and noteworthy

* v4.0.2 - work in progress
    * Improved debug logging
    * Improved code quality slightly
* v4.0.1 - 2018-11-12
    * Fixed OSGI ServiceProvider configuration
    * Removed `com.helger.quartz.xml` package
* v4.0.0 - 2017-12-06
    * Updated to ph-commons 9.0.0
* v3.6.1 - 2017-03-29
    * Started updating MiniQuartz API for Java 8
* v3.6.0 - 2016-12-12
    * Moved AbstractScopeAwareJob to ph-web (reverted dependencies)
* v3.5.0 - 2016-07-22
    * Using a forked version of Quartz with less dependencies - "Mini quartz"  

# Maven usage

Add the following to your pom.xml to use this artifact:

```xml
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>ph-schedule</artifactId>
  <version>4.0.1</version>
</dependency>
```

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
