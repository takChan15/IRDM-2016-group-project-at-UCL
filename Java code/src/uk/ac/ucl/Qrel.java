package uk.ac.ucl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class Qrel implements Comparable<Qrel>{
	String document_id ;
	int topic_no ;
	int subtopic_no ;
	double judgment ;
	
	public enum QREL_TYPE {
		ADHOC, DIVERSITY
	}
	
	/*
	 * Initialize the Qrel object with query, document and 
	 * its relevance grade.
	 */
	public Qrel(String topic, String doc_id, String rel) {
		document_id = doc_id;
		topic_no = Integer.parseInt(topic);
		judgment = Double.parseDouble(rel);
		
	}

	public static Qrel findQrelByTopicId(int topic_no, ArrayList<Qrel> qrelList){
		for(Qrel qrel:qrelList){
			if (qrel.topic_no == topic_no)
				return qrel;
		}
		return null;
	}
	
	public static ArrayList <Qrel> loadQrels(String file_name,  QREL_TYPE qrel_type) {

		ArrayList <Qrel> qrels = new ArrayList<Qrel>();
		try {
			BufferedReader ifile = new BufferedReader(new FileReader(new File (file_name)));
			String [] split;
			String line;
			Qrel temp = null;

			while((line=ifile.readLine())!=null)
			{
				split = line.split("\t");
				if (qrel_type  == QREL_TYPE.ADHOC)
				{
					// Qrel structure: TOPIC ITERATION DOCUMENT# RELEVANCE
					temp = new Qrel (split[0], split[2], split[3]); 
				}
				else if (qrel_type == QREL_TYPE.DIVERSITY)
				{
					// Qrel structure: TOPIC INTENT DOCUMENT# RELEVANCE
					temp = new Qrel (split[0], split[1], split[2], split[3]); 
				}
				if (temp!=null)
					qrels.add(temp);

			}
			ifile.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return qrels;

	}


	/*
	 * Initialize the Qrel object with query, subtopic, document and 
	 * its relevance grade with respect to query subtopic.
	 */
	public Qrel(String topic, String subtopic,  String doc_id, String rel) {
		document_id = doc_id;
		topic_no = Integer.parseInt(topic);
		judgment = Double.parseDouble(rel);
		subtopic_no = Integer.parseInt(subtopic);
		
	}

	public int compareTo(Qrel o) {
		if (judgment < o.judgment)
			return -1;
		else if (judgment == o.judgment)
			return 0;
		else
			return 1;
	}
	
	public boolean equals(Object o) {
        if (!(o instanceof Qrel))
            return false;
        Qrel n = (Qrel) o;
        return n.document_id.equals(document_id) &&
        		n.judgment == judgment &&
        		n.subtopic_no == subtopic_no &&
        		n.topic_no == topic_no;
    }

    public int hashCode() {
        return 31*document_id.hashCode();
    }


	public String getDocument_id() {
		return document_id;
	}


	public void setDocument_id(String document_id) {
		this.document_id = document_id;
	}


	public int getTopic_no() {
		return topic_no;
	}


	public void setTopic_no(int topic_no) {
		this.topic_no = topic_no;
	}


	public int getSubtopic_no() {
		return subtopic_no;
	}


	public void setSubtopic_no(int subtopic_no) {
		this.subtopic_no = subtopic_no;
	}


	public double getJudgment() {
		return judgment;
	}


	public void setJudgment(double judgment) {
		this.judgment = judgment;
	}

}
