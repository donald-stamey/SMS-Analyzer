import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

//takes an sms backup file and can create more readable backups and can perform statistical analysis
public class ScanTexts {
	private static ArrayList<String> contactNumbers = new ArrayList<>(); //phone numbers of all unique contacts
	private static ArrayList<smsContact> contacts = new ArrayList<>(); //all unique contacts, in same order as contactNumbers
	private static int index; //index inside a read line
	
	public static void main(String[] args) throws IOException {
		readFile();
    }
	
	//reads an sms backup file, and executes all necessary commands
	private static void readFile() throws IOException {
		BufferedReader file = getFile();
		if(file == null) {
			return;
		}
		for(int i = 0; i < 8; i++) { //goes through header
			file.readLine();
		}
        boolean done = false;
        while(file.ready() && !done) { //scans whole file
        	done = checkIfDone(file.readLine());
        }
        boolean backup = askYesNo("Would you like to generate .txt file backups of all sms messages?");
        String backupPath = null;
        if(backup) {
        	backupPath = getBackupDir();
        	if(backupPath == null) {
        		backup = false;
        	}
        }
        for(int i = 0; i < contacts.size(); i++) {
        	smsContact current = contacts.get(i);
        	current.finalCalculations();
        	if(backup) {
        		current.writeToFile(backupPath);
        	}
        }
        if(askYesNo("Would you like to generate an excel file containing statistical analyses of your sms messages?")) {
        	smsStatsToExcel.createExcel(contacts);
        }
        JOptionPane.showMessageDialog(null, "Program complete.", "SMS ANALYZER", JOptionPane.INFORMATION_MESSAGE);
        file.close();
	}
	
	
	//gets the original backup file
	private static BufferedReader getFile() throws IOException {
		File file = chooseFileDir("Select the backup file", JFileChooser.FILES_ONLY);
		if(file == null) {
			return null;
		}
		return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }
	
	//gets the directory to put the new backups into
	private static String getBackupDir() {
		File file = chooseFileDir("Select the directory to save the backups. Creates new directory \"Backups\".", JFileChooser.DIRECTORIES_ONLY);
		if(file == null) {
			return null;
		}
		File backupFolder = new File(file.getAbsolutePath() + "\\Backups");
		backupFolder.mkdir();
		return backupFolder.getAbsolutePath();
	}
	
	//lets the user choose a file or directory
	public static File chooseFileDir(String title, int mode) {
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle(title);
		fc.setFileSelectionMode(mode);
		fc.setPreferredSize(new Dimension(1000, 600));
		if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		} else {
			if(askYesNo("Try again?")){
				return chooseFileDir(title, mode);
			} else {
				return null;
			}
		}
	}
	
	//returns true when end of sms backup is reached
	private static boolean checkIfDone(String line) {
		if(line.charAt(3) != 's') {
			return true;
		} else { //not done
			parse(line);
			return false;
		}
	}
	
	//returns a boolean for a yes or no question, yes = true & no = false
	private static boolean askYesNo(String question) {
		int choice = JOptionPane.showConfirmDialog(null, question, "SMS ANALYZER", JOptionPane.YES_NO_OPTION);
		if(choice == JOptionPane.YES_OPTION) {
			return true;
		} else if(choice == JOptionPane.NO_OPTION) {
			return false;
		} else {
			return askYesNo(question);
		}
	}
	
	//parses a line from the sms backup
	private static void parse(String line) {
		String address = parseAddress(line);
		long computerDate = parseComputerDate(line);
		boolean sent = parseSent(line);
		String body = parseBody(line);
		String contact = parseName(line);
		
		int contactIndex = contactNumbers.indexOf(address); //checks where contact is
		if(contactIndex == -1) { //contact not found, so add a new one
			smsContact newContact = new smsContact(contact, address, computerDate);
			contactIndex = contactNumbers.size();
			contactNumbers.add(address);
			contacts.add(newContact);
		}
		contacts.get(contactIndex).addMessage(new Message(computerDate, sent, body)); //adds new message to the specific smsContact
	}
	
	//returns the phone number of the other person
	private static String parseAddress(String line) {
		index = 29; //goes to start of phone number
		StringBuilder address = new StringBuilder();
		if(line.substring(index, index + 4).equals("null")) { //no phone number
			return "0000000000";
		}
		while(line.charAt(index) != '"') {
			if(line.charAt(index) >= '0' && line.charAt(index) <= '9') { //only adds numbers
				address.append(line.charAt(index));
			}
			index++;
		}
		index++;
		return address.substring(address.length() - 10).toString(); //gets rid of country code
	}
	
	//returns UNIX date of the message
	private static long parseComputerDate(String line) {
		while(line.charAt(index) != '"') { //goes to start of date
			index++;
		}
		index++;
		
		StringBuilder computerDate = new StringBuilder();
		while(line.charAt(index) != '"') {
			computerDate.append(line.charAt(index));
			index++;
		}
		return Long.parseLong(computerDate.toString());
	}
	
	//returns true if sent message, false if received
	private static boolean parseSent(String line) {
		index += 8; //goes to sent character
		return (line.charAt(index) == '2');
	}
	
	//returns body of message
	private static String parseBody(String line) {
		index+= 23; //goes to start of body
		char limitChar = line.charAt(index); //grabs character that bounds the body, either ' or "
		index++;
		StringBuilder body = new StringBuilder();
		while(line.charAt(index) != limitChar) {
			body.append(line.charAt(index));
			index++;
		}
		return body.toString();
	}
	
	//returns the contact name of the other person
	private static String parseName(String line) {
		while(line.charAt(index) != 'm') { //goes to near start of name
			index++;
		}
		index += 4;
		StringBuilder contact = new StringBuilder();
		while(line.charAt(index) != '"') {
			contact.append(line.charAt(index));
			index++;
		}
		return contact.toString();
	}
}