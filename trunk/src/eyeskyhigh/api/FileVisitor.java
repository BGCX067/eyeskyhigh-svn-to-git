package eyeskyhigh.api;

import java.io.File;

import org.eclipse.core.runtime.CoreException;

public abstract class FileVisitor {

	public void visit(File file) throws CoreException {
		if (file.canRead()) {
			if (file.isDirectory()) {
				File[] children = file.listFiles();
				for(File tFile : children) {
					this.visit(tFile);
				}
			}
			else {
				this.doAction(file);
			}
		}
	}

	protected abstract void doAction(File file) throws CoreException;	
}
