package uk.ac.ucl;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * This class transform HomeDepot query files (train.csv and test.csv) into
 * standards TREC qrel file format
 * 
 * @author taklumbo
 *
 */
public class HomeDepotToQrel {
/*
201 0 clueweb12-0000tw-05-12114 1
TOPIC ITERATION DOCUMENT# RELEVANCE
 */
	//"id","product_uid","product_title","search_term","relevance"
	String HomeDepotTopicFile;
	String TargetFile;
	
	public static void main(String[] p) throws IOException{
		HomeDepotToQrel trecTopic = new HomeDepotToQrel(
				"/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/test.csv", 
				"/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/test_set_qrel_adhoc.txt");
		trecTopic.parse();
	}
	
	public HomeDepotToQrel(String homeDepotTopicFile, String targetFile){
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
			writer.write(topicId + "\t0\t" + 
					record.get("product_uid").trim());
			if (record.isMapped("relevance"))
				writer.write("\t" + record.get("relevance").trim());
			else
				writer.write("\t-1");
			writer.newLine();
		}
		in.close();
		writer.flush();
		writer.close();
	}
}
