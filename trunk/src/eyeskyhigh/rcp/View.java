package eyeskyhigh.rcp;

import java.io.File;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.ViewPart;

import eyeskyhigh.api.IndexTask;
import eyeskyhigh.api.SearchHit;
import eyeskyhigh.api.SearchLocation;
import eyeskyhigh.api.SearchQuery;
import eyeskyhigh.api.SearchTask;
import eyeskyhigh.api.Task;
import eyeskyhigh.lucene.SearchEngineWrapper;
import eyeskyhigh.lucene.demo.FileDocument;

public class View extends ViewPart implements Observer {
	public static final String ID = "eyeskyhigh.rcp.view";

	private final View _this = this;
    private Section searchInputSection;
    private Text tFiles, tText;
    private Button chText, bSearch;
    
    private Section searchOutputSection;
    private TableViewer tvResults;
    private Table tblResults;
    private FormToolkit toolkit;
    private ScrolledForm form;
    
    private Section indexHashSection;
    private Table tblIndexHash;
    private CheckboxTableViewer tvIndexHash;
    private Button bAdd, bRemove;
    
    private SearchTask searchTask;
    
    public final static int TABLE_RESULTS_COLUMN_FILE_IDX = 0;
    public final static int TABLE_RESULTS_COLUMN_PATH_IDX = 1;
    public final static int TABLE_RESULTS_COLUMN_RELEVANCE_IDX = 2;
    
    public final static String[] TABLE_RESULTS_COLUMNS_TEXTS = {
    		"File",
    		"Path",
    		"Relevance"
    };
    
    public final static int[] TABLE_RESULTS_COLUMNS_WIDTHS = {
    		150,
    		275,
    		100
    };
    
    public final static int TABLE_INDEX_HASH_COLUMN_PATH_IDX = 0;
    public final static int TABLE_INDEX_HASH_COLUMN_INDEXED_IDX = 1;
    public final static int TABLE_INDEX_HASH_COLUMN_HASHED_IDX = 2;
    
    protected final static String[] TABLE_INDEX_HASH_COLUMNS_TEXTS = {
    		"Path",
    		"Indexed",
    		"Hashed"
    };
    
    protected final static int[] TABLE_INDEX_HASH_COLUMNS_WIDTHS = {
    		400, 100, 100
    };
    
    private static class SearchResultsTableLabelProvider extends LabelProvider implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			//element is a SearchLocation
			switch(columnIndex) {
			case TABLE_RESULTS_COLUMN_FILE_IDX:
				return ((SearchHit)element).file.getName();
			case TABLE_RESULTS_COLUMN_PATH_IDX:
				return ((SearchHit)element).file.getPath();
			case TABLE_RESULTS_COLUMN_RELEVANCE_IDX:
				return ((SearchHit)element).score + "";
			}
			return null;
		}
    	
    }
    
	private static class LocationsTableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		public String getColumnText(Object element, int columnIndex) {
			//element is a SearchLocation
			switch(columnIndex) {
			case TABLE_INDEX_HASH_COLUMN_PATH_IDX:
				return ((SearchLocation)element).path.getAbsolutePath();
			case TABLE_INDEX_HASH_COLUMN_INDEXED_IDX:
				return ((SearchLocation)element).indexed.toString();
			case TABLE_INDEX_HASH_COLUMN_HASHED_IDX:
				return ((SearchLocation)element).hashed.toString();
			}
			return null;
		}
	}
    
    private class LocationsCellModifier implements ICellModifier {
		
		public boolean canModify(Object element, String property) {
			//the path column is not editable but the rest are
			return !property.equals(TABLE_INDEX_HASH_COLUMNS_TEXTS[TABLE_INDEX_HASH_COLUMN_PATH_IDX]);
		}

		public Object getValue(Object element, String property) {
		     if (element instanceof Item) {
		         element = ((Item)element).getData();
		     }
		     if (property.equals(TABLE_INDEX_HASH_COLUMNS_TEXTS[TABLE_INDEX_HASH_COLUMN_PATH_IDX])) {
		    	 return ((SearchLocation)element).path;
		     }
		     if (property.equals(TABLE_INDEX_HASH_COLUMNS_TEXTS[TABLE_INDEX_HASH_COLUMN_INDEXED_IDX])) {
		    	 return ((SearchLocation)element).indexed.get();
		     }
		     if (property.equals(TABLE_INDEX_HASH_COLUMNS_TEXTS[TABLE_INDEX_HASH_COLUMN_HASHED_IDX])) {
		    	 return ((SearchLocation)element).hashed.get();
		     }
		     return null;
		}

		public void modify(Object element, String property, Object value) {
		     if (element instanceof Item) {
		         element = ((Item)element).getData();
		     }
		     if (property.equals(TABLE_INDEX_HASH_COLUMNS_TEXTS[TABLE_INDEX_HASH_COLUMN_INDEXED_IDX])) {
		    	 //change the indexed state of the location
		    	 ((SearchLocation)element).indexed.set((Boolean)value);
		    	 tvIndexHash.update(element, new String[] {property});
		    	 //create the indexing task and schedule its run
		    	 IndexTask indexTask = new IndexTask(((SearchLocation)element).path.getAbsolutePath(), IndexTask.ADD_INDEX);
		    	 indexTask.addObserver(_this);
		    	 ESHPlugin.getDefault().getRichLucene().doTask(indexTask);
		     }
		     else if (property.equals(TABLE_INDEX_HASH_COLUMNS_TEXTS[TABLE_INDEX_HASH_COLUMN_HASHED_IDX])) {
		    	 //change the indexed state of the location
		    	 ((SearchLocation)element).hashed.set((Boolean)value);
		    	 tvIndexHash.update(element, new String[] {property});
		     }
		}
	}
    
    
	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
	    this.toolkit = new FormToolkit(parent.getDisplay());
	    parent.setLayout(new GridLayout(1, false));
	    form = toolkit.createScrolledForm(parent);
	    form.setText("Eye Sky High");
        toolkit.decorateFormHeading(form.getForm());
        form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        Composite body = form.getBody();
        TableWrapLayout layout = new TableWrapLayout();
        layout.numColumns = 2;
        body.setLayout(layout);
        body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.createSearchInputSection(body);
        this.createSearchOutputSection(body);
        this.createIndexHashSection(body);
        this.initialize();
	}

    private void createSearchInputSection(Composite parent) {
        this.searchInputSection = this.toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
        this.searchInputSection.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB));
        this.searchInputSection.setText("Search Input");
        this.searchInputSection.setLayout(new GridLayout(1, false));
        
        Composite client = this.toolkit.createComposite(this.searchInputSection);
        
        client.setLayout(new GridLayout(3, false));
        client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.searchInputSection.setClient(client);
        
        //put the search controls
        Label tLabel = this.toolkit.createLabel(client, "Search for");
        this.tFiles = this.toolkit.createText(client, "");
        this.tFiles.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1));
        this.tFiles.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				handleFilesModified();
			}
        });
        this.chText = this.toolkit.createButton(client, "Find text", SWT.CHECK);
        this.chText.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		handleTextSelected();
        	}
        });
        this.tText = this.toolkit.createText(client, "");
        this.tText.setEnabled(false);
        this.tText.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1));
        this.tText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				handleTextModified();
			}
        });
        this.bSearch = this.toolkit.createButton(client, "Start search", SWT.NONE);
        this.bSearch.setLayoutData(new GridData(GridData.END, GridData.BEGINNING, false, false));
        this.bSearch.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		handleSearch();
        	}
        });
    }
	
	private void createSearchOutputSection(Composite parent) {
        this.searchOutputSection = this.toolkit.createSection(parent, ExpandableComposite.TITLE_BAR);
        this.searchOutputSection.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB));
        this.searchOutputSection.setText("Search Output");
        this.searchOutputSection.setLayout(new GridLayout(1, false));

        Composite client = this.toolkit.createComposite(this.searchOutputSection);
        client.setLayout(new GridLayout(1, false));
        client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.searchOutputSection.setClient(client);
        
        this.tblResults = this.toolkit.createTable(client, SWT.BORDER | SWT.FULL_SELECTION);
        this.tblResults.setHeaderVisible(true);
        this.tblResults.setLinesVisible(true);
        this.tblResults.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        TableColumn tableColumn;
        for(int i = 0; i < TABLE_RESULTS_COLUMNS_TEXTS.length; i++) {
        	tableColumn = new TableColumn(tblResults, SWT.NONE);
        	tableColumn.setText(TABLE_RESULTS_COLUMNS_TEXTS[i]);
        	tableColumn.setWidth(TABLE_RESULTS_COLUMNS_WIDTHS[i]);
        }
        this.tvResults = new TableViewer(tblResults);
        this.tvResults.setLabelProvider(new SearchResultsTableLabelProvider());
        this.tvResults.setContentProvider(new ArrayContentProvider());
    }
	
	private void createIndexHashSection(Composite parent) {
		this.indexHashSection = this.toolkit.createSection(parent, ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
				| ExpandableComposite.EXPANDED);
        this.indexHashSection.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.BOTTOM, 1, 2));
        this.indexHashSection.setText("Watched Folders");
        this.indexHashSection.setLayout(new GridLayout(1, false));
        
        Composite client = this.toolkit.createComposite(this.indexHashSection);
        client.setLayout(new GridLayout(2, false));
        client.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.indexHashSection.setClient(client);
        
        this.tblIndexHash = this.toolkit.createTable(client, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
        this.tblIndexHash.setHeaderVisible(true);
        this.tblIndexHash.setLinesVisible(true);
        this.tblIndexHash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 2));
        TableColumn tableColumn;
        for(int i = 0; i < TABLE_INDEX_HASH_COLUMNS_TEXTS.length; i++) {
        	tableColumn = new TableColumn(this.tblIndexHash, SWT.NONE);
        	tableColumn.setText(TABLE_INDEX_HASH_COLUMNS_TEXTS[i]);
        	tableColumn.setWidth(TABLE_INDEX_HASH_COLUMNS_WIDTHS[i]);
        }
        this.tvIndexHash = new CheckboxTableViewer(this.tblIndexHash);
        this.tvIndexHash.setColumnProperties(TABLE_INDEX_HASH_COLUMNS_TEXTS);
        this.tvIndexHash.setLabelProvider(new LocationsTableLabelProvider());
        this.tvIndexHash.setContentProvider(new ArrayContentProvider());
        this.tvIndexHash.setCellModifier(new LocationsCellModifier());
        this.tvIndexHash.setCellEditors(new CellEditor[] {null, new CheckboxCellEditor(tblIndexHash),
        		new CheckboxCellEditor(tblIndexHash)});
        //enable the remove button when an element in the table is selected
        this.tblIndexHash.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		bRemove.setEnabled(((IStructuredSelection)tvIndexHash.getSelection()).toArray().length > 0);
        	}
        });
        this.tvIndexHash.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				String scope = ((SearchLocation)event.getElement()).path.getAbsolutePath();
				if (event.getChecked()) {
					searchTask.addScope(scope);
				}
				else {
					searchTask.removeScope(scope);
				}
			}
        });
        
        this.bAdd = this.toolkit.createButton(client, "Add new", SWT.NONE);
        this.bAdd.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		handleAddLocation();
        	}
        });
        this.bRemove = this.toolkit.createButton(client, "Remove", SWT.NONE);
        this.bRemove.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		handleRemoveLocation();
        	}
        });
        this.bRemove.setEnabled(false);
	}

	private void initialize() {
		ESHPlugin.getDefault().getLucene().addObserver(this);
		this.searchTask = new SearchTask();
		this.tvIndexHash.setInput(ESHPlugin.getDefault().getLucene().getMonitoredLocations());
	}
	
	private void handleTextModified() {
		this.searchTask.getSearchQuery(FileDocument.FIELD_CONTENTS).query = this.tText.getText();
	}

	/**
	 * Enables or disables the "text" textbox, depending on the selection.
	 * It also adds/removes a search query for the content field.
	 */
	private void handleTextSelected() {
		if (this.chText.getSelection()) {
			this.searchTask.addSearchQuery(new SearchQuery(this.tText.getText(), FileDocument.FIELD_CONTENTS));
			this.tText.setEnabled(true);
		}
		else {
			this.searchTask.removeSearchQuery(FileDocument.FIELD_CONTENTS);
			this.tText.setEnabled(false);
		}
	}

	/**
	 * Modifies the query of the path search query in the search task (if one exists)
	 * or creates a new path search query using the content of the tFiles textbox.
	 */
	private void handleFilesModified() {
		SearchQuery searchQuery = this.searchTask.getSearchQuery(FileDocument.FIELD_PATH);
		if (searchQuery != null) {
			searchQuery.query = this.tFiles.getText();
		}
		else {
			searchQuery = new SearchQuery(this.tFiles.getText(), FileDocument.FIELD_PATH);
			this.searchTask.addSearchQuery(searchQuery);
		}
	}

	private void handleSearch() {
		if (this.tFiles.getText().equals("")) {
			this.form.setMessage("Please enter a search query for the name of the files!", IMessage.ERROR);
			return;
		}
		if (this.chText.getSelection() && this.tText.getText().equals("")) {
			this.form.setMessage("You have activated searching inside files. Please enter a " +
					"search query for this criteria!", IMessage.ERROR);
			return;
		}
		if (this.tvIndexHash.getCheckedElements().length == 0) {
			this.form.setMessage("Please check at least one of the monitored locations where you wish to search!", IMessage.ERROR);
			return;
		}
		//if it got here then go ahead with the search! - remove the error messages & empty the results table
		this.form.setMessage(null, IMessage.NONE);
		this.tvResults.setInput(new SearchHit[] {});
		//delete old observers & reset the task (current object but old search - that might still be in progress)
		this.searchTask.reset();
		this.searchTask.deleteObservers();
		this.searchTask.addObserver(this);
		
		ESHPlugin.getDefault().getRichLucene().doTask(this.searchTask);
	}
	
	private void handleAddLocation() {
		// TODO Auto-generated method stub
		DirectoryDialog dialog = new DirectoryDialog(this.getSite().getShell());
		String location;
		if ((location = dialog.open()) != null) {
			SearchEngineWrapper lucene = ESHPlugin.getDefault().getLucene();
			lucene.monitorLocation(location);
		}
	}

	private void handleRemoveLocation() {
		// TODO Auto-generated method stub
		
	}
	
    /**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		this.tFiles.setFocus();
	}

	/* (non-Javadoc)
	 * Called by a task that has an update
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(final Observable arg0, final Object arg1) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (arg0 instanceof IndexTask) {
					String[] messages = ((Task)arg0).getMessages();
					form.setMessage(messages[messages.length - 1], IMessage.INFORMATION);
				}
				else if (arg0 instanceof SearchEngineWrapper) {
					tvIndexHash.setInput(((SearchEngineWrapper)arg0).getMonitoredLocations());
				}
				else if (arg0 instanceof SearchTask) {
					tvResults.setInput(((SearchTask)arg0).getPartialResults());
				}
			}
		});
		
	}
}