package filebrowser.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import filebrowser.model.ComplexResult;
import filebrowser.model.Resource;
import filebrowser.model.ZipData;

@Path("/tree")
public class DataResource {
	private static final String PATH = "path";
	private static final String FSS = "FSS";
	private static final String OCTETSTREAM = "application/octet-stream";
	private static final String CONTENTTYPEZIP = "application/zip";
	private static final String CONTENTDISPOSITION = "Content-Disposition";
	private static final String ATTACHMENT = "attachment; filename=";
	private static final String ZIPTYPE = ".zip";
	private static final String ZIPFILENAME = "packageZip";
	private static final String MEDIATYPEJSON = "application/json";
	private static final String ERRORMESSAGE = "Path directory is not correct";
	private static final String ERROREMPTYPATH = "Path cannot be empty.";
	private static final String PAGENUMBER = "pageNumber";
	private static final String ROOTPATH = "/usr/sap/ljs/FSS";
	private static final String SNAPSHOT = ".snapshot";
	private static final String PATHLINE = "/";
	private static final int PAGECONTENTSIZE = 10;
	
	
	@POST
	@Consumes(MEDIATYPEJSON)
	@Produces(MEDIATYPEJSON)
	public Response post(String jsonRequest, @Context UriInfo uriInfo) throws JSONException {
		JSONObject jsonObject = new JSONObject(jsonRequest);
		String currentDirectory = null;
		try {
			currentDirectory = validateFilePath(jsonObject.getString(PATH));
		} catch (IOException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERRORMESSAGE).build();
		}
		int pageNumber = Integer.parseInt(jsonObject.getString(PAGENUMBER));
		String newDirectory = currentPageContent(currentDirectory, pageNumber);
	
		URI uri = uriInfo.getAbsolutePathBuilder().build();
		return Response.created(uri).entity(newDirectory).build();
	}

	@POST
	@Path("/download")
	public void postDownload(@Context HttpServletResponse response, @Context HttpServletRequest request, @FormParam(PATH) String path) throws ServletException {
		try {
			String currentPath = checkPath(path);
			File file = new File(currentPath);
			
			String fileName = file.getName();

			response.setContentType(OCTETSTREAM);
			response.setHeader(CONTENTDISPOSITION, ATTACHMENT + fileName);
			ServletOutputStream out = response.getOutputStream();

			FileInputStream fileIn = null;
			try {
				fileIn = new FileInputStream(file);
				byte[] buf = new byte[8192];
				int bytesread = 0, bytesBuffered = 0;
				while ((bytesread = fileIn.read(buf)) > -1) {
					out.write(buf, 0, bytesread);
					bytesBuffered += bytesread;
					if (bytesBuffered > 1024 * 1024) { 
						bytesBuffered = 0;
						out.flush();
					}
				}
			} finally {
				if (out != null) {
					fileIn.close();
					out.flush();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@POST
	@Path("/zipdownload")
	public void postZipDownload(@Context HttpServletResponse response, @Context HttpServletRequest request, @FormParam(PATH) String path) throws ServletException {
		try {
			String currentPath = checkPath(path);

			List<String> filePaths = getFilePath(currentPath);

			File fileZip = new File(filePaths.get(0));
			String zipFileName = null;
			if (filePaths.size() > 1) {
				zipFileName = ZIPFILENAME + ZIPTYPE;
			} else {
				zipFileName = fileZip.getName() + ZIPTYPE;
			}

			response.setContentType(CONTENTTYPEZIP);
			response.setHeader(CONTENTDISPOSITION, ATTACHMENT + zipFileName);

			ZipData.zipFolder(filePaths, response);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@POST
	@Path("/delete")
	@Consumes(MEDIATYPEJSON)
	@Produces(MEDIATYPEJSON)
	public Response postDelete(String jsonRequest, @Context UriInfo uriInfo) throws JSONException, URISyntaxException, IOException {
		JSONObject jsonObject = new JSONObject(jsonRequest);
		String currentDirectory = null;
			try {
				currentDirectory = validateFilePath(jsonObject.getString(PATH));
			} catch (IOException e) {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ERRORMESSAGE).build();
			}

			List<String> filePaths = getFilePath(currentDirectory);
			for (String file : filePaths) {
				String filePath = validateDeletePath(file);
				File files = new File(filePath);
				deleteFiles(files);
			}
			
			int pageNumber = Integer.parseInt(jsonObject.getString(PAGENUMBER));
			String newDirectory = currentPageContent(filePaths.get(0).substring(0, filePaths.get(0).lastIndexOf(PATHLINE)), pageNumber);
		
			URI uri = uriInfo.getAbsolutePathBuilder().build();
			return Response.created(uri).entity(newDirectory).build();
	}
	
	private void deleteFiles(File files) {
		if (files.isDirectory() == false) {
			files.delete();
		} else {
			File[] listFiles = files.listFiles();
			for (File file : listFiles) {
				deleteFiles(file);
			}
			files.delete();
		}
	}

	private static List<String> getFilePath(String path) {
		List<String> filePaths = new ArrayList<String>();

		String[] directories = path.split(",");
		for (String currentDirectory : directories) {
			filePaths.add(currentDirectory);
		}
		return filePaths;
	}

	public static String checkPath(String path) throws IOException {
		if (path.length() != 0) {
			return path;
		} else {
			throw new IOException(ERROREMPTYPATH);
		}
	}

	public static String validateFilePath(String currentPath) throws IOException {
		if (currentPath.contains(FSS)) {
			return currentPath;
		} else {
			throw new IOException(ERRORMESSAGE);
		}
	}
	
	public static String validateDeletePath(String file) throws IOException {
		if (file.length() > ROOTPATH.length() && !file.equals(SNAPSHOT)) {
			String subPath = file.substring(ROOTPATH.length() + 1);
			String volumeName;
			if (subPath.contains(PATHLINE)) {
				volumeName = subPath.substring(0, subPath.indexOf(PATHLINE));
			} else {
				volumeName = subPath;
			}

			if (subPath.contains(volumeName) && (subPath.length() > volumeName.length())) {
				return file;
			} else {
				throw new IOException(ERRORMESSAGE);
			}
		} else {
			throw new IOException(ERRORMESSAGE);
		}
	}
	
	public static String validateUploadPath(String file) throws IOException {
		if (file.length() > ROOTPATH.length()) {
			String subPath = file.substring(ROOTPATH.length() + 1);
			String volumeName;
			if (subPath.contains(PATHLINE)) {
				volumeName = subPath.substring(0, subPath.indexOf(PATHLINE));
			} else {
				volumeName = subPath;
			}

			if (subPath.contains(volumeName)) {
				return file;
			} else {
				throw new IOException(ERRORMESSAGE);
			}
		} else {
			throw new IOException(ERRORMESSAGE);
		}
	}
	
	private static String currentPageContent(String currentDirectory, int pageNumber) {
		Gson gsonBuilder = new GsonBuilder().create();
		int size = pageNumber * PAGECONTENTSIZE;
		List<Resource> listFilesAndDirectories = listFiles(currentDirectory);
		int sizeOfContent = listFilesAndDirectories.size();	
		pageNumber = size - PAGECONTENTSIZE;
		List<Resource> partOfListFilesAndDirectories = getAllMessagesPaginated(listFilesAndDirectories, pageNumber, size);
		ComplexResult result = new ComplexResult(partOfListFilesAndDirectories, sizeOfContent);
		String newDirectory = gsonBuilder.toJson(result);
		return newDirectory;
	}

	private static List<Resource> listFiles(String path) {
		File[] files = new File(path).listFiles();
		List<Resource> allResources = new ArrayList<Resource>();
		Arrays.sort(files);
		for (File file : files) {
			if (file.isDirectory()) {
				allResources.add(new Resource(file.getAbsolutePath(), file.getName(),
						file.isDirectory(), file.length()));
			}
		}
		for (File file : files) {
			if (!file.isDirectory()) {
				allResources.add(new Resource(file.getAbsolutePath(), file.getName(),
						file.isDirectory(), file.length()));
			}
		}
		return allResources;
	}
	
	private static List<Resource> getAllMessagesPaginated(List<Resource> listFilesAndDirectories, int start, final int partitionSize) {
	    final int size = listFilesAndDirectories.size();
	    return listFilesAndDirectories.subList(start, Math.min(size, start + partitionSize));
	}
}