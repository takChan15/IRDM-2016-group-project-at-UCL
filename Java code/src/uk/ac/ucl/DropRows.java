package uk.ac.ucl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * Since the FeatureGenerator and TerrierFeatureGenerator created files with different
 * number of rows, I needed to make them be have the exact same numbers of rows. This
 * is to simplify file merging later
 * @author taklumbo
 *
 */
public class DropRows {
	String rowsToRetainFile;
	String fileToPrune;
	String targetFile;
	
	public DropRows(String rowsToRetainFile, String fileToPrune, String targetFile){
		this.rowsToRetainFile = rowsToRetainFile;
		this.fileToPrune = fileToPrune;
		this.targetFile = targetFile;
	}
	
	public static void main(String[] p) throws IOException{
		String dir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles/train_set/";
		String rowsToRetainFile = dir + "all_ids.txt";
		String fileToPrune = dir + "train_product.csv";//"Features_train.csv";
		String targetFile = dir + "cleaned_product.csv";
		DropRows dropRows = new DropRows(rowsToRetainFile, fileToPrune, targetFile);
		dropRows.run();
	}
	
	public void run() throws IOException{
		@SuppressWarnings("unchecked")
		List<String> rowsToRetain = FileUtils.readLines(new File(rowsToRetainFile));
		BufferedReader reader = new BufferedReader(new FileReader(fileToPrune));
		String line;
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile));
		while((line = reader.readLine()) != null){
			if (line.trim().equals(""))
				continue;
			
			String[] fields = line.split(",");
			String primaryKey = fields[0] + "," + fields[1];
			
			if (!rowsToRetain.contains(primaryKey))
				continue;
			
			writer.write(line);
			writer.newLine();
		}
		
		writer.flush();
		writer.close();
		reader.close();		
	}

}
