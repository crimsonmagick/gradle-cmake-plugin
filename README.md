# gradle-cmake-plugin
This plugin allows to configure and build using CMake. 

## Prerequisites

* `CMake` installed on the system. Available [here](https://www.cmake.org "CMake Homepage").

## To apply the plugin:

**plugins DSL**

```groovy
plugins {
  id 'dev.welbyseely.gradle-cmake-plugin' version '0.1.0'
}
```

**Legacy plugin application**

```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath 'dev.welbyseely:gradle-cmake-plugin:0.1.0'
  }
}

apply plugin: "dev.welbyseely.gradle-cmake-plugin"
```

and configure by:

```groovy
cmake {
  // optional configration to path of cmake. Not required if cmake is on the path.
  executable='/my/path/to/cmake'
  // optional working folder. default is ./build/cmake
  workingFolder=file("$buildDir/cmake")

  ////////////////////
  // cmakeConfigure parameters
  ////////////////////
  // optional source folder. This is where the main CMakeLists.txt file resides. Default is ./src/main/cpp
  sourceFolder=file("$projectDir/src/main/cpp")
  // optional install prefix. By default, install prefix is empty.
  installPrefix="${System.properties['user.home']}"
  // select a generator (optional, otherwise cmake's default generator is used)
  generator='Visual Studio 15 2017'
  // set a platform for generators that support it (usually Visual Studio)
  platform='x64'
  // set a toolset generators that support it (usually only Visual Studio)
  toolset='v141'
  // optionally set to build static libs
  buildStaticLibs=true
  // optionally set to build shared libs
  buildSharedLibs=true
  // define arbitrary CMake parameters. The below adds -Dtest=hello to cmake command line.
  defs.test='hello'

  ////////////////////
  // cmakeBuild parameters
  ////////////////////
  // optional configuration to build
  buildConfig='Release'
  // optional build target
  buildTarget='install'
  // optional build clean. if set to true, calls cmake --build with --clean-first
  buildClean=false
}
```

## Auto-created tasks

* *cmakeConfigure*: Calls CMake to generate your build scripts in the folder selected by workingFolder.

* *cmakeBuild*: Calls CMake --build in the folder selected by workingFolder to actually build.

* *cmakeClean*: Cleans the workingFolder.

* *cmakeGenerators*: Trys to list the generators available on the current platform by parsing `cmake --help`'s output.

## Examples

clean, configure and build:

```bash
./gradlew cmakeClean cmakeConfigure cmakeBuild
```

if you have assemble and clean tasks in your gradle project already you can also use:
	
```bash
assemble.dependsOn cmakeBuild
cmakeBuild.dependsOn cmakeConfigure
clean.dependsOn cmakeClean
```

and just call

```bash
./gradlew clean assemble
```

If you want to get the output of cmake, add -i to your gradle call, for example:
	
```bash
./gradlew cmakeConfigure -i
```

## Custom tasks

You can create custom tasks the following way:

```groovy
task configureFoo(type: dev.welbyseely.CMakeConfigureTask) {
  sourceFolder=file("$projectDir/src/main/cpp/foo")
  workingFolder=file("$buildDir/cmake/foo")
  // ... other parameters you need, see above, except the ones listed under cmakeBuild Parameters
}

task buildFoo(type: dev.welbyseely.CMakeBuildTask) {
  workingFolder=file("$buildDir/cmake/foo")
  // ... other parameters you need, see above, except the ones listed under cmakeConfigure parameters
}

buildFoo.dependsOn configureFoo // optional --- make sure its configured when you run the build task
```
### Multiple targets (cross-compilation)
If you need to configure for multiple targets you can use the `targets` property:

```groovy
cmake {
  sourceFolder = "$projectDir/src"
  buildSharedLibs = true
  buildClean = true
  buildConfig = 'Release'
  targets {
    windows {
      final os = OperatingSystem.WINDOWS
      workingFolder = new File(project.getBuildDir(), "cmake" + File.separator + os.nativePrefix)
      platform='x64'
    }
    linux {
      final os = OperatingSystem.LINUX
      workingFolder = new File(project.getBuildDir(), "cmake" + File.separator + os.nativePrefix)
      platform = 'x64'
    }
    mac {
      final os = OperatingSystem.MAC_OS
      workingFolder = new File(project.getBuildDir(), "cmake" + File.separator + os.nativePrefix)
      platform = 'arm64'
    }
  }
}

```

### Custom tasks using main configuration

As an alternative to using `targets` you can "import" the settings you've made in the main configuration "cmake" using the 'configureFromProject()' call:

```groovy
cmake {
  executable='/my/path/to/cmake'
  workingFolder=file("$buildDir/cmake")
  sourceFolder=file("$projectDir/src/main/cpp")
  installPrefix="${System.properties['user.home']}"

  generator='Visual Studio 15 2017'
  platform='x64'
}

task cmakeConfigureX86(type: dev.welbyseely.CMakeConfigureTask) {
  configureFromProject() // uses everything in the cmake { ... } section.

  // overwrite target platform
  platform='x86'
  // set a different working folder to not collide with default task
  workingFolder=file("$buildDir/cmake_x86")
}

task cmakeBuildX86(type: dev.welbyseely.CMakeBuildTask) {
  configureFromProject() // uses everything in the cmake { ... } section.
  workingFolder=file("$buildDir/cmake_x86")
}

cmakeBuildX86.dependsOn cmakeConfigureX86
```

## Versioning

The project uses [Semantic Versioning 1.0.0](https://semver.org/spec/v1.0.0.html): MAJOR_VERSION.MINOR_VERSION.MAINTENANCE_VERSION

* Major Version
  * Indicates backwards compatibility
  * When backwards compatibility is broken, this number will be incremented
* Minor Version
  * Indicates a new feature
  * When a new backwards-compatible feature is added, this number will be incremented
* Maintenance Version
  * Indicates a path
  * This number will be incremented for bug fixes and other miscellaneous updates that do not introduce new functionality to the plugin

## License

All these plugins are licensed under the Apache License, Version 2.0 with no warranty (expressed or implied) for any purpose.
