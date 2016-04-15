package i2r.hlt;

import java.io.File;
import java.io.IOException;

import i2r.hlt.wrapper.TerrierWrapper;

/** To analyse the index for number of documents. Then printing top N terms in those 
 * documents based on TF-IDF scores.
 * 
 * @author Parth Gupta
 *
 */
public class IndexAnalysis {
	TerrierWrapper terrier;
	public static void main(String[] args) throws IOException {
		IndexAnalysis analysis = new IndexAnalysis();
		
		// specify full path to Terrier home directory
		String terrier_home = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/terrier-core-4.1";
		
		//String indexPath = terrier_home+"/var/index/tut/";
		String indexPath = terrier_home+"/var/index/";
		String lang = "";
		
		String prefix = "home_depot_index";
		String path_to_data = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/Data/";
		
		analysis.terrier = new TerrierWrapper(terrier_home);
		analysis.terrier.setIndex(indexPath, prefix);
		if(!new File(indexPath+prefix+".docid.map").exists()) {
			analysis.terrier.prepareIndex(path_to_data, "txt", lang, true, true);
		}
		
		analysis.terrier.loadIndex(indexPath, prefix, lang);
		float idf = analysis.terrier.getIDF("oven");
		float tf = analysis.terrier.getRawTF("oven");
		//int df = analysis.terrier.get
		gnu.trove.map.hash.TObjectIntHashMap<String> map = analysis.terrier.idReverseMap;
		
		int docid = analysis.terrier.idReverseMap.get("100006.txt");
		String[] top = analysis.terrier.topTerms(docid, 50);
		
		System.out.println("Top 5 Terms (TF-IDF)");
		for(String s: top)
			System.out.println("\t"+s);
		System.out.println();
		/*
		for(int i: analysis.terrier.idMap.keys()) {
			System.out.println("docid: " + i + "\t" + "DocName: " +analysis.terrier.idMap.get(i));
			String[] top = analysis.terrier.topTerms(i, 10);
			
			System.out.println("Top 5 Terms (TF-IDF)");
			for(String s: top)
				System.out.println("\t"+s);
			System.out.println();
			
			// Following command prints the terms of the document with docid.
//			analysis.terrier.printTermsOfDocument(i);
		}
		
		*/
		
	}
}