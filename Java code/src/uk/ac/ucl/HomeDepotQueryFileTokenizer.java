package uk.ac.ucl;

import i2r.hlt.Retrieval;
import i2r.hlt.wrapper.TerrierWrapper;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.terrier.terms.PorterStemmer;


/**
 * This class generates query files that eliminates stop words. It also stems
 * all the words
 * 
 * @author taklumbo
 *
 */
public class HomeDepotQueryFileTokenizer {
	String TargetDir;
	TerrierWrapper terrier;
	String RootDir;
	String DataSet;
	String QueryFile;
	
	public HomeDepotQueryFileTokenizer(String rootDir, String dataSet, String queryFile) throws IOException{
		TargetDir = rootDir + "RawData/";
		
		DataSet = dataSet;
		RootDir = rootDir;
		String terrier_home = rootDir + "terrier-core-4.1/";
		QueryFile = queryFile;
		String prefix = dataSet + "_index"; 
		String path_to_data = rootDir + "data/" + dataSet;
		
		String indexPath = terrier_home+"var/index/";
		
		String lang = "en";
		
		Retrieval ret = new Retrieval(terrier_home, prefix, indexPath, path_to_data, lang);
		terrier = ret.terrier;
	}
	
	public static void main(String[] p) throws IOException{
		//This is the directory where you have your HomeDepot raw data files
		String rootDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/";
		String dataSet = "all";
		String queryFile = "train.csv";
		
		HomeDepotQueryFileTokenizer gen = 
				new HomeDepotQueryFileTokenizer(rootDir, dataSet, queryFile);
		gen.parse();
	}
	
	public void parse() throws IOException{
		generateQueryFile();
	}
	
	private void generateQueryFile() throws IOException{
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(RootDir + "RawData/lsi_" + QueryFile.split("\\.")[0] + "_" + DataSet + ".csv"));
		writer.write("id,product_uid,product_title,search_term,relevance");
		writer.newLine();
		
		//"id","product_uid","product_title","search_term","relevance"
		Reader in = new FileReader(RootDir + "RawData/" + QueryFile);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		for (CSVRecord record : records) {
			String productId = record.get("product_uid").trim();
			if (productId.equals("")) 
				continue;
			writer.write(record.get("id"));
			writer.write("," + record.get("product_uid"));
			writer.write("," + record.get("product_title").replaceAll(",", ""));
			String searchTerm = record.get("search_term").replaceAll(",", "");
			
			if (searchTerm.trim().equals("")){
				int i = 0;
				i++;
			}
			//searchTerm = terrier.tokenizeTerrier(searchTerm);
			//searchTerm = removeStopWordsAndStem(searchTerm).replaceAll(",", "");
			String newSearchTerm = Utils.refactorQueryForTerrier2(searchTerm);
			newSearchTerm = stem(newSearchTerm);
			if (newSearchTerm.trim().equals(""))
				writer.write("," + searchTerm);
			else
				writer.write("," + newSearchTerm);

			if(record.isMapped("relevance"))
				writer.write("," + record.get("relevance"));
			writer.newLine();
		}
		writer.flush();
		writer.close();
		in.close();
	}
	
	PorterStemmer stemmer = new PorterStemmer();
	private String stem(String text) throws IOException{
		StringBuilder cleanedText = new StringBuilder();
		
		String tokenizedText = terrier.tokenizeTerrier(text);
		for(String term:tokenizedText.split(" ")){
			//if(terrier.isStopWord(term))
				//continue;
			
			if (cleanedText.length() > 0)
				cleanedText.append(" ");
			cleanedText.append(stemmer.stem(term));
		}
		
		return cleanedText.toString();
	}
}
