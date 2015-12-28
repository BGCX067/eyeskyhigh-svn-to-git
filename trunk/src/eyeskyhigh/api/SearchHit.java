package eyeskyhigh.api;

import java.io.File;

public class SearchHit {
	public File file;
	public float score;

	public SearchHit(File f, float score) {
		this.file = f;
		this.score = score;
	}
}
