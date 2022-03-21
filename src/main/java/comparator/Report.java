package comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Report {

  public String filename;
  public List<Line> diff_hb_lines = new ArrayList<>();
  public List<Line> diff_bt_lines = new ArrayList<>();
  public Set<Integer> chunks = new TreeSet<>();
  public TreeMap<String, Integer> conversions = new TreeMap<>();

  public Report(String filename) {
    this.filename = filename;
  }

  public void add(Line hb_line, Line bt_line) {
    diff_hb_lines.add(hb_line);
    diff_bt_lines.add(bt_line);
  }

  public void print() {
    System.out.println("\n\t**** COMPARING " + filename);

    for (int i = 0; i < diff_bt_lines.size(); i++) {
      if (chunks.contains(i)) {
        System.out.println("\t\t\t=======================================");
      }
      System.out.println("\t\t\t" + diff_hb_lines.get(i) + "  ///  " + diff_bt_lines.get(i));
    }

    System.out.println(
        "\t\t\t CHECK : " + (diff_bt_lines.size() > 0 ? ("KO (" + diff_bt_lines.size()
            + " lines differ)") : " OK"));
    if (conversions.size() > 0) {
      System.out.println("\t\t\t CLASS CONVERSIONS : " + conversions.size());
      for (Map.Entry<String, Integer> entry : conversions.entrySet()) {
        System.out.println("\t\t\t\t" + entry.getKey() + " : " + entry.getValue());
      }
    }
  }

  public void chunk(int idx) {
    chunks.add(idx);
  }

  public int getDiffSize() {
    return diff_hb_lines.size();
  }

  public void addConversion(String key) {
    conversions.put(key, 1 + conversions.getOrDefault(key, 0));
  }
}
