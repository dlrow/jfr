package jfr;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

class ecidCell {
	int x;
	int y;

	public ecidCell(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}

}

public class ReadReportAndWriteToExcel {
	static Map<String, ecidCell> ecidPos = new HashMap<>();

	public static void main(String[] args) {
		String excelPath = GenerateReports.getExcelPath();
		populateTime(excelPath);
	}

	private static void populateTime(String excelPath) {
		Map<String, List<String>> ecids = GenerateReports.getEcids(excelPath);
		Map<String, String> ecidTime = getTimeForEachEcids(ecids);
		writeToExcel(ecidTime, excelPath);
	}

	private static Map<String, String> getTimeForEachEcids(Map<String, List<String>> ecids) {
		Map<String, String> ecidTime = new HashMap<String, String>();
		for (Entry<String, List<String>> entry : ecids.entrySet()) {

			for (String ecid : entry.getValue()) {
				String fileName = ecid;
				if (fileName.indexOf('^') > -1) {
					fileName = fileName.replace('^', '_');
				}
				try {
					BufferedReader in = new BufferedReader(
							new FileReader("Reports"+File.separator + entry.getKey() + File.separator + fileName + File.separator+".html"));
					String str;
					while ((str = in.readLine()) != null) {
						if (str.contains("Duration")) {
							int indexOfDuration = str.indexOf("Duration");
							int begin = str.indexOf("text", indexOfDuration);
							int end = str.indexOf("td", begin);
							String time = str.substring(begin + 6, end - 2);
							ecidTime.put(ecid, time);
						}
					}
					in.close();
				} catch (IOException e) {
				}
			}
		}
		return ecidTime;
	}

	private static void writeToExcel(Map<String, String> ecidTime, String excelPath) {
		try {

			FileInputStream excelFile = new FileInputStream(new File(excelPath));
			Workbook workbook = new XSSFWorkbook(excelFile);
			Sheet datatypeSheet = workbook.getSheetAt(0);
			for (Entry<String, String> entry : ecidTime.entrySet()) {
				ecidCell currEcidCell = ecidPos.get(entry.getKey());
				Cell cell = datatypeSheet.getRow(currEcidCell.x).createCell(currEcidCell.y + 1);
				cell.setCellValue(entry.getValue());
			}

			FileOutputStream outFile = new FileOutputStream(new File(excelPath));
			workbook.write(outFile);
			outFile.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
