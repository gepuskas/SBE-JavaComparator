package comparator;

import java.util.Collection;
import java.util.UUID;

public class Line implements Comparable<Line> {
  public String content;
  public int line_n;

  public UUID pairingId = null;

  public Line(String content, int line_n) {

    //clean the line
    this.content = trimLine(content);
    this.line_n = line_n;
  }

  public static Line empty() {
    return new Line("", -1);
  }

  private static String trimLine(String s) {
    return s.trim().replace("\t", "");
  }

  public static boolean compareSets(Collection<Line> aa, Collection<Line> bb) {

    if (merge(aa).equals(merge(bb))) {
      UUID uuid = UUID.randomUUID();
      for (Line a : aa) {
        a.mark(uuid);
      }
      for (Line b : bb) {
        b.mark(uuid);
      }
      return true;
    }
    return false;
  }

  public static void mark(Line... aa) {
    UUID uuid = UUID.randomUUID();

    for (Line a : aa) {a.mark(uuid);}
  }

  private static String merge(Collection<Line> lines) {
    StringBuilder aaa = new StringBuilder();

    for (Line l : lines) {
      aaa.append(l.noSpaces());
    }
    return aaa.toString();
  }

  @Override
  public int compareTo(Line o) {
    return this.content.compareTo(o.content);
  }

  @Override
  public String toString() {
    if (line_n == -1) {return "";}
    return "@line: " + line_n + " " + content;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Line) {
      Line obj = ((Line) o);
      if (pairingId == null && obj.pairingId == null) {
        boolean same = this.noSpaces().equals(((Line) o).noSpaces());
        if (same) {
          UUID id = UUID.randomUUID();
          pairingId = id;
          obj.pairingId = id;
          //   System.out.println("\t\t\t PAIRED: " + this + "  ///  " + obj);
          return true;
        }
      } else {return pairingId != null && obj.pairingId != null && pairingId.equals(obj.pairingId);}
    }

    return false;
  }

  public boolean isMarked() {
    return pairingId != null || line_n == -1;
  }

  public void mark(UUID id) {
    pairingId = id;
  }

  public String noSpaces() {
    return content.replaceAll(" ", "");
  }
}