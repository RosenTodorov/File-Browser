package filebrowser.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

public class ZipData {
	private static final int BUFFER_SIZE = 4096;

	public static void zipFolder(List<String> filePaths, HttpServletResponse response) throws IOException {
		try (ZipOutputStream zipStream = new ZipOutputStream(response.getOutputStream())) {
			for (String currentDirectory : filePaths) {
				File directory = new File(currentDirectory);
				
				zipFile(directory, directory.getName(), zipStream);
			}
		}
	}

	private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOutputStrem) throws IOException {
		if (fileToZip.isDirectory()) {
			File[] children = fileToZip.listFiles();
			for (File childFile : children) {
				zipFile(childFile, fileName + "/" + childFile.getName(), zipOutputStrem);
			}
		  return;
		}	
		FileInputStream fileInputStream = new FileInputStream(fileToZip);
		
		zipOutputStrem.putNextEntry(new ZipEntry(fileName));
		
		byte[] buffer = new byte[BUFFER_SIZE];
		int readBytesCount = 0;
		while ((readBytesCount = fileInputStream.read(buffer)) >= 0) {
			zipOutputStrem.write(buffer, 0, readBytesCount);
		}
		fileInputStream.close();
	}
}
