import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.swing.JFileChooser;

//used to fill cells
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

//used in creating graphs
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;

//turns statistics from an sms backup into a readable excel file
public class smsStatsToExcel {
	private static final String[] DAYSOFTHEWEEK = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
	private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	private static final int[] DAYSINMONTHS = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
	private static final String[] LABELS = {"Time", "#", "Day of Week", "#", "Day of Month", "#", "Month", "#", "Day in Year", "#",
			"Date", "#", "Most used words", "#"}; //labels of the columns
	private static final int TOPWORDS = 100; //number of top words to print
	private static final int GRAPHWIDTH = 20000; //in units of 1/256th of a character width
	private static final int GRAPHHEIGHT = 15; //in rows
	
	//sets up file and makes calls to fill up
	public static void createExcel(ArrayList<smsContact> contacts) throws IOException {
		XSSFWorkbook wb = new XSSFWorkbook();
		for(int contactNumber = 0; contactNumber < contacts.size(); contactNumber++) {
			smsContact contact = contacts.get(contactNumber);
			XSSFSheet sht = wb.createSheet(new Integer(contactNumber).toString());
			wb.setSheetName(contactNumber, contact.getName() + "(" + contact.getNumber() + ")");
			createRows(sht, contact);
			printCells(sht, contact);
			resizeColumns(sht);
			printAllGraphs(sht, contact);
		}
		OutputStream xFile = getExcelDir();
		if(xFile != null) {
			wb.write(xFile);
		}
		wb.close();
	}
	
	//creates all rows/cells to be filled
	private static void createRows(Sheet sht, smsContact contact) {
		int size = Math.max(contact.getAllDays().length,
				Math.max(smsContact.DAYSINYEAR, smsContact.TIMEOFDAYRESOLUTION)) + 1; //finds largest data set
		for(int i = 0; i < size; i++) { //creates all possible rows
			sht.createRow(i);
		}
	}
	
	private static void printCells(Sheet sht, smsContact contact) {
		printFirstRow(sht);
		int col = 1;
		col = printTimeOfDay(sht, contact, col);
		col = printDayInWeek(sht, contact, col);
		col = printDayInMonth(sht, contact, col);
		col = printMonthInYear(sht, contact, col);
		col = printDayInYear(sht, contact, col);
		col = printAllDays(sht, contact, col);
		col = printWords(sht, contact, col);
		printNumbers(sht, contact, col);
	}
	
	//resizes columns to fit text
	private static void resizeColumns(Sheet sht) {
		sht.setColumnWidth(0, GRAPHWIDTH);
		for(int i = 1; i < LABELS.length + 2; i += 2) {
			sht.autoSizeColumn(i);
		}
	}
	
	private static void printAllGraphs(XSSFSheet sht, smsContact contact) throws IOException {
		int[] graphSizes = {smsContact.TIMEOFDAYRESOLUTION, smsContact.DAYSINWEEK, smsContact.DAYSINMONTH, smsContact.MONTHSINYEAR,
				smsContact.DAYSINYEAR, contact.getAllDays().length, TOPWORDS};
		for(int graph = 0; graph < graphSizes.length; graph++) {
			printGraph(sht, 0, graph * GRAPHHEIGHT, 1, (graph + 1) * GRAPHHEIGHT, (graph * 2) + 1, graphSizes[graph]);
		}
	}
	
	private static OutputStream getExcelDir() throws IOException {
		File file = ScanTexts.chooseFileDir("Select the directory to save the excel file", JFileChooser.DIRECTORIES_ONLY);
		if(file == null) {
			return null;
		}
		File smsFolder = new File(file.getAbsolutePath() + "\\smsResults.xlsx");
		return new FileOutputStream(smsFolder);
	}
	
	//prints labels for the columns
	private static void printFirstRow(Sheet sht) {
		Row row = sht.getRow(0);
		for(int i = 0; i < LABELS.length; i++) {
			row.createCell(i + 1).setCellValue(LABELS[i]);
		}
	}
	
	//prints the time of day and number of texts in next column
	private static int printTimeOfDay(Sheet sht, smsContact contact, int col) {
		int[] timeOfDay = contact.getTimeOfDay();
		for(int i = 0; i < timeOfDay.length; i++) {
			int min = (i % smsContact.HOURFACTOR) * smsContact.MINUTEFACTOR;
			String time = i / smsContact.HOURFACTOR + ":" + (min < 10 ? "0" : "") + min; //adds a 0 if min is only 1 digit
			Row currentRow = sht.getRow(i+1);
			currentRow.createCell(col).setCellValue(time);
			currentRow.createCell(col + 1).setCellValue(timeOfDay[i]);
		}
		return col + 2;
	}
	
	//prints the day in week and number of texts in next column
	private static int printDayInWeek(Sheet sht, smsContact contact, int col) {
		int[] dayInWeek = contact.getDayInWeek();
		for(int i = 0; i < dayInWeek.length; i++) {
			Row currentRow = sht.getRow(i+1);
			currentRow.createCell(col).setCellValue(DAYSOFTHEWEEK[i]);
			currentRow.createCell(col + 1).setCellValue(dayInWeek[i]);
		}
		return col + 2;
	}
	
	//prints the day in month and number of texts in next column
	private static int printDayInMonth(Sheet sht, smsContact contact, int col) {
		int[] dayInMonth = contact.getDayInMonth();
		for(int i = 0; i < dayInMonth.length; i++) {
			Row currentRow = sht.getRow(i+1);
			currentRow.createCell(col).setCellValue(i + 1);
			currentRow.createCell(col + 1).setCellValue(dayInMonth[i]);
		}
		return col + 2;
	}
	
	//prints the month in year and number of texts in next column
	private static int printMonthInYear(Sheet sht, smsContact contact, int col) {
		int[] monthInYear = contact.getMonthInYear();
		for(int i = 0; i < monthInYear.length; i++) {
			Row currentRow = sht.getRow(i+1);
			currentRow.createCell(col).setCellValue(MONTHS[i]);
			currentRow.createCell(col + 1).setCellValue(monthInYear[i]);
		}
		return col + 2;
	}
	
	//prints the day in year and number of texts in next column
	private static int printDayInYear(Sheet sht, smsContact contact, int col) {
		int[][] dayInYear = contact.getDayInYear();
		int row = 1;
		for(int i = 0; i < MONTHS.length; i++) {
			for(int j = 0; j < DAYSINMONTHS[i]; j++) {
				Row currentRow = sht.getRow(row);
				currentRow.createCell(col).setCellValue(MONTHS[i] + " " + (j + 1));
				currentRow.createCell(col + 1).setCellValue(dayInYear[i][j]);
				row++;
			}
		}
		return col + 2;
	}
	
	//list the number of days where messages were sent since the first message and number of texts each day in next column
	private static int printAllDays(Sheet sht, smsContact contact, int col) {
		String[] allDaysLabels = contact.getAllDaysLabels();
		int[] allDays = contact.getAllDays();
		for(int i = 0; i < allDays.length; i++) {
			Row currentRow = sht.getRow(i+1);
			currentRow.createCell(col).setCellValue(allDaysLabels[i]);
			currentRow.createCell(col + 1).setCellValue(allDays[i]);
		}
		return col + 2;
	}
	
	//print the top 100 most used words and how many times they were used in the next column
	private static int printWords(Sheet sht, smsContact contact, int col) {
		ArrayList<WordWithIncidence> words = contact.getWords();
		int length = words.size();
		int rank = 1;
		for(int i = length - 1; i > length - (TOPWORDS + 1) && i > -1; i--) { //counts TOPWORDS or all if less than TOPWORDS
			Row currentRow = sht.getRow(rank);
			currentRow.createCell(col).setCellValue(rank + " - " + words.get(i).getWord()); //prints rank and word
			currentRow.createCell(col + 1).setCellValue(words.get(i).getCount()); //prints incidence of word
			rank++;
		}
		return col + 2;
	}
	
	//prints some statistics
	private static void printNumbers(Sheet sht, smsContact contact, int col) {
		String[] numbers = contact.getNumbers();
		for(int i = 0; i < numbers.length; i++) {
			sht.getRow(i+1).createCell(col).setCellValue(numbers[i]);
		}
	}
	
	//prints a graph
	private static void printGraph(XSSFSheet sht,
			int leftCol, int topRow, int rightCol, int botRow, int colIndex, int range) throws IOException {
        XSSFDrawing drawing = sht.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, leftCol, topRow, rightCol, botRow);
        XSSFChart chart = drawing.createChart(anchor);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle(sht.getRow(0).getCell(colIndex).toString());
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle(sht.getRow(0).getCell(colIndex + 1).toString());

        XDDFDataSource<String> x = XDDFDataSourcesFactory.fromStringCellRange(
        		sht, new CellRangeAddress(1, range, colIndex, colIndex));
        XDDFNumericalDataSource<Double> y = XDDFDataSourcesFactory.fromNumericCellRange(
        		sht, new CellRangeAddress(1, range, colIndex + 1, colIndex + 1));

        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(x, y);
        series.setSmooth(false);
        series.setMarkerStyle(MarkerStyle.NONE);
        chart.plot(data);
        solidLineSeries(data, 0, PresetColor.CORNFLOWER_BLUE);
    }
	
	//provided by Apache
	private static void solidLineSeries(XDDFChartData data, int index, PresetColor color) {
        XDDFSolidFillProperties fill = new XDDFSolidFillProperties(XDDFColor.from(color));
        XDDFLineProperties line = new XDDFLineProperties();
        line.setFillProperties(fill);
        XDDFChartData.Series series = data.getSeries(index);
        XDDFShapeProperties properties = series.getShapeProperties();
        if (properties == null) {
            properties = new XDDFShapeProperties();
        }
        properties.setLineProperties(line);
        series.setShapeProperties(properties);
    }
}