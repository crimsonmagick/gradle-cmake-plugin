package dev.welbyseely;

import org.gradle.api.GradleException;
import org.gradle.api.GradleScriptException;
import org.gradle.api.logging.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

public class CMakeExecutor {

  private final Logger logger;
  private final String taskName;

  CMakeExecutor(final Logger logger, final String taskName) {
    this.logger = logger;
    this.taskName = taskName;
  }

  protected void exec(final List<String> cmdLine, final File workingFolder) throws GradleException {
    // log command line parameters
    StringBuilder sb = new StringBuilder("  CMakePlugin.task " + taskName + " - exec: ");
    for (String s : cmdLine) {
      sb.append(s).append(" ");
    }
    logger.info(sb.toString());

    // build process
    ProcessBuilder pb = new ProcessBuilder(cmdLine);
    pb.directory(workingFolder);

    final ExecutorService executor = Executors.newFixedThreadPool(2);

    try {
      // make sure working folder exists
      workingFolder.mkdirs();

      // start
      Process process = pb.start();

      Future<Void> stdoutFuture = executor.submit(() -> {
        readStream(process.getInputStream(), true);
        return null;
      });
      Future<Void> stderrFuture = executor.submit(() -> {
        readStream(process.getErrorStream(), false);
        return null;
      });

      int retCode = process.waitFor();
      warnIfTimeout(stdoutFuture,
        "CMakeExecutor[" + taskName + "]Warn: timed out waiting for stdout to be closed.");
      warnIfTimeout(stderrFuture,
        "CMakeExecutor[" + taskName + "]Warn: timed out waiting for stderr to be closed.");
      if (retCode != 0) {
        throw new GradleException("[" + taskName + "]Error: CMAKE returned " + retCode);
      }
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw new GradleScriptException("CMakeExecutor[" + taskName + "].", e);
    } finally {
      executor.shutdown();
    }
  }

  private void readStream(final InputStream inputStream, boolean isStdOut) {
    final Stream<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines();
    if (isStdOut) {
      lines.forEach(logger::info);
    } else {
      lines.forEach(logger::error);
    }
  }

  private void warnIfTimeout(final Future<Void> future, final String message)
    throws ExecutionException, InterruptedException {
    try {
      future.get(3, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      logger.warn(message);
    }
  }
}

