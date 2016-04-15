package i2r.hlt;

import i2r.hlt.wrapper.TerrierWrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.terrier.matching.ResultSet;

import uk.ac.ucl.Result;
import uk.ac.ucl.Utils;
import antlr.RecognitionException;
import antlr.TokenStreamException;

public class Retrieval {
	public TerrierWrapper terrier;
	String terrier_home;
	String prefix;
	String indexPath;
	String path_to_data;
	String lang;
	public static String BM25 = "BM25";
	public static String TF_IDF = "TF_IDF";
	
	public Retrieval(String terrier_home, String prefix, String indexPath, 
			String path_to_data, String lang) throws IOException{
		this.terrier_home = terrier_home;
		this.prefix = prefix;
		this.indexPath = indexPath;
		this.path_to_data = path_to_data;
		this.lang = lang;
		
		boolean stopword_removal = true;
		boolean stem = true;
		
		terrier = new TerrierWrapper(terrier_home);
		terrier.setIndex(indexPath, prefix);
		if(!new File(indexPath+prefix+".docid.map").exists()) {
			terrier.prepareIndex(path_to_data, "txt", lang, stopword_removal, stem);
		}
		
		terrier.loadIndex(indexPath, prefix, lang);
		terrier.setStopwordRemoval(lang);
		terrier.setStemmer(lang);
		
		
		System.setProperty("ignore.low.idf.terms", "false");
		
	}
	
	public List<Result> getResults(String query, String match_model, 
			boolean queryExpansion, int n) throws RecognitionException, TokenStreamException, IOException{
		ResultSet rs = terrier.getResultSet(query, match_model, queryExpansion, n);
		
		int[] docid = rs.getDocids();
		double[] scores = rs.getScores();
		if(scores == null || scores.length == 0){
			return null;
		}
		List<Double> minMaxScores = Utils.getMinMaxNormalizedScore(scores);
		List<Double> zScores = Utils.getZScore(scores);
		
		ArrayList<Result> results = new ArrayList<Result>();
		for(int i=0; i<docid.length; i++) {
			Result aResult = new Result(docid[i], terrier.idMap.get(docid[i]), 
					scores[i], minMaxScores.get(i), zScores.get(i));
			results.add(aResult);
		}
		return results;
	}
	
	public static void main(String[] args) throws IOException, RecognitionException, TokenStreamException {
		// specify full path to Terrier home directory
		String terrier_home = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/terrier-core-4.1";
		
		String indexPath = terrier_home+"/var/index/";
		String prefix = "allData_index";
		String path_to_data = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/allData/";
		String lang = "en";
		
		Retrieval ret = new Retrieval(terrier_home, prefix, indexPath, 
				path_to_data, lang);
		
		List<Result> results = ret.getResults("rainbird sprinkler", Retrieval.TF_IDF, true, 100);
		for(Result aResult:results){
			System.out.println("Docid: " + aResult.getTerrierDocId() +
							   "\tScore:" + aResult.getScore() + 
							   "\tDocName:" + aResult.getDocName() + "\t" + 
							   aResult.getNormalizedMinMaxScore() + "\t" +
							   aResult.getNormalizedZScore());
		}
	}
}