# ph-schedule

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.helger.schedule/ph-schedule-parent-pom/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.helger.schedule/ph-schedule-parent-pom) 
[![javadoc](https://javadoc.io/badge2/com.helger.schedule/ph-schedule-parent-pom/javadoc.svg)](https://javadoc.io/doc/com.helger.schedule/ph-schedule-parent-pom)
[![CodeCov](https://codecov.io/gh/phax/ph-schedule/branch/master/graph/badge.svg)](https://codecov.io/gh/phax/ph-schedule)

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

# News and noteworthy

* v5.0.0 - 2023-01-12
    * Using Java 11 as the baseline
    * Updated to ph-commons 11
* v4.2.0 - 2021-03-21
    * Updated to ph-commons 10
    * Changed Maven group ID from `com.helger` to `com.helger.schedule`
* v4.1.1 - 2020-09-17
    * Updated dependencies
* v4.1.0 - 2020-03-29
    * Improved debug logging
    * Improved code quality slightly
    * Updated to ph-commons 9.4.0
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

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.