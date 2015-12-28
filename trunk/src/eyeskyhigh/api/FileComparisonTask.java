package eyeskyhigh.api;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FileComparisonTask extends Task {

    private File[] files = null;
    
    /**
     * A set of sets, each containing one or more files that are equal to one another.
     * This is where the result of the file comparison task is stored.
     * It is updated as the task progresses.
     */
    private ConcurrentLinkedQueue<File[]> comparedFiles = new ConcurrentLinkedQueue<File[]>();
    
    public FileComparisonTask(File[] files) {
        super(Task.TASK_FILE_COMPARISON);
        this.files = files;
    }

    public File[] getFiles() {
        return files;
    }
    
    /**
     * Adds a set of equal files to the results list and increases the overall progress of the task with progress.
     * @param equalFilesSet A subset of files equal to one another. 
     * @param progress The percent from the total results set that equalFilesSet represents.
     * @throws IllegalStateException If the method is called after the task has ended.
     */
    public void addResults(File[] equalFilesSet, int progress) throws IllegalStateException {
        if (this.isDone()) {
            throw new IllegalStateException();
        }
        comparedFiles.add(equalFilesSet);
        try {
            super.progress(progress, null);
        } catch (Exception e) {}
    }
    
    /**
     * Returns the files grouped by content equality. It can only be called after the task has finished.
     * @return The result of the file comparison. Null if an exception has occured.
     * @throws IllegalStateException
     */
    public File[][] getResults() throws IllegalStateException {
        if (!this.isDone()) {
            throw new IllegalStateException();
        }
        return this.getException() == null ? this.comparedFiles.toArray(new File[][] {}) : null;
    }
    
    /* (non-Javadoc)
     * Progress should never be called directly from this class.
     * Progress info is handled automatically from FileComparisonTask#addResults(File[], int)
     * @see eyeskyhigh.api.Task#progress(int, java.lang.String[])
     */
    @Override
    public void progress(int progress, String[] messages) throws IllegalAccessException {
        throw new IllegalAccessException();
    }
}
