# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build / Test

Maven multi-module project. Java 17 minimum (since 6.0.0); CI also builds against 21 and 25.

```bash
# Build + test everything (from repo root)
mvn install

# Build a single module
mvn -pl ph-mini-quartz install
mvn -pl ph-schedule  install

# Run a single test class
mvn -pl ph-mini-quartz -Dtest=CronExpressionTest test

# Run a single test method
mvn -pl ph-mini-quartz -Dtest=CronExpressionTest#testCronExpressionPassingMidnight test
```

The `forbidden-apis` plugin is configured in the parent POM with the `ph-forbidden-apis` Java 9 signature list. `StdSchedulerFactory` is intentionally excluded because it reads `System.getProperties()`.

## Module Layout

This repo ships two artifacts under groupId `com.helger.schedule`:

### `ph-mini-quartz` — package `com.helger.quartz.*`

A stripped-down fork of Quartz 2.2.3 with minimal external dependencies (only `ph-io` + `ph-datetime` at runtime). It is the engine; consumers can use it directly without `ph-schedule`.

Key sub-packages — most of the original Quartz public API surface is preserved:
- root `com.helger.quartz` — public API: `IScheduler`, `IJob`, `ITrigger`, `JobBuilder`, `TriggerBuilder`, `CronExpression`, `SimpleScheduleBuilder`, `CronScheduleBuilder`, `DateBuilder`, `JobKey`, `TriggerKey`, etc.
- `core` — `QuartzScheduler` (real implementation behind `StdScheduler`), `QuartzSchedulerThread`, `JobRunShell`, `ListenerManager`.
- `impl` — `StdSchedulerFactory` (reads `quartz.properties`), `StdScheduler` (thin facade over `QuartzScheduler`), `JobDetail`, `JobExecutionContext`, plus `calendar/`, `triggers/`, `matchers/` subpackages.
- `simpl` — default plug-ins: `SimpleThreadPool`, `RAMJobStore`, `SimpleJobFactory`, classloader helpers, `*InstanceIdGenerator`.
- `spi` — extension interfaces: `IJobStore`, `IThreadPool`, `IJobFactory`, `IClassLoadHelper`, `ISchedulerPlugin`, `IOperableTrigger`.
- `listeners`, `plugins/history`, `plugins/management` — built-in listener and plugin implementations.
- `utils/counter` — sampled counter utilities used by `SampledStatistics`.

Defaults (from `src/main/resources/quartz/quartz.properties`): `SimpleThreadPool` with 10 threads, `RAMJobStore`, scheduler instance name `DefaultQuartzScheduler`, misfire threshold 60s. Override by placing a `quartz.properties` on the classpath or by setting system properties read by `StdSchedulerFactory`.

### `ph-schedule` — package `com.helger.schedule.*`

A thin Quartz-aware integration layer that depends on `ph-mini-quartz`, `ph-collection`, and `ph-scopes`. This is where most application code should plug in.

- `quartz.GlobalQuartzScheduler` — `AbstractGlobalSingleton` (ph-scope) wrapping a single auto-started `IScheduler`. Adds a `StatisticsJobListener` for every job. `onDestroy` shuts the scheduler down. Use `getInstance().scheduleJob(...)` / `scheduleJobNowOnce(...)` / `unscheduleJob(...)` / `pauseJob(...)` / `resumeJob(...)` for the common path.
- `quartz.QuartzSchedulerHelper` — static accessor (`getScheduler()`, `getSchedulerState()`, `getSchedulerMetaData()`) backed by a private `StdSchedulerFactory`. `GlobalQuartzScheduler` is built on top of this.
- `quartz.ESchedulerState` — `STARTED` / `STANDBY` / `SHUTDOWN`.
- `quartz.trigger.JDK8TriggerBuilder` — replacement for Quartz's `TriggerBuilder` using `java.time.LocalDateTime` instead of `java.util.Date`. Prefer this over `com.helger.quartz.TriggerBuilder` in new code.
- `quartz.listener.LoggingJobListener` / `StatisticsJobListener` — drop-in `IJobListener` implementations.
- `quartz.utils.JobKeyGroupMatcher` / `TriggerKeyGroupMatcher` — group-name matchers.
- `job.AbstractJob` — base class for application jobs. Subclasses implement `onExecute(JobDataMap, IJobExecutionContext)`; `beforeExecute` / `afterExecute` are overridable hooks. Wraps execution with `StopWatch`-based timing, success/failure counters via `StatisticsManager`, and dispatches exceptions through the global `AbstractJob.exceptionCallbacks()` (`IJobExceptionCallback`) before re-throwing as `JobExecutionException`.
- `jobstore.BaseJobStore` — alternative `IJobStore` implementation (RAM-like) with ph-commons collections and a `SimpleReadWriteLock`. Use when you need a job store you can subclass; `RAMJobStore` in `ph-mini-quartz` remains the default.
- `config.ThirdPartyModuleProvider_ph_schedule` — registered via `META-INF/services/com.helger.base.thirdparty.IThirdPartyModuleProviderSPI` for ph-commons module reporting.

## Architectural Conventions

- The two-module split is deliberate: `ph-mini-quartz` MUST NOT depend on `ph-scopes`. Scope-aware behavior lives in `ph-schedule`. Don't pull `ph-schedule`-only types into the `com.helger.quartz.*` packages.
- The "mini" in mini-quartz means: no XML job config, no JDBC job store, no clustering, no remoting (`QuartzServer` is a stub), and `com.helger.quartz.xml` was deleted in v4.0.1. Don't try to re-add removed subsystems without explicit user direction.
- Most jobs should extend `com.helger.schedule.job.AbstractJob` rather than implementing `IJob` directly — it provides statistics, exception dispatch, and lifecycle hooks consumers depend on.
- New triggers should be built through `JDK8TriggerBuilder` (LocalDateTime), not the legacy `TriggerBuilder` (java.util.Date).
- Coding style follows the user's global rules in `~/.claude/rules/` (Hungarian notation, JSpecify, ph-commons collections, `LOGGER` without prefix, inline string concatenation in log calls — no SLF4J `{}` placeholders, license header on every Java file). The `ph-mini-quartz` sources retain the original Terracotta copyright line in addition to the Philip Helger copyright.

## Release / Distribution

- Snapshots auto-deploy on every push to the `central-portal-snapshots` Sonatype repo via `.github/workflows/maven.yml` (Java 17 job, `-P release-snapshot deploy`).
- OSGi bundling was removed in 6.1.1; do not re-add `bundle` packaging or `maven-bundle-plugin` configuration.
