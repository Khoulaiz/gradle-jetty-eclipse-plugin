This is work in progress. If it works, I will create a proper release and tell you how to use it. Stay tuned...

# Gradle Jetty Plugin for Jetty based on Eclipse packages

## Intention

I needed a jetty plugin for gradle for the newer versions of jetty. Because I couldn't find any ready-to-use plugins,
 I started to create one myself. I never hat the intention to create a script-compatible version of the plugin. Instead
 my goal was to create a feature-compatible jetty plugin for the eclipse generation.

I quickly noticed that the original gradle jetty plugin is more or less a plain port of the maven jetty plugin.
 The original jettyRun task is IMHO a hard to maintain jetty hack, because jetty doen't really support a scattered
 webapp. I also think the configuration of the original jettyRun task is not very gradle-like (lots of unnecessary
 double configuration).

So I decided to change a few thinks. I hope, you find this plugin still useful. I've read that the gradle developers
 are working on a new test framework integration, that supports a number of web container. I use the jetty plugin for
 rapid development and because it's easy and fast to use. It has a reasonable default configuration and should work
 for most webapps without any additional configuration.

## Differences to the original jetty plugin

### Task names

First of all, I changed the name of the tasks and the plugin from jetty to jettyEclipse. This was necessary, because
 the classes of the original plugin live within the gradle distribution. Even if you don't do `apply 'jetty'` in your
 buildscript, the classes are automatically imported and if task classes only differ in their packages, there will be
 conflicts and strange errors.

### Merged tasks

I've merged the tasks `jettyRun` and `jettyRunWar`. Now there is only one task called `jettyEclipseRun`. The task is
 always based on a war file. You can define the war file directly or you can make the `jettyEclipseRun` task dependent
 to a war task (that produces a war file). See the build.gradle file in the example directory.

While the `jettyEclipseRun` task is running, you can trigger some actions via keyboard:
1. Pressing `ENTER` reloads the webapp
2. Pressing `r + ENTER` rebuilds and reloads the webapp
3. Pressing `R + ENTER` rebuilds the webapp (without reloading)

### Automatic rebuilds

You can schedule automatic rebuilds every x seconds. Gradle does a fantastic job in recognizing changed input for the
 build. If no input changed, Gradle detects this fact and skips all build tasks and the output keeps unchanged. Only if
 input files have changed, the rebuild will produce a new output file. This is very efficient and should be comparable
 to the old input file scanning of the old `jettyRun` task.

### Automatic rebuilds with automatic reloading

If you combine both features, you should be able to change any kind of source file that takes part in the war output.
 The change will trigger a recreation of the war file during the automatic build. A changed war file will either
 automatically reload the webapp or print a message to the console so you can manually reload the webapp.

## Usage

### jettyEclipse

Your need to add the following lines to your build script:

    import com.sahlbach.gradle.plugins.jettyEclipse.*

    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath (group: 'com.sahlbach.gradle', name: 'gradle-jetty-eclipse-plugin', version: '1.9.0')
        }
    }
    apply plugin: 'jettyEclipse'

After that you can configure global settings of the plugin using the `jettyEclipse` extension. The following parameters
 can be set:

| Name                     | Type          | Default       | Purpose
| ------------------------ |:-------------:| ------------- | --------------------------------------------------------------------
| httpPort                 | int           | 8080          | Jetty Server will listen to this port
| stopPort                 | int           | 8090          | Port to listen for stop request via the jettyEclipseStop task
| stopKey                  | String        | stop          | Key to provide when stopping jetty via the jettyEclipseStop task
| warFile                  | File          | -             | War File to use for the web app (if not set, the jettyEclipseRun
|                          |               |               | task will search for a dependent War task and use its output war)
| scanIntervalInSeconds    | int           | 5             | scans the war file for changes every x seconds
| contextPath              | String        | /             | the context path of the webapp
| webDefaultXml            | File          | jetty default | jetty default web.xml (applied before web.xml of war)
| overrideWebXml           | File          | -             | will be applied after the webapp web.xml
| jettyConfig              | File          | jetty default | Location of a jetty XML configuration file whose contents will
|                          |               |               | be applied before any plugin configuration
| automaticReload          | boolean       | false         | true: the webapp is reloaded after changes were detected
|                          |               |               | false: changes are reported to console, reload must be triggered
|                          |               |               |        manually via the console
| rebuildIntervalInSeconds | int           | -             | > 0: starts background builds every x seconds
| rebuildTask              | Task          | dependent war | the task that is used for the background rebuild
|                          |               | type task     |
| requestLog               | File          | -             | NCSA request log of the jetty server
| passwordFile             | File          | -             | text file containing user database. Will setup Basic
|                          |               |               | Authenticator with this database. The format of the file is
|                          |               |               | `user: password[,role]`.
| additionalRuntimeJars    | File[]        | -             | list of additional jars that will be added to the classpath
| daemon                   | boolean       | false         | will start the jetty server detached

### jettyEclipseRun

Tasks of type `JettyEclipseRun` can have all the above parameters. The task will use the values of `jettyEclipse`, but
 can be overridden with local values.

### jettyEclipseStop

Tasks of type `JettyEclipseStop` can have the `stopPort` and `stopKey` parameter. To stop a `jettyEclipseRun` task using
 a `jettyEclipseStop` task, the parameters have to correlate, of course.

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
