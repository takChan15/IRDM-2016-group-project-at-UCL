package uk.ac.ucl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import i2r.hlt.Retrieval;

/**
 * A class that generate TFIDF scores for every pair of expanded query and product.
 * Terrier is used
 * 
 * @author taklumbo
 *
 */
public class MyExpQuery_TF_IDF {
	String dataSet; //all, title, desc, attrib
	String rootDir;
	String terrier_home;
	String topicFile;
	String targetDir;
	
	public MyExpQuery_TF_IDF(String dataSet, String rootDir, String topicFile, String targetDir) throws IOException{
		this.dataSet = dataSet;
		this.rootDir = rootDir;
		terrier_home = rootDir + "terrier-core-4.1/";
		this.topicFile = topicFile;
		this.targetDir = targetDir;
	}
	
	public static void main(String[] p) throws IOException{
		//Settings -----------------------------------------------
		String set = "train";
		String dataSet[] = {"all"}; //all, title, desc, attrib
		//--------------------------------------------------------
		
		String rootDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/";
		String topicFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles/" + set + "_set/MergedFeatures.csv";
		String targetDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles/" + set + "_set/";
		
		for(int i=0; i < dataSet.length; i++){
			MyExpQuery_TF_IDF myTfIdf = new MyExpQuery_TF_IDF(dataSet[i], rootDir, topicFile, targetDir);
			myTfIdf.getScores();
		}
	}
	
	public void getScores() throws IOException{
		String prefix = dataSet + "_index"; 
		String path_to_data = rootDir + "data/" + dataSet;
		
		String indexPath = terrier_home + "var/index/";
		
		String lang = "en";
		
		Retrieval ret = new Retrieval(terrier_home, prefix, indexPath, path_to_data, lang);
		//String productId = "100023";
		//String docName = path_to_data + "/" + productId + ".txt";
		//String query = "CONCRETE & MASONRY CLEANER & ETCHER";
		//System.out.println(Utils.calculateTdIdf(ret.terrier, Utils.refactorQueryForTerrier2(query), docName));
		
		Reader in = new FileReader(topicFile);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(targetDir, "myTf_Idf_expQuery_" + dataSet + ".csv")));
		
		//"id","product_uid","product_title","search_term","relevance"
		writer.write("queryId,productId,norm_myTfIdf_expQuery_" + dataSet);
		writer.newLine();
		
		for (CSVRecord record : records) {
			String topicId = record.get("queryId").trim();
			if (topicId.equals("")) 
				continue;
			
			String productId = record.get("productId").trim();
			String expQuery = Utils.refactorQueryForTerrier2(record.get("expQuery_" + dataSet).trim());
			
			double norm_tf_idf = 0.0;
			if (expQuery.trim().length() > 0){
				norm_tf_idf = Utils.calculateTf_Idf(ret.terrier, expQuery, path_to_data + "/" + productId + ".txt");
				norm_tf_idf = norm_tf_idf / (expQuery.split(" ").length);
			}
			
			writer.write(topicId + "," + productId + "," + norm_tf_idf);
			writer.newLine();
		}
		in.close();
		writer.flush();
		writer.close();
	}
}
