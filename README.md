#ph-schedule

Java scheduling library based on Quartz with scope support (see [ph-commons](https://github.com/phax/ph-commons))

Versions <= 1.8.3 are compatible with ph-commons < 6.0.
Versions >= 2.0.0 are compatible with ph-commons >= 6.0.

#News and noteworthy

  * v3.6.0
    * Moved AbstractScopeAwareJob to ph-web (reverted dependencies)
  * v3.5.0
    * Using a forked version of Quartz with less dependencies  

#Maven usage
Add the following to your pom.xml to use this artifact:
```
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>ph-schedule</artifactId>
  <version>3.5.0</version>
</dependency>
```

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodeingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a>
