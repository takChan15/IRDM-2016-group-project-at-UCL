package i2r.hlt;

import gnu.trove.map.hash.TIntFloatHashMap;
import i2r.hlt.wrapper.TerrierWrapper;

import java.io.File;
import java.io.IOException;

import org.terrier.matching.ResultSet;
/** This class shows how to create a term-document matrix for a collection.
 * First it creates an index of the document based on the indexing options like
 * stop-word removal, stemming etc.
 * The row of each matrix (i) is a document and the columns (j) are the terms in it.
 * So entry (i,j) will be the either TF-IDF score of term j in document i or 
 * word count of term j in document i based on the boolean 'wordcount' in the 
 * getVector() function.
 * This Method only gives those entries which are non-zero, so easily the matrix can 
 * be converted in a boolean Matrix.
 * 
 * @author Parth Gupta
 *
 */
public class TDMatrix {
	TerrierWrapper terrier;
	
	public static void main(String[] args) throws IOException {
		TDMatrix mtx = new TDMatrix();
		
		// specify full path to Terrier home directory
		String terrier_home = "/home/parth/workspace/terrier-3.5/";
		
		String indexPath = terrier_home+"/var/index/tut/";
		String prefix = "en";
		String path_to_data = "";
		String lang = "en";
		
		boolean stopword_removal = true;
		boolean stem = true;
		
		mtx.terrier = new TerrierWrapper(terrier_home);
		mtx.terrier.setIndex(indexPath, prefix);
		if(!new File(indexPath+prefix+".docid.map").exists()) {
			mtx.terrier.prepareIndex(path_to_data, "txt", lang, stopword_removal, stem);
		}
		
		mtx.terrier.loadIndex(indexPath, prefix, lang);
		mtx.terrier.setStopwordRemoval(lang);
		mtx.terrier.setStemmer(lang);
		
		System.setProperty("ignore.low.idf.terms", "false");
		
		
		
		for(int i=0; i<mtx.terrier.getTotIndexedDocs(); i++) {
			TIntFloatHashMap vec = mtx.terrier.getVector(i, true);
			for(int j:vec.keys())
				System.out.println(i+"\t"+j+"\t"+vec.get(j));
		}
		
		
	}
}