package uk.ac.ucl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import edu.stanford.nlp.process.Morphology;

/**
 * A class to generate BrandMatches feature. 
 * 
 * In the method "main" there are some directory setting you need to change.
 * Then you can run it. It will generate for you the feature file
 * 
 * @author taklumbo
 *
 */
public class FeatureGenerator {
	final static String NO_MATCH_QUERY_PRODUCT_BRANDS = "0";
	final static String NO_BRAND_IN_QUERY = "1";
	final static String MATCH_QUERY_PRODUCT_BRANDS = "2";
	
	String DescriptionFile;
	String AttribFile;
	String QueryFile;
	String TargetDir;
	String BrandsFile;
	LinkedHashMap<String, Product> Products;
	LinkedHashMap<Integer, Query> Queries;
	HashSet<String> Brands;
	
	public FeatureGenerator(String descriptionFile, String attribFile, String queryFile, 
			String targetDir, String brandsFile){
		DescriptionFile = descriptionFile;
		AttribFile = attribFile;
		QueryFile = queryFile;
		TargetDir = targetDir;
		BrandsFile = brandsFile;
	}
	
	public static void main(String[] p) throws IOException{
		//This is the directory where you have your HomeDepot raw data files
		String dir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/";
		
		//This is the directory where the feature file will be saved
		String targetDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles";
		
		String brandsFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/Brands.txt";
		
		String descriptionFile = dir + "product_descriptions.csv";
		String attribFile = dir + "attributes.csv";
		
		//test.csv or train.csv
		String queryFile = dir + "train.csv"; 
		
		FeatureGenerator gen = 
				new FeatureGenerator(descriptionFile, attribFile, queryFile, targetDir, brandsFile);
		gen.parse();
	}
	
	public void parse() throws IOException{
		Products = new LinkedHashMap<String, Product>();
		Queries = new LinkedHashMap<Integer, Query>();
		readProductDescriptionFile();
		readAttribFile();
		readQueryFile(QueryFile);
		readBrandFile();
		generateFeatureFile();
	}
	
	private void readBrandFile() throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(BrandsFile));
		Brands = new HashSet<String>();
		String line;
		while((line = reader.readLine()) != null){
			if (line.trim().equals(""))
				continue;
			Brands.add(line.replaceAll(",", ""));
		}
		reader.close();
	}
	
	private void generateFeatureFile() throws IOException{
		ArrayList<LinkedHashMap<String, String>> rows = new ArrayList<LinkedHashMap<String,String>>();
		File dir = new File(TargetDir);
		String fileName = new File(QueryFile).getName().replaceAll(".csv", "");
		fileName = "Features_" + fileName + ".csv";
		File dataFile = new File(dir, fileName);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
			    new FileOutputStream(dataFile), "UTF-8"));
		
		int i = 1;
		LinkedHashSet<String> brands = new LinkedHashSet<String>();
		for(Entry<Integer, Query> entry:Queries.entrySet()){
			Query query = entry.getValue();
			Product product = Products.get(query.productId);

			LinkedHashMap<String, String> aRow = new LinkedHashMap<String, String>();
			aRow.put("queryId", String.valueOf(query.queryId));
			aRow.put("productId", query.productId);
			aRow.put("originalQuery", query.searchTerm.replaceAll(",", ""));
			
			String productBrand = getBrand(product);
			aRow.put("productBrand", productBrand);
			
			//aRow.put("numberOfWordsInQuery", getNumberOfWordsInQuery(query, product));
			//aRow.put("numberOfMatchedWords_query_productTitle", getNumberOfMatchedWords_query_productTitle(query, product));
			
			String searchTerms = query.searchTerm.toLowerCase().replaceAll(",", "");
			String query_product_brand_matching; 
			String brandInQuery = queryContainsBrand(searchTerms);
			if (!brandInQuery.equals("")) {
				if (matchBrand(searchTerms, productBrand)){
					query_product_brand_matching = MATCH_QUERY_PRODUCT_BRANDS;
				}else{
					query_product_brand_matching = NO_MATCH_QUERY_PRODUCT_BRANDS;
				}
			}else{
				query_product_brand_matching = NO_BRAND_IN_QUERY;
			}
			aRow.put("brandInQuery", brandInQuery);
			aRow.put("brandMatches", query_product_brand_matching);

			//Relevance is not given for test.csv
			if (query.relevance != -1)
				aRow.put("y", String.valueOf(query.relevance)); //This is the target variable
			rows.add(aRow);
			
			if (i == 1){
				StringBuilder header = new StringBuilder();
				for(String columnName:aRow.keySet()){
					if(header.length() > 0)
						header.append(",");
					header.append(columnName);
				}
				
				writer.write(header.toString());
				writer.newLine();
			}
			
			writeFeaturesToFile(aRow, writer);
			
			brands.add(productBrand);
			i++;
		}
		writer.flush();
		writer.close();
	}
	
	private boolean matchBrand(String query, String brand){
		String[] queryTerms = query.split(" ");
		for(String term:queryTerms){
			if (brand.contains(term))
				return true;
		}
		
		return false;
	}
	
	private String queryContainsBrand(String query){
		String[] queryFields = query.split(" ");
		int queryLength = queryFields.length;
		
		for(String aBrand:Brands){
			int aBrandLength = aBrand.replaceAll(",", "").split(" ").length;
			if (aBrandLength > queryLength)
				continue;
			
			if(aBrandLength == 1){
				//For one word brand, we must find whole word match to avoid false positive
				//boolean match = Pattern.matches("(^| )" + aBrand + "( |$)", query);
				boolean match = false;
				for(String field:queryFields){
					if (field.equals(aBrand)){
						match = true;
						break;
					}
				}
				
				if (match == true)
					return aBrand;
			}else if (query.contains(aBrand))
				return aBrand;			
		}
		return "";
	}
	
	static Pattern brandPattern = Pattern.compile("^(\\D+)");
	private String getBrand(Product product){
		String brand = "";
		for(Attribute attr:product.Attributes){
			if (attr.Name.equals("MFG Brand Name")){
				brand = attr.Value.toLowerCase().replaceAll(",", "");
			}
		}
		
		//Matcher matcher = brandPattern.matcher(product.Title);
		//if (matcher.find()){
			//brand = matcher.group(1).toLowerCase();
		//}
		
		if (brand.equals("unbranded") || brand.equals("n/a"))
			brand = "";
		
		String[] fields = brand.split(" ");
		if (fields.length <= 3)
			return brand;
		else{
			return fields[0] + " " + fields[1] + " " + fields[2];
		}
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
			
			double relevance = -1;
			if (record.isMapped("relevance")){
				relevance = Double.parseDouble(record.get("relevance"));
			}
			Query query = new Query(Integer.parseInt(record.get("id")), 
								    productId, 
								    record.get("product_title"), 
								    record.get("search_term"),
								    relevance);
			Queries.put(Integer.parseInt(record.get("id")), query);
		}
		in.close();
	}
	
	private void writeFeaturesToFile(ArrayList<LinkedHashMap<String, String>> rows) throws IOException{
		File dir = new File(TargetDir);
		String fileName = new File(QueryFile).getName().replaceAll(".csv", "");
		fileName = "Features_" + fileName + ".csv";
		File dataFile = new File(dir, fileName);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
			    new FileOutputStream(dataFile), "UTF-8"));
		
		StringBuilder header = new StringBuilder();
		for(String columnName:rows.get(0).keySet()){
			if(header.length() > 0)
				header.append(",");
			header.append(columnName);
		}
		
		writer.write(header.toString());
		writer.newLine();
		
		for(LinkedHashMap<String, String> aRow:rows){
			StringBuilder values = new StringBuilder();
			for(String value:aRow.values()){
				if(values.length() > 0)
					values.append(",");
				values.append(value);
			}
			writer.write(values.toString());
			writer.newLine();
		}
		
		writer.flush();
		writer.close();
	}
	
	private void writeFeaturesToFile(LinkedHashMap<String, String> aRow, BufferedWriter writer) 
			throws IOException{

		StringBuilder values = new StringBuilder();
		for(String value:aRow.values()){
			if(values.length() > 0)
				values.append(",");
			values.append(value);
		}
		writer.write(values.toString());
		writer.newLine();
	}
	
	private String getNumberOfWordsInQuery(Query query, Product product){
		return String.valueOf(query.searchTerm.split(" ").length);
	}

	private String getNumberOfMatchedWords_query_productTitle(Query query, Product product){
		List<String> queryWords = getStemmedWords(query.searchTerm.split(" "));
		List<String> productTitleWords = getStemmedWords(query.productTitle.split(" "));
		
		int count = 0;
		for(String aQueryWord:queryWords){
			if (productTitleWords.contains(aQueryWord))
				count++;
		}
		
		return String.valueOf(count);
	}
	
	Morphology morphology = new Morphology();
	private List<String> getStemmedWords(String[] words){
		ArrayList<String> stemmedWords = new ArrayList<String>();
		for(String aWord:words){
			stemmedWords.add(morphology.stem(aWord));
		}
		return stemmedWords;
	}
}
