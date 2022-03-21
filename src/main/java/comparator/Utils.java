package comparator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Utils {

  public static void findDirectory(File parentDirectory, String suffix, Set<String> set) {
    File[] files = parentDirectory.listFiles();
    for (File file : files) {
      if (file.isFile()) {
        continue;
      }
      if (file.getName().endsWith(suffix)) {
        set.add(file.getName());
      }
    }
  }

  //GET ALL JAVA FILES
  public static TreeMap<Path, File> walkDirectory(String rootDir, String suffix) throws
      IOException {
    // using `Files.walk()` method
    return new TreeMap(Files.walk(Paths.get(rootDir))
        .filter(Files::isRegularFile).filter(x -> x.getFileName().toString().endsWith(suffix))
        .collect(Collectors.toMap(Path::getFileName, Path::toFile)));
  }


  public static List<Line> trimFile(List<String> lines) {
    int start = 0;

    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains("class") || lines.get(i).contains("interface")) {
        start = i;
        break;
      }
    }
    //remove imports
    lines = lines.subList(start, lines.size() - 1);

    List<Line> list = new ArrayList<>();
    boolean isIgnoreSection = false;
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      isIgnoreSection = isIgnore(line, isIgnoreSection);

      //remove useless lines
      if (!isIgnoreSection && isLineRelevant(line)) {
        list.add(new Line(line, start + i + 1));
      }
    }
    return list;
  }

  private static boolean isIgnore(String line, boolean isIgnoreSection) {
    if (!isIgnoreSection && line.equals("//*COMPARATOR-IGNORE-START")) {
      return true;
    }
    if (isIgnoreSection && line.equals("//*COMPARATOR-IGNORE-STOP")) {
      return false;
    }
    return isIgnoreSection;


  }

  private static boolean isLineRelevant(String line) {
    line = line.trim();
    return (line.length() > 3 && !line.startsWith("/") && !line.startsWith("@")
        && !line.startsWith("*") && !line.startsWith("LOG."));
  }

}
