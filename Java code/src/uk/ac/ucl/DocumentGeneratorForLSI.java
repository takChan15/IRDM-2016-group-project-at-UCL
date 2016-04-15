package uk.ac.ucl;

import i2r.hlt.Retrieval;
import i2r.hlt.wrapper.TerrierWrapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.terrier.terms.PorterStemmer;


/**
 * This class generates product files and eliminates stop words. It also stems
 * all the words. Each product is saved in an individual file, with the product
 * id as the name of the file
 * 
 * @author taklumbo
 *
 */
public class DocumentGeneratorForLSI {
	String DescriptionFile;
	String AttribFile;
	String TargetDir;
	LinkedHashMap<String, Product> Products;
	TerrierWrapper terrier;
	String RootDir;
	String DataSet;
	
	boolean addTitle = true;
	boolean addDescription = true;
	boolean addAttrib = true;
	
	public DocumentGeneratorForLSI(String rootDir, String dataSet) throws IOException{
		DescriptionFile = rootDir + "RawData/product_descriptions.csv";
		AttribFile = rootDir + "RawData/attributes.csv";
		TargetDir = rootDir + "data/lsi_" + dataSet;
		
		DataSet = dataSet;
		RootDir = rootDir;
		String terrier_home = rootDir + "terrier-core-4.1/";
		
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
		
		DocumentGeneratorForLSI gen = 
				new DocumentGeneratorForLSI(rootDir, dataSet);
		gen.parse();
	}
	
	public void parse() throws IOException{
		Products = new LinkedHashMap<String, Product>();
		readProductDescriptionFile();
		readAttribFile();
		readQueryFile(RootDir + "RawData/train.csv");
		readQueryFile(RootDir + "RawData/test.csv");
		generateDocuments();
	}
	
	private void generateDocuments() throws IOException{
		File dir = new File(TargetDir);
		for(Product product:Products.values()){
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir, product.Id + ".txt")));
			writer.write(getDocText(product));
			writer.flush();
			writer.close();
		}
	}
	
	private String getDocText(Product product) throws IOException{
		StringBuilder docText = new StringBuilder();
		if(addTitle){
			docText.append(removeStopWordsAndStem(product.Title));
		}
		
		if(addDescription){
			if (docText.length() > 0)
				docText.append("\n");
			docText.append(removeStopWordsAndStem(product.Description));
		}
		
		if (addAttrib){
			for(Attribute attrib:product.Attributes){
				if (docText.length() > 0)
					docText.append("\n");
				docText.append(removeStopWordsAndStem(attrib.Name + " " + attrib.Value));
			}
		}
		return docText.toString();
	}
	
	PorterStemmer stemmer = new PorterStemmer();
	private String removeStopWordsAndStem(String text) throws IOException{
		StringBuilder cleanedText = new StringBuilder();
		
		String tokenizedText = terrier.tokenizeTerrier(text);
		for(String term:tokenizedText.split(" ")){
			if(terrier.isStopWord(term))
				continue;
			if (cleanedText.length() > 0)
				cleanedText.append(" ");
			cleanedText.append(stemmer.stem(term));
		}
		
		return cleanedText.toString();
	}
	
	private void readProductDescriptionFile() throws IOException{
		Reader in = new FileReader(DescriptionFile);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		for (CSVRecord record : records) {
			String productId = record.get("product_uid").trim();
			if (productId.equals("")) 
				continue;
			Product product = new Product();
			product.Id = productId;
			product.Description = record.get("product_description");
			product.Attributes = new ArrayList<Attribute>();
			if (Products.containsKey(product.Id))
				throw new IllegalArgumentException("Id " + product.Id + " already exists");
		    Products.put(product.Id, product);
		}
		in.close();
	}
	
	private void readAttribFile() throws IOException{
		//"product_uid","name","value"
		Reader in = new FileReader(AttribFile);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		Product product = null;
		for (CSVRecord record : records) {
			String productId = record.get("product_uid").trim();
			if(productId.equals(""))
				continue;
			
			if (product == null || !product.Id.equals(productId)){
				product = Products.get(productId);
				if (product == null){
					throw new IllegalArgumentException("Attrib product id not found " + record.toString());
				}
			}
			
			String attribName = "";
			if (!record.get("name").startsWith("Bullet"))
				attribName = record.get("name");
			
			Attribute attrb = new Attribute(attribName, record.get("value"));
			product.Attributes.add(attrb);
		}
		in.close();
	}

	private void readQueryFile(String file) throws IOException{
		//"id","product_uid","product_title","search_term","relevance"
		Reader in = new FileReader(file);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		Product product = null;
		for (CSVRecord record : records) {
			String productId = record.get("product_uid").trim();
			if (productId.equals("")) 
				continue;
			
			if (product == null || !product.Id.equals(productId))
				product = Products.get(productId);
			
			product.Title = record.get("product_title");
		}
		in.close();
	}
}
