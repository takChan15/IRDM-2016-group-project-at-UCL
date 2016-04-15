package uk.ac.ucl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/**
 * A utility to transform HomeDepot files into Trec data files format
 * 
 * @author taklumbo
 *
 */
public class HomeDepotToTrecDataFile {
	String DescriptionFile;
	String AttribFile;
	String TrainFile;
	String TestFile;
	String TargetDir;
	LinkedHashMap<String, Product> Products;
	static String prefix;
	
	static boolean IncludeTitle = false;
	static boolean IncludeDescription = false;
	static boolean IncludeAttrib = true;
	
	public HomeDepotToTrecDataFile(String descriptionFile, String attribFile, String trainFile,
			String testFile, String targetDir){
		DescriptionFile = descriptionFile;
		AttribFile = attribFile;
		TrainFile = trainFile;
		TestFile = testFile;
		TargetDir = targetDir;
	}
	
	static{
		if (IncludeTitle && IncludeDescription && IncludeAttrib){
			prefix = "all";
		}else if (IncludeTitle){
			prefix = "title";
		}else if (IncludeDescription){
			prefix = "desc";
		}else if (IncludeAttrib){
			prefix = "attrib";
		}
	}
	
	//&nbsp;
	//sidewalks100%
	//informationRevives
	public static void main(String[] p) throws IOException{
		String dir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/RawData/";
		String descriptionFile = dir + "product_descriptions.csv";
		String attribFile = dir + "attributes.csv";
		String trainFile = dir + "train.csv";
		String testFile = dir + "test.csv";
		String targetDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/data/" + prefix + "/";
		
		HomeDepotToTrecDataFile homeD = 
				new HomeDepotToTrecDataFile(descriptionFile, attribFile, trainFile, testFile, targetDir);
		homeD.parse();
	}
	
	public void parse() throws IOException{
		Products = new LinkedHashMap<String, Product>();
		readProductDescriptionFile();
		readAttribFile();
		readQueryFile(TrainFile);
		readQueryFile(TestFile);
		generateDataFiles();
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
	
	static String part1 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head>";
	static String part2 = "<meta http-equiv=\"Content-Language\" content=\"en-us\" />  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /> </head> <body>";
	static String part3 = " </body></html>";
	private void generateDataFiles() throws IOException{
		File dir = new File(TargetDir);
		for(Entry<String, Product> entry:Products.entrySet()){
			File dataFile = new File(dir, entry.getKey() + ".txt");
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				    new FileOutputStream(dataFile), "UTF-8"));
			writer.write(part1);
			writer.newLine();
			
			Product product = entry.getValue();
			if (IncludeTitle){
				writer.write("<title>" + product.Title + "</title>");
				writer.newLine();
			}else{
				writer.write("<title></title>");
				writer.newLine();
			}
			
			writer.write(part2);
			writer.newLine();
			
			if (IncludeDescription){
				writer.write("<h1>" + product.Description + "</h1>");
				writer.newLine();
			}
			
			if (IncludeAttrib){
				for(Attribute attrib:product.Attributes){
					writer.write("<h2>" + attrib.Name + " " + attrib.Value + "</h2>");
					writer.newLine();
				}
			}
			writer.write(part3);
			writer.flush();
			writer.close();
		}
	}
	

}
