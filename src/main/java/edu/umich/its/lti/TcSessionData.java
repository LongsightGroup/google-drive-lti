package edu.umich.its.lti;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * 
 * @author ranaseef
 *
 */
public class TcSessionData {
	// Instance variables -------------------------------------------

	private final String id = UUID.randomUUID().toString();
	private String consumerKey;
	private String contextId;
	private String contextLabel;
	private String contextTitle;
	private String membershipsId;
	private String membershipsUrl;
	private String resourceLinkId;
	private String userEmailAddress;
	private String userId;
	private String userImageUrl;
	private String userNameFull;
	private String userRoles;
	private String[] userRoleArray;


	// Constructors -------------------------------------------------

	public TcSessionData(HttpServletRequest request) {
		loadTcParameters(request);
		if (getIsEmpty(userEmailAddress)) {
			throw new IllegalStateException(
					"Google Drive LTI was opened by user without email address:"
					+ " please verify the tool is configured with"
					+ " imsti.releaseemail = 'on' for course (context_id) '"
					+ getContextId()
					+ "'");
		}
	}


	// Public methods -----------------------------------------------

	/**
	 * This creates JSON with configuration of Google Drive, for use by the
	 * browser to manage the site's Google Resources.
	 */
	public String getGoogleDriveConfigJson()
	throws IOException
	{
		StringBuilder result = new StringBuilder("googleDriveConfig = {");
		String courseId = getContextId();
		if ((courseId == null) || courseId.trim().equals("")) {
			throw new RuntimeException(
					"Google Drive LTI request made without context_id!");
		}
		result.append("\"tp_id\" : \"")
				.append(escapeJson(getId()))
				.append("\"");
		result.append(", \"user\" : ");
		appendUserJson(result);
		result.append(", \"folder\" : ");
		appendFolderJson(result);
		result.append(", \"course_id\" : \"")
				.append(escapeJson(getContextId()))
				.append("\"");
		// End the JSON object
		result.append("}");
		return result.toString();
	}

	/**
	 * Returns true if the user has the given role (match is case sensitive)
	 */
	public boolean getHasRole(String role) {
		String[] roleArray = getUserRoleArray();
		if (roleArray != null) {
			for (int idx = 0; idx < roleArray.length; idx++) {
				if (role.equals(roleArray[idx])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return true if the user has role "Instructor"
	 */
	public boolean getIsInstructor() {
		return getHasRole("Instructor");
	}

	public void loadTcParameters(HttpServletRequest request) {
		setConsumerKey(request.getParameter("oauth_consumer_key"));
		setContextId(request.getParameter("context_id"));
		setContextLabel(request.getParameter("context_label"));
		setContextTitle(request.getParameter("context_title"));
		setMembershipsId(request.getParameter("ext_ims_lis_memberships_id"));
		setMembershipsUrl(request.getParameter("ext_ims_lis_memberships_url"));
		setResourceLinkId(request.getParameter("resource_link_id"));
		setUserEmailAddress(request.getParameter("lis_person_contact_email_primary"));
		setUserId(request.getParameter("user_id"));
		setUserImageUrl(request.getParameter("user_image"));
		setUserNameFull(request.getParameter("list_person_name_full"));
		setUserRoles(request.getParameter("roles"));
	}

	public boolean matchTpId(String value) {
		return getId().equals(value);
	}


	// Public property accessory methods ----------------------------

	public String[] getUserRoleArray() {
		return userRoleArray;
	}

	public String getId() {
		return id;
	}

	public String getConsumerKey() {
		return consumerKey;
	}

	public void setConsumerKey(String value) {
		consumerKey = value;
	}

	public String getContextId() {
		return contextId;
	}

	public void setContextId(String value) {
		contextId = value;
	}

	public String getContextLabel() {
		return contextLabel;
	}

	public void setContextLabel(String value) {
		contextLabel = value;
	}

	public String getContextTitle() {
		return contextTitle;
	}

	public void setContextTitle(String value) {
		contextTitle = value;
	}

	public String getMembershipsId() {
		return membershipsId;
	}

	public void setMembershipsId(String value) {
		membershipsId = value;
	}

	public String getMembershipsUrl() {
		return membershipsUrl;
	}

	public void setMembershipsUrl(String value) {
		membershipsUrl = value;
	}

	public String getResourceLinkId() {
		return resourceLinkId;
	}

	public void setResourceLinkId(String value) {
		resourceLinkId = value;
	}

	public String getUserEmailAddress() {
		return userEmailAddress;
	}

	public void setUserEmailAddress(String value) {
		userEmailAddress = value;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String value) {
		userId = value;
	}

	public String getUserImageUrl() {
		return userImageUrl;
	}

	public void setUserImageUrl(String value) {
		userImageUrl = value;
	}

	public String getUserNameFull() {
		return userNameFull;
	}

	public void setUserNameFull(String value) {
		userNameFull = value;
	}

	public String getUserRoles() {
		return userRoles;
	}

	public void setUserRoles(String value) {
		userRoles = value;
		setUserRoleArray(value.split(","));
	}


	// Private methods ----------------------------------------------

	private void appendFolderJson(StringBuilder result) {
		// 2 - Begin Adding the folder
		result.append("{");
		// 2a - Folder's title
		result.append("\"title\" : \"")
				.append(escapeJson(getContextTitle()))
				.append("\"");
		// 2 - End Adding the folder
		result.append("}");
	}

	private void appendUserJson(StringBuilder result) {
		// 1 - Begin Adding User
		result.append("{");
		// 1a - full name
		result.append(" \"name\" : '")
				.append(escapeJson(getUserNameFull()))
				.append("'");
		// 1b - email address
/*		String userEmailAddress = escapeJson(getUserEmailAddress());
		result.append(", \"emailAddress\" : '")
				.append(userEmailAddress)
				.append("'");*/
		// 1c - roles
		String[] roleArray = getUserRoleArray();
		result.append(", \"roles\" : [ ");
		for (int idx = 0; idx < roleArray.length; idx++) {
			if (idx > 0) {
				result.append(",");
			}
			result.append("'").append(escapeJson(roleArray[idx])).append("'");
		}
		result.append("]");
		// 1 - End Adding User
		result.append("}");
	}

	/**
	 * Returns the value escaped properly for placement as value in JSON; null
	 * is returned as ''
	 */
	private String escapeJson(String value) {
		return (value == null) ? "" : StringEscapeUtils.escapeEcmaScript(value);
	}

	private boolean getIsEmpty(String value) {
		return (value == null) || (value.trim() == "");
	}

	private void setUserRoleArray(String[] value) {
		userRoleArray = value;
	}
}
