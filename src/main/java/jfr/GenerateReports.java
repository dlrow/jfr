package jfr;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class GenerateReports {

	public static void main(String[] args) {
		String excelPath = getExcelPath();
		generateReport(excelPath);
	}

	protected static String getExcelPath() {
		String excelFileName = "";
		File file = new File(System.getProperty("user.dir"));
		File[] list = file.listFiles();
		if (list != null)
			for (File fil : list) {
				if (!fil.isDirectory() && fil.getName().contains(".xls")) {
					excelFileName = fil.getName();
				}
			}
		return excelFileName;
	}

	private static void generateReport(String excelPath) {
		Map<String, List<String>> ecids = getEcids(excelPath);
		List<String> commandsToExecute = createCommands(ecids);
		executeCommands(commandsToExecute);
		System.out.println("report generated");
	}

	private static void executeCommands(List<String> commandsToExecute) {
		Runtime rt = Runtime.getRuntime();
		for (String command : commandsToExecute) {
			try {
				rt.exec(new String[] { "/bin/bash", "-c", command });
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static List<String> createCommands(Map<String, List<String>> ecids) {
		List<String> commands = new ArrayList<>();
		for (Entry<String, List<String>> entry : ecids.entrySet()) {

			for (String ecid : entry.getValue()) {
				String fileName = ecid;
				if (fileName.indexOf('^') > -1) {
					fileName = fileName.replace('^', '_');
				}
				String dir = "Reports"+File.separator + entry.getKey() + File.separator + fileName;
				new File(dir).mkdirs();
				String cmd = "java -jar " + "JFRParser.jar" + " /jfr " + entry.getKey() + " /o " + dir + File.separator+" /ecid \""
						+ ecid + "\"";
				commands.add(cmd);
			}
		}
		return commands;
	}

	protected static Map<String, List<String>> getEcids(String excelPath) {
		Map<String, List<String>> ecids = new HashMap<>();
		Map<Integer, String> latestJfrFoundForColumn = new HashMap<>();
		try {
			InputStream excelFile = new FileInputStream(new File(excelPath));
			Workbook workbook = new XSSFWorkbook(excelFile);
			Sheet datatypeSheet = workbook.getSheetAt(0);
			Iterator<Row> iterator = datatypeSheet.iterator();

			while (iterator.hasNext()) {
				Row currentRow = iterator.next();
				Iterator<Cell> cellIterator = currentRow.iterator();
				while (cellIterator.hasNext()) {
					Cell currentCell = cellIterator.next();
					if (currentCell.getCellTypeEnum() == CellType.STRING) {
						String curr = currentCell.getStringCellValue();
						if (curr.indexOf("jvm_flightRecording") > -1 && curr.indexOf(".jfr") > -1) {
							latestJfrFoundForColumn.put(currentCell.getColumnIndex(), curr);
						} else if (curr.length() == "005aXu_I7HK3z035Rnh8id0003kz0001jf".length()) {
							String jfrName = latestJfrFoundForColumn.get(currentCell.getColumnIndex());
							if (!ecids.containsKey(jfrName))
								ecids.put(jfrName, new ArrayList<>());
							ecids.get(jfrName).add(curr);
							ReadReportAndWriteToExcel.ecidPos.put(curr,
									new ecidCell(currentCell.getRowIndex(), currentCell.getColumnIndex()));
						}
					}

				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ecids;
	}

}
