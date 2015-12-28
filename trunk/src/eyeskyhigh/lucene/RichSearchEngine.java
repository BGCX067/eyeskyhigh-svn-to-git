package eyeskyhigh.lucene;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.ui.progress.UIJob;

import eyeskyhigh.api.FileVisitor;
import eyeskyhigh.api.HashTask;
import eyeskyhigh.api.IndexTask;
import eyeskyhigh.api.SearchHit;
import eyeskyhigh.api.SearchLocation;
import eyeskyhigh.api.SearchQuery;
import eyeskyhigh.api.SearchTask;
import eyeskyhigh.api.Task;
import eyeskyhigh.lucene.demo.FileDocument;
import eyeskyhigh.rcp.ESHPlugin;

public class RichSearchEngine {

	private SearchEngineWrapper lucene;
	
	private static class RWDouble {
		double value = 0;
	}
	
	public RichSearchEngine(SearchEngineWrapper lucene) {
		this.lucene = lucene;
	}
	
	public Job doTask(Task task) {
		switch(task.getType()) {
			case Task.TASK_INDEX:
				return this.doIndexTask((IndexTask)task);
			case Task.TASK_HASH:
				return this.doHashTask((HashTask)task);
			case Task.TASK_SEARCH:
				return this.doSearchTask((SearchTask)task);
		}
		return null;
	}

	private Job doIndexTask(final IndexTask task) {
		Job indexJob = new Job("Indexing " + task.getLocations()[0] + "...") {
			private final AtomicInteger filesNo = new AtomicInteger(0);
			@Override
			public IStatus run(final IProgressMonitor monitor) {
				try {
					monitor.beginTask("Indexing", 100);
					monitor.subTask("Calculating duration estimate");
					//first calculate an estimate of the actual duration of the process (visit each resource)
					String[] locations = task.getLocations();
					for(int i = 0; i < locations.length; i++) {
						new FileVisitor() {
							@Override
							protected void doAction(File file) throws CoreException {
								filesNo.incrementAndGet();
							}
						}.visit(new File(locations[i]));
					}
					//done calculating estimate
					monitor.worked(5);
					try {
						task.progress(5, new String[] {"Finished estimating duration"});
					} catch (Exception e) {
						e.printStackTrace();
						throw new CoreException(
								new Status(IStatus.ERROR, ESHPlugin.PLUGIN_ID, IStatus.ERROR, "Indexing failed: " + e.getMessage(), e));
					}
					//now do the actual indexing
					final double unitaryProgress = filesNo.get() / 95;
					final RWDouble crrProgress = new RWDouble();
					SearchLocation tLocation;
					for(int i = 0; i < locations.length; i++) {
						//if this location overlaps with an existing monitored one, skip it
						tLocation = new SearchLocation(new File(locations[i]),
								new AtomicBoolean(true), new AtomicBoolean(false));
						if (lucene.overlapLocation(tLocation)) {
							continue;
						}
						//if lucene is already monitoring this resource, update it 
						if (lucene.getSearchLocation(tLocation.path.getAbsolutePath()) != null) {
							lucene.getSearchLocation(tLocation.path.getAbsolutePath()).indexed.set(true);
						}
						//otherwise add a new location to the monitoring list
						else {
							lucene.monitorLocation(tLocation);							
						}
						
						new FileVisitor() {
							@Override
							protected void doAction(File file) throws CoreException {
								try {
									lucene.indexFile(file);
									crrProgress.value += unitaryProgress;
									//notify the progress monitor
									monitor.worked((int)(crrProgress.value / 1));
									crrProgress.value %= 1;
									//notify the task that will notify its observers
									task.progress((int)(crrProgress.value) / 1, new String[] {
											"Finished indexing " + file.getAbsolutePath()});
								} catch (Exception e) {
									e.printStackTrace();
									throw new CoreException(
											new Status(IStatus.ERROR, ESHPlugin.PLUGIN_ID, IStatus.ERROR, "Indexing failed: " + e.getMessage(), e));
								}
							}
						}.visit(new File(locations[i]));
						lucene.optimizeIndex();
						lucene.commitIndex();
					}
					return new Status(IStatus.OK, ESHPlugin.PLUGIN_ID, IStatus.OK, "Indexing finished ok", null);
				} catch (Exception e) {
					e.printStackTrace();
					return new Status(IStatus.ERROR, ESHPlugin.PLUGIN_ID, IStatus.ERROR, "Indexing failed: " + e.getMessage(), e);
				}
			}
		};
		indexJob.schedule();
		return indexJob;
	}
	
	private Job doHashTask(HashTask task) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Job doSearchTask(final SearchTask task) {
		Job searchJob = new Job("Searching... " ) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					SearchQuery contentQuery;
					String[] scopes = task.getScopes();
					String fileNameQuery = task.getSearchQuery(FileDocument.FIELD_PATH).query;
					BooleanQuery fQuery = new BooleanQuery();
					//here compose the query for the path
					for(int i = 0; i < scopes.length; i++) {
						fQuery.add(new WildcardQuery(new Term(FileDocument.FIELD_PATH, scopes[i] + fileNameQuery)),
								BooleanClause.Occur.SHOULD);
					}
					fQuery.setMinimumNumberShouldMatch(1);
					//here compose the query for the content (if specified)
					if ((contentQuery = task.getSearchQuery(FileDocument.FIELD_CONTENTS)) != null) {
						BooleanQuery tQuery = new BooleanQuery();
						tQuery.add(fQuery, BooleanClause.Occur.MUST);
						tQuery.add(new WildcardQuery(new Term(contentQuery.field, contentQuery.query)),
								BooleanClause.Occur.MUST);
						fQuery = tQuery;
					}
					lucene.searchFiles(fQuery, new HitCollector() {
						@Override
						public void collect(int arg0, float arg1) {
							Document doc;
							try {
								System.out.println("found " + arg0);
								doc = lucene.getDocument(arg0);
								task.addResults(new SearchHit[] {new SearchHit(new File(doc.get(FileDocument.FIELD_PATH)), arg1)}, 0);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					return new Status(IStatus.ERROR, ESHPlugin.PLUGIN_ID, IStatus.ERROR, "Searching failed: " + e.getMessage(), e);
				}
				return new Status(IStatus.OK, ESHPlugin.PLUGIN_ID, IStatus.OK, "Searching finished ok", null);
			}
			
		};
		searchJob.schedule();
		return searchJob;
	}
}
