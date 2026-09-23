# ph-schedule

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger.schedule/ph-schedule-parent-pom/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger.schedule/ph-schedule-parent-pom/)
[![javadoc](https://javadoc.io/badge2/com.helger.schedule/ph-mini-quartz/javadoc.svg)](https://javadoc.io/doc/com.helger.schedule/ph-mini-quartz)

> If this project saved you some time or made your day a little easier, a star would mean a lot — it helps others find it too.
<!-- ph-badge-end -->

Java scheduling library based on Quartz with ph-scope support (see [ph-commons](https://github.com/phax/ph-commons))

# Maven usage

Add the following to your pom.xml to use this artifact, replacing `x.y.z` with the effective version number:

```xml
<dependency>
  <groupId>com.helger.schedule</groupId>
  <artifactId>ph-schedule</artifactId>
  <version>x.y.z</version>
</dependency>
```

# Security notes

This library is an in-process scheduler. Three points are worth knowing when integrating it into a host application:

* **`SchedulerRepository` is process-wide.** `com.helger.quartz.impl.SchedulerRepository` is a static singleton keyed by scheduler name, and `lookup(String)` / `lookupAll()` are public with no access check. In a deployment where `ph-mini-quartz` lives in a *shared* classloader serving multiple applications (e.g. dropped into `$CATALINA_HOME/lib` of a Tomcat hosting several WARs), application A can retrieve and manipulate application B's scheduler. Either ship `ph-mini-quartz` inside each application's own classloader (e.g. `WEB-INF/lib`), or do not mix mutually untrusting applications in the same JVM.
* **`PropertySettingJobFactory` and untrusted `JobDataMap` contents.** By default this factory invokes any public setter on the Job class whose name matches a key in the merged `JobDataMap`. If the `JobDataMap` is populated from external input, an attacker can invoke arbitrary setters — including any with side effects. Since v6.1.1 you can restrict the factory to a fixed set of keys via `setAllowedProperties(Collection<String>)` or `addAllowedProperty(String)`; non-listed keys are then skipped (or warned/thrown about, depending on the existing flags). The default factory used by `StdSchedulerFactory` is `SimpleJobFactory`, which does not call any setters — `PropertySettingJobFactory` is opt-in.
* **`org.quartz.properties` is a trust-sensitive system property.** `StdSchedulerFactory` reads it as a filesystem path with no canonicalization, then loads it via `FileInputStream`. The property values inside that file (`org.quartz.threadPool.class`, `org.quartz.jobStore.class`, `org.quartz.plugin.*`, listener classes, etc.) become arguments to `Class.forName(...).newInstance()`. This is by design for plugin-driven scheduling, but it means an attacker who can set this system property at JVM startup can load arbitrary classes from the classpath. Treat it like a `-D` flag passed by an operator; never derive it from data your application receives at runtime.

# News and noteworthy

v6.2.0 - 2026-09-23
* Added the new classes `JobExecutionError`, `JobExecutionErrorRegistry` and `ErrorHistoryJobListener` in package `com.helger.schedule.quartz.listener`. The listener remembers the most recent failed job executions per job, so that the reason of a failure can be inspected without consulting the log file. `GlobalQuartzScheduler` registers it by default, next to the `StatisticsJobListener`.
  At most `JobExecutionErrorRegistry.getMaxErrorsPerJob ()` (default 5) errors are kept per job; `JobExecutionErrorRegistry.setMaxErrorsPerJob (0)` disables the collection entirely. The causing `Throwable` is deliberately not retained - class name, message and stack trace are extracted eagerly, so no reference graph is kept alive
* `StatisticsJobListener` now additionally collects the runtime of every finished job execution in a timer statistics handler, providing minimum, average and maximum runtime per job class
* Added the public constants `StatisticsJobListener.STATS_PREFIX`, `STATS_SUFFIX_EXEC`, `STATS_SUFFIX_ERROR`, `STATS_SUFFIX_VETOED` and `STATS_SUFFIX_TIME` as well as the new static method `StatisticsJobListener.getStatisticsName (Class)`, so that the collected statistics can be read back without duplicating the name building
* Added the new enum constant `ESchedulerState.NOT_STARTED` for a scheduler that exists but was never started. Quartz reports standby mode for that state as well, so previously it was indistinguishable from an explicit standby
* **Potentially breaking**: `QuartzSchedulerHelper.getSchedulerState ()` now returns `ESchedulerState.NOT_STARTED` instead of `ESchedulerState.STANDBY` for a scheduler that was never started. It also evaluates `isShutdown ()` first and no longer throws an `IllegalStateException` if no state matches
* Added `GlobalQuartzScheduler.removeJobListener (String)`, `getAllJobListeners ()` and `getJobListenerOfName (String)`, as previously job listeners could only be added
* Added the JSpecify annotations `@NonNull` and `@Nullable` as well as `@Nonempty`, `@Nonnegative`, `@ReturnsMutableCopy`, `@ReturnsMutableObject` and `@ReturnsImmutableObject` to the whole API of `ph-mini-quartz` and `ph-schedule`
* Replaced the manual `null`, empty and range checks with `ValueEnforcer` calls throughout both modules. Note that `ValueEnforcer.notNull` throws a `NullPointerException` where previously an `IllegalArgumentException` was thrown
* **Potentially breaking**: `CronScheduleBuilder.cronSchedule (String)` now throws an `IllegalArgumentException` instead of a `RuntimeException` for an invalid cron expression
* **Potentially breaking**: `DailyTimeIntervalScheduleBuilder.endingDailyAfterCount (int)` now throws an `IllegalStateException` instead of an `IllegalArgumentException` if `startingDailyAt` was not called before
* **Potentially breaking**: `GlobalQuartzScheduler.scheduleJob (...)` now throws an `IllegalStateException` instead of a `RuntimeException` if the job could not be scheduled
* `IListenerManager.addTriggerListener (ITriggerListener, IMatcher)` is now declared `@NonNull` for the matcher, matching what the implementation always enforced
* Fixed `SimpleTrigger.getClone ()` throwing a `NullPointerException` if no start time was set
* All `ToStringGenerator.append` field names now start with an uppercase character
* Added `JobExecutionException.setUnscheduleFiringTrigger (boolean)` and `setUnscheduleAllTriggers (boolean)`. The backing fields were `final false` with no way to set them, so `unscheduleFiringTrigger ()` and `unscheduleAllTriggers ()` always returned `false` and the resulting `SET_TRIGGER_COMPLETE` / `SET_ALL_JOB_TRIGGERS_COMPLETE` instructions in `AbstractTrigger.executionComplete` were unreachable
* Implemented `CronExpression.getTimeBefore (Date)` (ported from Quartz 2.5.2, binary search over `getTimeAfter`); it previously always returned `null`. As a result `CronTrigger.getFinalFireTime ()` now returns the correct value if an end time is set. `CronExpression.getFinalFireTime ()` remains unimplemented - upstream Quartz has not solved QUARTZ-423 either
* `SimpleClassLoadHelper.getClassLoader ()` no longer tries to reflectively call the JVM internal `ClassLoader.getCallerClassLoader`. That method was removed from the JDK years ago, so the lookup always failed and the class-loader of this class was used anyway
* Added `QuartzScheduler.removeInternalTriggerListener (String)` and `notifySchedulerListenersScheduled (ITrigger)`. The previous names `removeinternalTriggerListener` and `notifySchedulerListenersSchduled` contained typos and are deprecated for removal
* Renamed the private fields `QuartzServer.sched`, `QuartzScheduler.holdToPreventGC`, `RAMJobStore.ttc` and `SchedulerMetaData.m_sSchedClass` to follow the naming conventions

v6.1.1 - 2026-05-18
* Removed OSGI bundling
* `QuartzSchedulerThread` now catches `Throwable` (instead of only `RuntimeException`) in its main loop, so the scheduler thread no longer dies silently on `Error`s like `OutOfMemoryError` or `NoClassDefFoundError`
* `QuartzSchedulerThread` now installs an `UncaughtExceptionHandler` so any remaining thread death is logged
* `QuartzSchedulerThread.setIdleWaitTime` now guards against a zero/negative `nextInt` bound
* `SimpleThreadPool.WorkerThread` now catches `Throwable` while running a job, so a worker thrown out by an `Error` is no longer leaked out of the pool
* `QuartzSchedulerThread` no longer re-asserts the interrupt flag inside its three inner `wait()` catches; the previous pattern caused a 100% CPU busy spin if the scheduler thread was externally interrupted, because each subsequent `wait()` re-threw `InterruptedException` immediately
* `QuartzSchedulerThread`'s outer `Throwable` catch now preserves the interrupt flag if it ever sees an `InterruptedException` (defensive — all known `wait()` sites catch it locally)
* `PropertySettingJobFactory` now supports an opt-in allow-list of property names via `setAllowedProperties(Collection)` / `addAllowedProperty(String)`. When set, only listed keys in the merged `JobDataMap` are eligible for setter invocation; non-listed keys are skipped (or warned/thrown about, depending on the existing flags). Default behavior is unchanged.

v6.1.0 - 2025-11-16
* Updated to ph-commons 12.1.0
* Using JSpecify annotations

v6.0.1 - 2025-10-28
* Added misfireInstruction support to `JDK8TriggerBuilder`

v6.0.0 - 2025-08-25
* Requires Java 17 as the minimum version
* Updated to ph-commons 12.0.0

v5.0.1 - 2024-03-27
* Updated to ph-commons 11.1.5
* Added Java 21 compatibility

v5.0.0 - 2023-01-12
* Using Java 11 as the baseline
* Updated to ph-commons 11

v4.2.0 - 2021-03-21
* Updated to ph-commons 10
* Changed Maven group ID from `com.helger` to `com.helger.schedule`

v4.1.1 - 2020-09-17
* Updated dependencies

v4.1.0 - 2020-03-29
* Improved debug logging
* Improved code quality slightly
* Updated to ph-commons 9.4.0

v4.0.1 - 2018-11-12
* Fixed OSGI ServiceProvider configuration
* Removed `com.helger.quartz.xml` package

v4.0.0 - 2017-12-06
* Updated to ph-commons 9.0.0

v3.6.1 - 2017-03-29
* Started updating MiniQuartz API for Java 8

v3.6.0 - 2016-12-12
* Moved AbstractScopeAwareJob to ph-web (reverted dependencies)

v3.5.0 - 2016-07-22
* Using a forked version of Quartz with less dependencies - "Mini quartz"  

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
