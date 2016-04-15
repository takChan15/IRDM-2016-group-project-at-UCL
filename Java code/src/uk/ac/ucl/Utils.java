package uk.ac.ucl;

import gnu.trove.map.hash.TIntIntHashMap;
import i2r.hlt.Retrieval;
import i2r.hlt.wrapper.TerrierWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.terrier.structures.Index;
import org.terrier.structures.MetaIndex;
import org.terrier.terms.PorterStemmer;

import antlr.RecognitionException;
import antlr.TokenStreamException;

public class Utils {
	static PorterStemmer stemmer = new PorterStemmer();
	
	static LinkedHashMap<String, String> terrier_patterns_replacem;
	static LinkedHashMap<String, Pattern> terrier_patterns = new LinkedHashMap<String, Pattern>();
	static {
		terrier_patterns_replacem = new LinkedHashMap<String, String>();
		terrier_patterns_replacem.put("(\\d+)(\\*)(\\d+( |$))", "$1 x $3"); //12*6 -> 12 x 6
		terrier_patterns_replacem.put("(\\d+)(x|X)(\\d+( |$))", "$1 x $3"); //12x6 -> 12 x 6
		terrier_patterns_replacem.put("(\\d+)( ?feet( |$))", "$1 ft ");
		terrier_patterns_replacem.put("(\\d+)( ?inches( |$))", "$1 in ");
		terrier_patterns_replacem.put("(\\d+)( ?inch( |$))", "$1 in ");
		terrier_patterns_replacem.put("(\\d+)(in\\.?( |$))", "$1 in ");
		terrier_patterns_replacem.put("(\\d+)(ft\\.?( |$))", "$1 ft ");
		terrier_patterns_replacem.put("(\\d+)(\\'( |$))", "$1 ");
		
		for(String patternString:terrier_patterns_replacem.keySet()){
			terrier_patterns.put(patternString, Pattern.compile(patternString));
		}
	}
	
	/**
	 * Calculates TD_IDF for a query in a document
	 * @param terrier
	 * @param query
	 * @param docName
	 * @return
	 * @throws IOException
	 */
	public static double calculateTf_Idf(TerrierWrapper terrier, String query, String docName) throws IOException{
		int docId = terrier.getDocIdByDocName(docName);
		double query_td_idf = 0.0;
		for(String aTerm:query.split(" ")){
			String stemmedTerm = stemmer.stem(aTerm);
			if (terrier.isStopWord(stemmedTerm))
				continue;
			
			int tf = terrier.getTF(docId, stemmedTerm);
			if (tf == 0)
				continue;
			double idf = terrier.getIDF(stemmedTerm);
			query_td_idf += tf * idf;
		}
		return query_td_idf;
	}
	
	public static String refactorQueryForTerrier(String query){
		String newQuery = query;
		for(Entry<String, String> entry:terrier_patterns_replacem.entrySet()){
			Pattern pattern = terrier_patterns.get(entry.getKey());
			Matcher matcher = pattern.matcher(newQuery);
			if (matcher.find()){
				newQuery = matcher.replaceAll(entry.getValue());
			}
		}
		return newQuery.replaceAll(",", "").replaceAll("\\s+", " ").replaceAll("\\.", " ");
	}	
	
	public static String refactorQueryForTerrier2(String query){
		String newQuery = query.toLowerCase();
		for(Entry<String, String> entry:terrier_patterns_replacem.entrySet()){
			Pattern pattern = terrier_patterns.get(entry.getKey());
			Matcher matcher = pattern.matcher(newQuery);
			if (matcher.find()){
				newQuery = matcher.replaceAll(entry.getValue());
			}
		}
		return newQuery.replaceAll(",", "").replaceAll("\\s+", " ").replaceAll("\\.", " ");
	}
	
	public static int getNumberOfQueryWordsInDoc(int docId, Retrieval ret, String query) throws IOException{
		TIntIntHashMap map = ret.terrier.docTF(docId);
		
		int count = 0;
		for(String queryWord:query.split(" ")){
			String stemmedWord = stemmer.stem(queryWord.toLowerCase());
			int termId = ret.terrier.getTermId(stemmedWord);
			if (termId != -1){
				if(map.containsKey(termId) && map.get(termId) > 0)
					count++;
			}
		}
		return count;
	}
	
	public static String getExpandedQuery(String query, Retrieval ret) 
			throws RecognitionException, TokenStreamException, IOException{
		try{
			Map<Integer, String> expandedTerms = 
					ret.terrier.getExpandedTerms(Retrieval.TF_IDF, "KLComplete", query);
			
			//Only check for terms that was not in the original query
			ArrayList<String> stemmedTerms = new ArrayList<String>();
			for(String term:query.split(" ")){
				stemmedTerms.add(stemmer.stem(term.toLowerCase()));
			}
				
			StringBuilder expandedQuery = new StringBuilder();
			for(String value:expandedTerms.values()){
				//0=convect^0.007103908236468859
				String anExpandedTerm = value.split("\\^")[0];
				if (!stemmedTerms.contains(anExpandedTerm))
					expandedQuery.append(anExpandedTerm).append(" ");
			}
			
			if (expandedQuery.length() == 0)
				return "";
			
			return expandedQuery.toString().trim();
		}catch(Exception e){
			return "";
		}
	}
	
	/**
	 * Uses the min-max normalization algorithm to normalize the score to 0 and 1
	 * @param doc_score
	 * @return
	 */
	public static List<Double> getMinMaxNormalizedScore(double[] doc_score){
		List<Double> newScores = new ArrayList<Double>();
		for(double aScore:doc_score)
			newScores.add(aScore);

		double oldMax = Collections.max(newScores);
		double oldMin = Collections.min(newScores);
		double newMax = 1;
		double newMin = 0;
		
		for (int i=0; i<newScores.size(); i++){
			double oldValue = newScores.get(i);
			double newValue = (((oldValue - oldMin) / (oldMax - oldMin)) * (newMax - newMin)) + newMin;
			newScores.set(i, newValue);
		}
		
		return newScores;
	}
	
	/**
	 * Uses the Z Score normalization algorithm to normalize the score 
	 * @param doc_score
	 * @return
	 */
	public static List<Double> getZScore(double[] doc_score){
		List<Double> newScores = new ArrayList<Double>();

		double sd = new StandardDeviation().evaluate(doc_score);
		double mean = new Mean().evaluate(doc_score);
		
		for(double score:doc_score){
			newScores.add((score - mean) / sd);
		}
		return newScores;
	}
	
	/**
	 * Returns a document id in Terrier, given its document name
	 * @param docName
	 * @param index
	 * @return
	 * @throws IOException
	 */
	public static int getDocIdByDocName(String docName, Index index) throws IOException{
		MetaIndex meta = index.getMetaIndex();
		 int docid = meta.getDocument("filename", docName);
		 return docid;
	}
	
	/**
	 * Uses the min-max normalization algorithm to normalize the score to 0 and 1
	 * @param doc_score
	 * @return
	 */
	public static double getMinMaxNormalizedScore(double doc_score, double oldMin, double oldMax){
		double newMax = 1;
		double newMin = 0;
		double newValue = (((doc_score - oldMin) / (oldMax - oldMin)) * (newMax - newMin)) + newMin;
		return newValue;
	}
}
