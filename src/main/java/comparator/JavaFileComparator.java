package comparator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class JavaFileComparator {

  private static final String SBE_PATH =
      "/Users/geremia.longobardo/workspace/SBE-BIG-TABLE/SBE";
  private static final String SUFFIX_HBASE = "-hbase";
  private static final String SUFFIX_BIGTABLE = "-bigtable";
  private static final String SUFFIX_JAVA = ".java";
  private static final int THRESOLD = 100;
  private static final int MAX_SPLIT_SEARCH_SIZE = 3;
  private static final int MAX_SPLIT_SEARCH_POS_SHIFT = 10;

  public static void main(String[] args) throws IOException {
    String persistenceDir = "";
    if (args.length > 0) {
      persistenceDir = args[0] + "/persistence";
    } else {persistenceDir = SBE_PATH + "/persistence";}
    File dir = new File(persistenceDir);
    TreeSet<String> bt_dirs = new TreeSet<>();
    TreeSet<String> hb_dirs = new TreeSet<>();

    Utils.findDirectory(dir, SUFFIX_HBASE, hb_dirs);
    Utils.findDirectory(dir, SUFFIX_BIGTABLE, bt_dirs);

    TreeMap<String, ArrayList<Report>> allReports = compare(persistenceDir, hb_dirs, bt_dirs);
    printAllReports(allReports);
  }

  private static void printAllReports(TreeMap<String, ArrayList<Report>> allReports) {

    TreeMap<Integer, ArrayList<String>> worst = new TreeMap<>();
    System.out.println("\n**** START  ***\n");
    int total = 0;
    for (Map.Entry<String, ArrayList<Report>> report : allReports.entrySet()) {
      System.out.println("\n**** COMPARING " + report.getKey() + " ***\n");
      int mod_total = 0;
      for (Report fileReport : report.getValue()) {
        fileReport.print();
        mod_total += fileReport.getDiffSize();
        worst.putIfAbsent(fileReport.getDiffSize(), new ArrayList<>());
        worst.get(fileReport.getDiffSize()).add(fileReport.filename);
      }
      total += mod_total;
      System.out.println("**** DONE  " + report.getKey() + " *** ");
    }
    System.out.println("**** TOTAL:  " + total + " *** ");

    int total_worst = 0;
    System.out.println("**** WORST CASES:   *** ");
    int i = 1;
    for (Map.Entry<Integer, ArrayList<String>> e : worst.descendingMap().entrySet()) {
      for (String s : e.getValue()) {
        if (e.getKey() == 0) {
          break;
        }
        System.out.println("\t" + s + " - " + e.getKey());
        total_worst += e.getKey();
        i++;
      }
    }
    if (total > 0) {
      System.out.println(
          "**** IMPACT WORST DIFF ON TOTAL:  " + (total_worst * 100) / total + "% *** ");
    }
    System.out.println("\n**** END  ***\n");
  }

  private static TreeMap<String, ArrayList<Report>> compare(String persistenceDir,
      TreeSet<String> hb_dirs,
      TreeSet<String> bt_dirs)
      throws IOException {

    TreeMap<String, ArrayList<Report>> allReports = new TreeMap<>();
    for (String bt_dir : bt_dirs) {
      String hb_dir = bt_dir.replace(SUFFIX_BIGTABLE, SUFFIX_HBASE);
      if (hb_dirs.contains(hb_dir)) {
        TreeMap<Path, File> bt_files =
            Utils.walkDirectory(persistenceDir + "/" + bt_dir, SUFFIX_JAVA);
        TreeMap<Path, File> hb_files =
            Utils.walkDirectory(persistenceDir + "/" + hb_dir, SUFFIX_JAVA);

        allReports.put(hb_dir + " - " + bt_dir, treeDiff(hb_files, bt_files));
      }
    }

    return allReports;
  }

  private static ArrayList<Report> treeDiff(TreeMap<Path, File> hb_files,
      TreeMap<Path, File> bt_files) throws IOException {

    printTreesDelta(hb_files, bt_files);

    List<Path> AandB = bt_files.keySet().stream()
        .filter(element -> hb_files.containsKey(element))
        .collect(Collectors.toList());

    ArrayList<Report> reports = new ArrayList<>();

    for (Path filePath : AandB) {
      //   if (filePath.toString().endsWith("WrappedAccountKeyDAO.java")) {
      File hb_file = hb_files.get(filePath);
      File bt_file = bt_files.get(filePath);

      List<Line> hb_lines = Utils.trimFile(Files.readAllLines(hb_file.toPath()));
      List<Line> bt_lines = Utils.trimFile(Files.readAllLines(bt_file.toPath()));
      Report report = fileCompare(hb_lines, bt_lines, hb_file.getName());
      reports.add(report);

      //          }
    }


    return reports;
  }

  private static Report fileCompare(List<Line> hb_lines, List<Line> bt_lines, String filename) {

    Report report = new Report(filename);

    int last_diff = -1;

    for (int i = 0; i < Math.max(hb_lines.size(), bt_lines.size()); i++) {

      if (printTails(hb_lines, bt_lines, i, report)) {
        continue;
      }

      Line hb_line = hb_lines.get(i);
      Line bt_line = bt_lines.get(i);

      if (!hb_line.equals(bt_line)) {
        //only relevant lines
        if (isRelevant(hb_line.noSpaces()) || isRelevant(bt_line.noSpaces())) {

          boolean foundHB;
          boolean foundBT;
          //check if same line is in a different position within THRESOLD
          foundHB = searchForward(hb_line, bt_lines, i, true, report);
          foundBT = searchForward(bt_line, hb_lines, i, false, report);

          if (foundHB && foundBT) {
            continue;
          }
          //check if same line has been split into more lines
          int toSkip = checkSameLineSplitIntoTwoOrThree(hb_lines, bt_lines, i);

          if (toSkip > 0) {
            i = i + toSkip;
            continue;
          }
        }
        //the lines might have changed
        hb_line = isRelevant(hb_lines.get(i).noSpaces()) ? hb_lines.get(i) : Line.empty();
        bt_line = isRelevant(bt_lines.get(i).noSpaces()) ? bt_lines.get(i) : Line.empty();
        if (hb_line.isMarked() && bt_line.isMarked()) {
          continue;
        }
        if (i - last_diff > 1) {
          report.chunk(report.diff_bt_lines.size());
        }
        report.add(hb_line.isMarked() ? Line.empty() : hb_line,
            bt_line.isMarked() ? Line.empty() : bt_line);

        last_diff = i;
      }
    }
    return report;
  }

  private static boolean isRelevant(String s) {
    return s.length() > 4;
  }

  private static void printTreesDelta(TreeMap<Path, File> hb_files, TreeMap<Path, File> bt_files) {
    List<Path> aWithoutB = hb_files.keySet().stream()
        .filter(x -> !bt_files.containsKey(x))
        .collect(Collectors.toList());

    List<Path> bWithoutA = bt_files.keySet().stream()
        .filter(x -> !hb_files.containsKey(x))
        .collect(Collectors.toList());

    for (Path hb_only : aWithoutB) {
      System.out.println("HB ONLY: " + hb_only);
    }
    for (Path bt_only : bWithoutA) {
      System.out.println("BT ONLY: " + bt_only);
    }
  }

  public static boolean searchForward(Line a_line,
      List<Line> b_lines, int pos, boolean isHbToBt, Report report) {
    if (a_line.isMarked()) {
      return true;
    }
    boolean found = false;
    if (isRelevant(a_line.noSpaces())) {
      for (int j = pos;
          j < Math.min(pos + THRESOLD, b_lines.size())
              && !found; j++) {
        Line fwb_line = b_lines.get(j);
        if (a_line.equals(fwb_line) || HBaseToBigTableUtils.isHBaseToBigTable(a_line, fwb_line,
            isHbToBt, report)) {
          found = true;
        }
      }
    }
    return found;
  }

  public static int checkSameLineSplitIntoTwoOrThree(List<Line> a_lines, List<Line> b_lines,
      int pos) {

    String a_line = a_lines.get(pos).noSpaces();
    if (!isRelevant(a_line)) {
      return 0;
    }

    for (int k = 0; k < MAX_SPLIT_SEARCH_POS_SHIFT; k++) {
      List<Line> aa = new ArrayList<>();
      for (int i = pos + k; i < Math.min(a_lines.size(), pos + MAX_SPLIT_SEARCH_SIZE); i++) {
        aa.add(a_lines.get(i));
        List<Line> bb = new ArrayList<>();
        for (int m = 0; m < MAX_SPLIT_SEARCH_POS_SHIFT; m++) {
          for (int j = pos + m; j < Math.min(b_lines.size(), pos + m + MAX_SPLIT_SEARCH_SIZE);
              j++) {
            bb.add(b_lines.get(j));
            if (Line.compareSets(aa, bb)) {
              if (i != j) {
                int diff = Math.abs(i - j);
                if (j > i) {
                  for (int l = i; l < pos + diff; l++) {
                    a_lines.add(l, Line.empty());
                  }
                } else {
                  for (int l = j; l < pos + diff; l++) {
                    b_lines.add(l, Line.empty());
                  }
                }
                return k == 0 || m > 0 ? diff : 0;
              }
            }
          }
        }
      }
    }
    return 0;
  }

  private static boolean printTails(List<Line> hb_lines, List<Line> bt_lines, int idx,
      Report report) {
    if (idx >= hb_lines.size()) {
      Line line = bt_lines.get(idx);
      if (!line.isMarked()) {
        report.add(Line.empty(), line);
      }
      return true;
    }
    if (idx >= bt_lines.size()) {
      Line line = hb_lines.get(idx);
      if (!line.isMarked()) {
        report.add(line, Line.empty());
      }
      return true;
    }
    return false;
  }

}
