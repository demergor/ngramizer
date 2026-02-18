package ngramizer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import ngramizer.analyzer.Analyzer;
import ngramizer.ngrams.Ngrams;
import ngramizer.normalizer.Normalizer;

public final class App {
  public static void main(String[] args) {
    Ngrams ngrams;
    try (Scanner scanner = new Scanner(System.in)) {
      ngrams = switch (args.length) {
        case 1 -> {
          System.out.print("Please enter a file to be analyzed below:\n");
          String input = scanner.nextLine();
          yield new Ngrams(args[0], input);
        }
        case 2 -> new Ngrams(args[0], args[1]);
        case 3, 4 -> new Ngrams (args[0], args[1], args[2]);
        default -> throw new RuntimeException(
          """
          Invalid amount of arguments given! \
          Expected: 
          '<output path> 
          [<input path>] 
          [<exclusion regex>] 
          [<remove repeating letters?>]'
          """
        );
      };
    }

    boolean removeRepetitions = false;
    if (args.length >= 4) {
      removeRepetitions = switch (args[3].toLowerCase()) {
        case "true", "yes", "y", "1" -> true;
        default -> false;
      };
    }

    ExecutorService exeService = Executors.newCachedThreadPool();
    Path normalizedPath;

    try {
      normalizedPath = Normalizer.run(ngrams, exeService, removeRepetitions);
    } catch (IOException e) {
      System.err.printf("Error normalizing the input file: %s", e.getMessage());
      e.printStackTrace();
      return;
    }

    try {
      Analyzer.run(
        normalizedPath == null ? ngrams.getInputPath() : normalizedPath,
        ngrams,
        exeService,
        true
      );
    } catch (IOException e) {
      System.err.printf("Error analyzing the input file: %s", e.getMessage());
      e.printStackTrace();
      return;
    }
  }
}
