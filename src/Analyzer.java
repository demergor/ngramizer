import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

class Analyzer {
  public static void run(Path inputPath, Ngrams ngrams, ExecutorService exeService) 
    throws IOException
  {
    Reader reader = new Reader(inputPath);
    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
      try {
        reader.populate();
      } catch (IOException | InterruptedException e) {
        System.err.printf(
            "Error reading input file into analyzer: %s\n", 
            e.getMessage()
        );
        e.printStackTrace();
      }
    }, exeService);
    while (!future.isDone() || !reader.queueEmpty()) {
      String line = reader.poll();
      if (line != null) {
        exeService.submit(() -> {
          analyzeLine(line, ngrams);
        });
      }
    }
    exeService.shutdown();
    try {
      if (!exeService.awaitTermination(1, TimeUnit.HOURS)) {
        System.err.println("Some tasks did not finish in time!");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      e.printStackTrace();
    } 

    print(ngrams);
  }

  public static void analyzeLine(String line, Ngrams ngrams) {
    String[] words = line.split(" ");
    char lastLetter = '\0';
    StringBuilder sb = new StringBuilder();
    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }
      int len = word.length();
      for (int i = 0; i < len; ++i) {
        char cur = word.charAt(i);
        Ngrams.increment(ngrams.getLetterFreqs(), cur);
        sb.setLength(0);
        if (i + 1 == len) {
          lastLetter = cur;
          Ngrams.increment(ngrams.getLastFreqs(), cur);
          break;
        } else if (i == 0) {
          Ngrams.increment(ngrams.getFirstFreqs(), cur);
          if (lastLetter != '\0') {
            Ngrams.increment(
                ngrams.getSpaceFreqs(),
                sb.append(lastLetter).append(cur).toString()
            );
            sb.setLength(0);
          }
        }
        sb.append(cur);
        if (word.charAt(i + 1) == cur) {
          continue;
        }
        sb.append(word.charAt(i + 1));
        Ngrams.increment(ngrams.getBiFreqs(), sb.toString());
        if (i + 2 >= len || word.charAt(i + 2) == cur) {
          continue;
        }
        sb.append(word.charAt(i + 2));
        Ngrams.increment(ngrams.getTriFreqs(), sb.toString());
        sb.delete(1, 2);
        Ngrams.increment(ngrams.getSkipFreqs(), sb.toString());
      }
    }
  }

  public static void print(Ngrams ngrams) throws IOException {
    Path analyzedOutputPath = ngrams.getOutputPath().resolve("analyzed");
    Files.createDirectories(analyzedOutputPath);
    Path outputFilePath = analyzedOutputPath.resolve("frequencies.txt");
    try (BufferedWriter writer = new BufferedWriter(
          new FileWriter(outputFilePath.toFile())
    )) {
      Ngrams.print(writer, Ngrams.sort(ngrams.getLetterFrequencies()));
    }

    try (BufferedWriter writer = new BufferedWriter(
          new FileWriter(outputFilePath.toFile(), true)
    )) {
      writer.newLine();
      Ngrams.print(writer, Ngrams.sort(ngrams.getNgramFrequencies()));
    }

    outputFilePath = analyzedOutputPath.resolve("asymmetries.txt");
    try (BufferedWriter writer = new BufferedWriter(
          new FileWriter(outputFilePath.toFile())
    )) {
      // Ngrams.print(writer, Ngrams.sort(ngrams.getNgramAsymmetries()));
    }
  }
}
