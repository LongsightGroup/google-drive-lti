package edu.umich.its.lti.google;

import java.io.IOException;

import javax.servlet.ServletException;

import edu.umich.its.lti.TcSessionData;
import edu.umich.its.lti.TcSiteToGoogleLink;
import edu.umich.its.lti.TcSiteToGoogleLinks;
import edu.umich.its.lti.TcSiteToGoogleStorage;

/**
 * This generates JSON object sent to the browser containing user name & roles,
 * and list of linked folders
 * <pre>
 * 	googleDriveConfig = {
 * 		  "tp_id" : ""
 * 		, "course" : { "id" : "", title: "" }
 * 		, "user" : { "name" : "", "roles" : [ "", "" ]}
 * 		, "linkedFolders" : [ "", "" ]
 * 
 * 	}
 * </pre>
 *  
 * @author Raymond Naseef
 *
 */
public class GoogleConfigJsonWriter {

	/**
	 * This creates JSON with configuration of Google Drive, for use by the
	 * browser to manage the site's Google Resources.
	 */
	static public String getGoogleDriveConfigJson(TcSessionData tcSessionData)
	throws IOException
	{
		StringBuilder result = new StringBuilder("{");
		String courseId = tcSessionData.getContextId();
		if ((courseId == null) || courseId.trim().equals("")) {
			throw new RuntimeException(
					"Google Drive LTI request made without context_id!");
		}
		result.append("\"tp_id\" : \"")
				.append(escapeJson(tcSessionData.getId()))
				.append("\"");
		result.append(", \"course\" : ");
		appendCourseJson(tcSessionData, result);
		result.append(", \"linkedFolders\" : ");
		appendLinkedFolders(tcSessionData, result);
		result.append(", \"user\" : ");
		appendUserJson(tcSessionData, result);
		// End the JSON object
		result.append("}");
		return result.toString();
	}

	/**
	 * Returns JavaScript setting the JSON configuration object to global
	 * variable "googleDriveConfig".
	 */
	static public String getGoogleDriveConfigJsonScript(
			TcSessionData tcSessionData)
	throws IOException
	{
		return "googleDriveConfig = " + getGoogleDriveConfigJson(tcSessionData);
	}


	// Static private methods ---------------------------------------

	static private void appendCourseJson(
			TcSessionData tcSessionData,
			StringBuilder result)
	{
		// 2 - Begin Adding the folder
		result.append("{ \"id\" : \"")
				.append(escapeJson(tcSessionData.getContextId()))
				.append("\"");
		// 2a - Folder's title
		result.append(", \"title\" : \"")
				.append(escapeJson(tcSessionData.getContextTitle()))
				.append("\"");
		// 2 - End Adding the folder
		result.append("}");
	}

	static private void appendLinkedFolders(
			TcSessionData tcSessionData,
			StringBuilder result)
	{
		/*result.append("[");
		try {
			TcSiteToGoogleLinks links = TcSiteToGoogleStorage
					.getLinkedGoogleFolders(tcSessionData.getContextId());
			boolean first = true;
			if (links != null) {
				for (TcSiteToGoogleLink link : links) {
					if (!first) {
						result.append(",");
					}
					result.append("\"")
							.append(escapeJson(link.getFolderId()))
							.append("\"");
					first = false;
				}
			}
		} catch (Exception err) {
			err.printStackTrace();
		}
		result.append("]");
		//System.out.println("What is this :" +result);*/
		
		//setting mapping
		result.append("[");
		try {
			TcSiteToGoogleLink link = TcSiteToGoogleStorage.getLinkingFromSettingService(tcSessionData);
			if(link!=null) {
				result.append("\"")
				.append(escapeJson(link.getFolderId()))
				.append("\"");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		result.append("]");
	}

	static private void appendUserJson(
			TcSessionData tcSessionData,
			StringBuilder result)
	{
		// 1 - Begin Adding User
		result.append("{");
		// 1a - full name
		result.append(" \"name\" : \"")
				.append(escapeJson(tcSessionData.getUserNameFull()))
				.append("\"");
		// 1b - email address
/*		String userEmailAddress = escapeJson(getUserEmailAddress());
		result.append(", \"emailAddress\" : '")
				.append(userEmailAddress)
				.append("'");*/
		// 1c - roles
		String[] roleArray = tcSessionData.getUserRoleArray();
		result.append(", \"roles\" : [ ");
		for (int idx = 0; idx < roleArray.length; idx++) {
			if (idx > 0) {
				result.append(",");
			}
			result.append("\"").append(escapeJson(roleArray[idx])).append("\"");
		}
		result.append("]");
		// 1 - End Adding User
		result.append("}");
	}

	/**
	 * Returns the value escaped properly for placement as value in JSON; null
	 * is returned as ''
	 */
	static private String escapeJson(String value) {
		return (value == null) ? "" : value.replace("\"", "\\\"");
	}
}
