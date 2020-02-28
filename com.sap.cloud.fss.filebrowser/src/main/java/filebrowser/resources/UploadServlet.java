package filebrowser.resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet("/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {
	private static final String REDIRECTPAGE = "/#page-1";

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			String filePath = request.getParameter("path");
			String currentPath = DataResource.checkPath(filePath);
			String validatePath = DataResource.validateFilePath(currentPath);

			String path = DataResource.validateUploadPath(validatePath);

			ArrayList<Part> fileParts = (ArrayList<Part>) request.getParts();

			for (Part filePart : fileParts) {
				if (filePart.getName().equals("file")) {
					String filename = filePart.getSubmittedFileName().toString();
					File file = new File(path);
					File uploads = new File(file, filename);

					try (InputStream input = filePart.getInputStream()) {
						Files.copy(input, uploads.toPath());
						response.setStatus(HttpServletResponse.SC_OK);
					}
				}
			}
			response.sendRedirect(REDIRECTPAGE);
		} catch (IOException e) {
			e.printStackTrace();
			response.sendRedirect(REDIRECTPAGE);
		}
	}
}