package uk.ac.ucl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/***
 * The feature enginerring step was not straightforward. We had to test 
 * the model and create additional features. Some features were expensive
 * to generate, so we created separate classes to generate them in 
 * individual files.
 * 
 * This class merges the individual features files into one
 * 
 * @author taklumbo
 *
 */
public class FeatureFilesMerger {
	String sourceDir;
	String targetFile;
	List<String> exceptionFiles;
	List<String> featuresToAdd;
	
	public FeatureFilesMerger (String sourceDir, String targetFile,
			List<String> exceptionFiles, List<String> featuresToAdd){
		this.sourceDir = sourceDir;
		this.targetFile = targetFile;
		this.exceptionFiles = exceptionFiles;
		this.featuresToAdd = featuresToAdd;
	}
	
	public static void main(String[] p) throws IOException{
		String set = "test";
		
		String sourceDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles/" + set +"_set/";
		String targetFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles/" + set +"_set/MergedFeatures.csv";
		List<String> filesToMerge = new ArrayList<String>();
		filesToMerge.add("Features.csv");
		filesToMerge.add("all_features.csv");
		filesToMerge.add("desc_features.csv");
		filesToMerge.add("attrib_features.csv");
		filesToMerge.add("title_features.csv");
		filesToMerge.add("products.csv");
		
		filesToMerge.add("myTf_Idf_all.csv");
		filesToMerge.add("myTf_Idf_title.csv");
		filesToMerge.add("myTf_Idf_desc.csv");
		filesToMerge.add("myTf_Idf_attrib.csv");
		
		filesToMerge.add("myTf_Idf_expQuery_all.csv");
		filesToMerge.add("myTf_Idf_expQuery_title.csv");
		filesToMerge.add("myTf_Idf_expQuery_desc.csv");
		filesToMerge.add("myTf_Idf_expQuery_attrib.csv");

		filesToMerge.add("docLength_all.csv");
		filesToMerge.add("docLength_title.csv");
		filesToMerge.add("docLength_desc.csv");
		filesToMerge.add("docLength_attrib.csv");
		
		filesToMerge.add("lsi_all.csv");
		
		String[] featuresToAdd = {"queryId","productId","productTitle", "originalQuery","refactoredQuery","expQuery_all",
				"expQuery_title","expQuery_desc","expQuery_attrib","productBrand","brandInQuery","sizeOfQuery",
				"brandMatches", "modelScore_all", "minMaxModelScore_all","modelZScore_all","productRank_all",
				"sizeOfExpQuery_all","ratioNumberOfQueryTermsIn_all","ratioNumberOfExpQueryTermsIn_all", 
				"modelScore_title", "minMaxModelScore_title","modelZScore_title","productRank_title",
				"sizeOfExpQuery_title","ratioNumberOfQueryTermsIn_title","ratioNumberOfExpQueryTermsIn_title", 
				"modelScore_desc", "minMaxModelScore_desc","modelZScore_desc","productRank_desc","sizeOfExpQuery_desc",
				"ratioNumberOfQueryTermsIn_desc","ratioNumberOfExpQueryTermsIn_desc","modelScore_attrib", 
				"minMaxModelScore_attrib","modelZScore_attrib","productRank_attrib","sizeOfExpQuery_attrib",
				"ratioNumberOfQueryTermsIn_attrib","ratioNumberOfExpQueryTermsIn_attrib","myTfIdf_all", 
				"myTfIdf_title", "myTfIdf_desc", "myTfIdf_attrib", "norm_myTfIdf_expQuery_all", "norm_myTfIdf_expQuery_title", 
				"norm_myTfIdf_expQuery_desc", "norm_myTfIdf_expQuery_attrib", "docLength_all", "docLength_title", 
				"docLength_desc", "docLength_attrib", "sim_score_all", "sim_rank_all", "y"};
		
		List<String> featuresToAddList = Arrays.asList(featuresToAdd);

		FeatureFilesMerger merger = 
				new FeatureFilesMerger(sourceDir, targetFile, filesToMerge, featuresToAddList);
		merger.merge();
		int s = 1;
		s++;
	}
	
	public void merge() throws IOException{
		ArrayList<Iterator<CSVRecord>> iteratorList = getIteratorList(sourceDir, exceptionFiles);
		BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile));
		
		appendHeader(featuresToAdd, writer);
		
		int i = 0;
		while(iteratorList.get(0).hasNext()){
			i++;
			//I'm assuming that all files have the same number of lines
			ArrayList<CSVRecord> records = new ArrayList<CSVRecord>();
			
			for(Iterator<CSVRecord> iterator:iteratorList){
				records.add(iterator.next());
			}
			
			appendRecord(records, writer, featuresToAdd);
		}
		writer.flush();
		writer.close();
		System.out.println(i);
	}
	
	public static void appendHeader(List<String> featuresToAdd, BufferedWriter writer) throws IOException{
		StringBuilder line = new StringBuilder();
		
		for(String header:featuresToAdd){
			if(line.length() > 0)
				line.append(",");
			line.append(header);
		}
		writer.write(line.toString());
		writer.newLine();
	}
	
	public static void appendRecord(ArrayList<CSVRecord> records, BufferedWriter writer, 
			List<String> featuresToAdd) throws IOException{
		//This map is used to filter our duplicated columns in the different files
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		
		for(CSVRecord aRecord:records){
			//Concatenate each lines of each file
			for(Entry<String, String> entry:aRecord.toMap().entrySet()){
				if (entry.getKey().equals("queryId") && map.containsKey("queryId")){
					if (!entry.getValue().equals(map.get("queryId"))){
						throw new IllegalArgumentException("Different queryId: " + entry.getValue() + 
								" != " + map.get("queryId"));
					}
				}
				String value = entry.getValue();
				
				if (entry.getKey().equals("sizeOfQuery")){
					if (value.equals("34")){
						//A temporary fix for queries with more than one space between word.
						//Already fixed in TerrierFeatureGenerator, but don't want to run it again now
						value = "4";
					} else if (value.equals("22")){
						value = "5";
					} else if (value.equals("m~m18 18-volt lithium-ion cordless 3/8 in. impact")){
						value = "7";
					} else if (value.equals("m~ultralight 1/2 in. x 4 ft. x 8 ft. gypsum board~")){
						value = "11";
					} else if (value.equals("pavers: floors for your outdoors")){
						value = "5";
					}else if (value.equals("screen doors: screen tight doors 32 in. unfinished wood t-ba")){
						value = "10";
					}
					//} else if ()
				}else if (entry.getKey().startsWith("productRank_") && value.equals("0.0")){
					//I decided that when terrier doesn't return the product, it should be
					//101 rather than 0. So, the greater the number, the lower the ranking score
					value = "101";
				}else if (entry.getKey().startsWith("modelScore_") && (value.trim().equals("0")) ||
						value.trim().equals("-Infinity")){
					value = "0.0";
				}
				map.put(entry.getKey(), value);
			}
		}
		
		StringBuilder line = new StringBuilder();
		for(String header:featuresToAdd){
			if (!map.containsKey(header)){
				if (header.equals("y"))
					continue;
				else
					throw new IllegalArgumentException("Header " + header + " not found");
			}
			if(line.length() > 0)
				line.append(",");
			
			line.append(map.get(header).replaceAll(",", ""));
		}
		writer.write(line.toString());
		writer.newLine();
	}
	
	public static Iterator<CSVRecord> getIterator(String file) throws IOException{
		Reader in = new FileReader(file);
		Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(in);
		return records.iterator();
	}
	
	public static ArrayList<Iterator<CSVRecord>> getIteratorList(
			String dir, List<String> filesToMerge) throws IOException{
		
		ArrayList<Iterator<CSVRecord>> iteratorList = new ArrayList<Iterator<CSVRecord>>();
		File dirObj = new File(dir);
		for(File aFile:dirObj.listFiles()){
			if(aFile.isDirectory())
				continue;
			if (!filesToMerge.contains(aFile.getName()))
				continue;
			iteratorList.add(getIterator(aFile.getAbsolutePath()));
		}
		return iteratorList;
	}
}
