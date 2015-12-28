package eyeskyhigh.api;

import java.util.Observable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Task extends Observable {
    
    public static final int TASK_SEARCH = 1;
    public static final int TASK_INDEX = 2;
    public static final int TASK_HASH = 3;
    public static final int TASK_FILE_COMPARISON = 4;
    
    private final static int MIN_PROGRESS = 0;
    private final static int MAX_PROGRESS = 100;
    
    private int type;
    
    private AtomicBoolean done = new AtomicBoolean(false);
    private AtomicInteger progress = new AtomicInteger(MIN_PROGRESS);
    private ConcurrentLinkedQueue<String> messages  = new ConcurrentLinkedQueue<String>();
    private Exception exception = null;
    private Boolean exceptionSemaphore = new Boolean(false);

    public Task(int type) {
        this.type = type;
    }
    
    public void reset() {
    	this.done.set(false);
    	this.progress.set(MIN_PROGRESS);
    	this.messages = new ConcurrentLinkedQueue<String>();
    	this.setException(null);
    }
    
    public int getType() {
        return type;
    }
    
    public int getProgress() {
        return this.progress.get();
    }
    
    /**
     * Increases the overall progress of the task with progress and adds the given message to the queue.
     * If the overall progress reaches MAX_PROGRESS it automatically calls the {@link Task#done()} method.
     * Throws an exception for each case of illegal usage.
     * @param progress The progress of the task since the last call to this method, as percent.
     * @param messages A string to be added to the queue of messages for this task. If null it is ignored.
     * @throws IllegalArgumentException If total progress is greater than MAX_PROGRESS
     * @throws IllegalStateException If the method is called after the task has ended.
     */
    public void progress(int progress, String[] messages) throws IllegalArgumentException, IllegalStateException,
        IllegalAccessException {
        if (this.isDone()) {
            throw new IllegalStateException();
        }
        if (this.progress.get() + progress > MAX_PROGRESS) {
            throw new IllegalArgumentException();
        }
        this.progress.getAndAdd(progress);
        if (messages != null) {
            for(String message : messages) {
                this.messages.add(message);
            }
        }
        if (this.progress.get() == MAX_PROGRESS) {
            this.done();
        }
        else {
            this.setChanged();
            this.notifyObservers();
        }
    }
    
    public void flushMessages() {
        if (!this.messages.isEmpty()) {
            this.messages.remove();
        }
    }
    
    public String[] getMessages() {
        return this.messages.toArray(new String[] {});
    }
    
    public void done() throws IllegalStateException {
        if (this.done.get()) {
            throw new IllegalStateException();
        }
        this.done.set(true);
        this.setChanged();
        this.notifyObservers();
    }
    
    public boolean isDone() {
        return done.get();
    }
    
    public Exception getException() {
        synchronized (exceptionSemaphore) {
            return exception;    
        }
    }

    public void setException(Exception exception) {
        synchronized (exceptionSemaphore) {
            this.exception = exception;
            if (this.exception != null) {
	            this.messages.add(exception.getMessage());
	            this.done();
            }
        }
    }
    
    public boolean isSuccessful() throws IllegalStateException {
        if (!this.isDone()) {
            throw new IllegalStateException();
        }
        synchronized (exception) {
            return this.exception != null;        
        }
    }
}