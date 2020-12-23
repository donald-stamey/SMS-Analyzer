//class for storing important information of an sms message
public class Message {
	private long computerDate; //stores date & time in UNIX
	private boolean sent; //true if message was sent, false if received
	private String body; //body of sms message
	
	public Message(long compDate, boolean snt, String bdy) {
		computerDate = compDate;
		sent = snt;
		body = bdy;
	}
	
	public long getComputerDate() {
		return computerDate;
	}
	
	public boolean getSent() {
		return sent;
	}
	
	public String getBody() {
		return body;
	}
}