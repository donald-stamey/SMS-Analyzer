//stores a word and how many times it appeared in a given source
public class WordWithIncidence implements Comparable<WordWithIncidence> {
	private String word;
	private int incidence; //how many times the word appeared
	
	public WordWithIncidence(String word, int count) {
		this.word = word;
		incidence = count;
	}
	
	public String getWord() {
		return word;
	}
	
	public int getCount() {
		return incidence;
	}
	
	public int compareTo(WordWithIncidence otherWord) {
		return new Integer(incidence).compareTo(new Integer(otherWord.getCount()));
	}
}