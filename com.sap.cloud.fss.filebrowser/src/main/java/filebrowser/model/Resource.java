package filebrowser.model;

public class Resource {
	private String path;
	private String name;
	private Boolean isFolder;
	private long size;

	public Resource(String path, String name, Boolean isFolder, long size) {
		super();
		this.path = path;
		this.name = name;
		this.isFolder = isFolder;
		this.size = size;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getIsFolder() {
		return isFolder;
	}

	public void setIsFolder(Boolean isFolder) {
		this.isFolder = isFolder;
	}

	public long getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
