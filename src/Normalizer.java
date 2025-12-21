import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public final class Normalizer {
  public static Path run(Ngrams ngrams, ExecutorService exeService) 
    throws IOException
  {
    if (ngrams.getRegex() == null) {
      return null;
    }

    Reader reader = new Reader(ngrams.getInputPath());
    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
      try {
        reader.populate();
      } catch (IOException | InterruptedException e) {
        System.err.printf(
            "Error reading input file into normalizer: %s",
            e.getMessage()
        );
        e.printStackTrace();
      }
    }, exeService);
    BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    Pattern pattern = Pattern.compile(ngrams.getRegex());
    while (!future.isDone() || !reader.queueEmpty()) {
      String line = reader.poll();
      if (line != null) {
        exeService.submit(() -> {
          try {
            queue.put(normalizeLine(line, pattern));
          } catch (InterruptedException e) {
            System.err.printf(
                "Error populating BlockingQueue<String> in normalizer:",
                e.getMessage()
            );
          }
        });
      }
    }

    Path normalizedOutputPath = IO.resolvePath(ngrams.getOutputPath(), "normalized");
    Files.createDirectories(normalizedOutputPath);
    normalizedOutputPath = normalizedOutputPath.resolve("normalized.txt");
    try (BufferedWriter writer = new BufferedWriter(
        new FileWriter(normalizedOutputPath.toFile())
    )) {
      print(queue, writer, ngrams);
    }
    return normalizedOutputPath;
  }

  public static String normalizeLine(String line, Pattern pattern) {
    return EXTRA_SPACES.matcher(
        pattern
        .matcher(line)
        .replaceAll("")
    ).replaceAll(" ").trim().toLowerCase();
  }

  public static void print(
      BlockingQueue<String> queue,
      BufferedWriter writer,
      Ngrams ngrams
  ) throws IOException {
    while (!queue.isEmpty()) {
      writer.write(queue.poll());
      writer.newLine();
    }
  }

  public static final Pattern EXTRA_SPACES = Pattern.compile("\\s{2,}");
}
