package uk.ac.ucl;

import java.io.IOException;

import org.jdom2.JDOMException;

import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * A class to run various features generator classes. 
 * @author taklumbo
 *
 */
public class FeatureGenManager {

	public static void main(String[] args) throws IOException, JDOMException, RecognitionException, TokenStreamException {
		String rootDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/";
		//This is the directory where you have your HomeDepot raw data files
		String rawDataDir = rootDir + "RawData/";
		
		String brandsFile = rootDir + "Brands.txt";
		
		String descriptionFile = rawDataDir + "product_descriptions.csv";
		String attribFile = rawDataDir + "attributes.csv";
		
		//test.csv or train.csv
		String[] queryFiles = {rawDataDir + "test.csv", rawDataDir + "train.csv"}; 
		//String[] queryFiles = {rawDataDir + "train.csv"}; 
		
		for(String aQueryFile:queryFiles){
			String targetDir;
			if (aQueryFile.contains("test.csv"))
				targetDir = rootDir + "featureFiles/test_set/";
			else
				targetDir = rootDir + "featureFiles/train_set/";
			
			FeatureGenerator gen = 
				new FeatureGenerator(descriptionFile, attribFile, aQueryFile, targetDir, brandsFile);
			//gen.parse();
		}
		
		//-------------------------
		
		String[] dataSets = {"all", "title", "desc", "attrib"};
		String[] qrelFiles = {"trainTrecTopics.txt", "testTrecTopics.txt"};
		String terrier_home = rootDir + "terrier-core-4.1/";
		
		for(String aQrelFile:qrelFiles){
			for(String aDataSet:dataSets){
				TerrierFeatureGenerator terrierGen = new TerrierFeatureGenerator
						(aDataSet, false, rootDir, terrier_home, aQrelFile);
				terrierGen.parse();
			}
		}
		
	}

}
