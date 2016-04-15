package uk.ac.ucl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

public class TrecTopic {
	int topicId;
	String query;
	
	public TrecTopic(int topicId, String query){ //, double score, double normalizedScore){
		this.topicId = topicId;
		this.query = query;
	}
	
	public static List<TrecTopic> loadTopicsFromFile(String fileName) throws JDOMException, IOException{
		SAXBuilder builder = new SAXBuilder();
		String str = FileUtils.readFileToString(new File(fileName), "utf-8");
		
		StringReader sReader = new StringReader("<root>" + str + "</root>");
		Document document = (Document) builder.build(sReader);
		Element rootNode = document.getRootElement();
		Namespace ns = rootNode.getNamespace();
		
		ArrayList<TrecTopic> topics = new ArrayList<TrecTopic>();
		
		//<top><num>2</num><title>angle bracket</title><description></description></top>
		for(Element topNode:rootNode.getChildren("top", ns)){
			TrecTopic aTopic = new TrecTopic(Integer.valueOf(topNode.getChildText("num", ns)),
					topNode.getChildText("title", ns));
				topics.add(aTopic);
		}
		return topics;
	}
	
	public static void main(String[] p) throws JDOMException, IOException{
		List<TrecTopic> topics = loadTopicsFromFile(
				"/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/trainTrecTopics.txt");
	}
}
