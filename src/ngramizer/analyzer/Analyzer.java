package ngramizer.analyzer;

import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import ngramizer.ngrams.Ngrams;
import ngramizer.io.Reader;

public final class Analyzer {
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
    List<Future<?>> tasks = new ArrayList<>();
    while (!future.isDone() || !reader.queueEmpty()) {
      String line = reader.poll();
      if (line != null) {
        tasks.add(exeService.submit(() -> {
          analyzeLineFreqs(line, ngrams);
        }));
      }
    }
    for (Future<?> task : tasks) {
      try {
        task.get();
      } catch (InterruptedException | ExecutionException e) {
        System.err.printf(
            "Error waiting for analyzation threads to finish: %s",
            e.getMessage()
        );
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

    ngrams.computeAsymmetries();
    ngrams.print();
  }

  public static void analyzeLineFreqs(String line, Ngrams ngrams) {
    String[] words = line.split(" ");
    char lastLetter = '\0';
    StringBuilder sb = new StringBuilder();
    for (String word : words) {
      if (word.isEmpty()) {
        continue;
      }
      int len = word.length();
      for (int i = 0; i < len; ++i) {
        sb.setLength(0);
        char cur = word.charAt(i);
        Ngrams.increment(ngrams.getLetterFreqs(), cur);
        if (i + 1 == len) {
          lastLetter = cur;
          Ngrams.increment(ngrams.getLastFreqs(), cur);
          break;
        } else if (i == 0) {
          Ngrams.increment(ngrams.getFirstFreqs(), cur);
          if (lastLetter != '\0' && lastLetter != cur) {
            Ngrams.increment(
                ngrams.getSpaceFreqs(),
                sb.append(lastLetter).append(cur).toString()
            );
            sb.setLength(0);
          }
        }
        sb.append(cur);
        sb.append(word.charAt(i + 1));
        Ngrams.increment(ngrams.getBiFreqs(), sb.toString());
        if (i + 2 >= len) {
          continue;
        }
        sb.append(word.charAt(i + 2));
        assert(sb.length() == 3);
        Ngrams.increment(ngrams.getTriFreqs(), sb.toString());
        sb.delete(1, 2);
        Ngrams.increment(ngrams.getSkipFreqs(), sb.toString());
        sb.setLength(0);
      }
    }
  }
}
