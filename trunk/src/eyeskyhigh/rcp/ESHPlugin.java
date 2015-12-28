package eyeskyhigh.rcp;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import eyeskyhigh.lucene.RichSearchEngine;
import eyeskyhigh.lucene.SearchEngineWrapper;

/**
 * The activator class controls the plug-in life cycle
 */
public class ESHPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "eyeskyhigh";

	// The shared instance
	private static ESHPlugin plugin;
	private SearchEngineWrapper lucene;
	private RichSearchEngine richLucene;
	
	/**
	 * The constructor
	 */
	public ESHPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		this.lucene = new SearchEngineWrapper("E:\\_work\\Facultate\\Master\\SOA\\index");
		this.lucene.initialize();
		this.richLucene = new RichSearchEngine(this.lucene);
		
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		this.lucene.dispose();
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ESHPlugin getDefault() {
		return plugin;
	}
	
	public SearchEngineWrapper getLucene() {
		return lucene;
	}
	
	public RichSearchEngine getRichLucene() {
		return richLucene;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
