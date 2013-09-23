gradle-jetty-eclipse-plugin
===========================

Jetty Plugin for Jetty based on Eclipse packages

This is work in progress. If it works, I will create a proper release and tell you how to use it. Stay tuned...

Versioning
----------
The first version of this plugin is 1.9.0. The version number has the following meaning:

### Major Number
This is the gradle major version. The plugin for gradle 1.x has a 1 here.

### Minor Number
This is the eclipse server version. So Jetty 9 has a 9 here.

### Build Number
This is the incremental release number of this plugin. We start with 0 and increment with every release.

So Version 1.9.0 is the first release for gradle 1.8+ containing a Jetty 9

Credits
-------
* to the implementors of the maven-jetty-plugin
* to the implementors of the build-in jetty plugin of gradle
* to [chriswk](https://github.com/chriswk) for his [gradle-jetty9-plugin](https://github.com/chriswk/gradle-jetty9-plugin)
  (I did not fork from him to get all the compile problems when upgrading the original jetty gradle plugin,
   but I took a bunch of solutions from him)

License
-------
This plugin is available under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
