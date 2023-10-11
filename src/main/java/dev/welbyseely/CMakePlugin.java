/**
 * Copyright 2019 Marco Freudenberger
 * Copyright 2023 Welby Seely
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        CMakePluginExtension extension = project.getExtensions().create("cmake", CMakePluginExtension.class, project);

        project.getTasks().register("cmakeConfigure", CMakeConfigureTask.class, task -> {
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
            task.getDef().set(extension.getDef());
        });

        project.getTasks().register("cmakeBuild", CMakeBuildTask.class, task -> {
            task.getExecutable().set(extension.getExecutable());
            task.getWorkingFolder().set(extension.getWorkingFolder());
            task.getBuildConfig().set(extension.getBuildConfig());
            task.getBuildTarget().set(extension.getBuildTarget());
            task.getBuildClean().set(extension.getBuildClean());
        });

        Task cmakeClean = project.task( "cmakeClean" ).doFirst(task -> {
            File workingFolder = extension.getWorkingFolder().getAsFile().get().getAbsoluteFile();
            if ( workingFolder.exists() ) {
                project.getLogger().info("Deleting folder " + workingFolder);
                if (!deleteDirectory(workingFolder))
                    throw new GradleException("Could not delete working folder " + workingFolder);
            }
        });
        cmakeClean.setGroup("cmake");
        cmakeClean.setDescription("Clean CMake configuration");

        Task cmakeGenerators = project.task( "cmakeGenerators" ).doFirst(task -> {
            ProcessBuilder pb = new ProcessBuilder(extension.getExecutable().getOrElse("cmake"), "--help");
            try {
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                boolean foundGenerators = false;
                while ((line = reader.readLine()) != null) {
                    if ( line.equals("Generators") )
                        foundGenerators = true;
                    if ( foundGenerators )
                        project.getLogger().log(LogLevel.QUIET, line);
                }
                process.waitFor();
            }
            catch (IOException | InterruptedException e ) {
                throw new GradleScriptException( "cmake --help failed.", e );
            }
        });
        cmakeGenerators.setGroup("cmake");
        cmakeGenerators.setDescription("List available CMake generators");

        TaskContainer tasks = project.getTasks();
        tasks.getByName("cmakeBuild").dependsOn(tasks.getByName("cmakeConfigure"));

        project.afterEvaluate(p -> {
            p.getTasks().named("clean").configure(task -> task.dependsOn("cmakeClean"));

            p.getTasks().named("build").configure(task -> task.dependsOn("cmakeBuild"));
        });
    }

}