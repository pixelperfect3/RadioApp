/** Represents a favorite (station/song) in the database **/

package radio.app;

public class Favorite {
	/** Properties **/
	public static enum Type {
		STATION, SONG;
	}
	
	private int id;
	private Type type;
	private String name;
	
	/** Methods **/
	
	public long getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Type getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public void setType(Type type) {
		this.type = type;
	}

	public void setName(String name) {
		this.name = name;
	}

	// Will be used by the ArrayAdapter in the ListView
	@Override
	public String toString() {
		return name;
	}
}
