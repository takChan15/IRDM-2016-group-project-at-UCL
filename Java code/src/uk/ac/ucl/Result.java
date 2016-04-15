package uk.ac.ucl;

import java.util.List;

public class Result {
	int terrierDocId;
	String docName;
	double score;
	double normalizedMinMaxScore;
	double normalizedZScore;
	
	public Result(int terrierDocId, String docId, double score, double normalizedMinMaxScore, double normalizedZScore){
		this.terrierDocId = terrierDocId;
		this.docName = docId;
		this.score = score;
		this.normalizedMinMaxScore = normalizedMinMaxScore;
		this.normalizedZScore = normalizedZScore;
	}

	public static Result findResultByDocName(String docName, List<Result> results){
		for(Result aResult:results){
			if (aResult.docName.equals(docName))
				return aResult;
		}
		return null;
	}
	
	/**
	 * Return the ranking of the document. If not found, returns -1
	 * @param docName
	 * @param results
	 * @return
	 */
	public static int getRankByDocName(String docName, List<Result> results){
		int i = 0;
		for(Result aResult:results){
			i++;
			if (aResult.docName.equals(docName))
				return i;
		}
		return 101;
	}
	
	public int getTerrierDocId() {
		return terrierDocId;
	}

	public void setTerrierDocId(int terrierDocId) {
		this.terrierDocId = terrierDocId;
	}

	public String getDocName() {
		return docName;
	}

	public void setDocName(String docName) {
		this.docName = docName;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getNormalizedMinMaxScore() {
		return normalizedMinMaxScore;
	}

	public void setNormalizedMinMaxScore(double normalizedMinMaxScore) {
		this.normalizedMinMaxScore = normalizedMinMaxScore;
	}

	public double getNormalizedZScore() {
		return normalizedZScore;
	}

	public void setNormalizedZScore(double normalizedZScore) {
		this.normalizedZScore = normalizedZScore;
	}
}
