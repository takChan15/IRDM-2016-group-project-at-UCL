package i2r.hlt.wrapper;


import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;




import org.terrier.applications.TRECIndexing;
import org.terrier.indexing.tokenisation.TokenStream;
import org.terrier.indexing.tokenisation.Tokeniser;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.querying.Manager;
import org.terrier.querying.Request;
import org.terrier.querying.SearchRequest;
import org.terrier.querying.parser.TerrierFloatLexer;
import org.terrier.querying.parser.TerrierLexer;
import org.terrier.querying.parser.TerrierQueryParser;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.DirectIndex;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.InvertedIndex;
import org.terrier.structures.MetaIndex;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.terms.BaseTermPipelineAccessor;
import org.terrier.terms.EnglishSnowballStemmer;
import org.terrier.terms.PorterStemmer;
import org.terrier.terms.SnowballStemmer;
import org.terrier.terms.TRv2PorterStemmer;
import org.terrier.terms.TRv2WeakPorterStemmer;
import org.terrier.terms.TermPipelineAccessor;
import org.terrier.terms.WeakPorterStemmer;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.TokenStreamSelector;

/** This class is the wrapper on top of terrier API. 
 * Use it for the most of IR related needs.
 * 
 * @author Parth Gupta (pgupta@dsic.upv.es)
 *
 */

public class TerrierWrapper {
	/** Terrier Home. Its crucial to set it in the constructer of the wrapper. */
	protected String terrier_home = "";
	/** It is used across termpipeline related functions to load proper stemmer, stopword file etc..*/
	protected String lang;
	
	protected String path_to_index = "";
	protected String indexPrefix = "";
	
	/** This statistics is collected ones the index is loaded using updateCollectionStatistics().*/
	protected long total_docs = 0;
	protected long total_tokens = 0;
	protected double maxIDF = 0;
	
	protected Index index;
	
	protected double paramC = 0.0;
	
	protected boolean stopword_removal;
	protected boolean stem;
	
	protected TermPipelineAccessor stopEngine;
	protected TermPipelineAccessor stemEngine;
	
	/** Index structures*/
	protected Lexicon<String> lex = null;
	protected DirectIndex di = null;
	protected DocumentIndex doi = null;
	
	
	/** Maps the languages to their ISO 639-1 Code, e.g. en-\>English, hi-\>Hindi etc.*/
	public Map<String, String> langCodeMap = new HashMap<String, String>();
	
	/** Maps original document names to docids*/
	public TObjectIntHashMap<String> idReverseMap = new TObjectIntHashMap<String>();
	
	/** Maps docids to their original docnames*/
	public TIntObjectHashMap<String> idMap = new TIntObjectHashMap<String>();
	
	/** Cantains the term-> IDF map*/
	public TIntFloatHashMap idfMap = new TIntFloatHashMap();
	public TerrierWrapper (String _terrierPath) {
		if(_terrierPath.endsWith("/")) {
			System.setProperty("terrier.home", _terrierPath);
			this.terrier_home = _terrierPath;
		}
		else {
			System.setProperty("terrier.home", _terrierPath+"/");
			this.terrier_home = _terrierPath+"/";
		}
		
		/** TODO: Laod more and add support for them*/
		this.langCodeMap.put("en", "English");
		this.langCodeMap.put("es", "Spanish");
		this.langCodeMap.put("de", "German");
		this.langCodeMap.put("it", "Italian");
		this.langCodeMap.put("hi", "Hindi");
	}
	
	public int getTF(int docid, String aTerm) throws IOException {
		String term = "";
		BitIndexPointer indexPointer = (BitIndexPointer)this.doi.getDocumentEntry(docid);
		
		IterablePosting postings = this.di.getPostings(indexPointer);
		while (postings.next() != IterablePosting.EOL) {
			Map.Entry<String,LexiconEntry> lee = this.lex.getLexiconEntry(postings.getId());
			
			term = lee.getKey();
			if (aTerm.equals(term))
				return postings.getFrequency();
		}
		return 0;
	}
	
	/** Sets the stopword removal and points to the stopwords file 
	 * at "$terrier_home/share/" location. Make sure that language files is present.
	 * 
	 * @param lang -- should follow the "en", "es" convention
	 */
	public void setStopwordRemoval(String lang) {
		this.stopword_removal = true;
		System.setProperty("stopwords.filename", "stopword-list-"+lang+".txt");
		this.stopEngine = new BaseTermPipelineAccessor("Stopwords");
	}
	
	/**
	 * Returns a document id in Terrier, given its document name
	 * @param docName
	 * @param index
	 * @return
	 * @throws IOException
	 */
	public int getDocIdByDocName(String docName) throws IOException{
		MetaIndex meta = index.getMetaIndex();
		String[] k = meta.getKeys();
		 int docid = meta.getDocument("filename", docName);
		 return docid;
	}
	
	public void setLanguage(String lang) {
		this.lang = lang;
	}
	
	public void setModelParamC(double c) {
		this.paramC = c;
	}
	
	public long getTotIndexedDocs() {
		return this.total_docs;
	}
	
	/** Sets the stemmer environment of the Terrier. Makes an 
	 * object of the TermPipeline and sets it with the stemmer.
	 * The Stemmer is Snowball and the possible laguages are
	 * English, Danish, Dutch, German, French, Finnish, Italian, Hugarian, 
	 * Norwegian, Portuguese, Romanian, Russian, Spanish, Swedish, Turkish
	 * 
	 * @param lang - ISO 639-1 Code of the language (e.g. "en", "es" etc.)
	 */
	public void setStemmer(String lang) {
		this.stem = true;
		this.stemEngine = new BaseTermPipelineAccessor(this.langCodeMap.get(lang)+"SnowballStemmer");
	}
	
	/** Set necessary location of the index with path and prefix*/
	public void setIndex(String _indexPath, String prefix) {
		this.path_to_index = _indexPath;
		this.indexPrefix = prefix;
	}
	
	/** Acts as a term pipeline
	 * 
	 * @param term
	 * @return "" if its a stopword or the stemmed output
	 */
	public String processTerm(String term) {
		String t=term;
		if(this.stopword_removal) {
			if(!this.isStopWord(term))
				t = term;
		}
		if(t.length()>0 && this.stem)
			t = this.stemEngine.pipelineTerm(t);
		if(!this.stopword_removal && !this.stem)
			t = term;
		return t;
	}
	
	public List<String> stopwords;
	/** This class tries to act as a customized user interface 
	 * to the terrier in order to get the ranked list for 
	 * given query and already built index with the specified model 
	 * with/without query expansion.
	 * 
	 * @param query = query in the String format (Make sure the query is tokenized already)
	 * @param match_model = Matching model like [TF_IDF, BM25, InL2, LemurTF_IDF, PL2, DFR_BM25]
	 * @param qe = whether to enable QE {@value true, false}
	 * @throws IOException 
	 */
	public ResultSet getResultSet(String query, 
			String match_model, 
			boolean qe,
			int N) throws RecognitionException, TokenStreamException, IOException {
		if(N>this.total_docs) {
			N = 0;
			System.out.println("#(Total Documents in Index) = " + this.total_docs + "\n" +
			"Hence setting N  = " + this.total_docs + "\n");
		}
		if(!stopword_removal && !stem)
			System.setProperty("termpipelines", "");
		if(stopword_removal) {
			this.setStopwordRemoval(lang);
			System.setProperty("stopwords.filename", "stopword-list-"+lang+".txt");
			System.setProperty("termpipelines", "Stopwords");
		}
		if(stem) {
			this.setStemmer(lang);
			System.setProperty("termpipelines", "Stopwords,"+this.langCodeMap.get(lang)+"SnowballStemmer");
			System.setProperty("stopwords.filename", "stopword-list-"+lang+".txt");
		}

		System.setProperty("string.use_utf","true");
		System.setProperty("tokeniser","UTFTokeniser");
		
		/** if you need top N results then set it to "N". "0" means all the documents in the index*/
		System.setProperty("matching.retrieved_set_size", Integer.toString(N));
		
		Manager manager = new Manager(this.index);
		SearchRequest srq = manager.newSearchRequest();

		TerrierLexer lexer = new TerrierLexer(new StringReader(query));
		TerrierFloatLexer flexer = new TerrierFloatLexer(lexer.getInputState());

		TokenStreamSelector selector = new TokenStreamSelector();
		selector.addInputStream(lexer, "main");
		selector.addInputStream(flexer, "numbers");
		selector.select("main");
		TerrierQueryParser parser = new TerrierQueryParser(selector);
		parser.setSelector(selector);

		srq.setQuery(parser.query());
		srq.addMatchingModel("Matching", match_model);
		
		if(match_model.toLowerCase().contains("hiemstra")) {
			if(this.paramC!=0.0)
				srq.setControl("c", Double.toString(paramC));
			else
				srq.setControl("c", "0.7");
		}

		if(qe) {
			srq.setControl("qe", (qe?"true":"false"));
			System.setProperty("querying.postprocesses.order", "QueryExpansion");
			System.setProperty("querying.postprocesses.controls","qe:QueryExpansion");
			manager.setProperty("querying.postprocesses.order", "QueryExpansion");
			manager.setProperty("querying.postprocesses.controls","qe:QueryExpansion");
		}
		
		//run the query
		manager.runPreProcessing(srq);
		manager.runMatching(srq);		
		manager.runPostProcessing(srq);
		manager.runPostFilters(srq);
		
		ResultSet rs = srq.getResultSet();
		
		return rs;
	}
	
	/** Updates some index statistics.*/
	public void updateCollectionStatistics() {
		this.total_docs = this.index.getCollectionStatistics().getNumberOfDocuments();
		this.total_tokens = this.index.getCollectionStatistics().getNumberOfTokens();
		this.loadIDFMapAndMaxIDF();
	}
	
	/** Use UTFTokenizer of terrier to tokenize given String.
	 * 
	 * @param s Input String
	 * @return Tokenized version
	 * @throws IOException
	 */
	public String tokenizeTerrier(String s) throws IOException {
		System.setProperty("tokeniser", "UTFTokeniser");
		Tokeniser tokeniser = Tokeniser.getTokeniser();
        
        TokenStream toks = tokeniser.tokenise(new StringReader(s));
        
        String tokens="";
        String token="";
        while(toks.hasNext()) {
        	if((token = toks.next())!= null)
        		tokens += " " + token;
        }
		return tokens; 
	}
	
	/** Stems using the Snowball Stemmer for aforementioned Language in the setStemmer().
	 * 
	 * @param s - term to Stem
	 * @return - Stemmed output
	 */
	public String stem(String s) {
		return this.stemEngine.pipelineTerm(s);
	}
	
/*	public void loadStopwords(File f) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
		this.stopwords = new ArrayList<String>();
		
		String line = "";
		
		while((line = br.readLine())!=null) {
			this.stopwords.add(line.trim());
		}
	}*/
	
	/** to check if given term is a stopword or not*/
	public boolean isStopWord(String term) {
		try {
			if(this.stopEngine.pipelineTerm(term)==null)
				return true;
			else
				return false;
		}
		catch(Exception e) {
			System.out.println("Stopword list is not yet specified. Please use \"setStopwordRemoval()\" method first with language.");
			return false;
		}
	}
	
	/** Converts a file into query. Useful when a complete document is used as a query.
	 * 
	 * @param f Input File
	 * @return tokenzed text of the file
	 * @throws IOException
	 */
	public String getQuery(File f) throws IOException {
		System.setProperty("tokeniser", "UTFTokeniser");
		Tokeniser tokeniser = Tokeniser.getTokeniser();
		
		
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
		int len_file_so = (int)f.length();
        char buf1[] = new char[len_file_so];
        br.read(buf1,0,len_file_so);
        
        StringBuffer source = new StringBuffer();
        for (int j = 0; j < len_file_so; j++){ 
        	source.append((char)buf1[j]);
        }
        
        TokenStream toks = tokeniser.tokenise(new StringReader(source.toString()));
        String query="";
        String token = "";
        while(toks.hasNext()) {
        	if( (token = toks.next())!= null) {
        		String processedToken = this.processTerm(token);
        		if(processedToken.length()>0 && processedToken!=null)
        			query += " " + processedToken;
        	}
        }
        br.close();
		return query; 
	}
	
	/** This method returns a \<Integer, String\> map 
	 * with Integer corresponding to the filename String. Also creates "$prefix.docid.map" file with this 
	 * information at the index path. 
	 * 
	 * @param path = path to the collection.spec file used for generating the index.
	 */
	public Map<Integer, String> learnDocId(String pathToCollectionSpec) throws IOException {
		BufferedReader br= new BufferedReader(new InputStreamReader(new FileInputStream(pathToCollectionSpec),"UTF-8"));
		String line;
		FileOutputStream fos = new FileOutputStream(this.path_to_index + this.indexPrefix+".docid.map");
		PrintStream p = new PrintStream(fos);
		Map<Integer, String> map = new HashMap<Integer, String>();
		int id=0;
		while((line = br.readLine())!=null) {
			if(line.startsWith("#")) {
				continue;
			}
			p.println(id + "\t" + line);
			map.put(id, line);
			id++;
		}
		br.close();
		p.close();
		fos.close();
		return map;
	}
	
	/** This method returns a \<Integer, String\> map 
	 * with Integer corresponding to the filename String. Also creates "$prefix.docid.map" file with this 
	 * information at the index path. 
	 */
	public void learnDocId() throws IOException {
		BufferedReader br= new BufferedReader(new InputStreamReader(new FileInputStream(this.terrier_home+"etc/collection.spec"),"UTF-8"));
		String line;
		FileOutputStream fos = new FileOutputStream(this.path_to_index + this.indexPrefix+".docid.map");
		PrintStream p = new PrintStream(fos);
		Map<Integer, String> map = new HashMap<Integer, String>();
		int id=0;
		while((line = br.readLine())!=null) {
			if(line.startsWith("#")) {
				continue;
			}
			p.println(id + "\t" + line);
			map.put(id, line);
			id++;
		}
		br.close();
		p.close();
		fos.close();
	}
	
	/** Learns the document name from the docids according to the mapfile
	 * 
	 * @param mapFile
	 * @return
	 * @throws IOException
	 */
	public TIntObjectHashMap<String> learnDocName(String mapFile) throws IOException {
		BufferedReader br= new BufferedReader(new InputStreamReader(new FileInputStream(mapFile),"UTF-8"));
		String line;
		TIntObjectHashMap<String> map = new TIntObjectHashMap<String>();
		while((line = br.readLine())!=null) {
			if(line.startsWith("#")) {
				continue;
			}
			String[] cols = line.split("\t");
			map.put(Integer.parseInt(cols[0]), cols[1].substring(cols[1].lastIndexOf("/")+1).trim());
		}
		br.close();
		return map;
	}
	
	/** Raw TF  = Collection TF of the term
	 * 
	 * @param term input term
	 * @return
	 */
	public int getRawTF(String term) {
		LexiconEntry le = lex.getLexiconEntry(term);
		if (le != null)
			return le.getFrequency();
		else
			return 0;
	}
	
	/** Returns the normalised TF of the 'term' devided by total tokens in the index
	 * 
	 * @param term
	 * @return
	 */
	public float getNormTF(String term) {
		LexiconEntry le = lex.getLexiconEntry(term);
		if (le != null)
			return ( (float) le.getFrequency() /(float) this.total_tokens);
		else
			return 0;
	}
	
	public float getTfOPCA(String term) {
		LexiconEntry le = lex.getLexiconEntry(term);
		if (le != null)
			return ((float)(Math.log((double)(1+ le.getFrequency()))) / (float) Math.log(2));
		else
			return 0;
	}
	
	/** Document Length of the document with docid.
	 * 
	 * @param docid
	 * @return
	 * @throws IOException
	 */
	public int getDocLength(int docid) throws IOException {
		return doi.getDocumentLength(docid);
	}
	
	/** Gives maximun TF within the document. Useful when collecting document level 
	 * probabilities of the terms.
	 * @param docid
	 * @return
	 * @throws IOException
	 */
	public int getMaxDocTF(int docid) throws IOException {
		int max = 0;
		IterablePosting postings = di.getPostings((BitIndexPointer)doi.getDocumentEntry(docid));
		while (postings.next() != IterablePosting.EOL) {
			if(postings.getFrequency()>max)
				max = postings.getFrequency();
		}
		return max;
	}
	
	/** Return the 'IDF' of the 'term'. Calculates based on the formula
	 * log(N/df) where N = total documents in the collection.
	 * 
	 * @param term
	 * @return
	 */
	public float getIDF(String term) {
		float idf = 0;
		long df = 0;

		LexiconEntry le = lex.getLexiconEntry(term);
		if (le != null)
			df = le.getDocumentFrequency();
		if(df!=0)
			idf = (float) Math.log10(((float)this.total_docs / (float) df));
		
		return idf;
	}
	
	/** Return the 'id' of the 'term' in the index.
	 * 
	 * @param term
	 * @return id - if present OR -1 - if not present
	 */
	public int getTermId(String term) {
		int id = -1;
		LexiconEntry le = lex.getLexiconEntry(term);
		if (le != null)
			id = le.getTermId();
		return id;
	}
	
	/** To get ID of a particular term. If the term is not present, returns null.
	 * 
	 * @param id
	 * @return
	 */
	public String getTerm(int id) {
		Map.Entry<String,LexiconEntry> lee = this.lex.getLexiconEntry(id);
		return lee.getKey();
	}
	
	/** To get the vector of the given document as TF-IDF score or Word Count. Binary Vector
	 * can easily be made based on the returned vector.
	 * 
	 * @param f Input File
	 * @param wordCount 
	 * @return
	 * @throws IOException
	 */
	public TIntFloatHashMap getVector(File f, boolean wordCount) throws IOException {
		TIntFloatHashMap vec = new TIntFloatHashMap();
		
		String fileStr = this.getQuery(f);
		String[] tokens = fileStr.split(" ");
		
		TObjectIntHashMap<String> tfMap = new TObjectIntHashMap<String>();
		for(String s: tokens) {
			if(tfMap.contains(s))
				tfMap.put(s, tfMap.get(s)+1);
			else
				tfMap.put(s, 1);
		}
		
		int maxTF= 1;
		for(String s: tfMap.keySet())
			if(tfMap.get(s)>maxTF)
				maxTF = tfMap.get(s);
		
		for(String t: tokens) {
			if(t.length()>1) {
				LexiconEntry le = this.lex.getLexiconEntry(t);
				if (le != null) {
					int id = this.getTermId(t);
					float idf = this.idfMap.get(id);
					
					if(this.maxIDF!=0)
						idf/=this.maxIDF;
					if(wordCount)
						vec.put(id, tfMap.get(t));
					else
						vec.put(id, (float)(((float)tfMap.get(t)/(float)maxTF)*idf) );
				}
			}
		}
		return vec;
	}
	
	/** This method prepares a word count or real valued vector for the input docid. The word count vector is if a term 
	 * is present with what frequency, while the real vector is the TF-IDF of the terms present in the document.
	 * 
	 * @param docid
	 * @param wordCount
	 * @return
	 * @throws IOException
	 */
	public TIntFloatHashMap getVector(int docid, boolean wordCount) throws IOException {
		TIntFloatHashMap vec = new TIntFloatHashMap();
		
		int maxTF= 1;
		if(docid!=-1)
			maxTF = this.getMaxDocTF(docid);
		
		IterablePosting postings = di.getPostings((BitIndexPointer)doi.getDocumentEntry(docid));
		while (postings.next() != IterablePosting.EOL) {
			int termId = postings.getId();
			int tf = postings.getFrequency();
			
			float idf = this.idfMap.get(termId);
			
			if(this.maxIDF!=0)
				idf/=this.maxIDF;
			
			if(wordCount)
				vec.put(termId, tf);
			else
				vec.put(termId, (float)(((float)tf/(float)maxTF)*idf) );
		}
		return vec;
	}
	
	/** Loads the IDF of all the terms in the index and set the maxIDF parameter for future use.
	 * 
	 */
	public void loadIDFMapAndMaxIDF() {
		float idf = 0;
		long df = 0;
		
		double max = 0.0;
		int tot_terms = lex.numberOfEntries();
		for(int i=0; i<tot_terms; i++) {
			Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(i);
			
			if (lee.getValue() != null)
				df = lee.getValue().getDocumentFrequency();
			if(df!=0)
				idf = (float) Math.log10(((float)this.total_docs / (float) df));
			
			this.idfMap.put(i, idf);
			if(idf>max)
				max= idf;
		}
		this.maxIDF = max;
	}
	
	/** Term-DocumentFrequency Map
	 * 
	 * @return
	 */
	public Map<String, Integer> getTermDFTable() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		int tot_terms = lex.numberOfEntries();
		for(int i=0; i<tot_terms; i++) {
			Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(i);
			map.put(lee.getKey(), lee.getValue().getDocumentFrequency());
		}
		return map;
	}
	
	
	public TIntFloatHashMap getTermIdDFTable() {
		if(this.idfMap==null)
			this.loadIDFMapAndMaxIDF();
		return this.idfMap;
	}
	
	public float getIdfOPCA(String term, int n) {
		float idf = 0;
		long df = 0;
		Lexicon<String> lex = this.index.getLexicon();
		LexiconEntry le = lex.getLexiconEntry(term);
		if (le != null)
			df = le.getDocumentFrequency();
		if(df!=0)
			idf = ((float) Math.log(((float)n / (float) df)) / (float) Math.log(2));

		return idf;
	}
	
	/** Calculates the TF-IDF scores of the terms of the specified document
	 * and returns top N terms in an array.
	 * 
	 * @param docid
	 * @param N
	 * @return
	 * @throws IOException
	 */
	public String[] topTerms(int docid, int N) throws IOException {
		String[] terms = new String[N];
		
		Map<String, Double> map = new HashMap<String, Double>();
		
		int df = 0;
		int TF=0;
		String term = "";
		BitIndexPointer indexPointer = (BitIndexPointer)this.doi.getDocumentEntry(docid);
		IterablePosting postings = this.di.getPostings(indexPointer);
		while (postings.next() != IterablePosting.EOL) {
			Map.Entry<String,LexiconEntry> lee = this.lex.getLexiconEntry(postings.getId());
			
			term = lee.getKey();
			TF = postings.getFrequency();
			
			LexiconEntry le = this.lex.getLexiconEntry(term);
			if (le != null)
				df =  le.getDocumentFrequency();
			
			double idf = this.getIDF(term);
			
			double score = TF*idf;
			map.put(term, score);
		}
		
		ValueComparator1 bvc =  new ValueComparator1(map);
		TreeMap<String,Double> sorted_scoremap = new TreeMap<String,Double>(bvc);
        sorted_scoremap.putAll(map);
		
        int count = 0;
        for(String s: sorted_scoremap.keySet()) {
        	if(count<N) {
        		terms[count] = s;
        		count++;
        	}
        	else 
        		break;
        }
		return terms;
	}
	
	/** This method creates the index of type, Anyway it considers the collection to be 
	 * SimpleFileCollection i.e. plain text.
	 * TODO Add support for other collection classes.
	 * 
	 * @param path = Path to create the index
	 * @param prefix = prefix to use for index
	 * @param collection_type = Type of the collection {@value "SimpleFileCollection"}
	 * @throws IOException 
	 */
	public void prepareIndex(String index_path, String prefix, 
			String collection_extention, 
			String lang, 
			boolean stopword_removal, 
			boolean stem) throws IOException {
		if(true || collection_extention.endsWith("txt") || collection_extention.endsWith("text")||collection_extention.endsWith("utf8"))
			System.setProperty("trec.collection.class", "SimpleFileCollection");
		if(!stopword_removal && !stem)
			System.setProperty("termpipelines", "");
		if(stopword_removal) {
			this.setStopwordRemoval(lang);
			System.setProperty("stopwords.filename", "stopword-list-"+lang+".txt");
			System.setProperty("termpipelines", "Stopwords");
		}
		if(stem) {
			this.setStemmer(lang);
			System.setProperty("termpipelines", "Stopwords,"+this.langCodeMap.get(lang)+"SnowballStemmer");
			System.setProperty("stopwords.filename", "stopword-list-"+lang+".txt");
		}
		
		
		System.setProperty("string.use_utf","true");
		System.setProperty("tokeniser","UTFTokeniser");
		
		TRECIndexing trecIndexing = new TRECIndexing(index_path, prefix);
		trecIndexing.index();
		
		this.index = Index.createIndex(index_path, prefix);
		updateCollectionStatistics();
		
		this.learnDocId();
		
		this.idMap = this.learnDocName(this.path_to_index+lang+".docid.map");
		for(int i: this.idMap.keys()) {
			this.idReverseMap.put(this.idMap.get(i), i);
		}
	}
	
	/** Indexes the data. Use this method for indexing. 
	 * 
	 * @param path_to_data
	 * @param collection_extention
	 * @param lang
	 * @param stopword_removal
	 * @param stem
	 * @throws IOException
	 */
	public void prepareIndex(String path_to_data, 
			String collection_extention, String lang, 
			boolean stopword_removal, boolean stem) throws IOException {
		if(!new File(this.path_to_index).exists())
			new File(this.path_to_index).mkdirs();
		
		this.loadDocuments(path_to_data);
		
		if(collection_extention.contains("txt") || collection_extention.contains("text"))
			System.setProperty("trec.collection.class", "SimpleFileCollection");
		if(!stopword_removal && !stem)
			System.setProperty("termpipelines", "");
		if(stopword_removal) {
			this.setStopwordRemoval(lang);
			System.setProperty("stopwords.filename", "stopword-list-"+lang+".txt");
			System.setProperty("termpipelines", "Stopwords");
		}
		if(stem) {
			this.setStemmer(lang);
			System.setProperty("termpipelines", "Stopwords,"+this.langCodeMap.get(lang)+"SnowballStemmer");
			System.setProperty("stopwords.filename", "stopword-list-"+lang+".txt");
		}
		
		
		System.setProperty("string.use_utf","true");
		System.setProperty("tokeniser","UTFTokeniser");
		
		TRECIndexing trecIndexing = new TRECIndexing(this.path_to_index, this.indexPrefix);
		trecIndexing.index();

    this.learnDocId();
    this.loadIndex(this.path_to_index, this.indexPrefix, lang);
	}
	
	public void loadIndex(String path_to_index, String prefix, String lang) throws IOException {
		this.setLanguage(lang);
		this.setIndex(path_to_index, prefix);
		this.index = Index.createIndex(path_to_index, prefix);
		
		this.idMap = this.learnDocName(this.path_to_index+prefix+".docid.map");
		for(int i: this.idMap.keys()) {
			this.idReverseMap.put(this.idMap.get(i), i);
		}
		
		this.lex = this.index.getLexicon(); 
		this.di = this.index.getDirectIndex();
		this.doi = this.index.getDocumentIndex();
		
		updateCollectionStatistics();
	}
	
	public void loadIndex(String path_to_index, String prefix, String lang,
			boolean stopword_removal,
			boolean stem) throws IOException {
		
		this.setLanguage(lang);
		
		if(stopword_removal)
			this.setStopwordRemoval(this.lang);
		if(stem)
			this.setStemmer(lang);
		
		this.setIndex(path_to_index, prefix);
		this.index = Index.createIndex(path_to_index, prefix);
		
		this.idMap = this.learnDocName(this.path_to_index+prefix+".docid.map");
		for(int i: this.idMap.keys()) {
			this.idReverseMap.put(this.idMap.get(i), i);
		}
		
		this.lex = this.index.getLexicon(); 
		this.di = this.index.getDirectIndex();
		this.doi = this.index.getDocumentIndex();
		
		updateCollectionStatistics();
	}
	
	/** This method prepares the list of files present in the directory 
	 * to be used for indexing. Basically creates "collection.spec" file
	 * used by terrier to index.
	 * 
	 *  TODO: It only supports the directory structure till depth 3. Extend it.
	 * 
	 * @param path_to_documents
	 * @throws IOException
	 */
	
	public void loadDocuments(String path_to_documents) throws IOException {
		File collectionSpecFile =  new File(this.terrier_home +"/etc/collection.spec");
		File dir = new File(path_to_documents);
		File[] files = dir.listFiles();
		
		FileOutputStream fos = new FileOutputStream(collectionSpecFile);
		PrintStream p = new PrintStream(fos);
		
		for(File f:files) {
			if (f.getName().equals(".DS_Store"))
				continue;
			if(f.isDirectory()) {
				File[] F1Dir = f.listFiles();
				for(File f1: F1Dir )  {
					if(f1.isDirectory()) {
						File[] F2Dir = f1.listFiles();
						for(File f2: F2Dir) {
							p.println(f2.getAbsolutePath());
						}
					}
					else
						p.println(f1.getAbsolutePath());
				}
					
			}
			else
				p.println(f.getAbsolutePath());
		}
		
		p.close();
		fos.close();
	}
	
	/** Prints the terms of the specifid dodid to the console.
	 * 
	 * @param id
	 * @throws IOException
	 */
	public void printTermsOfDocument(int id) throws IOException {
		int[][] postings = di.getTerms(id);
			
		for(int i=0;i<postings[0].length; i++){
			Entry<String, LexiconEntry> le =lex.getLexiconEntry(postings[0][i]);
			System.out.println(le.getKey() + "-- id = " + le.getValue().getTermId()+ " with frequency "+ postings[1][i]);
		}
	}
	
	/** Generates a map of terms in the document and its within document term frequencies.
	 * 
	 * @param docid
	 * @return
	 * @throws IOException
	 */
	public TIntIntHashMap docTF(int docid) throws IOException {
		TIntIntHashMap map = new TIntIntHashMap();
		Lexicon<String> lex = this.index.getLexicon();
		IterablePosting postings = di.getPostings((BitIndexPointer)doi.getDocumentEntry(docid));
		while (postings.next() != IterablePosting.EOL) {
			Map.Entry<String,LexiconEntry> lee = lex.getLexiconEntry(postings.getId());
			map.put(lee.getValue().getTermId(), postings.getFrequency());
		}
		return map;
	}
	public TIntObjectHashMap<TIntFloatHashMap> termDocMatrixOPCA(int n) throws IOException {
		TIntObjectHashMap<TIntFloatHashMap> map = new TIntObjectHashMap<TIntFloatHashMap>();
		for(int doc = 0; doc< n; doc++) {
			TIntIntHashMap tfMap = this.docTF(doc);
			TIntFloatHashMap inner_map = new TIntFloatHashMap();
			for(int i: tfMap.keys()) {
				float weight = tfMap.get(i);
//				weight = this.getTfOPCA(le.getKey()) * this.getIdfOPCA(le.getKey(), 2*n);
				inner_map.put(i, weight);
			}
				
/*			for(int i=0;i<postings[0].length; i++) {
				Entry<String, LexiconEntry> le =lex.getLexiconEntry(postings[0][i]);
				float weight = 0;
//				weight = this.getTfOPCA(le.getKey()) * this.getIdfOPCA(le.getKey(), 2*n);
				weight = this.getRawTF(le.getKey());
				inner_map.put(le.getValue().getTermId(), weight);			
			}*/
			map.put(doc, inner_map);
		}
		
		return map;
	}
	
	public int getDimension() {
		return lex.numberOfEntries();
	}
	
	/** This method returns the expanded query with specified pseudo relevance feedback model 
	 * and relevance model. For possible qeModel see 
	 * http://terrier.org/docs/v3.5/javadoc/org/terrier/matching/models/queryexpansion/package-summary.html 
	 * 
	 * @param model
	 * @param qeModel
	 * @param query
	 * @return
	 * @throws RecognitionException
	 * @throws TokenStreamException
	 * @throws IOException
	 */
	public Map<Integer, String> getExpandedTerms(String model, String qeModel, String query) throws RecognitionException, TokenStreamException, IOException {
		Manager manager = new Manager(index);
		SearchRequest srq = manager.newSearchRequest();
		
		srq.setControl("expansion.terms", "5");
		srq.setControl("expansion.documents", "1");
		
		manager.setProperty("expansion.terms", "5");
		manager.setProperty("expansion.documents", "1");
		
		System.setProperty("expansion.terms", "5");
		System.setProperty("expansion.documents", "1");
		
		

		TerrierLexer lexer = new TerrierLexer(new StringReader(query));
		TerrierFloatLexer flexer = new TerrierFloatLexer(lexer.getInputState());

		TokenStreamSelector selector = new TokenStreamSelector();
		selector.addInputStream(lexer, "main");
		selector.addInputStream(flexer, "numbers");
		selector.select("main");
		TerrierQueryParser parser = new TerrierQueryParser(selector);
		parser.setSelector(selector);

		srq.setQuery(parser.query());
		srq.addMatchingModel("Matching", model);
		srq.setControl("qemodel",qeModel);
		srq.setControl("expansion.terms", "5");
//		System.setProperty("trec.qe.model", qeModel);
//		srq.setQuery(query);
		srq.setControl("qe", "true");
		
		
		System.setProperty("querying.postprocesses.order", "QueryExpansion");
		System.setProperty("querying.postprocesses.controls","qe:QueryExpansion");
		
		manager.setProperty("querying.postprocesses.order", "QueryExpansion");
		manager.setProperty("querying.postprocesses.controls","qe:QueryExpansion");
		
		//run the query
		manager.runPreProcessing(srq);
		manager.runMatching(srq);
		String[] temp = srq.getQuery().toString().split(" ");

		srq.setControl("expansion.terms", "5");
		srq.setControl("expansion.documents", "1");
		
		manager.setProperty("expansion.terms", "5");
		manager.setProperty("expansion.documents", "1");
		
		System.setProperty("expansion.terms", "5");
		System.setProperty("expansion.documents", "1");
		System.setProperty("expansion.mindocuments", "1");
		
		if(temp.length > 1) {
			manager.runPostProcessing(srq);
			manager.runPostFilters(srq);
		}
		
//		System.out.println(srq.getQuery().toString());
		Map<Integer, String> termWeightMap = new HashMap<Integer, String>();
		MatchingQueryTerms queryTerms = ((Request)srq).getMatchingQueryTerms();
		final String[] newQueryTerms = queryTerms.getTerms();
		final double[] newQueryTermWeights = queryTerms.getTermWeights();
		
		for(int i=0; i<newQueryTerms.length; i++) {
			termWeightMap.put(i, newQueryTerms[i]+"^"+newQueryTermWeights[i]);
		}
		
		return termWeightMap;
	}
	public static void main(String[] args) throws IOException {
		String lang = "en";
		TerrierWrapper terrier = new TerrierWrapper("/home/parth/workspace/terrier-3.5");
		terrier.loadIndex("/home/parth/workspace/terrier-3.5/var/index/movie-dic/", "en", lang);
		
		 MetaIndex meta = terrier.index.getMetaIndex();
		 //get docno of document with id 10
		 String terms[] = terrier.topTerms(25000, 3);
		 System.out.println(terms[0] + " " + terms[1] + " " + terms[2]);
	}
}

/** This is to sort the hash maps based on its values (type Double).
 * If you wish to change it for other types like (Integer, Float etc.) then change the casting.
 * This is currently sorts the hashmap in decreasing order like ranklist.
 * If you need to change it to ascending order then reverse the "/<" sign to "/>".
 *  
 * Courtesy: some StackOverflow page.
 */

class ValueComparator1 implements Comparator {

	  Map base;
	  public ValueComparator1(Map base) {
	      this.base = base;
	  }

	  public int compare(Object a, Object b) {

	    if((Double)base.get(a) < (Double)base.get(b)) {
	      return 1;
	    } else if((Double)base.get(a) == (Double)base.get(b)) {
	      return 0;
	    } else {
	      return -1;
	    }
	  }
	}
