package i2r.hlt;

import i2r.hlt.wrapper.TerrierWrapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.terrier.matching.ResultSet;

import antlr.RecognitionException;
import antlr.TokenStreamException;
/** This class shows an example of how to get expanded terms for the user query using 
 * different Matching models and Query Expansion models of Terrier.
 * 
 * Possible Query Expansion Models can be found at 
 * http://terrier.org/docs/v3.5/javadoc/org/terrier/matching/models/queryexpansion/package-summary.html
 * 
 * @author Parth Gupta
 *
 */
public class PseudoRelevanceFeedback {
	public TerrierWrapper terrier;
	public static void main(String[] args) throws RecognitionException, TokenStreamException, IOException {
		PseudoRelevanceFeedback prf = new PseudoRelevanceFeedback();
		
		// specify full path to Terrier home directory
		String terrier_home = "/home/parth/workspace/terrier-3.5/";
		
		String indexPath = terrier_home+"/var/index/tut/";
		String prefix = "en";
		String path_to_data = "";
		String lang = "en";
		
		boolean stopword_removal = true;
		boolean stem = true;
		
		prf.terrier = new TerrierWrapper(terrier_home);
		prf.terrier.setIndex(indexPath, prefix);
		if(!new File(indexPath+prefix+".docid.map").exists()) {
			prf.terrier.prepareIndex(path_to_data, "txt", lang, stopword_removal, stem);
		}
		
		prf.terrier.loadIndex(indexPath, prefix, lang);
		prf.terrier.setStopwordRemoval(lang);
		prf.terrier.setStemmer(lang);
		
		System.setProperty("ignore.low.idf.terms", "false");
		
		
		Map<Integer, String> map = prf.terrier.getExpandedTerms("TF_IDF", "Bo1" , "singapore city");
		
		for(int i=0; i<map.size(); i++)
			System.out.println(i + " " + map.get(i));
	}
}