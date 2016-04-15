package uk.ac.ucl;

public class Query {
	int queryId;
	String productId;
	String productTitle;
	String searchTerm;
	double relevance;
	
	public Query(int queryId, String productId, String productTitle, String searchTerm, double relevance){
		this.queryId = queryId;
		this.productId = productId;
		this.productTitle = productTitle;
		this.searchTerm = searchTerm;
		this.relevance = relevance;
	}
}
