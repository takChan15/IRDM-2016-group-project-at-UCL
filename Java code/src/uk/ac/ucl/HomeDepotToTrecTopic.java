package uk.ac.ucl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * A utility class to transform HomeDepot files into TREC topic files formt
 * @author taklumbo
 *
 */
public class HomeDepotToTrecTopic {
/*
 * <top>
<num>201
<title>raspberry pi
<description>What is a raspberry pi?
</top>
 */
	//"id","product_uid","product_title","search_term","relevance"
	String HomeDepotTopicFile;
	String TargetFile;
	
	public static void main(String[] p) throws IOException{
		HomeDepotToTrecTopic trecTopic = new HomeDepotToTrecTopic(
				"/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/train.csv", 
				"/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/trainTrecTopics.txt");
		trecTopic.parse();
	}
	
	public HomeDepotToTrecTopic(String homeDepotTopicFile, String targetFile){
		HomeDepotTopicFile = homeDepotTopicFile;
		TargetFile = targetFile;
	}
	
	public void parse() throws IOException{
		Reader in = new FileReader(HomeDepotTopicFile);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		BufferedWriter writer = new BufferedWriter(new FileWriter(TargetFile));
		
		for (CSVRecord record : records) {
			String topicId = record.get("id").trim();
			if (topicId.equals("")) 
				continue;
			writer.write("<top><num>" + topicId + "</num><title>" + 
					record.get("search_term").replaceAll("&",  "&amp;")
											 .replaceAll("<", "&lt;")
											 .replaceAll(">", "&gt;")
											 .replaceAll("'", "&apos;")
											 .replaceAll("\"", "&quot;")
					.trim() + "</title><description></description></top>");
			writer.newLine();
		}
		in.close();
		writer.flush();
		writer.close();
	}
}
