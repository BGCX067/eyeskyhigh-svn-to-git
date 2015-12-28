package eyeskyhigh.api;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class SearchLocation implements Cloneable {
	public File path;
	public AtomicBoolean indexed;
	public AtomicBoolean hashed;
	
	public SearchLocation(File path, AtomicBoolean indexed, AtomicBoolean hashed) {
		this.path = path;
		this.indexed = indexed;
		this.hashed = hashed;
	}	
	
	@Override
	public Object clone() {
		return new SearchLocation(new File(this.path.getAbsolutePath()), 
				new AtomicBoolean(this.indexed.get()),
				new AtomicBoolean(this.hashed.get()));
	}
}
