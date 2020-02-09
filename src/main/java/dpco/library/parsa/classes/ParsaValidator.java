package dpco.library.parsa.classes;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;

import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.PortalUtil;

/**
 * simple validator class
 *
 * @author p.mihandoost
 * @version 1.0.1
 */
public class ParsaValidator {

	/**
	 * it stores map of parameter `name` to their rules
	 */
	private Map<String, String> rules;

	/**
	 * it stores map of rule name to its given error message (user give it. default
	 * is empty)
	 */
	private Map<String, String> messages = new HashMap<String, String>();

	/**
	 * it use to get value of parameters in original servlet request
	 */
	private HttpServletRequest servletRequest;

	/**
	 * * it use to get value of parameters from action request
	 */
	private PortletRequest actionRequest;

	/**
	 * it stores error messages
	 */
	private List<String> errors = new ArrayList<String>();

	/**
	 * store name of current running rule (without `Rule` word). (main usage in
	 * sendMessage method)
	 */
	private String ruleName;



	/**
	 * default constructor
	 *
	 * @param param
	 */
	private ParsaValidator(Map<String, String> param, PortletRequest request) {
		this.rules = param;
		this.actionRequest = request;
	}



	/**
	 * factory of this class from Liferay portal
	 *
	 * @param param
	 * @return ParsaValidator
	 */
	public static ParsaValidator getValidator(Map<String, String> param, PortletRequest request) {
		HttpServletRequest servletRequest = PortalUtil
				.getOriginalServletRequest(PortalUtil.getHttpServletRequest(request));
		ParsaValidator newValidator = new ParsaValidator(param, request);
		newValidator.setServletRequest(servletRequest);
		return newValidator;
	}



	/**
	 * @param messages the messages to set
	 */
	public ParsaValidator setMessages(Map<String, String> messages) {
		this.messages = messages;
		return this;
	}



	/**
	 * @param servletRequest the servletRequest to set
	 */
	public void setServletRequest(HttpServletRequest servletRequest) {
		this.servletRequest = servletRequest;
	}



	/**
	 * @return the errors
	 */
	public List<String> getErrors() {
		return errors;
	}



	/**
	 * analyze list of parameter with their rules and set error
	 */
	public ParsaValidator analyze() {
		for (Map.Entry<String, String> entry : rules.entrySet()) {
			String[] paramRules = entry.getValue().split("\\|");
			// call rule methods
			for (String methodName : paramRules) {

				String parameterValue = actionRequest.getParameter(entry.getKey());

				if (parameterValue == null)
					parameterValue = servletRequest.getParameter(entry.getKey());

				String[] methodWithValue = methodName.split(":");

				try {

					if (isRuleWithValue(methodWithValue))
						callRuleMethod(methodWithValue[0], entry.getKey(), parameterValue, methodWithValue[1]);
					else
						callRuleMethod(methodName, entry.getKey(), parameterValue);

				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}

			}
		}

		setSessionErrors();
		return this;
	}



	/**
	 * check existing of parameter in the request
	 *
	 * @param paramValue
	 * @return boolean
	 */
	public boolean requiredRule(String paramName, String paramValue) {
		if (paramValue == null || paramValue.trim().isEmpty()) {
			if (messages.get(paramName + ".required") != null)
				errors.add(messages.get(paramName + ".required"));
			else
				errors.add(paramName + " is empty");

			return false;
		}

		return true;
	}



	/**
	 * check locale inputs that at least have one value for one of the locales
	 *
	 * @param paramName
	 * @param paramValue
	 * @return
	 */
	public boolean requiredAnyLocaleRule(String paramName, String paramValue) {

		Map<Locale, String> localeStringMap = LocalizationUtil.getLocalizationMap(actionRequest, paramName);

		for (Map.Entry<Locale, String> map : localeStringMap.entrySet()) {
			if (map.getValue() != null && !map.getValue().isEmpty())
				return true;
		}

		setError(paramName, "All locales value for " + paramName + " is empty");
		return false;
	}



	/**
	 * check length string inputs
	 *
	 * @param paramName
	 * @param paramValue
	 * @param max
	 * @return boolean
	 */
	public boolean maxRule(String paramName, String paramValue, String max) {
		if (paramValue.length() > Integer.parseInt(max)) {
			if (messages.get(paramName + ".max") != null)
				errors.add(messages.get(paramName + ".max"));
			else
				errors.add(paramName + " is bigger than " + max);

			return false;
		}

		return true;
	}



	/**
	 * check length string inputs
	 *
	 * @param paramName
	 * @param paramValue
	 * @param min
	 * @return boolean
	 */
	public boolean minRule(String paramName, String paramValue, String min) {
		if (paramValue.length() < Integer.parseInt(min)) {

			if (messages.get(paramName + ".min") != null)
				errors.add(messages.get(paramName + ".min"));
			else
				errors.add(paramName + " is smaller than " + min);

			return false;
		}

		return true;
	}



	/**
	 * check value be a integer
	 *
	 * @param paramName
	 * @param paramValue
	 * @return
	 */
	public boolean intRule(String paramName, String paramValue) {

		try {
			int intVal = Integer.parseInt(paramValue);
			return true;
		} catch (Exception e) {

			/**
			 * existing check is not responsible of this method
			 */
			if (paramValue == null || paramValue.isEmpty())
				return true;

			if (messages.get(paramName + ".int") != null)
				errors.add(messages.get(paramName + ".int"));
			else
				errors.add(paramName + " is not a int");

			return false;
		}

	}



	/**
	 * check value be a long
	 *
	 * @param paramName
	 * @param paramValue
	 * @return
	 */
	public boolean longRule(String paramName, String paramValue) {

		try {
			long intVal = Long.parseLong(paramValue);
			return true;
		} catch (Exception e) {

			/**
			 * existing check is not responsible of this method
			 */
			if (paramValue == null || paramValue.isEmpty())
				return true;

			if (messages.get(paramName + ".long") != null)
				errors.add(messages.get(paramName + ".long"));
			else
				errors.add(paramName + " is not a long");

			return false;
		}

	}


	/**
	 * 
	 * ckeck string that must be a email
	 * 
	 * @param paramName
	 * @param paramValue
	 * @return
	 */
	public boolean emailRule(String paramName, String paramValue) {
		String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
		/**
		 * existing check is not responsible of this method
		 */
		if (paramValue == null || paramValue.isEmpty())
			paramValue = "";

		boolean isValid = paramValue.matches(regex);

		if (isValid) {
			return true;
		}

		setError(paramName, paramName + " is not a valid email");

		return false;

	}


	/**
	 * check type of file that be one of the given types
	 *
	 * @param paramName
	 * @param paramValue
	 * @param types
	 * @return boolean
	 */
	public boolean fileTypeRule(String paramName, String paramValue, String types) {

		String[] typesName = types.split(",");
		UploadPortletRequest upRequest = PortalUtil.getUploadPortletRequest(actionRequest);
		File file = upRequest.getFile(paramName);

		/**
		 * existing check is not responsible of this method
		 */
		if (file == null || !file.exists())
			return true;

		boolean hasAnyType = false;
		String fileType = getFileExtension(file.getName());
		for (String type : typesName) {
			if (fileType.equalsIgnoreCase(type)) {
				hasAnyType = true;
				break;
			}
		}

		if (!hasAnyType)
			if (messages.get(paramName + ".fileType") != null)
				errors.add(messages.get(paramName + ".fileType"));
			else {
				errors.add(paramName + " type is not available");
				return false;
			}

		return true;
	}



	/**
	 * check mime type of file with its bytes
	 * 
	 * usage mimeType:yourFileMime (Note : yourFileMime is not file extention)
	 * 
	 * @param paramName
	 * @param paramValue
	 * @param types
	 * @return boolean
	 */
	public boolean mimeTypeRule(String paramName, String paramValue, String types) {
		String[] typesName = types.split(",");
		UploadPortletRequest upRequest = PortalUtil.getUploadPortletRequest(actionRequest);
		File file = upRequest.getFile(paramName);

		/**
		 * existing check is not responsible of this method
		 */
		if (file == null || !file.exists())
			return true;

		boolean hasAnyType = false;
		byte[] fileBytes = fileIntoByteArray(file);
		/**
		 * it maybe null and when it's null we check fileByte array starting string with given type name
		 */
		String mimeType = getFileMIME(fileBytes);

		String fileBytesStr = new String(fileBytes);
		for (String type : typesName) {
			if (mimeType != null && mimeType.equalsIgnoreCase(type)) {
				hasAnyType = true;
				break;
			}

			if (fileBytesStr.startsWith(type)) {
				hasAnyType = true;
				break;
			}
		}

		if (!hasAnyType) {
			setError(paramName, paramName + " doesn't have correct mime");
			return false;
		}

		return true;
	}


	
	/**
	 * set limit on file size
	 * 
	 * 
	 * @param paramName
	 * @param paramValue
	 * @param sizeLimit in megabyte
	 * @return boolean
	 */
	public boolean fileSizeRule(String paramName, String paramValue, String sizeLimit) {
		UploadPortletRequest upRequest = PortalUtil.getUploadPortletRequest(actionRequest);
		File file = upRequest.getFile(paramName);

		double limit = Double.parseDouble(sizeLimit);

		/**
		 * existing check is not responsible of this method
		 */
		if (file == null || !file.exists())
			return true;

		// in meg
		double fileSize = (file.length() / (1024 * 1024));
		
		if(fileSize>limit){			
			setError(paramName, paramName + " over size limitation");
			return false;
		}
		
		return true;
	}


	/**
	 * 
	 * check file thats exists or not
	 * 
	 * @param paramName
	 * @param paramValue
	 * @return
	 */
	public boolean requiredFileRule(String paramName, String paramValue) {
		UploadPortletRequest upRequest = PortalUtil.getUploadPortletRequest(actionRequest);
		File file = upRequest.getFile(paramName);

		if (file != null && file.exists()) {
			return true;
		}

		setError(paramName, paramName + " File doen't exists");

		return false;
	}



	/**
	 * set validation error message
	 *
	 * @param paramName
	 * @param defaultMessage
	 */
	private void setError(String paramName, String defaultMessage) {
		if (messages.get(paramName + "." + ruleName) != null)
			errors.add(messages.get(paramName + "." + ruleName));
		else
			errors.add(defaultMessage);
	}



	/**
	 * call rule method by given rule name
	 *
	 * @param methodName     name of rule
	 * @param parameterName  name of request parameter
	 * @param parameterValue value of request parameter
	 * @throws NoSuchMethodException
	 */
	private void callRuleMethod(String methodName, String parameterName, String parameterValue)
			throws NoSuchMethodException {
		ruleName = methodName;
		try {
			Method method = this.getClass().getMethod(methodName + "Rule", String.class, String.class);
			method.invoke(this, parameterName, parameterValue);
		} catch (NoSuchMethodException e) {
			// rethrow with new message
			throw new NoSuchMethodException("there is no rule with " + methodName + " name");
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}



	/**
	 * call rule method by given rule name
	 *
	 * @param methodName     name of rule
	 * @param parameterName  name of request parameter
	 * @param parameterValue value of request parameter
	 * @param value          value of rule (everything after `:` in rule definition)
	 * @throws NoSuchMethodException
	 */
	private void callRuleMethod(String methodName, String parameterName, String parameterValue, String value)
			throws NoSuchMethodException {
		ruleName = methodName;

		try {
			Method method = this.getClass().getMethod(methodName + "Rule", String.class, String.class, String.class);
			method.invoke(this, parameterName, parameterValue, value);
		} catch (NoSuchMethodException e) {
			// rethrow with new message
			throw new NoSuchMethodException("there is no rule with " + methodName + " name");
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}



	/**
	 * check value for rule is exist or not
	 *
	 * @param methodName
	 * @return boolean
	 */
	private boolean isRuleWithValue(String[] methodName) {
		try {
			String test = methodName[1];
			return true;
		} catch (Exception e) {
			return false;
		}
	}



	/**
	 * 
	 * call(set) SessionError to show error messages in UI
	 */
	private void setSessionErrors() {
		if (this.getErrors().size() > 0)
			for (String error : errors) {
				SessionErrors.add(this.actionRequest, error);
			}
	}



	/**
	 * get type of file with its name
	 *
	 * @param fileName
	 * @return String it's type of file
	 */
	private String getFileExtension(String fileName) {
		String[] dotSeperated = fileName.split("\\.");
		return dotSeperated[dotSeperated.length - 1];
	}



	/**
	 * convert file into byte array
	 * 
	 * @param file
	 * @return byte[]
	 */
	private byte[] fileIntoByteArray(File file) {
		FileInputStream fileInputStream = null;
		byte[] bFile = new byte[(int) file.length()];
		try {
			// convert file into array of bytes
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bFile);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bFile;
	}



	/**
	 * try to fetch mime type of file from its bytes
	 * 
	 * @param bytes
	 * @return String
	 */
	private String getFileMIME(byte[] bytes) {
		InputStream is = new ByteArrayInputStream(bytes);
		String mimeType = null;
		try {
			mimeType = URLConnection.guessContentTypeFromStream(is);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mimeType;
	}
}
