package jfr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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

//stores position of ecids on excel sheet
class ecidCell {
	int x;
	int y;

	public ecidCell(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}

}

public class GenerateReports  {

	// ecid to it's position on excel sheet
	static Map<String, ecidCell> ecidPos = new HashMap<>();

	// ecids(plural) to their corresponding .jfr
	static Map<String, List<String>> ecids;
	
	static ProgressStatus pgs = new ProgressStatus();

	public static void main(String[] args) {
		pgs.setProgressValue(10);
		generateReport();
		populateTime();
	}

	static void populateTime() {
		String excelPath = getExcelPath();
		pgs.setProgressValue(80);
		Map<String, String> ecidTime = getTimeForEachEcids(ecids);
		pgs.setProgressValue(90);
		writeToExcel(ecidTime, excelPath);
		pgs.setProgressValue(100);
	}

	private static void generateReport() {
		String excelPath = getExcelPath();
		pgs.setProgressValue(20);
		ecids = getEcids(excelPath);
		pgs.setProgressValue(30);
		List<String> commandsToExecute = createCommands(ecids);
		pgs.setProgressValue(50);
		try {
			executeCommands(commandsToExecute);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		pgs.setProgressValue(70);
	}

	//executes commands
	private static void executeCommands(List<String> commandsToExecute) throws InterruptedException {
		Runtime rt = Runtime.getRuntime();
		String os = System.getProperty("os.name").toLowerCase();
		for (String command : commandsToExecute) {
			try {
				if (os.contains("win")) {
					ProcessBuilder pb = new ProcessBuilder(new String[] { "cmd.exe", "/c", command });
				    Process p = pb.start(); 
					p.waitFor();
				} else {
					Process p = rt.exec(new String[] { "/bin/bash", "-c", command });
					p.waitFor();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	//creates commands to be executed 
	private static List<String> createCommands(Map<String, List<String>> ecids) {
		List<String> commands = new ArrayList<>();
		for (Entry<String, List<String>> entry : ecids.entrySet()) {

			for (String ecid : entry.getValue()) {
				String fileName = ecid;
				if (fileName.indexOf('^') > -1) {
					fileName = fileName.replace('^', '_');
				}
				String dir = "Reports" + File.separator + entry.getKey() + File.separator + fileName;
				new File(dir).mkdirs();
				String cmd = "java -jar " + "JFRParser.jar" + " /jfr " + entry.getKey() + " /o " + dir + File.separator
						+ " /ecid \"" + ecid + "\"";
				commands.add(cmd);
			}
		}
		return commands;
	}

	// gets the excel name from current directory
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

	// reads the excel for ecids
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
							ecidPos.put(curr, new ecidCell(currentCell.getRowIndex(), currentCell.getColumnIndex()));
						}
					}

				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ecids;
	}
	
	// creates a map of ecids to their corresponding time
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

	
	//writes time to excel sheet 
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
