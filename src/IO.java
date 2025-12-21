import java.nio.file.Path;

class IO {
  public static Path resolvePath(Path path, String s) {
    if (path.getParent().getFileName().toString().equals(s)) {
      return path;
    }

    return path.resolve(s);
  }
}
