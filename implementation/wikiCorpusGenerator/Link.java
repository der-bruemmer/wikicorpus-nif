package wikiCorpusGenerator;

public class Link {

	private String uri = "";
	private String linkText = "";
	private int wordStart = 0;
	private int wordEnd = 0;
	private boolean doesExist = false;
	
	public Link() {
		
	}

	public boolean isDoesExist() {
		return doesExist;
	}

	public void setDoesExist(boolean doesExist) {
		this.doesExist = doesExist;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getLinkText() {
		return linkText;
	}

	public void setLinkText(String linkText) {
		this.linkText = linkText;
	}

	public int getWordStart() {
		return wordStart;
	}

	public void setWordStart(int wordStart) {
		this.wordStart = wordStart;
	}

	public int getWordEnd() {
		return wordEnd;
	}

	public void setWordEnd(int wordEnd) {
		this.wordEnd = wordEnd;
	}
	
}
