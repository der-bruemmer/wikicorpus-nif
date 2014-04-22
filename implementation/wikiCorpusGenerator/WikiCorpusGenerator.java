package wikiCorpusGenerator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract NIF from Abstract triples including links
 * TODO: External Links are ignored at the moment
 * TODO: links that are plain text are not recognized AND exploded (www. wikipedia. de)
 * TODO: external data like wiki links and spotlight not in there
 * @author Martin Brümmer
 *
 */

public class WikiCorpusGenerator {

	private String abstractText = "";
	private boolean writePrefix = true;
	private int fileNumber = 0;
	
	public WikiCorpusGenerator() {
		
	}
	
	public Set<String> readResources(String fileName) {
		Set<String> resources = new HashSet<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String line;
			
			int count = 0;
		
			while((line = br.readLine()) != null) {  
				resources.add(line.trim());
			}
			br.close();
		} catch(FileNotFoundException fnf) {
			System.out.println("File not found: " + fileName);
		} catch(IOException ioe) {
			System.out.println("Cannot read file: " + fileName);
		} 
		return resources;
	}
	
	public void readFile(String fileName, Set<String> resources) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(fileName));
			String line;
			
			int count = 0;
		
			while((line = br.readLine()) != null) {  
				count ++;
				if(count%1000==0)
					System.out.println(count);
//				if(count>100000) {
//					count = 0;
//					fileNumber++;
//					writePrefix=true;
//				}
				String parse = parseLine(line, resources);
				if(parse!=null)
					writeResources(parse, "/home/martin/Documents/enwiki/out"+fileNumber+".ttl");
				else
					continue;
	    	
			}
			br.close();
		} catch(FileNotFoundException fnf) {
			System.out.println("File not found: " + fileName);
		} catch(IOException ioe) {
			System.out.println("Cannot read file: " + fileName);
		} 
		
		return;
	}
	
	private String parseLine(String line, Set<String> resources) {
		String out = "";
		line = line.replace("<a", "!!!!!!");
		line = line.replace("!!!!!!!!!!!!", "!!!!!! !!!!!!");
		int firstBreak = line.indexOf(" ");
		
		if(firstBreak <=1)
			return null;
		String uri = line.substring(1,firstBreak-1);
		
		if(resources!=null) {
			if(resources.contains(uri.trim())) {
				System.out.println("Found: "+uri);
			} else {
				return null;
			}
		}
		
//		System.out.println("URI:" + uri);
	
		int secondBreak = line.indexOf(" ", firstBreak+1);
		String abstractString = line.substring(secondBreak);
		
//		System.out.println(abstractString.substring(2,abstractString.length()-6));
		this.abstractText = "";
		List<Link> links = getLinkAndText(abstractString);
		int contextEnd = abstractText.length();
		String context = this.makeContext(abstractText, uri, contextEnd);
//		System.out.println(context);
		String words = this.makeWordsFromLinks(links, uri, contextEnd);
		if(!words.isEmpty()) {
			context = context + words;
		}
//			System.out.println(words);
		return context;
	}
	
	private void writeResources(String res, String outFile) {
		try {
			
			BufferedWriter dumpWriter = new BufferedWriter(new FileWriter(outFile, true));	
			if(writePrefix) {
				dumpWriter.append(makePrefixString());
				writePrefix = false;
			}
			dumpWriter.append(res);
			dumpWriter.flush();
			dumpWriter.close();

		} catch (FileNotFoundException fnf) {
			System.out.println("Could not write file "+outFile);
			fnf.printStackTrace();
//			errors++;
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private List<Link> getLinkAndText(String line) {
		line = line.substring(2,line.length()-6);
		if(line.contains("</div>")) 
			line = line.substring(line.indexOf("</div>")+6);

//		System.out.println(line);
		String[] link = new String[2];
		String[] parts = line.split("!!!!!!");

		String abstractText = "";
		List<Link> links = new ArrayList<Link>();
		
		for(int i = 0; i<parts.length; i++) {
			//this is clean text
//			System.out.println("Abstract: "+abstractText);
			if(i%2==0) {
//				System.out.println(parts[i]);
				abstractText+=parts[i];
			} //this is a link 
			else {
				//no link
				if(!parts[i].startsWith("=")&&!parts[i].trim().startsWith("rel=")&&!parts[i].trim().startsWith(" class=")) {
					abstractText+=parts[i];
					for(int j=i;j<parts.length-1;j++) {
						parts[j]=parts[j+1];
					}
					continue;
				}
				Link linkObj = parseLink(parts[i], abstractText.length());
				if(linkObj==null)
					continue;
				abstractText+=linkObj.getLinkText();
				if(linkObj.isDoesExist()){
					links.add(linkObj);
				}
			}
			
		}
		//TODO:maybe remove multiple whitespaces
		this.abstractText = abstractText;
		return links;
	}
	
	private Link parseLink(String linkString, int start) {
		Link link = new Link();
//		System.out.println("LinkString "+linkString);
		
		String word = linkString.substring(linkString.indexOf("!!!!!")+5);
		//something went wrong, use the title attribute instead
		if(!linkString.contains("!!!!!")) {
//			word = linkString.substring(linkString.indexOf("title=\\\"")+8);
//			System.out.println(word);
//			System.out.println(linkString);
//			if(word.contains("("))
//				word = word.substring(0,word.indexOf("(")).trim();
//			linkString = linkString + "!!!!!";
			return null;
		}
		link.setLinkText(word);
			
		//external links excluded
		if(linkString.contains("href")) {
			link.setDoesExist(false);
			return link;
		}	
		
		if(linkString.contains("/mediawiki/")&&!linkString.contains(":")) {
//			System.out.println(linkString);
//			System.out.println(word);
			link.setUri(makeUri(linkString.substring(linkString.indexOf("/mediawiki/"),linkString.indexOf("!!!!!"))));
			link.setDoesExist(true);
		} else {
			link.setDoesExist(false);
		}
		link.setWordStart(start);
		link.setWordEnd(start+word.length());
		return link;
	}
	
	private String makeUri(String linkString) {
		linkString = "http://dbpedia.org/resource/"+linkString.substring(linkString.indexOf("title=")+6);
		//accounting for wrong break after dot by dbpedia extraction framework
		linkString = linkString.replace(". ", ".");
		
		if(linkString.contains(" ")) {
			linkString = linkString.substring(0, linkString.indexOf(" "));
		}
		if(linkString.contains("&")) {
			linkString = linkString.substring(0, linkString.indexOf("&"));
		}
//		System.out.println(linkString);
		return linkString;
	}
	
	public String makePrefixString() {
		String prefix = "";
		prefix += "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n";
		prefix += "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n";
		prefix += "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n";
		prefix += "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";
		prefix += "@prefix nif: <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#> .\n";
		prefix += "@prefix itsrdf: <http://www.w3.org/2005/11/its/rdf#> .\n\n";
		return prefix;
	}
	
	public String makeContext(String text, String url, int contextEnd) {
		String context = "";
		context+="<"+url+"#char=0,"+contextEnd+">\n";
		context+="\ta nif:String , nif:Context , nif:RFC5147String ;\n";
		context+="\tnif:isString \"\"\""+ text +"\"\"\"^^xsd:string;\n";
		context+="\tnif:beginIndex \"0\"^^xsd:nonNegativeInteger;\n";
		context+="\tnif:endIndex \""+ contextEnd +"\"^^xsd:nonNegativeInteger;\n";
		String sourceUri = "http://en.wikipedia.org/wiki"+url.substring(url.lastIndexOf("/"),url.length());
		context+="\tnif:sourceUrl <"+sourceUri+"> .\n\n";
		return context;
	}
	
	public String makeWordsFromLinks(List<Link> links, String contextUri, int contextEnd) {
		String words = "";
		
		for(Link link : links) {
			String turtle = "<"+contextUri+"#char="+link.getWordStart()+","+link.getWordEnd()+">\n";
			turtle+="\ta nif:String , nif:RFC5147String ;\n";
			turtle+="\tnif:referenceContext <"+contextUri+"#char=0,"+contextEnd+"> ;\n";
			turtle+="\tnif:anchorOf \"\"\""+link.getLinkText()+"\"\"\"^^xsd:string ;\n";
			turtle+="\tnif:beginIndex \""+link.getWordStart()+"\"^^xsd:nonNegativeInteger ;\n";
			turtle+="\tnif:endIndex \""+link.getWordEnd()+"\"^^xsd:nonNegativeInteger ;\n";
			
			if(link.getLinkText().split(" ").length>1) { 
				turtle+="\ta nif:Phrase ;\n";
			}
			else {
				turtle+="\ta nif:Word, nif:Phrase ;\n";
			}		
			turtle+="\titsrdf:taIdentRef  <"+link.getUri()+"> .\n\n";

			words += turtle;
		}
			
		return words;
	}
	
	public static void main(String[] args) {
		WikiCorpusGenerator wiki = new WikiCorpusGenerator();
		Set<String> resources = wiki.readResources("/home/martin/Documents/enwiki/flatbreads");
		wiki.readFile("/home/martin/Documents/enwiki/20140402/enwiki-20140402-long-abstracts.nt", resources);
	}
	
}
