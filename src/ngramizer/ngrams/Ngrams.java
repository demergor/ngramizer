package ngramizer.ngrams;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.Writer;

import ngramizer.io.IO;

public final class Ngrams {
  public Ngrams(String outputPathString, String inputPathString) {
    inputPath = IO.expand(Path.of(inputPathString));
    outputPath = IO.expand(Path.of(outputPathString));
    if (Files.isRegularFile(outputPath)) {
      throw new RuntimeException("Specified output path must be a directory!");
    }
    regex = "";
  }

  public Ngrams(String outputPathString, String inputPathString, String regex) {
    this(outputPathString, inputPathString);
    if (regex == null) {
      this.regex = "";
      return;
    }

    this.regex = switch (regex.toLowerCase()) {
      case "standard" -> 
        "[^a-zA-Z0-9äöüÄÖÜß.,!?;:\"'()\\-\\\\€@#$%&/*|~^°<>+\\[\\]{}_´` ]+";
      case "alphabetic" -> "[^a-zA-ZäöüÄÖÜß ]+";
      case "numeric" -> "[^0-9 ]+";
      case "alphanumeric" -> "[^a-zA-Z0-9äöüÄÖÜß ]+";
      case "unicode" -> "[^\\p{L}\\p{N} ]+";
      case "nonletters" -> "[a-zA-ZäöüÄÖÜß]+";
      default -> regex; 
    };
  }

  public void computeAsymmetries() {
    StringBuilder sb = new StringBuilder();
    int size = getNgramFrequencies().size();
    assert(size == getNgramAsymmetries().size());
    for (int i = 0; i < size; ++i) {
      Map<String, Integer> curFreqs;
      Map<String, Integer> curAsyms;
      switch(i) {
        case 0: 
          curFreqs = biFreqs; 
          curAsyms = biAsyms;
          break;
        case 1: 
          curFreqs = triFreqs; 
          curAsyms = triAsyms;
          break;
        case 2: 
          curFreqs = skipFreqs; 
          curAsyms = skipAsyms;
          break;
        case 3: 
          curFreqs = spaceFreqs; 
          curAsyms = spaceAsyms;
          break;
        default: throw new RuntimeException("Invalid amount of maps to populate!");
      }

      for (Map.Entry<String, Integer> entry : curFreqs.entrySet()) {
        sb.setLength(0);
        String key = entry.getKey();
        String revKey = sb.append(key).reverse().toString();

        if (curAsyms.containsKey(key) || curAsyms.containsKey(revKey)) {
          continue;
        }

        int value = entry.getValue();
        Integer revKeyValue = curFreqs.get(revKey);

        if (revKeyValue == null) {
          revKeyValue = 0;
        }

        assert(revKeyValue != null);
        curAsyms.put(revKey, revKeyValue - value);
        curAsyms.put(key, value - revKeyValue);
      }
    }
  }

  public static <T> LinkedHashMap<String, Map<T, Integer>> sort(
      Map<String, Map<T, Integer>> maps
  ) {
    LinkedHashMap<String, Map<T, Integer>> sorted = new LinkedHashMap<>();
    for (Map.Entry<String, Map<T, Integer>> map : maps.entrySet()) {
      if (map.getValue() == null || map.getValue().isEmpty()) {
        throw new RuntimeException(
            String.format("Error sorting maps: %s Map is empty!", map.getKey())
        );
      }
      List<Map.Entry<T, Integer>> entryList =
        new ArrayList<>(map.getValue().entrySet());
      entryList.sort((a, b) -> b.getValue().compareTo(a.getValue()));
      LinkedHashMap<T, Integer> temp = new LinkedHashMap<>();
      for (Map.Entry<T, Integer> entry : entryList) {
        temp.put(entry.getKey(), entry.getValue());
      }
      sorted.put(map.getKey(), temp);
    }
    return sorted;
  }

  public void print (boolean shouldPrintCombinatorial) throws IOException {
    Path analyzedOutputPath = outputPath.resolve("analyzed");
    Files.createDirectories(analyzedOutputPath);
  
    Path frequencyPath = analyzedOutputPath.resolve("frequencies");
    Files.createDirectories(frequencyPath);

    for (
      Map.Entry<String, Map<Character, Integer>> entry :
      sort(getLetterFrequencies()).entrySet()
    ) {
      String fileName = entry
        .getKey()
        .replaceAll("frequencies", "")
        .replaceAll(" letter", "")
        .trim() + ".txt";
      Path outputFilePath = frequencyPath.resolve(fileName);

      try (BufferedWriter writer = Files.newBufferedWriter(
          outputFilePath,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING
      )) {
        printMap(writer, entry.getValue());
      }
    }

    for (
      Map.Entry<String, Map<String, Integer>> entry : 
      sort(getNgramFrequencies()).entrySet()
    ) {
      String fileName = entry.getKey().replaceAll("frequencies", "").trim() + ".txt";
      Path outputFilePath = frequencyPath.resolve(fileName);

      try (BufferedWriter writer = Files.newBufferedWriter(
        outputFilePath,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )) {
        printMap(writer, entry.getValue());
      }
    }

    Path asymmetryPath = analyzedOutputPath.resolve("asymmetries");
    Files.createDirectories(asymmetryPath);

    for (
      Map.Entry<String, Map<String, Integer>> entry : 
      sort(getNgramAsymmetries()).entrySet()
    ) { 
      String fileName = entry.getKey().replaceAll("asymmetries", "").trim() + ".txt";
      Path outputFilePath = asymmetryPath.resolve(fileName);

      try (BufferedWriter writer = Files.newBufferedWriter(
        outputFilePath,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )) {
        printMap(writer, entry.getValue());
      }
    }

    if (!shouldPrintCombinatorial) {
      return;
    }

    Path combinatorialPath = frequencyPath.resolve("combinatorial");
    Files.createDirectories(combinatorialPath);

    for (
      Map.Entry<String, Map<String, Integer>> entry : 
      sort(getCombMaps(getNgramFrequencies())).entrySet()
    ) {
      String fileName = entry
        .getKey()
        .replaceAll("combinatorial", "")
        .replaceAll("frequencies", "")
        .trim() + 
        ".txt";
      Path outputFilePath = combinatorialPath.resolve(fileName);

      try (BufferedWriter writer = Files.newBufferedWriter(
        outputFilePath, 
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )) {
        printMap(writer, entry.getValue());
      }
    }
  }

  private static <T> void printMap(Writer writer, Map<T, Integer> map) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<T, Integer> entry : map.entrySet()) {
      sb.append(entry.getKey());
      sb.append(" : ");
      sb.append(entry.getValue());
      sb.append(NEWLINE);

      writer.write(sb.toString());
      sb.setLength(0);
    }
  }

  private static Map<String, Map<String, Integer>> getCombMaps(
    Map<String, Map<String, Integer>> maps
  ) throws IOException {
    Map<String, Map<String, Integer>> combMaps = new LinkedHashMap<>();
    for (Map.Entry<String, Map<String, Integer>> map : maps.entrySet()) {
      Map <String, Integer> singleCombMap = new LinkedHashMap<>();
      for (Map.Entry<String, Integer> entry : map.getValue().entrySet()) {
        String ngram = entry.getKey();
        if (combMaps.get(ngram) != null) {
          continue;
        }

        int combValue = 0;
        List<String> anagrams = new ArrayList<>();

        for (Map.Entry<String, Integer> otherEntry : map.getValue().entrySet()) {
          String otherNgram = otherEntry.getKey();
          if (!anagrams(ngram, otherNgram)) {
            continue;
          }

          boolean alreadyIncluded = false;
          for (String s : anagrams) {
            if (s.equals(otherNgram)) {
              alreadyIncluded = true;
              break;
            }
          }

          if (!alreadyIncluded) {
            anagrams.add(otherNgram);
            combValue += otherEntry.getValue();
          }
        }

        for (String s : anagrams) {
          singleCombMap.put(s, combValue);
        }
      }

      String combName = "combinatorial " + map.getKey();
      combMaps.put(combName, singleCombMap);
    }

    return combMaps;
  }

  public Map<String, Map<Character, Integer>> getLetterFrequencies() {
    Map<String, Map<Character, Integer>> maps = new LinkedHashMap<>();
    maps.put("letter frequencies", letterFreqs);
    maps.put("first letter frequencies", firstFreqs);
    maps.put("last letter frequencies", lastFreqs);
    return maps;
  }

  public Map<String, Map<String, Integer>> getNgramFrequencies() {
    Map<String, Map<String, Integer>> maps = new LinkedHashMap<>();
    maps.put("bigram frequencies", biFreqs);
    maps.put("trigram frequencies", triFreqs);
    maps.put("skipgram frequencies", skipFreqs);
    maps.put("spacegram frequencies", spaceFreqs);
    return maps;
  }

  public Map<String, Map<String, Integer>> getNgramAsymmetries() {
    Map<String, Map<String, Integer>> maps = new LinkedHashMap<>();
    maps.put("bigram asymmetries", biAsyms);
    maps.put("trigram asymmetries", triAsyms);
    maps.put("skipgram asymmetries", skipAsyms);
    maps.put("spacegram asymmetries", spaceAsyms);
    return maps;
  }

  public static void increment(ConcurrentMap<String, Integer> map, String key) {
    map.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
  }

  public static void increment(ConcurrentMap<Character, Integer> map, Character key) {
    map.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
  }

  public Path getOutputPath() {
    return outputPath;
  }

  public Path getInputPath() {
    return inputPath;
  }

  public String getRegex() {
    return regex;
  }

  public ConcurrentMap<Character, Integer> getLetterFreqs() {
    return letterFreqs;
  }

  public ConcurrentMap<Character, Integer> getFirstFreqs() {
    return firstFreqs;
  }

  public ConcurrentMap<Character, Integer> getLastFreqs() {
    return lastFreqs;
  }

  public ConcurrentMap<String, Integer> getBiFreqs() {
    return biFreqs;
  }

  public ConcurrentMap<String, Integer> getTriFreqs() {
    return triFreqs;
  }

  public ConcurrentMap<String, Integer> getSkipFreqs() {
    return skipFreqs;
  }

  public ConcurrentMap<String, Integer> getSpaceFreqs() {
    return spaceFreqs;
  }

  public ConcurrentMap<String, Integer> getBiAsyms() {
    return biAsyms;
  }

  public ConcurrentMap<String, Integer> getTriAsyms() {
    return triAsyms;
  }

  public ConcurrentMap<String, Integer> getSkipAsyms() {
    return skipAsyms;
  }

  public ConcurrentMap<String, Integer> getSpaceAsyms() {
    return spaceAsyms;
  }

  private static final String NEWLINE = System.lineSeparator();
  private Path outputPath;
  private Path inputPath;
  private String regex;

  private ConcurrentHashMap<Character, Integer> 
    letterFreqs = new ConcurrentHashMap<>(), 
    firstFreqs = new ConcurrentHashMap<>(),
    lastFreqs = new ConcurrentHashMap<>();

  private ConcurrentHashMap<String, Integer> 
    biFreqs = new ConcurrentHashMap<>(),
    triFreqs = new ConcurrentHashMap<>(),
    skipFreqs = new ConcurrentHashMap<>(),
    spaceFreqs = new ConcurrentHashMap<>();

  private ConcurrentHashMap<String, Integer>
    biAsyms = new ConcurrentHashMap<>(),
    triAsyms = new ConcurrentHashMap<>(),
    skipAsyms = new ConcurrentHashMap<>(),
    spaceAsyms = new ConcurrentHashMap<>();

  private static boolean anagrams(String s, String t) {
    if (s.length() != t.length()) {
      return false;
    }

    char[] sArr = s.toCharArray();
    char[] tArr = t.toCharArray();

    Arrays.sort(sArr);
    Arrays.sort(tArr);

    return Arrays.equals(sArr, tArr);
  }
}
