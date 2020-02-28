package filebrowser.model;

import java.util.List;

public class ComplexResult {
	List<Resource> directory;
	int size;

    public ComplexResult(List<Resource> directory, int size) {
        this.directory = directory;
        this.size = size;
    }
}
