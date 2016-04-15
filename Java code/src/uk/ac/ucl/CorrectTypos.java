package uk.ac.ucl;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * This class replaces typos in the queries. It uses a spellchecked list of queries, posted
 * by steubk
 * at https://www.kaggle.com/steubk/home-depot-product-search-relevance/fixing-typos/discussion
 * 
 * @author taklumbo
 *
 */
public class CorrectTypos {
	
	public static void main(String[] p) throws IOException{
		//String queryFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/original_train.csv";
		//String dictionaryFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/dictionary.csv";
		//String resultFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/train.csv";
		
		String queryFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/original_test.csv";
		String dictionaryFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/dictionary.csv";
		String resultFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/test.csv";

		
		Reader queryFileIn = new FileReader(queryFile);
		Iterable<CSVRecord> queryFileRecord = CSVFormat.EXCEL.withHeader().parse(queryFileIn);
		
		Reader dicitonaryIn = new FileReader(dictionaryFile);
		Iterable<CSVRecord> dictionaryFileRecord = CSVFormat.EXCEL.withHeader().parse(dicitonaryIn);
		
		LinkedHashMap<String, String> dictionary = new LinkedHashMap<String, String>();
		for (CSVRecord record : dictionaryFileRecord) {
			if (record.get("originalQuery").trim().equals(""))
				continue;
			dictionary.put(record.get("originalQuery"), record.get("correctedQuery"));
		}
		
		FileWriter csvFileWriter = new FileWriter(resultFile);
        System.out.println("Creating CSVPrinter");
        CSVPrinter csvPrinter = new CSVPrinter(csvFileWriter, CSVFormat.EXCEL);
        
		//"id","product_uid","product_title","search_term","relevance"
		for (CSVRecord record : queryFileRecord) {
			String productId = record.get("product_uid").trim();
			
			if (productId.equals("")) 
				continue;
			
			
			String id = record.get("id").trim();
			csvPrinter.print(id);
			csvPrinter.print(productId);
			
			String productTitle = record.get("product_title");
			csvPrinter.print(productTitle);
			
			String searchTerm = record.get("search_term");
			
			
			if (dictionary.containsKey(searchTerm)){
				searchTerm = dictionary.get(searchTerm);
			}
			csvPrinter.print(searchTerm);
			
			if(record.isMapped("relevance")){
				String relevance = record.get("relevance");
				csvPrinter.print(relevance);
			}
			csvPrinter.println();
		}
		csvPrinter.flush();
		csvPrinter.close();
		dicitonaryIn.close();
		queryFileIn.close();
	}
}
