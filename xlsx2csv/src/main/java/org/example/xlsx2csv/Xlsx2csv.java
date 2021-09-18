package org.example.xlsx2csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class Xlsx2csv {

  private boolean mus;

  public boolean ismus() {
    return mus;
  }

  public Xlsx2csv mus(boolean mus) {
    this.mus = mus;
    return this;
  }

  public boolean filter(File f) {
    return f != null && f.exists() && !f.isHidden() && f.getName().endsWith(".xlsx");
  }

  public String toCsv(File f, List<File> targetDirectories) {
    String fileName = getFileName(f);

    long start = System.currentTimeMillis();
    try (Workbook workbook = WorkbookFactory.create(f, null, true)) {
      for (Sheet sheet : workbook) {
        handleSheet(targetDirectories, fileName, sheet);
        //only handle the first sheet
        if (ismus()) {
          break;
        }
      }

      return String.format("转换%s, 耗时约:%s\n", fileName,
          System.currentTimeMillis() - start);
    } catch (Exception e) {
      e.printStackTrace();
      return String.format("%s  转换失败:\n%s\n%s\n", fileName, e, e.getMessage());
    }
  }

  private void handleSheet(List<File> targetDirectories, String workBookName, Sheet sheet)
      throws IOException {
    int maxCell = 0;
    for (Row r : sheet) {
      maxCell = Math.max(maxCell, r.getLastCellNum());
    }

    FormulaEvaluator evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
    NumberFormat nf = NumberFormat.getInstance();
    //设置保留多少位小数
    nf.setMaximumFractionDigits(20);
    // 取消科学计数法
    nf.setGroupingUsed(false);

    int rowNum = 0, cellNum = 0;

    List<CSVPrinter> printers = new ArrayList<>(targetDirectories.size());
    try {
      for (File f : targetDirectories) {
        String csvFileName = String
            .format("%s%s%s.csv", f.getAbsolutePath(), File.separator,
                getCsvFileName(workBookName, sheet));
        CSVPrinter printer = new CSVPrinter(
            new OutputStreamWriter(new FileOutputStream(csvFileName)),
            CSVFormat.EXCEL);
        printers.add(printer);
      }

      Iterator<Row> rowIterator = sheet.iterator();
      while (rowIterator.hasNext()) {
        Row r = rowIterator.next();
        rowNum = r.getRowNum();
        for (int c = 0; c < maxCell; c++) {
          cellNum = c;
          Cell cell = r.getCell(c);
          if (cell == null) {
            printAll(printers, null);
            continue;
          }

          try {
            CellValue value = evaluator.evaluate(cell);
            if (value == null) {
              printAll(printers, null);
            } else {
              switch (value.getCellType()) {
                case STRING:
                  printAll(printers, value.getStringValue());
                  break;
                case BOOLEAN:
                  printAll(printers, value.getBooleanValue());
                  break;
                case BLANK:
                  printAll(printers, null);
                  break;
                case ERROR:
                  printAll(printers, value.getErrorValue());
                  break;
                case NUMERIC: {
                  double doubleValue = value.getNumberValue();
                  printAll(printers, nf.format(doubleValue));
                }
                break;
              }
            }
          } catch (Exception ignore) {
            //验证公式错误，忽略
            printAll(printers, cell);
          }
        }

        if (rowIterator.hasNext()) {
          for (CSVPrinter p : printers) {
            p.println();
          }
        }
      }
      for (CSVPrinter printer : printers) {
        printer.flush();
      }
    } catch (Exception e) {
      throw new RuntimeException(String
          .format("%s_%s, 第%s行第%s列 转换错误\n", workBookName, sheet.getSheetName(), rowNum, cellNum),
          e);
    } finally {
      for (CSVPrinter printer : printers) {
        printer.close();
      }
    }
  }


  private void printAll(Iterable<CSVPrinter> printers, Object o) throws IOException {
    for (CSVPrinter printer : printers) {
      printer.print(o);
    }
  }

  private String getCsvFileName(String fileName, Sheet sheet) {
    if (ismus()) {
      int idx = fileName.indexOf("-");
      return 0 < idx ? fileName.substring(0, idx).toLowerCase() : fileName;
    } else {
      return (fileName + sheet.getSheetName()).toLowerCase();
    }
  }


  private String getFileName(File f) {
    String fileName = f.getName();
    fileName = fileName.substring(0, fileName.lastIndexOf("."));
    return fileName;
  }

}
