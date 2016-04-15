package i2r.hlt;

import java.io.File;
import java.io.IOException;

import i2r.hlt.wrapper.TerrierWrapper;

/** This class shows how to index the collection. Load it into the memory.
 * Finally prints the size of the vocabulary (lexicon size).
 * 
 * @author parth
 *
 */
public class Indexing {
	TerrierWrapper terrier;
	public static void main(String[] args) throws IOException {
		
		// Set following two variables with full path
		String terrier_home = "";
		String path_to_data = "";
		String lang = "";
		
		
		Indexing index = new Indexing();
		index.terrier = new TerrierWrapper(terrier_home);
		
		// This is the place where the index will be created
		String indexPath = terrier_home+"/var/index/tut/";
		// Prefix is useful where there will be multiple indexes at the same directory for example, 
		// different languages.
		String prefix = "en";
		
		// This module indexes the collection if it is already not indexed.
		index.terrier.setIndex(indexPath, prefix);
		if(!new File(indexPath+prefix+".docid.map").exists()) {
			System.out.print("The index is not present, so indexing the collection...");
			index.terrier.prepareIndex(path_to_data, "txt", "en", true, true);
			System.out.println("Done!");
		}
		
		// After index is created or already present, it is loaded into memory by following command
		index.terrier.loadIndex(indexPath, prefix, lang);
		
		// It prints the total dimension = total tokens in the index.
		System.out.println(index.terrier.getDimension());
	}
}