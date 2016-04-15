package uk.ac.ucl;

import i2r.hlt.Retrieval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.JDOMException;

import antlr.RecognitionException;
import antlr.TokenStreamException;

/**
 * Generate features using Terrier
 * 
 * @author taklumbo
 *
 */
public class TerrierFeatureGenerator {
	//A arbitrary maximum limit placed on the number of query words. Use for data scaling
	static int maxNumberOfWords = 20;
	
	String dataSet; //all, title, desc, attrib
	boolean expandedRetrieval;
	String rootDir;
	String terrier_home;
	String trecTopicsFile;
	
	public TerrierFeatureGenerator(String dataSet, boolean expandedRetrieval, String rootDir,
			String terrier_home, String trecTopicsFile){
		this.dataSet = dataSet;
		this.expandedRetrieval = expandedRetrieval;
		this.rootDir = rootDir;
		this.terrier_home = terrier_home;
		this.trecTopicsFile = trecTopicsFile;
	}
	
	public void parse() throws JDOMException, IOException, RecognitionException, TokenStreamException{
		boolean isTestset = trecTopicsFile.contains("test");
		
		String prefix = dataSet + "_index"; 
		String path_to_data = rootDir + "data/" + dataSet;
		
		ArrayList<Qrel> qrelList;
		if (isTestset){
			qrelList = Qrel.loadQrels(
					rootDir + "test_set_qrel_adhoc.txt", //
					Qrel.QREL_TYPE.ADHOC);
		}else{
			qrelList = Qrel.loadQrels(
					rootDir + "train_set_qrel_adhoc.txt", //
					Qrel.QREL_TYPE.ADHOC);
		}
		
		String indexPath = terrier_home+"var/index/";
		
		String lang = "en";
		
		List<TrecTopic> topics = TrecTopic.loadTopicsFromFile(rootDir + trecTopicsFile);
		
		Retrieval ret = new Retrieval(terrier_home, prefix, indexPath, path_to_data, lang);
		
		
		String resultDir;
		if (isTestset)
			resultDir = rootDir + "featureFiles/test_set/";
		else
			resultDir = rootDir + "featureFiles/train_set/";
		
		String resultFile;
		if (expandedRetrieval){
			resultFile = resultDir + dataSet + "_features_queryExp.csv";
		}else{
			resultFile = resultDir + dataSet + "_features.csv";
		}
		
		File file = new File(resultFile + ".err");
		PrintWriter errorStream = new PrintWriter(file);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile));
			writer.write("queryId" +
					  ",productId" + 
					  ",originalQuery"+
					  ",refactoredQuery"+
	  				  ",expQuery_" + dataSet +
					  ",modelScore_" + dataSet +
					  ",minMaxModelScore_" + dataSet +
					  ",modelZScore_" + dataSet +
					  //",numberOfQueryTermsIn_"+ dataSet +
					  ",productRank_"+ dataSet +
					  //",numberOfExpQueryTermsIn_"+ dataSet +
					  ",sizeOfQuery" +
					  ",sizeOfExpQuery_"+ dataSet +
					  ",ratioNumberOfQueryTermsIn_" + dataSet +
					  ",ratioNumberOfExpQueryTermsIn_" + dataSet);
			if (!isTestset){
				writer.write(",y");
			}
		writer.newLine();
		
		for(TrecTopic aTopic:topics){
			try{
				String refactoredQuery = aTopic.query;//.replaceAll("\\.", " ").replaceAll(",", "").toLowerCase();
				refactoredQuery = Utils.refactorQueryForTerrier(refactoredQuery);
				
				int sizeOfQuery = refactoredQuery.split(" ").length;
				
				Qrel qrel = Qrel.findQrelByTopicId(aTopic.topicId, qrelList);
				
				int docId = ret.terrier.getDocIdByDocName(path_to_data + "/" + qrel.document_id + ".txt");
				
				int numberOfQueryWordsInDoc = Utils.getNumberOfQueryWordsInDoc(docId, ret, refactoredQuery);
				String expandedQuery = Utils.getExpandedQuery(refactoredQuery, ret);
				int sizeOfExpQuery = expandedQuery.split(" ").length;
				int numberOfExpandedTermsInDoc = Utils.getNumberOfQueryWordsInDoc(docId, ret, expandedQuery);
				double ratioNumberOfQueryWordsInDoc = ((double)numberOfQueryWordsInDoc / (double)sizeOfQuery);
				double ratioNumberOfExpandedTermsInDoc = 0;
				if(sizeOfExpQuery > 0){
					ratioNumberOfExpandedTermsInDoc = 
							((double)numberOfExpandedTermsInDoc / (double)sizeOfExpQuery);
				}
				
				List<Result> results = ret.getResults(refactoredQuery, Retrieval.TF_IDF, expandedRetrieval, 100);
				if (results == null){
					//Terrier wasn't able to calculate scores
					writer.write(aTopic.topicId + "," + 
								  qrel.document_id + "," +
								  aTopic.query.replaceAll(",", "") + "," +
								  refactoredQuery + "," +
								  expandedQuery + "," +
								  "0," +
								  "0," +
								  "0," +
								  //numberOfQueryWordsInDoc + "," +
								  "0," +
								  //numberOfExpandedTermsInDoc + "," +
								  sizeOfQuery + "," + 
								  sizeOfExpQuery + "," + 
								  ratioNumberOfQueryWordsInDoc + "," + 
								  ratioNumberOfExpandedTermsInDoc);
								  
					if (!isTestset){
						writer.write("," + qrel.judgment);
					}
					writer.newLine();
					continue;
				}
				
				Result aResult = Result.findResultByDocName(qrel.document_id + ".txt", results);
				double tf_idf = 0;
				double minMax_TF_IDF = 0;
				double zscore_TF_IDF = 0;
				double productRank = Result.getRankByDocName(qrel.document_id + ".txt", results);
				//productRank = Utils.getMinMaxNormalizedScore(productRank, 0, 100);
				
				if(aResult != null){
					tf_idf = aResult.getScore();
					zscore_TF_IDF = aResult.getNormalizedZScore();
					minMax_TF_IDF = aResult.normalizedMinMaxScore;
				}
				
				writer.write(aTopic.topicId + "," + 
							 qrel.document_id + "," +
							 aTopic.query.replaceAll(",", "") + "," +
							 refactoredQuery + "," +
							 expandedQuery + "," +
							 tf_idf + "," +
							 minMax_TF_IDF + "," +
							 zscore_TF_IDF + "," +
							 //numberOfQueryWordsInDoc + "," +
							 productRank + "," +
							 //numberOfExpandedTermsInDoc + "," +
							 sizeOfQuery + "," + 
							 sizeOfExpQuery + "," +
							 ratioNumberOfQueryWordsInDoc + "," + 
							 ratioNumberOfExpandedTermsInDoc);
				if (!isTestset){
					writer.write("," + qrel.judgment);
				}
				writer.newLine();
			}catch(Exception e){
				errorStream.write("ERROR: Topic Id: " + aTopic.topicId + "\tQuery: " + aTopic.query);
				errorStream.write(System.getProperty("line.separator"));
				e.printStackTrace(errorStream);
				
				Qrel qrel = Qrel.findQrelByTopicId(aTopic.topicId, qrelList);
				writer.write(aTopic.topicId + "," + 
						  qrel.document_id + "," +
						  aTopic.query.replaceAll(",", "") + "," +
						  "," +
						  "," +
						  "0," +
						  "0," +
						  "0," +
						  //numberOfQueryWordsInDoc + "," +
						  "0," +
						  //numberOfExpandedTermsInDoc + "," +
						  aTopic.query.replaceAll("\\s+", " ") + "," + 
						  "0," + 
						  "0," + 
						  "0");
				if (!isTestset){
					writer.write("," + qrel.judgment);
				}
				writer.newLine();
			}
			
		}
		writer.flush();
		writer.close();
		errorStream.flush();
		errorStream.close();

	}
	
	/*
	public static void main(String[] args) throws IOException, RecognitionException, TokenStreamException, JDOMException {
		String dataSet = "all"; //all, title, desc, attrib
		boolean expandedRetrieval = false;
		
		String prefix = dataSet + "_index"; 
		String path_to_data = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/data/" + dataSet;
		
		ArrayList<Qrel> qrelList = Qrel.loadQrels(
				"/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/qrel_adhoc.txt", 
				Qrel.QREL_TYPE.ADHOC);
		
		// specify full path to Terrier home directory
		String terrier_home = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/terrier-core-4.1";
		
		String indexPath = terrier_home+"/var/index/";
		
		String lang = "en";
		
		List<TrecTopic> topics = TrecTopic.loadTopicsFromFile(
				"/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/trainTrecTopics.txt");
		
		Retrieval ret = new Retrieval(terrier_home, prefix, indexPath, 
				path_to_data, lang);
		
		String resultFile;
		if (expandedRetrieval){
			resultFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles/" + dataSet + "_features_queryExp.csv";
		}else{
			resultFile = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/featureFiles/" + dataSet + "_features.csv";
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile));
			writer.write("queryId" +
					  ",productId" + 
					  ",originalQuery"+
					  ",refactoredQuery"+
	  				  ",expQuery_" + dataSet +
					  //",modelScore_" + dataSet +
					  ",minMaxModelScore_" + dataSet +
					  ",modelZScore_" + dataSet +
					  //",numberOfQueryTermsIn_"+ dataSet +
					  ",productRank_"+ dataSet +
					  //",numberOfExpQueryTermsIn_"+ dataSet +
					  ",sizeOfQuery" +
					  ",sizeOfExpQuery_"+ dataSet +
					  ",ratioNumberOfQueryTermsIn_" + dataSet +
					  ",ratioNumberOfExpQueryTermsIn_" + dataSet +
					  ",y");
		writer.newLine();
		
		for(TrecTopic aTopic:topics){
			try{
				String refactoredQuery = aTopic.query.replaceAll("\\.", " ").replaceAll(",", "").toLowerCase();
				refactoredQuery = Utils.refactorQueryForTerrier(refactoredQuery);
				
				int sizeOfQuery = refactoredQuery.split(" ").length;
				
				Qrel qrel = Qrel.findQrelByTopicId(aTopic.topicId, qrelList);
				
				int docId = ret.terrier.getDocIdByDocName(path_to_data + "/" + qrel.document_id + ".txt");
				
				int numberOfQueryWordsInDoc = Utils.getNumberOfQueryWordsInDoc(docId, ret, refactoredQuery);
				String expandedQuery = Utils.getExpandedQuery(refactoredQuery, ret);
				int sizeOfExpQuery = expandedQuery.split(" ").length;
				int numberOfExpandedTermsInDoc = Utils.getNumberOfQueryWordsInDoc(docId, ret, expandedQuery);
				double ratioNumberOfQueryWordsInDoc = ((double)numberOfQueryWordsInDoc / (double)sizeOfQuery);
				double ratioNumberOfExpandedTermsInDoc = 0;
				if(sizeOfExpQuery > 0){
					ratioNumberOfExpandedTermsInDoc = 
							((double)numberOfExpandedTermsInDoc / (double)sizeOfExpQuery);
				}
				
				List<Result> results = ret.getResults(refactoredQuery, Retrieval.TF_IDF, expandedRetrieval, 100);
				if (results == null){
					//Terrier wasn't able to calculate scores
					writer.write(aTopic.topicId + "," + 
								  qrel.document_id + "," +
								  aTopic.query.replaceAll(",", "") + "," +
								  refactoredQuery + "," +
								  expandedQuery + "," +
								  //"0," +
								  "0," +
								  "0," +
								  //numberOfQueryWordsInDoc + "," +
								  "0," +
								  //numberOfExpandedTermsInDoc + "," +
								  sizeOfQuery + "," + 
								  sizeOfExpQuery + "," + 
								  ratioNumberOfQueryWordsInDoc + "," + 
								  ratioNumberOfExpandedTermsInDoc + "," +
								  qrel.judgment);
					writer.newLine();
					continue;
				}
				
				Result aResult = Result.findResultByDocName(qrel.document_id + ".txt", results);
				double tf_idf = 0;
				double minMax_TF_IDF = 0;
				double zscore_TF_IDF = 0;
				double productRank = Result.getRankByDocName(qrel.document_id + ".txt", results);
				//productRank = Utils.getMinMaxNormalizedScore(productRank, 0, 100);
				
				if(aResult != null){
					tf_idf = aResult.getScore();
					zscore_TF_IDF = aResult.getNormalizedZScore();
					minMax_TF_IDF = aResult.normalizedMinMaxScore;
				}
				
				writer.write(aTopic.topicId + "," + 
							 qrel.document_id + "," +
							 aTopic.query.replaceAll(",", "") + "," +
							 refactoredQuery + "," +
							 expandedQuery + "," +
							 //tf_idf + "," +
							 minMax_TF_IDF + "," +
							 zscore_TF_IDF + "," +
							 //numberOfQueryWordsInDoc + "," +
							 productRank + "," +
							 //numberOfExpandedTermsInDoc + "," +
							 sizeOfQuery + "," + 
							 sizeOfExpQuery + "," +
							 ratioNumberOfQueryWordsInDoc + "," + 
							 ratioNumberOfExpandedTermsInDoc + "," +
							 qrel.judgment);
				writer.newLine();
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("ERROR: Topic Id: " + aTopic.topicId + "\tQuery: " + aTopic.query);
				
			}
			
		}
		writer.flush();
		writer.close();
		

	}
*/
}
