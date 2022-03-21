package comparator;

import com.sun.tools.javac.util.Pair;

import java.util.ArrayList;

public class HBaseToBigTableUtils {

  public static ArrayList<Pair<String, String>> conversionMap =
      new ArrayList<Pair<String, String>>() {
        {
          add(Pair.of("HTableInterface", "Table"));
          add(Pair.of("HTableInterface ", "Table "));
          add(Pair.of("hTableInterface", "table"));
          add(Pair.of("HConnection", "Connection"));
          add(Pair.of("put.add(", "put.addColumn(")); //to
          add(Pair.of("driver.getConnection().listTableNames()", "admin.listTableNames()"));
          add(Pair.of("HBaseAdmin hbaseAdmin = new HBaseAdmin(hDriver.getConnection())",
              "Admin hbaseAdmin = hDriver.getConnection().getAdmin()"));
          add(Pair.of("HBaseAdmin admin = new HBaseAdmin(hDriver.getConnection())", "Admin admin = hDriver.getConnection().getAdmin()"));
          add(Pair.of("new HBaseAdmin(hDriver.getConnection())", "hDriver.getConnection().getAdmin()"));
          add(Pair.of("HBaseAdmin admin", "Admin admin"));
          add(Pair.of("HBaseAdmin ", "Admin "));
          add(Pair.of("HBaseAdmin", "Admin"));
          add(Pair.of("ACCOUNT_KEY_TABLE", "tableName"));
          add(Pair.of("Bytes.toBytes(ACCOUNT_KEY_TABLE)", "tableName"));
          add(Pair.of("TableName.valueOf(\"test\")", "tableName"));
          add(Pair.of("hBaseAdmin", "admin"));  //we don't want to rename the variable on bigtable
          add(Pair.of("IOException | InterruptedException", "IOException"));
          add(Pair.of("InterruptedException | IOException", "IOException"));
          add(Pair.of("anyString()", "any(TableName.class)"));
          add(Pair.of("tableName", "hDriver.getFullTableName(tableName)"));
          add(Pair.of("getTableName()", "hDriver.getFullTableName(tableName)"));
          add(Pair.of("getTableName()", "hDriver.getFullTableName(getTableName())"));
          add(Pair.of("Bytes.toBytes(TABLE)", "tableName"));
          add(Pair.of("MockHTable ", "Table "));
          add(Pair.of("TABLE", "tableName"));
          add(Pair.of("TABLE", "TABLE_NAME"));
          add(Pair.of("TABLE", "table.getName()"));
          add(Pair.of("\"test\".getBytes()", "tableName"));
          add(Pair.of("\"test\".getBytes()", "TableName.valueOf(\"test\")"));
          add(Pair.of("eq(AKI_TABLE_NAME)", "any(TableName.class)"));
          add(Pair.of("TABLE", "hDriver.getFullTableName(TABLE)"));
          add(Pair.of("ACCOUNT_KEY_TABLE", "table.getName()"));
          add(Pair.of("ACCOUNT_KEY_TABLE", "tableName"));
          add(Pair.of("hDriver.getNamespace(), tableName", "hDriver.getFullTableName(tableName)"));
          add(Pair.of("anyString(), eq(\"test\")", "any(TableName.class)"));
          add(Pair.of("connection.listTableNames()", "admin.listTableNames()"));
          add(Pair.of("new HBaseAdmin(hDriver.getConnection())", "hDriver.getConnection().getAdmin()"));
          add(Pair.of("HBaseAdmin admin = new HBaseAdmin(driver.getConnection())", "Admin admin = driver.getConnection().getAdmin()"));
        }
      };

  public static boolean isHBaseToBigTable(Line a_line, Line b_line, boolean isHbToBt,
      Report report) {
    String hb = a_line.content;

    if (hb.equals(b_line)) {
      return false;
    }

    for (Pair<String, String> entry : conversionMap) {
      String key = isHbToBt ? entry.fst : entry.snd;
      String value = isHbToBt ? entry.snd : entry.fst;
      hb = a_line.content;
      if (hb.contains(key)) {
        hb = hb.replace(key, value);
        String aa = hb.replaceAll(" ", "");
        String bb = b_line.noSpaces();
        aa = aa.replaceAll("[(){]", "").replaceAll("try", "").replaceAll(";", "");
        bb = bb.replaceAll("[(){]", "").replaceAll("try", "").replaceAll(";", "");
        if (aa.equals(bb)) {
          Line.mark(a_line, b_line);

          report.addConversion("\"" + entry.fst + "\" -> \"" + entry.snd + "\"");
          return true;
        }
      }
    }

    return false;
  }



}
