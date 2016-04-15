package uk.ac.ucl;

import i2r.hlt.Retrieval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.RefAddr;

public class Test {

	//static Pattern oneWordBrand = Pattern.compile("(^(\\p{Alpha})|\\p{Punct}| )+\\d");
	
	public static void main(String[] args) throws IOException {
		String dataSet = "all"; //all, title, desc, attrib
		boolean expandedRetrieval = false;
		String rootDir = "/Users/taklumbo/Ucl_assignments/IRDM/HomeDepot/";
		String terrier_home = rootDir + "terrier-core-4.1/";
		String trecTopicsFile;
		
		//boolean isTestset = trecTopicsFile.contains("test");
		
		String prefix = dataSet + "_index"; 
		String path_to_data = rootDir + "data/" + dataSet;
		
		/*
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
		*/
		String indexPath = terrier_home+"var/index/";
		
		String lang = "en";
		
		//List<TrecTopic> topics = TrecTopic.loadTopicsFromFile(rootDir + trecTopicsFile);
		
		Retrieval ret = new Retrieval(terrier_home, prefix, indexPath, path_to_data, lang);
		String productId = "100023";
		String docName = path_to_data + "/" + productId + ".txt";
		String query = "CONCRETE & MASONRY CLEANER & ETCHER";
		System.out.println(Utils.calculateTf_Idf(ret.terrier, Utils.refactorQueryForTerrier2(query), docName));
	}
	
	
	

}
