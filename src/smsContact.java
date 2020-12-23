import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

//contains all messages and metadata with a specific contact, can create backup of all messages
public class smsContact {
	private static final long ONEDAY = 86400000L; //one day in milliseconds
	private static final DecimalFormat DF = new DecimalFormat("###.###");
	private static final DateFormat PRINTFORMAT = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
	private static final DateFormat PRINTDATEFORMAT = new SimpleDateFormat("YYYY-MM-dd");
	//formats to be parsed into indexes for arrays
	private static final DateFormat HOURFORMAT = new SimpleDateFormat("HH");
	private static final DateFormat MINUTEFORMAT = new SimpleDateFormat("mm");
	private static final DateFormat SECONDFORMAT = new SimpleDateFormat("ss");
	private static final DateFormat MILLIFORMAT = new SimpleDateFormat("SSSS");
	private static final DateFormat DAYOFWEEKFORMAT = new SimpleDateFormat("u");
	private static final DateFormat DAYOFMONTHFORMAT = new SimpleDateFormat("dd");
	private static final DateFormat MONTHINYEARFORMAT = new SimpleDateFormat("MM");
	//values for lengths of arrays
	public static final int TIMEOFDAYRESOLUTION = 288; //number of segments day should be split into
	public static final int HOURFACTOR = TIMEOFDAYRESOLUTION / 24; //used to go between hour and index
	public static final int MINUTEFACTOR = 1440 / TIMEOFDAYRESOLUTION; //used to go between minute and index
	public static final int DAYSINWEEK = 7;
	public static final int DAYSINMONTH = 31;
	public static final int MONTHSINYEAR = 12;
	public static final int DAYSINYEAR = 366;
	
	private String name = ""; //name of contact
	private String number = ""; //phone number of contact
	private int sent = 0; //number of messages sent to contact
	private int received = 0; //number of messages received from contact
	private long firstMessageFloor = 0L; //date of first message at time 00:00:00:0000
	private long finalMessageDate = 0L; //date & time of last message
	private int[] timeOfDay = new int[TIMEOFDAYRESOLUTION]; //number of texts at time of day
	private int[] dayInWeek = new int[DAYSINWEEK]; //number of texts in the day in week, 0 = Monday, 1 = Tuesday, etc.
	private int[] dayInMonth = new int[DAYSINMONTH]; //number of texts in the day in month
	private int[] monthInYear = new int[MONTHSINYEAR]; //number of texts in the month in year, 0 = January, 1 = February, etc.
	private int[][] dayInYear = new int[MONTHSINYEAR][DAYSINMONTH];  //number of texts in the day in year
	private int[] allDays = new int[8096]; //number of texts each day from the first day to last day with message
	private String[] allDaysLabels; //dates for allDays
	private HashMap<String, Integer> words = new HashMap<>(); //unique words and how many times they appear in all texts
	private ArrayList<Message> messages = new ArrayList<>(); //list of all messages
	
	public smsContact(String nm, String nmbr, long date) {
		name = nm;
		number = nmbr;
		Date currentDate = new Date(date);
		int hour = Integer.parseInt(HOURFORMAT.format(currentDate));
		int minute = Integer.parseInt(MINUTEFORMAT.format(currentDate));
		int second = Integer.parseInt(SECONDFORMAT.format(currentDate));
		int milli = Integer.parseInt(MILLIFORMAT.format(currentDate));
		firstMessageFloor = date - (hour * 3600000) - (minute * 60000) - (second * 1000) - milli;
		
	}
	
	public String getName() {
		return name;
	}
	
	public String getNumber() {
		return number;
	}
	
	public int[] getTimeOfDay() {
		return timeOfDay;
	}
	
	public int[] getDayInWeek() {
		return dayInWeek;
	}
	
	public int[] getDayInMonth() {
		return dayInMonth;
	}
	
	public int[] getMonthInYear() {
		return monthInYear;
	}
	
	public int[][] getDayInYear() {
		return dayInYear;
	}
	
	public int[] getAllDays() {
		return allDays;
	}
	
	public String[] getAllDaysLabels() {
		return allDaysLabels;
	}
	
	//returns a list of unique words sorted by incidence
	public ArrayList<WordWithIncidence> getWords() {
		ArrayList<WordWithIncidence> wordsAndIncidence = new ArrayList<>();
		Set<String> allWords = words.keySet();
		for(String currentWord : allWords) {
			wordsAndIncidence.add(new WordWithIncidence(currentWord, words.get(currentWord)));
		}
		Collections.sort(wordsAndIncidence);
		return wordsAndIncidence;
	}
	
	//returns some statistics about the smses with contact
	public String[] getNumbers() {
		String[] numbers = new String[5];
		numbers[0] = "Number of messages: " + messages.size();
		numbers[1] = "Number of messages sent: " + sent + " = " + DF.format((double)sent/(sent + received)*100) + "%";
		numbers[2] = "Number of messages received: " + received + " = " + DF.format((double)received/(sent + received)*100) + "%";
		numbers[3] = "Number of messages per day on average: " + DF.format((double)(sent + received)/allDays.length);
		numbers[4] = "Number of unqiue words: " + words.size();
		return numbers;
	}
	
	//intakes a message to extrapolate data from
	public void addMessage(Message newMessage) {
		checkSent(newMessage.getSent());
		scanBody(newMessage.getBody());
		addToArrays(newMessage.getComputerDate());
		messages.add(newMessage);
		finalMessageDate = newMessage.getComputerDate();
	}
	
	//checks if the message was received or sent
	private void checkSent(boolean wasSent) {
		if(wasSent == true) {
			sent++;
		} else {
			received++;
		}
	}
	
	//parses all words in body of message, some emojis and symbols will show as #xxxxxx
	private void scanBody(String body) {
		Scanner toSeparate = new Scanner(body);
		toSeparate.useDelimiter("[\\s*\\&\\,\\;\\.\\!\\?\\:\"\\$\\(\\)\\*\\/\\+\\-\\=\\>\\<\\@\\^\\_\\~\\[\\]\\{\\}]");
		while(toSeparate.hasNext()) {
			String word = toSeparate.next().toLowerCase();
			if(!word.equals("")) { //not delimited
				Integer value = words.get(word);
				if(value == null) { //not found
					words.put(word, new Integer(1));
				} else { //already exists
					int temp = value.intValue(); //gets old value
					temp++;
					words.put(word, new Integer(temp));
				}
			}
		}
		toSeparate.close();
	}
	
	//uses to date of message to add to arrays
	private void addToArrays(long date) {
		Date currentDate = new Date(date);
		int hour = Integer.parseInt(HOURFORMAT.format(currentDate));
		int minute = Integer.parseInt(MINUTEFORMAT.format(currentDate));
		int dayOfWeek = Integer.parseInt(DAYOFWEEKFORMAT.format(currentDate));
		int dayOfMonth = Integer.parseInt(DAYOFMONTHFORMAT.format(currentDate));
		int month = Integer.parseInt(MONTHINYEARFORMAT.format(currentDate));
		timeOfDay[(hour * HOURFACTOR) + (minute / MINUTEFACTOR)]++;
		dayInWeek[dayOfWeek - 1]++;
		dayInMonth[dayOfMonth - 1]++;
		dayInYear[month - 1][dayOfMonth - 1]++;
		monthInYear[month - 1]++;
		allDays[(int)((date - firstMessageFloor) / ONEDAY)]++;
	}
	
	//does final calculations after data collection
	public void finalCalculations() throws IOException {
		int daysLength = (int)((finalMessageDate - firstMessageFloor) / ONEDAY) + 1; //number of days between first and last message inclusive
		int[] newDays = new int[daysLength]; //shrinks allDays[] if messages span less than 8096 days
		System.arraycopy(allDays, 0, newDays, 0, daysLength);
		allDays = newDays;
		allDaysLabels = new String[allDays.length];
		long currentDate = firstMessageFloor;
		for(int i = 0; i < allDaysLabels.length; i++) {
			allDaysLabels[i] = PRINTDATEFORMAT.format(new Date(currentDate));
			currentDate += ONEDAY;
		}
	}
	
	//creates a txt file backup of all messages with contact
	public void writeToFile(String pth) throws IOException{
		String path = (pth + "\\" + name + "(" + number + ").txt");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8));
		for(int i = 0; i < messages.size(); i++) {
			Message current = messages.get(i);
			//prints date & time, sender, and body
			writer.write("[" + PRINTFORMAT.format(new Date(current.getComputerDate())) + "] "
					+ (current.getSent() ? "You" : name) + ": " + current.getBody() + "\r\n");
		}
		writer.close();
	}
}