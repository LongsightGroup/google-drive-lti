/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2013 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package edu.umich.its.lti.google;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionId;
import com.google.api.services.drive.model.PermissionList;

import edu.umich.its.google.oauth.GoogleAccessToken;
import edu.umich.its.google.oauth.GoogleSecurity;
import edu.umich.its.google.oauth.GoogleServiceAccount;
import edu.umich.its.lti.GoogleCache;
import edu.umich.its.lti.TcSessionData;
import edu.umich.its.lti.TcSiteToGoogleLink;
import edu.umich.its.lti.TcSiteToGoogleStorage;
import edu.umich.its.lti.utils.OauthCredentials;
import edu.umich.its.lti.utils.RequestSignatureUtils;
import edu.umich.its.lti.utils.RosterClientUtils;

/**
 * Servlet with doGet() that allows browser to request an access token or to
 * open a JSP page, and doPost() to get request from LTI Client to load the
 * resources associated with the client's site.
 * 
 * NOTE: doGet() needs security to ensure the request is correct.  This can be
 * done confirming details sent by the client server, client site ID and user. 
 * 
 * @author Raymond Naseef
 *
 **/
/**
 * @author pushyami
 *
 */
/**
 * @author pushyami
 * 
 */
public class GoogleLtiServlet extends HttpServlet {

	private static String GOOGLE_DRIVE = "";
	private static String LINK_GOOGLE_DRIVE = "";
	static ResourceBundle resource;
	
	

	// Enum ---------------------------------------------------------

	// Specifications for different JSP pages used by the LTI. This is passed
	// to root JSP files with properties to manage display of the page contents.
	public enum JspPage {

		// Home page shows Google Resources with functions to act upon them
		Home("pages/show-google-drive.jsp", GOOGLE_DRIVE, null),
		// Link Folder page shows instructor folders they own, so they can link
		// 1+ folders to the site
		LinkFolder("pages/link-google-drive.jsp", LINK_GOOGLE_DRIVE,
				new String[] { "Instructor" });

		// Instance variables -----------------------------

		private String pageFileUrl;
		private String pageTitle;
		private String[] roles;

		// Constructors -----------------------------------

		private JspPage(String pageFileUrlValue, String pageTitleValue,
				String[] rolesValue) {
			pageTitle = pageTitleValue;
			pageFileUrl = pageFileUrlValue;
			roles = rolesValue;
		}

		// Public methods ---------------------------------

		public String getPageFileUrl() {
			return pageFileUrl;
		}

		public String getPageTitle() {
			return pageTitle;
		}

		public String[] getRoles() {
			return roles;
		}

		public boolean verifyAllowedRoles(String[] userRoles) {
			boolean result = false;
			String[] allowedRoles = getRoles();
			if (allowedRoles != null) {
				// Only proceed if there are user roles to check; otherwise,
				// user does not have any allowed role
				if (userRoles != null) {
					for (int allowedRoleIdx = 0; !result
							&& (allowedRoleIdx < allowedRoles.length); allowedRoleIdx++) {
						String allowedRole = allowedRoles[allowedRoleIdx];
						for (int userRoleIdx = 0; !result
								&& (userRoleIdx < userRoles.length); userRoleIdx++) {
							String userRole = userRoles[userRoleIdx];
							result = userRole.equals(allowedRole);
						}
					}
				}
			} else {
				// Page with null roles are open to all
				result = true;
			}
			return result;
		}
	}

	// Constants -----------------------------------------------------

	private static final long serialVersionUID = -21239787L;
	private static final Log M_log = LogFactory.getLog(GoogleLtiServlet.class);

	private static final String SESSION_ATTR_TC_DATA = "TcSessionData";
	private static final String GOOGLE_SERVICE_ACCOUNT_PROPS_PREFIX = "googleDriveLti";
	private static final String EXPECTED_LTI_MESSAGE_TYPE = "basic-lti-launch-request";
	private static final String EXPECTED_LTI_VERSION = "LTI-1p0";
	private static final String JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON = "GoogleDriveConfigJson";
	public static final String PARAMETER_ACTION = "requested_action";
	// Special request to monitor this service is alive: this returns "Hi"
	private static final String PARAM_ACTION_VERIFY_SERVICE_IS_ALIVE = "checkServiceIsAlive";
	public static final String PARAM_ACTION_CHECK_BACK_BUTTON = "checkBackButton";
	private static final String PARAM_ACTION_LINK_GOOGLE_FOLDER = "linkGoogleFolder";
	public static final String PARAM_ACTION_UNLINK_GOOGLE_FOLDER = "unlinkGoogleFolder";
	public static final String PARAM_ACTION_GIVE_ROSTER_ACCESS = "giveRosterAccess";
	private static final String PARAM_ACTION_GIVE_CURRENT_USER_ACCESS = "giveCurrentUserAccess";
	public static final String PARAM_ACTION_REMOVE_ROSTER_ACCESS = "removeRosterAccess";
	private static final String PARAM_ACTION_GET_ACCESS_TOKEN = "getAccessToken";
	public static final String PARAM_ACTION_OPEN_PAGE = "openPage";
	public static final String PARAM_OPEN_PAGE_NAME = "pageName";
	private static final String PARAM_ACCESS_TOKEN = "access_token";
	private static final String PARAM_OWNER_ACCESS_TOKEN ="getOwnerToken";
	private static final String PARAM_INSTRUCTOR_ACCESS_TOKEN_SETTING_SERVICE ="getIntructorTokenSS";
	private static final String PARAM_FILE_ID = "file_id";
	private static final String PARAM_SEND_NOTIFICATION_EMAILS = "send_notification_emails";
	private static final String PARAM_TP_ID = "tp_id";
	private static final String FOLDER_TITLE = "folderTitle";
	private static final String SUCCESS = "SUCCESS";
	private static final String NOSUCCESS = "NOSUCCESS";
	private static final int ZERO=0;
	//creating a constant to hold the value stored in the Setting service(SS) in session.   
	public static final String SETTING_SERVICE_VALUE_IN_SESSION = "SettingValue";
	//creating a constant to hold the access token generated, in session.
	private static final String ACCESS_TOKEN_IN_SESSION = "accessToken";
	
	// Constructors --------------------------------------------------

	public GoogleLtiServlet() {
	}

	// Public methods ------------------------------------------------

	public void doError(HttpServletRequest request,
			HttpServletResponse response, String message)
					throws java.io.IOException {
		String returnUrl = request
				.getParameter("launch_presentation_return_url");
		if (!getIsEmpty(returnUrl)) {
			// Looks like client is LTI consumer: return error message in URL
			if (returnUrl.indexOf('?') > 1) {
				returnUrl += "&lti_msg="
						+ URLEncoder.encode(message, HTTP.UTF_8);
			} else {
				returnUrl += "?lti_msg="
						+ URLEncoder.encode(message, HTTP.UTF_8);
			}
			response.sendRedirect(returnUrl);
		} else {
			// Client not recognized: simply print the message as the response
			PrintWriter out = response.getWriter();
			out.println(message);
		}
	}

	// Protected methods ---------------------------------------------

	/**
	 * Returns a simple page indicating they found this servlet.
	 */
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			String requestedAction = request.getParameter(PARAMETER_ACTION);
			// This is used to monitor this service is alive: return "Hi"
			if (PARAM_ACTION_VERIFY_SERVICE_IS_ALIVE.equals(requestedAction)) {
				response.getWriter().print("Hi");
				return;
			}
			TcSessionData tcSessionData = retrieveLockFromSession(request);
			if (!verifyGet(request, response, tcSessionData, requestedAction)) {
				return; // Quick return to simplify code
			}
			if (PARAM_ACTION_LINK_GOOGLE_FOLDER.equals(requestedAction)) {
				linkGoogleFolder(request, response, tcSessionData);
			} else if (PARAM_ACTION_CHECK_BACK_BUTTON.equals(requestedAction)) {
				checkBackButtonHit(request, response, tcSessionData);
			} else if (PARAM_ACTION_UNLINK_GOOGLE_FOLDER
					.equals(requestedAction)) {
				unlinkGoogleFolder(request, response, tcSessionData);
			} else if (PARAM_ACTION_GIVE_ROSTER_ACCESS
					.equals(requestedAction)) {
				insertRosterPermissions(request, response, tcSessionData);
			} else if (PARAM_ACTION_GIVE_CURRENT_USER_ACCESS
					.equals(requestedAction)) {
				insertCurrentUserPermissions(request, response, tcSessionData);
			} else if (PARAM_ACTION_REMOVE_ROSTER_ACCESS
					.equals(requestedAction)) {
				removePermissions(request, response, tcSessionData);
			} else if (PARAM_ACTION_GET_ACCESS_TOKEN.equals(requestedAction)) {
				getGoogleAccessToken(request, response, tcSessionData);
			}else if (PARAM_OWNER_ACCESS_TOKEN.equals(requestedAction)) {
				getOwnerAccessToken(request, response, tcSessionData);
			}else if (PARAM_INSTRUCTOR_ACCESS_TOKEN_SETTING_SERVICE.equals(requestedAction)){
				getInstructorEmailAddressFromSettingService(request, response, tcSessionData);
			}else if (PARAM_ACTION_OPEN_PAGE.equals(requestedAction)) {
				loadJspPage(request, response, tcSessionData);
			} else {
				M_log.warn("Request action unknown: \"" + requestedAction
						+ "\"");
			}
		} catch (Exception e) {
			// this catches ServletException and IOException
			M_log.error("GET request failed", e);

		}
	}

	/**
	 * Verifies if the request is valid; if so, this initializes Google Drive so
	 * the browser may make requests to see resources associated with the given
	 * ToolConsumer(TC) site.
	 */
	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			if (verifyPost(request, response)) {
				bundleManipulation(request);
				TcSessionData tcSessionData = lockInSession(request);
				if (tcSessionData==null) {
					doError(request, response,
							resource.getString("gd.launch.post.failure"));
					return;
				}
				List<String> roster = RosterClientUtils.getRoster(tcSessionData);
				int googleSharingLimit = 0;
				String googleSharingLimitString = getGoogleServiceAccount().getGoogleSharingLimit();
				if(googleSharingLimitString==null||(googleSharingLimitString.isEmpty())) {
					doError(request, response,
							resource.getString("gd.launch.error.msg.google.sharing.limit.missing"));
					M_log.error("The Google sharing Limit variable is missing from googleServiceProps.properties file");
					return;
				}else {
					googleSharingLimit=Integer.parseInt(googleSharingLimitString);
				}
				/*Checking if the roster size is greater than Google accepted limit. Made the limit configurable in case google changes that in future.
				The Logic (roster-1) meaning, the person sharing his folder is also included in roster. 
				So the Folder is already owned by him so google won't apply the sharing limit on owner of the folder */
				
				if(googleSharingLimit!=ZERO) {
				if((roster.size()-1)>=googleSharingLimit) {
					StringBuilder sb =new StringBuilder();
					sb.append("The Roster size is greater than the google acceptable support while sharing a folder/file. the roster size:  \"");
					sb.append(roster.size());
					sb.append("\" for the context id: \"");
					sb.append(tcSessionData.getContextId());
					sb.append("\" User Id: \"");
					sb.append(tcSessionData.getUserId());
					sb.append("\" Email Address: \"");
					sb.append(tcSessionData.getUserEmailAddress());
					sb.append("\"");
					M_log.error(sb.toString());
					doError(request, response,
							MessageFormat.format(resource.getString("gd.launch.error.msg.roster.size.greater.than.google.approved"), googleSharingLimit));
					return;
				}
				}
				TcSiteToGoogleLink link = TcSiteToGoogleStorage
						.getLinkingFromSettingService(tcSessionData,request);
				
				if ((link != null)) {
					loadJspPage(request, response, tcSessionData, JspPage.Home);
				} else if(tcSessionData.getIsInstructor()) {
					loadJspPage(request, response, tcSessionData,JspPage.LinkFolder);
					}
				else {
					loadJspPage(request, response, tcSessionData, JspPage.Home);
				}
			}
		} catch (Exception e) {
			M_log.error("POST request failed", e);
		}
	}

	// Private methods ----------------------------------------------
	/**
	 * gets the locale information and instantiate the Resource bundle to handle
	 * the localization. Only supports language/country and not region in Locale
	 * object
	 * 
	 * @param request
	 */
	private void bundleManipulation(HttpServletRequest request) {
		String language = null;
		String country = null;
		String locale = request.getParameter("launch_presentation_locale");
		StringTokenizer tempStringTokenizer = new StringTokenizer(locale, "_");
		if (tempStringTokenizer.hasMoreTokens()) {
			language = tempStringTokenizer.nextToken();
		}
		if (tempStringTokenizer.hasMoreTokens()) {
			country = tempStringTokenizer.nextToken();
		}

		resource = ResourceBundle.getBundle("googleDriveLTIProps", new Locale(
				language, country));
		GOOGLE_DRIVE = resource.getString("gd.header1.linked.view");
		LINK_GOOGLE_DRIVE = resource.getString("gd.header1.linking.view");

	}

	/**
	 * Saves relationship of folder and site in LTI setting service, 
	 * 
	 * @param request
	 * @param response
	 * @param tcSessionData
	 * @throws Exception 
	 */
	private void linkGoogleFolder(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData) throws Exception  {
		String folderId = request.getParameter(PARAM_FILE_ID);
		TcSiteToGoogleLink newLink = new TcSiteToGoogleLink(
				tcSessionData.getContextId(),
				tcSessionData.getUserEmailAddress(), tcSessionData.getUserId(),
				folderId);
		// relationship between the folder and the site is being set in the
		// Setting service.
		try {
			if(TcSiteToGoogleStorage.setLinkingToSettingService(tcSessionData,newLink,request)) {
			response.getWriter().print(SUCCESS);
			}
			else {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().print(resource.getString("gd.error.linking.setting.service"));
				StringBuilder s=new StringBuilder();
				s.append("A request for ");
				s.append("sharing of Folder failed for the Site Id: \"");
				s.append(tcSessionData.getContextId());
				s.append("\" User Id: \"");
				s.append(tcSessionData.getUserId());
				s.append("\" Email Address: \"");
				s.append(tcSessionData.getUserEmailAddress());
				s.append("\"");
				M_log.error(s.toString());
			}
				
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(resource.getString("gd.error.linking.setting.service"));
			StringBuilder s=new StringBuilder();
			s.append("A request for ");
			s.append("sharing of Folder failed for the Site Id: \"");
			s.append(tcSessionData.getContextId());
			s.append("\" User Id: \"");
			s.append(tcSessionData.getUserId());
			s.append("\" Email Address: \"");
			s.append(tcSessionData.getUserEmailAddress());
			s.append("\"");
			M_log.error(s.toString(),e);
		}
	}

	/**
	 * Removes relationship of folder and site from the  LTI setting service, 
	 * 
	 * @param request
	 * @param response
	 * @param tcSessionData
	 * @throws Exception 
	 */
	private void unlinkGoogleFolder(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData) throws Exception {
		try {
			if(
			TcSiteToGoogleStorage
					.setUnLinkingToSettingService(tcSessionData,request)) {
				response.getWriter().print(SUCCESS);
			}
			else {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().print(resource.getString("gd.error.unlinking.setting.service"));
				StringBuilder s=new StringBuilder();
				s.append("A request for ");
				s.append("unsharing of Folder failed for the Site Id: \"");
				s.append(tcSessionData.getContextId());
				s.append("\" User Id: \"");
				s.append(tcSessionData.getUserId());
				s.append("\" Email Address: \"");
				s.append(tcSessionData.getUserEmailAddress());
				s.append("\"");
				M_log.error(s.toString());
			}
			
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(resource.getString("gd.error.unlinking.setting.service"));
			StringBuilder s=new StringBuilder();
			s.append("A request for ");
			s.append("unsharing of Folder failed for the Site Id: \"");
			s.append(tcSessionData.getContextId());
			s.append("\" User Id: \"");
			s.append(tcSessionData.getUserId());
			s.append("\" Email Address: \"");
			s.append(tcSessionData.getUserEmailAddress());
			s.append("\"");
			M_log.error(s.toString(),e);
		}

	}

	/**
	 * If the instructor hit back button in the browser from the shared view
	 * this checks to see if the settings service value stored in session has any thing in it. If it
	 * does then it will redirect to the Shared view. This check here eliminate
	 * a potential bug as instructor should only go back to the create and
	 * shared view is by unlinking the folder and not hitting back button.
	 * 
	 * @param request
	 * @param response
	 * @param tcSessionData
	 * @throws IOException
	 * @throws ServletException
	 */
	private void checkBackButtonHit(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData)
					throws IOException, ServletException {
		String link = getSettingsValueFromSession(request);
		if (link==null) {
			response.getWriter().print(NOSUCCESS);
		} else {
			response.getWriter().print(SUCCESS);
		}
	}

	/**
	 * This records TcSessionData holding LTI session specific data, and returns
	 * it for sharing it's unique key with the browser, ensuring requests coming
	 * from the browser carrying this key are for the correct LTI session.
	 * 
	 * @param request
	 *            HttpServletRequest holding the session
	 */
	private TcSessionData lockInSession(HttpServletRequest request) {
		// Store TC data in session.
		TcSessionData result = null;
		String ltiSecret = getGoogleServiceAccount().getLtiSecret();
		String ltiKey = getGoogleServiceAccount().getLtiKey();
		String ltiUrl = getGoogleServiceAccount().getLtiUrl();
		String ltiKeyFromLaunch = request.getParameter("oauth_consumer_key");
		if((ltiKey.equals(ltiKeyFromLaunch))) {
			OauthCredentials oauthCredentials = new OauthCredentials(ltiKey,ltiSecret,ltiUrl);
			result = new TcSessionData(request,oauthCredentials);
		}else {
			M_log.error("The LTI key from the launch of the application is not same as LTI key from the properties file");
			return result;
		}
		if (getIsEmpty(result.getUserEmailAddress())) {
			throw new IllegalStateException(
					"Google Drive LTI was opened by user without email address. Please verify the tool is configured by checking the SEND EMAIL ADDRESSES TO EXTERNAL TOOL option for course (context_id): "
							+ result.getContextId());
		}
		request.setAttribute(SESSION_ATTR_TC_DATA, result);
		request.getSession().setAttribute(SESSION_ATTR_TC_DATA, result);
		return result;
	}

	
	/**
	 * 
	 * @param request
	 *            HttpServletRequest holding the session
	 * @return TcSessionData for this session; null if there is none
	 */
	private TcSessionData retrieveLockFromSession(HttpServletRequest request) {
		TcSessionData result = (TcSessionData) request.getSession()
				.getAttribute(SESSION_ATTR_TC_DATA);
		request.setAttribute(SESSION_ATTR_TC_DATA, result);
		return result;
	}

	private boolean verifyGet(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData,
			String requestedAction) throws ServletException, IOException {
		boolean result = false;
		StringBuilder sb = new StringBuilder();
		if (tcSessionData != null) {
			String requestTpId = request.getParameter(PARAM_TP_ID);
			if (!getIsEmpty(requestTpId)
					&& tcSessionData.matchTpId(requestTpId)) {
				result = true;
				return result;
			} else {
				sb.append("A request \"");
				sb.append(requestedAction);
				sb.append("\" was made to Google Drive LTI with unmatched ");
				sb.append(" authority key: given \"");
				sb.append(requestTpId);
				sb.append("\", expected \"");
				sb.append(tcSessionData.getId());
				sb.append("\".");
				M_log.warn(sb.toString());
				doError(request, response,
						"The server failed to match the authority key for this request.");
			}
		} else {
			sb.append("A request \"");
			sb.append(requestedAction);
			sb.append("\" was made to Google Drive LTI, and there is no data in the session from a post made by Tool Consumer(TC).");
			M_log.warn(sb.toString());
			doError(request, response,
					"No action taken: the request could not be verified in this session.");
		}
		return result;
	}

	/**
	 * This verifies the post has expected parameters from valid LTI client, and
	 * the request has matching signature. If not verified, this sends error to
	 * client; caller should not attempt to proceed.
	 */
	private boolean verifyPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// ResourceBundle properties = ResourceBundle.getBundle("ltis");
		// 1 - verify the expected parameters exist
		boolean result = EXPECTED_LTI_MESSAGE_TYPE.equals(request
				.getParameter("lti_message_type"))
				&& EXPECTED_LTI_VERSION.equals(request
						.getParameter("lti_version"))
						&& !getIsEmpty(request.getParameter("oauth_consumer_key"))
						&& !getIsEmpty(request.getParameter("resource_link_id"));
		if (!result) {
			doError(request, response,
					"Post apparently not from LTI Consumer, as parameters are missing or invalid.");
		}
		// 2 - verify signature
		result = RequestSignatureUtils.verifySignature(request,
				request.getParameter("oauth_consumer_key"),
				getGoogleServiceAccount().getLtiSecret(),getGoogleServiceAccount().getLtiUrl());
		if (!result) {
			doError(request, response, "Request signature is invalid.");
		}
		return result;
	}

	/**
	 * @return true if the given object is null or trimmed = ""
	 */
	private boolean getIsEmpty(String item) {
		return (item == null) || (item.trim().equals(""));
	}

	/**
	 * This puts the Google Configuration object into the request, for inserting
	 * onto the HTML page.
	 * 
	 * @param request
	 */
	private void retrieveGoogleDriveConfigFromSession(
			TcSessionData tcSessionData, HttpServletRequest request)
					throws IOException {
		request.setAttribute(JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON,
				GoogleConfigJsonWriter
				.getGoogleDriveConfigJsonScript(tcSessionData,request));
	}

	private void insertRosterPermissions(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData)
					throws ServletException, IOException {
		HashMap<String, HashMap<String, String>> roster = getRoster(request, tcSessionData);
		insertPermissions(request, response, tcSessionData, roster);
		// Title set in request by insertPermissions: get and clear it
		request.removeAttribute(FOLDER_TITLE);
		response.getWriter().print(SUCCESS);
	}

	private void insertCurrentUserPermissions(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData)
					throws Exception {
		String userRole=null;
		String emailAddress = tcSessionData.getUserEmailAddress();
	 if(tcSessionData.getIsInstructor()) {
			userRole="Instructor";
		}
	 else
	 {
		 userRole="Learner";
	 }
		if (getIsEmpty(emailAddress)) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(resource.getString("gd.error.permission.single.user"));
			M_log.error("Error: unable to handle single user permissions - the ToolConsumer(TC) did not sent the current user's email address");
			return;
		}
		List<String> emailAddresses = new ArrayList<String>();
		HashMap<String, String> singleUser = new HashMap<String, String>();
		singleUser.put(emailAddress, userRole);
		emailAddresses.add(emailAddress);
		if (insertCurrentPermissionsForSingleUser(request, response, tcSessionData, singleUser) == 1) {
			response.getWriter().print(SUCCESS);
		}
		else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(resource.getString("gd.error.permission.single.user"));
			StringBuilder s=new StringBuilder();
			s.append(" Insertion of permission Failed to google for single User to shared folder of with User Id: \"" );
			s.append(tcSessionData.getUserId());
			s.append("\" and Email Address: \"");
			s.append(tcSessionData.getUserEmailAddress());
			s.append("\" for the Site Id: \"");
			s.append(tcSessionData.getContextId());
			s.append("\"");
			M_log.error(s.toString());
			
		}
	}
	
	private int insertCurrentPermissionsForSingleUser(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData,
			HashMap<String,String> singleUser) throws Exception {
		M_log.debug("In the Insersion of permission call for single user.....");
		int result = 0;
		try {
			if (!validatePermissionsRequiredParams(request, response,
					tcSessionData)) {
				return 0;
			}
			FolderPermissionsHandler handler = getHandler(request, response,
					tcSessionData);
			// google file object
			File file = handler.getFile();
			if (file == null) {
				StringBuilder s =new StringBuilder();
			     s.append("Error: unable to insert Google Folder permissions for single user, as the folder was not retrieved from Google Drive for user email address: \"");
			     s.append(tcSessionData.getUserEmailAddress());
			     s.append(" \" and User id : \"");
			     s.append(tcSessionData.getUserId());
			     s.append("\" for the Site Id: \"");
				 s.append(tcSessionData.getContextId());
				 s.append("\"");
				M_log.error(s.toString());
				return 0; // Quick return to simplify code
			}
			// Ugly way to pass title to the calling method
			request.setAttribute(FOLDER_TITLE, file.getTitle());
			boolean sendNotificationEmails = Boolean.parseBoolean(request
					.getParameter(PARAM_SEND_NOTIFICATION_EMAILS));
			// Insert permission for each given person
			for ( Entry<String, String> entry : singleUser.entrySet()) {
			    String emailAddress = entry.getKey();
			    String roles = entry.getValue();
			    if (!getIsEmpty(emailAddress)
						&& !handler.getIsInstructor(emailAddress)) {
					// If result not null, the user has permission >= inserted
					if (null != handler.insertPermission(emailAddress,roles,
							sendNotificationEmails)) {
						result++;
					}
				}
			}
			
		} catch (Exception err) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			StringBuilder s=new StringBuilder();
			s.append(" Insertion of permission Failed to google for single User to shared folder of with User Id: \"" );
			s.append(tcSessionData.getUserId());
			s.append("\" and Email Address: \"");
			s.append(tcSessionData.getUserEmailAddress());
			s.append("\" for the Site Id: \"");
			s.append(tcSessionData.getContextId());
			s.append("\"");
			M_log.error(s.toString(),err);
		}
		return result;
	}
	
	/**
	 * Gives people with the given email addresses read-only access to students
	 * for the given shared folder. Multiple instructors in the roster who are not owner of the shared folder
	 * are given can edit access.The instructor who is expected to be the owner of the shared folder 
	 * their permissions are not touched.
	 * 
	 * If people already have higher permissions, this will not affect that.
	 * This would be the case because the instructor already gave them those
	 * permissions.
	 * 
	 * @return Number of permissions that were successfully inserted
	 */
	
	private int insertPermissions(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData,
			HashMap<String,HashMap<String, String>> roster) throws ServletException, IOException {
		M_log.debug("In the Insertion of permission call......");
		int result = 0;
		try {
			if (!validatePermissionsRequiredParams(request, response,
					tcSessionData)) {
				return 0;
			}
			FolderPermissionsHandler handler = getHandler(request, response,
					tcSessionData);
			// google file object
			File file = handler.getFile();
			if (file == null) {
				StringBuilder s =new StringBuilder();
			     s.append("Error: unable to insert Google Folder permissions, as the folder was not retrieved from Google Drive for Instructor email address: \"");
			     s.append(tcSessionData.getUserEmailAddress());
			     s.append(" \" and User id : \"");
			     s.append(tcSessionData.getUserId());
			     s.append("\" for the Site Id: \"");
				 s.append(tcSessionData.getContextId());
				 s.append("\"");
				M_log.error(s.toString());
				return 0; // Quick return to simplify code
			}
			// Ugly way to pass title to the calling method
			request.setAttribute(FOLDER_TITLE, file.getTitle());
			boolean sendNotificationEmails = Boolean.parseBoolean(request
					.getParameter(PARAM_SEND_NOTIFICATION_EMAILS));
			// Insert permission for each given person
			M_log.debug("Starting Google Api call for inserting the Permissions  ");
			for ( Entry<String, HashMap<String, String>> entry : roster.entrySet()) {
			    String emailAddress = entry.getKey();
			    HashMap<String, String> value = entry.getValue();
			    String roles = value.get("role");
			    if (!getIsEmpty(emailAddress)
						&& !handler.getIsInstructor(emailAddress)) {
					// If result not null, the user has permission >= inserted
					if (null != handler.insertPermission(emailAddress,roles,
							sendNotificationEmails)) {
						result++;
					}
				}
			}
			
		} catch (Exception err) {
			StringBuilder s=new StringBuilder();
			s.append(" Insertion of permissions Failed to google for the class roster to shared folder of Instructor with User Id: \"" );
			s.append(tcSessionData.getUserId());
			s.append("\" and Email Address: \"");
			s.append(tcSessionData.getUserEmailAddress());
			s.append("\" for the Site Id: \"");
			s.append(tcSessionData.getContextId());
			s.append("\"");
			M_log.error(s.toString(),err);
		}
		M_log.debug("Number of Permissions Successfully Inserted: "+result+" / "+(roster.size()-1));
		return result;
	}
	/**
	 * Getting the instructors email address( that is needed during for manipulating permission calls) stored in the Setting service(SS) from the Session instead of SS 
	 * as call to SS intermittently not fetching correct value.
	 * @param request
	 * @param response
	 * @param tcSessionData
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */

	private FolderPermissionsHandler getHandler(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData)
					throws ServletException, IOException {
		M_log.debug("In the Folder Permission handler call for request: " +request.getParameter(PARAMETER_ACTION));
		FolderPermissionsHandler result = null;
		String siteId = tcSessionData.getContextId();
		String fileId = request.getParameter(PARAM_FILE_ID);
		TcSiteToGoogleLink link=null;
		String instructorEmailAddress = "";
		String value = getSettingsValueFromSession(request);
		if(value!=null) {
			link = TcSiteToGoogleStorage.parseLink(value);
			instructorEmailAddress = link.getUserEmailAddress();
		} else {
			link = GoogleCache.getInstance().getLinkForSite(siteId);
			if(link == null) {
				StringBuilder sb = new StringBuilder();
				sb.append("Error: cannot modify permissions to folder #");
				sb.append(fileId);
				sb.append(" - did not find link with course #");
				sb.append(tcSessionData.getContextId());
				M_log.warn(sb.toString());
				return null;
			}
			instructorEmailAddress = link.getUserEmailAddress();
			GoogleCache.getInstance().clearLinkForSite(siteId);
		}
		
		GoogleCredential googleCredential = null;
		if (instructorEmailAddress.equalsIgnoreCase(tcSessionData
				.getUserEmailAddress())) {
			// Logged in user is instructor: use their access token
			googleCredential = getGoogleCredential(request);
		} else {
			// This is unlikely to happen for whole roster, but will be
			// useful for code modifying a single student's/ or other instructor 
			//permissions in roster
			googleCredential = GoogleSecurity.authorize(
					getGoogleServiceAccount(), instructorEmailAddress);
		}
		Drive drive = GoogleSecurity.getGoogleDrive(googleCredential);
		result = new FolderPermissionsHandler(link, drive, fileId);
		return result;
	}
	/**
	 * Helper method to get the stored value <site_id>,<user_id>,<user_email_address>,<google-folder-id> from the session 
	 * @param request
	 * @return
	 */

	private String getSettingsValueFromSession(HttpServletRequest request) {
		String value=(String)request.getSession().getAttribute(SETTING_SERVICE_VALUE_IN_SESSION);
		M_log.debug("The value in the session while getting it: "+value);
		return value;
	}
	
	/**
	 * This function is useful in finding the list of users on file
	 * or folder  and from that list we are able to get the owner of the file/folder
	 * and grab owner's email address and generate the Owner access token 
	 * to delete a file/folder
	 * by a user who only has can edit right on the folder/file.
	 * @throws Exception 
	 * 
	 */
	
	private void getOwnerAccessToken(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData) throws Exception {
		String fileId = request.getParameter(PARAM_FILE_ID);
		String userEmailAddress = tcSessionData.getUserEmailAddress();
		String ownerOfTheFileEmailAddress=null;
		GoogleCredential credential = GoogleSecurity.authorize(
				getGoogleServiceAccount(), userEmailAddress);
		Drive drive = GoogleSecurity.getGoogleDrive(credential);
		try {
			PermissionList list = drive.permissions().list(fileId).execute();
            List<Permission> items = list.getItems();
            for (Permission permission : items) {
            	String role = permission.getRole();
            	if(role.equals("owner")) {
            		 ownerOfTheFileEmailAddress = permission.getEmailAddress();
            		 break;
            	}
			}
            getGoogleOwnerAccessToken(request, response, tcSessionData,ownerOfTheFileEmailAddress);
			
		} catch (Exception e) {
			M_log.error("Failed to get the owner email address for the a given folder",e);
			response.getWriter().print("ERROR");
		}
		
	}

	/**
	 * Removes permissions to the given folder to people in the roster.
	 * Permissions for owners of the folder are not
	 * touched.
	 */
	
	private void removePermissions(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData)
					throws Exception {
		M_log.debug("In the Removal of permission call");
		int numberOfPermissionsRemoved = 0;
		int rosterSize=0;
		try {
			if (!validatePermissionsRequiredParams(request, response,
					tcSessionData)) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().print(resource.getString("gd.permission.error.six"));
				return;
			}
			FolderPermissionsHandler handler = getHandler(request, response,
					tcSessionData);
			File file = handler.getFile();
			if (file == null) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				StringBuilder s =new StringBuilder();
			     s.append("Error: unable to remove Google Folder permissions, as the folder was not retrieved from Google Drive for Instructor email address: \"");
			     s.append(tcSessionData.getUserEmailAddress());
			     s.append(" \" and User id : \"");
			     s.append(tcSessionData.getUserId());
			     s.append("\" for the Site Id: \"");
				 s.append(tcSessionData.getContextId());
				 s.append("\"");
				M_log.error(s.toString());
				response.getWriter().print(resource.getString("gd.permission.error.six"));
				return; // Quick return to simplify code
			}
			
			HashMap<String,HashMap<String, String>> roster = getRoster(request,tcSessionData);
            Set<String> rosterEmailAddressKey = roster.keySet();
            rosterSize = rosterEmailAddressKey.size();
            for (String emailAddress : rosterEmailAddressKey) {
                if (!getIsEmpty(emailAddress)
                        && !handler.getIsInstructor(emailAddress)) {
                	M_log.debug("Removal of permission call to google for user: "+emailAddress);
                       PermissionId permissionIDOfEachPersonWithGoogleAccount = handler.getDrive().permissions().getIdForEmail(emailAddress).execute();
                        if (handler.removePermission(permissionIDOfEachPersonWithGoogleAccount.getId())) {
                            numberOfPermissionsRemoved++;
                        }
                }
            }
            if(numberOfPermissionsRemoved==(rosterSize-1)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().print(SUCCESS);
                }
            else {
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().print(resource.getString("gd.permission.error.six"));
                        StringBuilder s=new StringBuilder();
                        s.append(" Some of google permissions removal failed for the class roster to shared folder of Instructor with User Id: \"" );
                        s.append(tcSessionData.getUserId());
                        s.append("\" and Email Address: \"");
                        s.append(tcSessionData.getUserEmailAddress());
                        s.append("\" for the Site Id: \"");
                        s.append(tcSessionData.getContextId());
                        s.append("\"");
                        M_log.error(s.toString());
                    
                }
		} catch (Exception err) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().print(resource.getString("gd.permission.error.six"));
			StringBuilder s=new StringBuilder();
			s.append(" Removal of google permissions Failed for the class roster to shared folder of Instructor with User Id: \"" );
			s.append(tcSessionData.getUserId());
			s.append("\" and Email Address: \"");
			s.append(tcSessionData.getUserEmailAddress());
			s.append("\" for the Site Id: \"");
			s.append(tcSessionData.getContextId());
			s.append("\"");
			M_log.error(s.toString(),err);
		}
		 M_log.debug("Number of permissions REMOVED Successfully: "+numberOfPermissionsRemoved+" / "+(rosterSize-1));
	}

	/**
	 * Send response to the browser with the access token for Google Drive and the given user email address.
	 * 
	 * If the user's email address is empty, write a message to the log and to the response.
	 * 
	 * If the access token string returned from Google is null, an error has occurred.  Most likely it means
	 * the user doesn't have a valid Google account.  Write a message to the log, then send the string "ERROR" as the
	 * response.
	 */
	
	private void getGoogleAccessToken(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData)
					throws IOException {
		String userEmailAddress = tcSessionData.getUserEmailAddress();
		if (getIsEmpty(userEmailAddress)) {
					M_log.error("Error: unable to get access token - the ToolProvider(TP) server does not know the user's email address.");
			response.getWriter().print("ERROR");
			return;
		}
		// Throws exception for bad email and other reasons.  Should we catch it?
		GoogleAccessToken accessToken =  GoogleSecurity.getGoogleAccessTokenWithTimeStamp(getGoogleServiceAccount(), userEmailAddress);
		request.getSession().setAttribute(ACCESS_TOKEN_IN_SESSION, accessToken);
		if(accessToken==null) {
			StringBuilder s=new StringBuilder();
			s.append(" ERROR: User \"" );
			s.append(tcSessionData.getUserSourceDid());
			s.append("\" does not have a valid Google account for Google Drive LTI.  Unable to get access token or too many credentials got created in a short span of time So google refused to give more access Tokens for (Email: \"");
			s.append(userEmailAddress);
			s.append("\" ; ID: \"");
			s.append(tcSessionData.getUserId());
			s.append(")\"");
			M_log.error(s.toString());
			response.getWriter().print("ERROR");
			return;
		}
		if(request.getMethod().equals("GET")) {
		StringBuilder jsonTokenObject = new StringBuilder("{");
		jsonTokenObject.append("\"access_token\" : \"").append(accessToken.getToken()).append("\"");
		jsonTokenObject.append(", \"time_stamp\" : \"").append(accessToken.getTimeTokenCreated()).append("\"");
		jsonTokenObject.append("} ");
		response.getWriter().print(jsonTokenObject.toString());
		}
	}
	/**
	 * This case help to determines if the  404 error occurs while showing a shared folder is due to user 
	 * don't has permission or shared folder has been deleted from the google drive interface. 
	 * This function call check to get the instructor email address from the setting service(SS) value stored in session(as the SS some time seems to be buggy)  and generates a token
	 * and with generated token check if the shared folder exist or not.
	 * 
	 */
	
	private void getInstructorEmailAddressFromSettingService(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData) throws Exception  {
		try {
			String link = getSettingsValueFromSession(request);
			if(link!=null) {
			String instructorEmailAddress = TcSiteToGoogleStorage.parseLink(link).getUserEmailAddress();
			getGoogleOwnerAccessToken(request, response, tcSessionData, instructorEmailAddress);
			}
			else {
			response.getWriter().print("ERROR");
			}
		} catch (Exception e) {
			M_log.error("Failed to Unshare info into the Setting Service",e);
			response.getWriter().print("ERROR");
		}
	}
	
	private void getGoogleOwnerAccessToken(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData,String ownerEmailAddress)
					throws IOException {
		String accessToken=null;
		if(ownerEmailAddress.equals(tcSessionData.getUserEmailAddress())) {
			GoogleAccessToken token=(GoogleAccessToken)request.getSession().getAttribute(ACCESS_TOKEN_IN_SESSION);
			if(token!=null) {
				accessToken=token.getToken();
			}
			else {
				M_log.error("Error: When retriving the accessToken from the Session");
			}
		}else {
	     accessToken = GoogleSecurity.getGoogleAccessToken(getGoogleServiceAccount(), ownerEmailAddress);
		}
		if (accessToken != null) {
			response.getWriter().print(accessToken);
		} else {
			M_log.warn("ERROR: User \""
					+ tcSessionData.getUserSourceDid()
					+ "\" got error in generating access token.  (Email: "
					+ ownerEmailAddress + "; ID: " + tcSessionData.getUserId()
					+ ")");
			response.getWriter().print("ERROR");
		}
	}
	
	

	private GoogleServiceAccount getGoogleServiceAccount() {
		return new GoogleServiceAccount(GOOGLE_SERVICE_ACCOUNT_PROPS_PREFIX);
	}

	private boolean validatePermissionsRequiredParams(
			HttpServletRequest request, HttpServletResponse response,
			TcSessionData tcSessionData) throws Exception {
		boolean result = true;
		if (getIsEmpty(tcSessionData.getUserEmailAddress())) {
			M_log.error("Error: unable to handle permissions - the request did not specify the email address.");
			result = false;
		}
		if (getIsEmpty(request.getParameter(PARAM_ACCESS_TOKEN))) {
			StringBuilder s =new StringBuilder();
		     s.append("Error: unable to handle permissions, as no Access Token was included in the request for User email address: \"");
		     s.append(tcSessionData.getUserEmailAddress());
		     s.append(" \" and User id : \"");
		     s.append(tcSessionData.getUserId());
		     s.append("\" for the Site Id: \"");
			 s.append(tcSessionData.getContextId());
			M_log.error(s.toString());
			result = false;
		}
		if (getIsEmpty(PARAM_FILE_ID)) {
			StringBuilder s =new StringBuilder();
		     s.append("Error: unable to handle permissions, as no file ID was included in the request for User email address: \"");
		     s.append(tcSessionData.getUserEmailAddress());
		     s.append(" \" and User id : \"");
		     s.append(tcSessionData.getUserId());
		     s.append("\" for the Site Id: \"");
			 s.append(tcSessionData.getContextId());
			M_log.error(s.toString());
			result = false;
		}
		return result;
	}

	private GoogleCredential getGoogleCredential(HttpServletRequest request) {
		GoogleCredential result = null;
		String accessToken = request.getParameter(PARAM_ACCESS_TOKEN);
		if (!getIsEmpty(accessToken)) {
			result = new GoogleCredential().setAccessToken(accessToken);
		}
		return result;
	}

	/**
	 * Makes direct server-to-server request to get the site's roster, and
	 * returns list of users' email addresses.
	 */
	
	private HashMap<String,HashMap<String, String>> getRoster(HttpServletRequest request,
			TcSessionData tcSessionData) throws ServletException, IOException {
		return RosterClientUtils.getRosterFull(tcSessionData);
	}


	/**
	 * Overload that gets JSP page to open from parameter in the request.
	 */
	private void loadJspPage(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData)
					throws ServletException, IOException {
		String pageName = request.getParameter(PARAM_OPEN_PAGE_NAME);
		loadJspPage(request, response, tcSessionData, JspPage.valueOf(pageName));
	}

	private Map<String, String> convertResourceBundleToMap(
			ResourceBundle resource) {
		Map<String, String> map = new HashMap<String, String>();

		Enumeration<String> keys = resource.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			map.put(key, resource.getString(key));
		}

		return map;
	}
	
	/**
	 * Forwards the request to open owner (container) JSP /view/root.jsp,
	 * loading the given JSP page as container's contents.
	 * 
	 * @param request
	 *            HttpServletRequest storing the JSP page for use by owner
	 * @param response
	 *            HttpServletResponse for forward
	 * @param tcSessionData
	 * @param jspPage
	 *            JspPage enum containing page-specific settings
	 * @throws ServletException
	 * @throws IOException
	 */
	private void loadJspPage(HttpServletRequest request,
			HttpServletResponse response, TcSessionData tcSessionData,
			JspPage jspPage) throws ServletException, IOException {
		if (jspPage.verifyAllowedRoles(tcSessionData.getUserRoleArray())) {
			request.setAttribute("jspPage", jspPage);
			retrieveGoogleDriveConfigFromSession(tcSessionData, request);

            Map<String, String> applicationProperties = convertResourceBundleToMap(resource);

            request.setAttribute("applicationProperties", applicationProperties);
            request.setAttribute("applicationPropertiesJson", new JacksonFactory().toString(applicationProperties));
            request.setAttribute("contextLabel",tcSessionData.getContextLabel());

			request.setAttribute("contextUrl",
					getGoogleServiceAccount().getContextURL());
			if(request.getMethod().equals("POST")) {
				getGoogleAccessToken(request, response, tcSessionData);
			}
			request.setAttribute("userEmailAddress",
					tcSessionData.getUserEmailAddress());
			getServletContext().getRequestDispatcher("/view/root.jsp").forward(
					request, response);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("Unauthorized attempt to acces JSP page ");
			sb.append(jspPage);
			sb.append(" requiring roles ");
			sb.append(Arrays.toString(jspPage.getRoles()));
			sb.append(" by ");
			sb.append(tcSessionData.getUserNameFull());
			sb.append(" <");
			sb.append(tcSessionData.getUserEmailAddress());
			sb.append("> with roles ");
			sb.append(Arrays.toString(tcSessionData.getUserRoleArray()));
			M_log.warn(sb.toString());
			loadJspPage(request, response, tcSessionData, JspPage.Home);
		}
	}

	// Inner classes ------------------------------------------------

	// Talking to google with respect to permission insertion and deletion
	private class FolderPermissionsHandler {
		// Instance variables ---------------------------------------

		private TcSiteToGoogleLink link;
		private Drive drive;
		private String fileId;

		// Constructors ---------------------------------------------

		FolderPermissionsHandler(TcSiteToGoogleLink link, Drive drive,
				String fileId) {
			setLink(link);
			setDrive(drive);
			setFileId(fileId);
		}

		// Public methods -------------------------------------------

		private File getFile() throws IOException {
			return getDrive().files().get(getFileId()).execute();
		}

		public boolean getIsInstructor(String userEmailAddress) {
			return getLink().getUserEmailAddress().equals(userEmailAddress);
		}

		private Permission insertPermission(String userEmailAddress,String role,
				boolean sendNotificationEmails) {
			M_log.debug("Inserting permission call to google for user: "+userEmailAddress);
			Permission result = null;
			try {
				Permission newPermission = new Permission();
				newPermission.setValue(userEmailAddress);
				newPermission.setType("user");
				if(role.equals("Instructor")) {
					newPermission.setRole("writer");
				}
				else {
				newPermission.setRole("reader");
				}
				result = getDrive().permissions()
						.insert(getFileId(), newPermission)
						.setSendNotificationEmails(sendNotificationEmails)
						.execute();
			} catch (Exception err) {
				StringBuilder sb = new StringBuilder();
				sb.append("Failed to insert permission for user \"");
				sb.append(userEmailAddress);
				sb.append("\" on file \"");
				sb.append(getFileId());
				sb.append("\"");
				M_log.warn(sb.toString());
				err.printStackTrace();
			}
			return result;
		}

		private boolean removePermission(String permissionId) {
			M_log.debug("and actual removal call to google");
			boolean result = false;
			try {
				getDrive().permissions().delete(getFileId(), permissionId)
				.execute();
				// No errors indicates this operation succeeded
				result = true;
			} catch (Exception err) {
				StringBuilder sb = new StringBuilder();
				sb.append("Failed to remove permission ");
				sb.append(permissionId);
				sb.append(" for user \"");
				sb.append(getLink().getUserEmailAddress());
				sb.append("\" on file \"");
				sb.append(getFileId());
				sb.append("\"");
				M_log.warn(sb.toString());
				err.printStackTrace();
			}
			return result;
		}

		// Public accessory methods ---------------------------------

		public TcSiteToGoogleLink getLink() {
			return link;
		}

		public void setLink(TcSiteToGoogleLink value) {
			link = value;
		}

		public Drive getDrive() {
			return drive;
		}

		public void setDrive(Drive value) {
			drive = value;
		}

		public String getFileId() {
			return fileId;
		}

		public void setFileId(String value) {
			fileId = value;
		}
	}
}
