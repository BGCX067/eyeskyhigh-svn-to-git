package eyeskyhigh.api;

public class IndexTask extends Task {

	public final static int ADD_INDEX = 1;
	public final static int REMOVE_INDEX = 2;
	
    private String[] locations; 
    private int action = 0;

	public IndexTask(String location, int action) {
        super(Task.TASK_INDEX);
        this.locations = new String[] { location };
        this.action = action;
    }
    
    public IndexTask(String[] locations, int action) {
        super(Task.TASK_INDEX);
        this.locations = locations;
        this.action = action;
    }

    public String[] getLocations() {
        return locations;
    }
    
    public int getAction() {
		return action;
	}

}