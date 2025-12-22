// TODO: See if I can implement an input directory rather than only files
package ngramizer.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public final class Reader {
  public Reader(Path inputPath) {
    this.inputPath = inputPath;
  }

  public boolean populate()
      throws IOException, InterruptedException 
  {
    try (BufferedReader reader = Files.newBufferedReader(inputPath)) {
      String line;
      while ((line = reader.readLine()) != null) {
        queue.put(line);
      }
    }
    return true;
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
