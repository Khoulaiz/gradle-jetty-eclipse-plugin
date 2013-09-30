This is work in progress. If it works, I will create a proper release and tell you how to use it. Stay tuned...

Gradle Jetty Plugin for Jetty based on Eclipse packages
=======================================================

Intention
---------
I needed a jetty plugin for gradle for the newer versions of jetty. Because I couldn't find any ready-to-use plugins,
I started to create one myself. I quickly noticed that the original plugin is more or less just a port of the maven
jetty plugin. Maintaining the jettyRun task is IMHO a huge jetty hack and hard to maintain. I also think the configuration
of the original jettyRun task is not very gradle-like (lots of unnecessary double configuration).

So the goal of this project is not being compatible to the old way to do it, but to provide a jetty plugin
for the eclipse generation jetty with _similar features_ as the old plugin.

Differences to the original jetty plugin
----------------------------------------
First of all, I changed the name of the tasks and the plugin from jetty to jettyEclipse. This was necessary, because
the classes of the original plugin live within the gradle distribution. Even if you don't do _apply 'jetty'_ in your
buildscript, the classes are automatically imported and if task classes only differ in their packages, there will be
conflicts and strange errors.

to be continued

Usage
-----
to be continued

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
