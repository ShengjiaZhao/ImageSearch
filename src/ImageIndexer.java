import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.*;   
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import javax.xml.parsers.*; 

public class ImageIndexer {
	private Analyzer analyzer; 
    private IndexWriter indexWriter;
    private float averageLength=1.0f;
    private String imgPath = "C:\\Tomcat7.0\\webapps\\ROOT\\";
    
    public ImageIndexer(String indexDir){
    	analyzer = new IKAnalyzer();
    	try{
    		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
    		Directory dir = FSDirectory.open(new File(indexDir));
    		iwc.setSimilarity(new BM25Similarity());
    		indexWriter = new IndexWriter(dir,iwc);
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
    
    
    public void saveGlobals(String filename){
    	try{
    		PrintWriter pw=new PrintWriter(new File(filename));
    		pw.println(averageLength);
    		pw.close();
    	}catch(IOException e){
    		e.printStackTrace();
    	}
    }
	
	/** 
	 * <p>
	 * index sogou.xml 
	 * 
	 */
	public void indexSpecialFile(String filename){
		try{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();   
			DocumentBuilder db = dbf.newDocumentBuilder();    
			org.w3c.dom.Document doc = db.parse(new File(filename));
			NodeList nodeList = doc.getElementsByTagName("pic");
			for(int i=0;i<nodeList.getLength();i++){
				Node node=nodeList.item(i);
				NamedNodeMap map=node.getAttributes();
				Node locate=map.getNamedItem("locate");
				Node bigClass=map.getNamedItem("bigClass");
				Node smallClass=map.getNamedItem("smallClass");
				Node query=map.getNamedItem("query");
				String absString=bigClass.getNodeValue()+" "+smallClass.getNodeValue()+" "+query.getNodeValue();
				
				Document document  =   new  Document();
				Field PicPathField  =   new  Field( "picPath" ,locate.getNodeValue(),Field.Store.YES, Field.Index.NO);
				Field abstractField  =   new  Field( "abstract" ,absString,Field.Store.YES, Field.Index.ANALYZED);
				averageLength += absString.length();
				document.add(PicPathField);
				document.add(abstractField);
				
				// Add the contents of the html file
				String pathString = locate.getNodeValue();
				pathString = pathString.replace(".jpg", ".html");
				
				File fileDir = new File(imgPath + pathString);
				while (fileDir.exists() && !fileDir.isDirectory()) {	// Use while here to facilitate break
					Pattern p = Pattern.compile("charset\\s*=\\s*([0-9a-zA-Z]+)");
					Matcher m = p.matcher(new String(Files.readAllBytes(Paths.get(imgPath + pathString))));
					String encoding;
					if (m.find()) {
						String encodingStr = m.group(1);
						if (encodingStr.equalsIgnoreCase("gb2312") ||
								encodingStr.equalsIgnoreCase("gbk")) {
							encoding = "GBK";
						} else if (encodingStr.equalsIgnoreCase("utf") ||
								encodingStr.equalsIgnoreCase("utf8")) {
							encoding = "UTF8";
						} else {
							break;
						}
					} else {
						break;
					}
					
					BufferedReader in = new BufferedReader(new InputStreamReader(
								new FileInputStream(fileDir), encoding));
	
					String totalStr = "", str;
					while ((str = in.readLine()) != null) {
					    //System.out.println(str);
						int beginIndex = -1;
						for (int j = 0; j < str.length();) {
					        int codepoint = str.codePointAt(j);				        
					        if (codepoint >= 0x4e00 && codepoint <= 0x9fa5) {//aracter.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN) {
					            if (beginIndex < 0)
					            	beginIndex = j;
					        } else {
					        	if (beginIndex >= 0) {
					        		if (j - beginIndex >= 4)
					        			totalStr += str.substring(beginIndex, j).trim() + " ";
					        		beginIndex = -1;
					        	}
					        }
					        j += Character.charCount(codepoint);
						}
					}
					Field htmlTextField = new Field("htmlText" , 
							totalStr ,Field.Store.YES, Field.Index.ANALYZED);
					averageLength += totalStr.length();
					document.add(htmlTextField);
					in.close();
					break;
				}
				

				indexWriter.addDocument(document);
				if(i%2000==0){
					System.out.println("process "+i);
				}
				//TODO: add other fields such as html title or html content 
				
			}
			averageLength /= indexWriter.numDocs();
			System.out.println("average length = "+averageLength);
			System.out.println("total "+indexWriter.numDocs()+" documents");
			indexWriter.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		ImageIndexer indexer=new ImageIndexer("forIndex/index");
		indexer.indexSpecialFile("input/sogou-utf8.xml");
		indexer.saveGlobals("forIndex/global.txt");
	}
}
