package eyeskyhigh.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import eyeskyhigh.api.SearchLocation;
import eyeskyhigh.api.SearchQuery;
import eyeskyhigh.api.SearchTask;
import eyeskyhigh.lucene.demo.FileDocument;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

public class SearchEngineWrapper extends Observable {

    private Hashtable<String, SearchLocation> monitoredLocations = new Hashtable<String, SearchLocation>();
	private String indexStoreLocation = null;
    private File indexStoreDir = null;
    private IndexWriter writer;
    private IndexReader reader;
    private Searcher searcher;
    private Analyzer analyzer;
    
    public SearchEngineWrapper(String indexLocation) {
        this.indexStoreLocation = indexLocation;
    }

    public void initialize() throws IOException {
        this.indexStoreDir = new File(this.indexStoreLocation);
        this.analyzer = new StandardAnalyzer();
        this.writer = new IndexWriter(this.indexStoreDir, this.analyzer, IndexWriter.MaxFieldLength.LIMITED);
        this.reader = IndexReader.open(this.indexStoreDir);
        this.searcher = new IndexSearcher(reader);
    }
    
    public void indexFile(File file) throws IOException {
        // do not try to index files that cannot be read
        if (file.canRead()) {
//            this.monitorLocation(new SearchLocation(file, new AtomicBoolean(true), new AtomicBoolean(false)));
            if (!file.isDirectory()) {
                System.out.println("adding " + file);
                try {
                    writer.addDocument(FileDocument.Document(file));
                }
                // at least on windows, some temporary files raise this
                // exception with an "access denied" message
                // checking if the file can be read doesn't help
                catch (FileNotFoundException fnfe) {
                    ;
                }
            }
            else {
            	 String[] files = file.list();
                 // an IO error could occur
                 if (files != null) {
                   for (int i = 0; i < files.length; i++) {
                     indexFile(new File(file, files[i]));
                   }
                 }
            }
        }
    }

    public void searchFiles(Query query, HitCollector collector) throws ParseException, IOException {
    	//check to see if the reader is up to date
    	if (!this.reader.isCurrent()) {
    		this.searcher.close();
//    		this.reader.reopen();
    		this.reader.close();
    		this.reader = IndexReader.open(this.indexStoreDir);
    		this.searcher = new IndexSearcher(this.reader);
    	}
		this.searcher.search(query, collector);
    }
    
    /**
     * Creates and returns an array containing COPIES of the monitored locations for this engine.
     * Because it creates a copy for each one, this is an expensive method so use with care.
     * @return The locations monitored by this engine.
     */
    public SearchLocation[] getMonitoredLocations() {
		SearchLocation[] toReturn = new SearchLocation[this.monitoredLocations.size()];
		int i = 0;
		for(SearchLocation tLocation : this.monitoredLocations.values()) {
			toReturn[i++] = (SearchLocation)tLocation.clone();
		}
		return toReturn;
	}
    
    protected void optimizeIndex() throws IOException {
    	writer.optimize();
    }
    
    protected void commitIndex() throws IOException {
    	writer.commit();
    }
    
    /**
     * Method tests if the given location overlaps with an existing monitored location.
     * A location overlaps with another if the first is the parent of the second or vice-versa.
     * 2 locations that are identical do not overlap! 
     * @param location
     * @return
     */
    public boolean overlapLocation(SearchLocation location) {
    	//if the given location is one of the monitored ones, return false.
    	if (this.monitoredLocations.containsKey(location.path.getAbsolutePath())) {
    		return false;
    	}
    	//search for overlaping
    	for(SearchLocation tLocation : this.monitoredLocations.values()) {
    		if ((location.path.getAbsolutePath().startsWith(tLocation.path.getAbsolutePath())
    				|| tLocation.path.getAbsolutePath().startsWith(location.path.getAbsolutePath()))
    				&& !location.path.getAbsolutePath().equals(tLocation.path.getAbsolutePath())) {
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * Adds the new location to the list but checks for duplicates or for locations
     * that already contain the given one. It also removes old locations that are 
     * contained by the new one, if it is the case.
     * @param location The new location to be added.
     */
    protected void monitorLocation(SearchLocation location) {
    	int i = 0;
    	//first check to see if this location is not contained by another existing one
    	for(SearchLocation tLocation : this.monitoredLocations.values()) {
    		if (location.path.getAbsolutePath().startsWith(tLocation.path.getAbsolutePath())) {
    			return;
    		}
    		else if (tLocation.path.getAbsolutePath().startsWith(location.path.getAbsolutePath())) {
    			this.monitoredLocations.remove(i);
    		}
    		i++;
    	}
    	this.monitoredLocations.put(location.path.getAbsolutePath(), location);
    	this.setChanged();
    	this.notifyObservers();
    }
    
    protected SearchLocation getSearchLocation(String path) {
    	return this.monitoredLocations.get(path);
    }
    
    protected Document getDocument(int index) throws CorruptIndexException, IOException {
    	return this.reader.document(index);
    }
    
    /**
     * Convenience method for {@link SearchEngineWrapper#monitorLocation(SearchLocation)}
     * where the SearchLocation has both "indexed" and "hashed" fields set to false.
     * Only a RichSearchEngine has the right to add a SearchLocation that has one of these 2 fields
     * set to true. 
     * @param location
     */
    public void monitorLocation(String location) {
    	this.monitorLocation(new SearchLocation(new File(location), 
    			new AtomicBoolean(false), 
    			new AtomicBoolean(false)));
    }
    
    public void dispose() {
    	try {
    		this.searcher.close();
    		this.reader.close();
    		this.writer.close();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
    }
    
    /**
     * {@inheritDoc}
     */
    public void refresh() {
      try {
        this.reader.reopen();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
}
