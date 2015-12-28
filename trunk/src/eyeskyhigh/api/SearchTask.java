package eyeskyhigh.api;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SearchTask extends Task {

    private Hashtable<String, SearchQuery> searchQueries = new Hashtable<String, SearchQuery>();
    private Vector<String> scope = new Vector<String>();

	private ConcurrentLinkedQueue<SearchHit> results = new ConcurrentLinkedQueue<SearchHit>();

    public SearchTask() {
        super(Task.TASK_SEARCH);
    }
    
    public void reset() {
    	super.reset();
    	this.results = new ConcurrentLinkedQueue<SearchHit>();
    }
	
    public void addSearchQuery(SearchQuery searchQuery) {
    	if (!this.searchQueries.containsKey(searchQuery.field)) {
    		this.searchQueries.put(searchQuery.field, searchQuery);    		
    	}
    }
    
    public void removeSearchQuery(String field) {
    	this.searchQueries.remove(field);
    }
    
    public SearchQuery getSearchQuery(String field) {
    	return this.searchQueries.get(field);
    }
    
    public SearchQuery[] getSearchQueries() {
    	return this.searchQueries.values().toArray(new SearchQuery[] {});
    }
    
	public String[] getScopes() {
		return scope.toArray(new String[] {});
	}

	public void addScope(String scope) {
		this.scope.add(scope);
	}
	
	public boolean removeScope(String scope) {
		return this.scope.remove(scope);
	}
    
    /**
     * Adds a set of files that matched the search query to the existing set.
     * It also increases the overall progress of the task with the value of "progress".
     * @param results Set of files that match the search query to add to the existing set of found files.
     * @param progress 
     * @throws IllegalStateException
     */
    public void addResults(SearchHit[] results, int progress) throws IllegalStateException {
        if (this.isDone()) {
            throw new IllegalStateException();
        }
        String[] messages = new String[results.length];
        for(int i = 0; i < results.length; i++) {
            this.results.add(results[i]);
            messages[i] = results[i].file.getAbsolutePath();
        }
        try {
            super.progress(progress, messages);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
    /**
     * Returns the matching files for this search query. It can only be called after the task has finished.
     * @return The result of the search query. Null if an exception has occurred.
     * @throws IllegalStateException
     */
    public SearchHit[] getResults() throws IllegalStateException {
        if (!this.isDone()) {
            throw new IllegalStateException();
        }
        return this.getException() == null ? this.results.toArray(new SearchHit[] {}) : new SearchHit[] {};
    }
    
    /**
     * Returns the matching files for this search query. It cannot be called if the search has finished.
     * @return The result of the search query. Null if an exception has occurred.
     * @throws IllegalStateException
     */
    public SearchHit[] getPartialResults() {
        return this.getException() == null ? this.results.toArray(new SearchHit[] {}) : new SearchHit[] {};
    }
    
    /* (non-Javadoc)
     * Progress should never be called directly from this class.
     * Progress info is handled automatically from SearchTask#addResults(File[], int)
     * @see eyeskyhigh.api.Task#progress(int, java.lang.String[])
     */
    @Override
    public void progress(int progress, String[] messages) throws IllegalAccessException {
        throw new IllegalAccessException();
    }
    
}