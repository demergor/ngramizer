package ngramizer.io;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class IO {
  public static Path resolvePath(Path path, String s) {
    Path parent = path.getParent();
    if (
        parent != null &&
        parent.getFileName() != null &&
        parent.getFileName().toString().equals(s)
    ) {
      return path;
    }

    return path.resolve(s);
  }

  public static Path expand(Path path) {
    if (path.getNameCount() == 0) {
      return path;
    }

    if (!path.getName(0).toString().equals("~")) {
      return path;
    }

    Path newPath = Paths.get(System.getProperty("user.home"));
    if (path.getNameCount() == 1) {
      return newPath;
    } else {
      return newPath.resolve(path.subpath(1, path.getNameCount()));
    }
  }
}
