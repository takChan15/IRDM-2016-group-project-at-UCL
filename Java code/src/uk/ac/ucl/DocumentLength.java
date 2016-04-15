package uk.ac.ucl;

import i2r.hlt.Retrieval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * This class uses Terrier to measure the length of the product description,
 * title, attributes
 * 
 * @author taklumbo
 *
 */
public class DocumentLength {
	String dataSet; //all, title, desc, attrib
	String rootDir;
	String terrier_home;
	String topicFile;
	String targetDir;
	
	public DocumentLength(String dataSet, String rootDir, String topicFile, String targetDir) throws IOException{
		this.dataSet = dataSet;
		this.rootDir = rootDir;
		terrier_home = rootDir + "terrier-core-4.1/";
		this.topicFile = topicFile;
		this.targetDir = targetDir;
	}
	
	public static void main(String[] p) throws IOException{
		//Settings -----------------------------------------------
		String set = "test";
		String dataSet[] = {"all", "title", "desc", "attrib"}; //all, title, desc, attrib
		//--------------------------------------------------------
		
		String rootDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/";
		String topicFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles/" + set + "_set/MergedFeatures.csv";
		String targetDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles/" + set + "_set/";
		
		for(int i=0; i < dataSet.length; i++){
			DocumentLength myTfIdf = new DocumentLength(dataSet[i], rootDir, topicFile, targetDir);
			myTfIdf.getLengths();
		}
	}
	
	public void getLengths() throws IOException{
		String prefix = dataSet + "_index"; 
		String path_to_data = rootDir + "data/" + dataSet;
		
		String indexPath = terrier_home + "var/index/";
		
		String lang = "en";
		
		Retrieval ret = new Retrieval(terrier_home, prefix, indexPath, path_to_data, lang);
		
		Reader in = new FileReader(topicFile);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(targetDir, "docLength_" + dataSet + ".csv")));
		
		writer.write("queryId,productId,docLength_" + dataSet);
		writer.newLine();
		
		
		HashMap<String, Integer> docLengthMap = new HashMap<String, Integer>();
		
		for (CSVRecord record : records) {
			String topicId = record.get("queryId").trim();
			if (topicId.equals("")) 
				continue;
			
			String productId = record.get("productId").trim();
			
			int docLength;
			if (docLengthMap.containsKey(productId)){
				docLength = docLengthMap.get(productId);
			}else{
				int docId = ret.terrier.getDocIdByDocName(path_to_data + "/" + productId + ".txt");
				docLength = ret.terrier.getDocLength(docId);
				docLengthMap.put(productId, docLength);
			}
			
			writer.write(topicId + "," + productId + "," + docLength);
			writer.newLine();
		}
		in.close();
		writer.flush();
		writer.close();
	}
	
	

}
