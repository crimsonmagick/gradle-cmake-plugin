package dev.welbyseely;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.gradle.api.GradleException;
import org.gradle.api.GradleScriptException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskContainer;

public class CMakePlugin implements Plugin<Project> {

  final private static String CMAKE_CONFIGURE = "cmakeConfigure";
  final private static String CMAKE_BUILD = "cmakeBuild";

  private boolean deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
      }
    }
    return directoryToBeDeleted.delete();
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply("base");
    final CMakePluginExtension extension = project.getExtensions()
      .create("cmake", CMakePluginExtension.class, project);

    final Task cmakeClean = project.task("cmakeClean").doFirst(task -> {
      File workingFolder = extension.getWorkingFolder().getAsFile().get().getAbsoluteFile();
      if (workingFolder.exists()) {
        project.getLogger().info("Deleting folder " + workingFolder);
        if (!deleteDirectory(workingFolder)) {
          throw new GradleException("Could not delete working folder " + workingFolder);
        }
      }
    });
    cmakeClean.setGroup("cmake");
    cmakeClean.setDescription("Clean CMake configuration");

    final Task cmakeGenerators = project.task("cmakeGenerators").doFirst(task -> {
      ProcessBuilder pb = new ProcessBuilder(extension.getExecutable().getOrElse("cmake"),
        "--help");
      try {
        // start
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        boolean foundGenerators = false;
        while ((line = reader.readLine()) != null) {
          if (line.equals("Generators")) {
            foundGenerators = true;
          }
          if (foundGenerators) {
            project.getLogger().log(LogLevel.QUIET, line);
          }
        }
        process.waitFor();
      } catch (IOException | InterruptedException e) {
        throw new GradleScriptException("cmake --help failed.", e);
      }
    });
    cmakeGenerators.setGroup("cmake");
    cmakeGenerators.setDescription("List available CMake generators");

    project.afterEvaluate(p -> {
      final TaskContainer tasks = project.getTasks();
      
      if (extension.getTargets().getTargetContainer().isEmpty()) {
        p.getTasks().register(CMAKE_CONFIGURE, CMakeConfigureTask.class, task -> {
          task.getExecutable().set(extension.getExecutable());
          task.getWorkingFolder().set(extension.getWorkingFolder());
          task.getSourceFolder().set(extension.getSourceFolder());
          task.getConfigurationTypes().set(extension.getConfigurationTypes());
          task.getInstallPrefix().set(extension.getInstallPrefix());
          task.getGenerator().set(extension.getGenerator());
          task.getPlatform().set(extension.getPlatform());
          task.getToolset().set(extension.getToolset());
          task.getBuildSharedLibs().set(extension.getBuildSharedLibs());
          task.getBuildStaticLibs().set(extension.getBuildStaticLibs());
          task.getDef().set(extension.getDefs().isPresent() ? extension.getDefs() : extension.getDef());
        });

        p.getTasks().register(CMAKE_BUILD, CMakeBuildTask.class, task -> {
          task.getExecutable().set(extension.getExecutable());
          task.getWorkingFolder().set(extension.getWorkingFolder());
          task.getBuildConfig().set(extension.getBuildConfig());
          task.getBuildTarget().set(extension.getBuildTarget());
          task.getBuildClean().set(extension.getBuildClean());
        });
      } else {
        extension.getTargets().getTargetContainer().getAsMap()
          .forEach((name, target) -> {
            tasks.register(CMAKE_CONFIGURE + name, CMakeConfigureTask.class, task -> {
              task.configureFromProject();
              if (target.getExecutable().isPresent()) task.getExecutable().set(target.getExecutable());
              if (target.getWorkingFolder().isPresent()) task.getWorkingFolder().set(target.getWorkingFolder());
              if (target.getSourceFolder().isPresent()) task.getSourceFolder().set(target.getSourceFolder());
              if (target.getConfigurationTypes().isPresent()) task.getConfigurationTypes().set(target.getConfigurationTypes());
              if (target.getInstallPrefix().isPresent()) task.getInstallPrefix().set(target.getInstallPrefix());
              if (target.getGenerator().isPresent()) task.getGenerator().set(target.getGenerator());
              if (target.getPlatform().isPresent()) task.getPlatform().set(target.getPlatform());
              if (target.getToolset().isPresent()) task.getToolset().set(target.getToolset());
              if (target.getBuildSharedLibs().isPresent()) task.getBuildSharedLibs().set(target.getBuildSharedLibs());
              if (target.getBuildStaticLibs().isPresent()) task.getBuildStaticLibs().set(target.getBuildStaticLibs());
              if (target.getDefs().isPresent()) task.getDef().set(target.getDefs());
            });
            tasks.register(CMAKE_BUILD + name, CMakeBuildTask.class, task -> {
              task.configureFromProject();
              if (target.getExecutable().isPresent()) task.getExecutable().set(target.getExecutable());
              if (target.getWorkingFolder().isPresent()) task.getWorkingFolder().set(target.getWorkingFolder());
              if (target.getBuildConfig().isPresent()) task.getBuildConfig().set(target.getBuildConfig());
              if (target.getBuildTarget().isPresent()) task.getBuildTarget().set(target.getBuildTarget());
              if (target.getBuildClean().isPresent()) task.getBuildClean().set(target.getBuildClean());
            });
          });
      }

      tasks.withType(CMakeBuildTask.class)
        .forEach(task -> task.dependsOn(tasks.withType(CMakeConfigureTask.class)));

      p.getTasks().named("clean").configure(task -> task.dependsOn("cmakeClean"));

      p.getTasks().named("build").configure(task -> task.dependsOn(tasks.withType(
        CMakeBuildTask.class)));
    });
  }

}