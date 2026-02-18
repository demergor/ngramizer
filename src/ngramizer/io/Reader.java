package ngramizer.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class Reader {
  public Reader(Path inputPath) {
    this.inputPath = inputPath;
  }

  public Reader(Path inputPath, BlockingQueue<String> queue) {
    this.inputPath = inputPath;
    this.queue = queue;
  }

  public void populate(ExecutorService exeService)
      throws IOException, InterruptedException 
  {
    if (Files.isDirectory(inputPath)) {
      List<CompletableFuture<Void>> tasks = new ArrayList<>();
      try (Stream<Path> paths = Files.walk(inputPath)) {
        paths
          .filter(Files::isRegularFile)
          .forEach(path -> {
            Reader reader = new Reader(path, queue);
            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
              try {
                reader.populate(exeService);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
              }
            }, exeService);
            tasks.add(task);
        });
      }
      for (CompletableFuture<Void> task : tasks) {
        task.join();
      }
      return;
    }

    try (BufferedReader reader = Files.newBufferedReader(inputPath)) {
      String line;
      while ((line = reader.readLine()) != null) {
        queue.put(line);
      }
    }
  }

  public String poll() {
    return queue.poll();
  }

  public boolean queueEmpty() {
    return queue.isEmpty();
  }

  private BlockingQueue<String> queue = new ArrayBlockingQueue<>(1000);
  private Path inputPath;
}
