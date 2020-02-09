package dpco.library.parsa.classes;

import java.io.File;
import java.io.IOException;

import javax.portlet.PortletRequest;

import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.PortalUtil;

/**
 * helper to upload file in easiest way in Liferay
 * 
 * @author p.mihandoost
 * @version 1.0.0
 *
 */
public class FileUploader {

	protected PortletRequest request;
	/**
	 * this file will be uploaded
	 */
	protected File file;

	/**
	 * use to get file from request
	 */
	protected String fileParam;
	/**
	 * it's file name when uploaded (default we use file.geName() for it)
	 */
	protected String fileName;
	/**
	 * your will be uploaded in this path on the server
	 */
	protected String filePath;

	/**
	 * default constructor
	 * 
	 * @param request
	 */
	public FileUploader(PortletRequest request, String fileParam, String filePath) {
		this.fileParam = fileParam;
		UploadPortletRequest upRequest = PortalUtil.getUploadPortletRequest(request);
		this.file = upRequest.getFile(fileParam);
		setFilePath(filePath);
		setFileName(file.getName());
		this.request = request;
	}

	/**
	 * get file path
	 * 
	 * @return String
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * get absolute path which will be uploaded path
	 * 
	 * @return {@link String}
	 */
	public String getAbsolute() {
		return filePath + File.separator + fileName;
	}

	/**
	 * set file path it necessary to upload file
	 * 
	 * @param filePath
	 * @return FileUploader
	 */
	public FileUploader setFilePath(String filePath) {
		String serverPath = System.getProperty("catalina.base");
		this.filePath = serverPath + File.separator + filePath.replace("/", File.separator);
		return this;
	}

	/**
	 * get current file name
	 * 
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * get current file name
	 * 
	 * @return
	 */
	public String getFileSuffix() {
		String suffix = fileName.split("\\.")[1];
		return suffix;
	}

	/**
	 * set or change file name
	 * 
	 * @param fileName
	 * @return {@link FileUploader}
	 */
	public FileUploader setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	/**
	 * get file which will be uploaded
	 * 
	 * @return
	 */
	public File getFile() {
		return file;
	}

	/**
	 * set file. from out of class you can set it
	 * 
	 * @param file
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/**
	 * 
	 * it will upload file in chosen path and return true if success
	 * 
	 * @throws IOException
	 */
	public void upload() throws IOException {
		File newFile = new File(filePath + File.separator + fileName);
		// copy from temp to filePath folder
		FileUtil.copyFile(file, newFile);
	}

	/**
	 * upload if file exits otherwise do nothing
	 * 
	 * @throws IOException
	 */
	public void safeUpload() throws IOException {
		if ((file != null) && file.exists()) {
			upload();
		}
	}
}
