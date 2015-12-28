package eyeskyhigh.api;

public class HashTask extends Task {

    private String[] locations = null;

    public HashTask(String location) {
        super(Task.TASK_HASH);
        this.locations = new String[] { location };
    }
    
    public HashTask(String[] locations) {
        super(Task.TASK_HASH);
        this.locations = locations;
    }

    public String[] getLocations() {
        return locations;
    }
}
