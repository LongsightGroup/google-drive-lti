package edu.umich.its.lti.google;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.protocol.HTTP;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

import edu.umich.its.google.oauth.GoogleSecurity;
import edu.umich.its.google.oauth.GoogleServiceAccount;
import edu.umich.its.lti.TcSessionData;
import edu.umich.its.lti.TcSiteToGoogleLink;
import edu.umich.its.lti.TcSiteToGoogleLinks;
import edu.umich.its.lti.TcSiteToGoogleStorage;
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
public class GoogleLtiServlet extends HttpServlet {
	// Enum ---------------------------------------------------------

	// Specifications for different JSP pages used by the LTI.  This is passed
	// to root JSP files with properties to manage display of the page contents.
	public enum JspPage {
		// Home page shows Google Resources with functions to act upon them
		Home("pages/show-google-drive.jsp", "Google Drive", null),
		// Link Folder page shows instructor folders they own, so they can link
		// 1+ folders to the site
		LinkFolder(
				"pages/link-google-drive.jsp",
				"Link Google Drive",
				new String[] {"Instructor"});


		// Instance variables -----------------------------

		private String pageFileUrl;
		private String pageTitle;
		private String[] roles;


		// Constructors -----------------------------------

		private JspPage(
				String pageFileUrlValue,
				String pageTitleValue,
				String[] rolesValue)
		{
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
					for (
							int allowedRoleIdx = 0;
							!result && (allowedRoleIdx < allowedRoles.length);
							allowedRoleIdx++)
					{
						String allowedRole = allowedRoles[allowedRoleIdx];
						for (
								int userRoleIdx = 0;
								!result && (userRoleIdx < userRoles.length); 
								userRoleIdx++)
						{
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
	private static final Logger M_log =
			Logger.getLogger(GoogleLtiServlet.class.toString());

	private static final String SESSION_ATTR_TC_DATA = "TcSessionData";
	private static final String GOOGLE_SERVICE_ACCOUNT_PROPS_PREFIX =
			"googleDriveLti";
	private static final String EXPECTED_LTI_MESSAGE_TYPE =
			"basic-lti-launch-request";
	private static final String EXPECTED_LTI_VERSION = "LTI-1p0";
	private static final String JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON =
			"GoogleDriveConfigJson";
	private static final String PARAMETER_ACTION = "requested_action";
	private static final String PARAM_ACTION_LINK_GOOGLE_FOLDER =
			"linkGoogleFolder";
	private static final String PARAM_ACTION_UNLINK_GOOGLE_FOLDER =
			"unlinkGoogleFolder";
	private static final String PARAM_ACTION_GIVE_ROSTER_ACCESS_READ_ONLY =
			"giveRosterAccessReadOnly";
	private static final String PARAM_ACTION_GIVE_CURRENT_USER_ACCESS_READ_ONLY
			= "giveCurrentUserAccessReadOnly";
	private static final String PARAM_ACTION_REMOVE_ROSTER_ACCESS =
			"removeRosterAccess";
	private static final String PARAM_ACTION_GET_ACCESS_TOKEN =
			"getAccessToken";
	private static final String PARAM_ACTION_OPEN_PAGE =
			"openPage";
	private static final String PARAM_OPEN_PAGE_NAME =
			"pageName";
	private static final String PARAM_ACCESS_TOKEN = "access_token";
	private static final String PARAM_FILE_ID = "file_id";
	private static final String PARAM_SEND_NOTIFICATION_EMAILS =
			"send_notification_emails";
	private static final String PARAM_TP_ID = "tp_id";


	// Constructors --------------------------------------------------

	public GoogleLtiServlet() {
	}


	// Public methods ------------------------------------------------

	public void doError(
			HttpServletRequest request,
			HttpServletResponse response, 
			String message)
		throws java.io.IOException
	{
		String returnUrl =
				request.getParameter("launch_presentation_return_url");
		if (!getIsEmpty(returnUrl)) {
			// Looks like client is LTI consumer: return error message in URL
			if (returnUrl.indexOf('?') > 1) {
				returnUrl +=
						"&lti_msg="
						+ URLEncoder.encode(message, HTTP.UTF_8);
			} else {
				returnUrl +=
						"?lti_msg="
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
	protected void doGet(
			HttpServletRequest request,
			HttpServletResponse response)
	throws ServletException, IOException
	{
		String requestedAction = request.getParameter(PARAMETER_ACTION);
		TcSessionData tcSessionData = retrieveLockFromSession(request);
		if (!verifyGet(request, response, tcSessionData, requestedAction)) {
			return;	// Quick return to simplify code
		}
		if (PARAM_ACTION_LINK_GOOGLE_FOLDER.equals(requestedAction)) {
			linkGoogleFolder(request, response, tcSessionData);
		} else if (PARAM_ACTION_UNLINK_GOOGLE_FOLDER.equals(requestedAction)) {
			unlinkGoogleFolder(request, response, tcSessionData);
		} else if (PARAM_ACTION_GIVE_ROSTER_ACCESS_READ_ONLY
				.equals(requestedAction))
		{
			insertRosterPermissions(request, response, tcSessionData);
		} else if (PARAM_ACTION_GIVE_CURRENT_USER_ACCESS_READ_ONLY
				.equals(requestedAction))
		{
			insertCurrentUserPermissions(request, response, tcSessionData);
		} else if (PARAM_ACTION_REMOVE_ROSTER_ACCESS.equals(requestedAction)) {
			removePermissions(request, response, tcSessionData);
		} else if (PARAM_ACTION_GET_ACCESS_TOKEN.equals(requestedAction)) {
			getGoogleAccessToken(request, response, tcSessionData);
		} else if (PARAM_ACTION_OPEN_PAGE.equals(requestedAction)) {
			loadJspPage(request, response, tcSessionData);
		} else {
			M_log.warning(
					"Request action unknown: \"" + requestedAction + "\"");
		}
	}

	/**
	 * Verifies if the request is valid; if so, this initializes Google Drive so
	 * the browser may make requests to see resources associated with the given
	 * TC site. 
	 */
	@Override
	protected void doPost(
			HttpServletRequest request,
			HttpServletResponse response)
	throws ServletException, IOException 
	{
		if (verifyPost(request, response)) {
			TcSessionData tcSessionData = lockInSession(request);
			String googleConfigJson = GoogleConfigJsonWriter
					.getGoogleDriveConfigJsonScript(tcSessionData);
			request.setAttribute(
					JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON,
					googleConfigJson);
			loadJspPage(request, response, tcSessionData, JspPage.Home);
		}
	}


	// Private methods ----------------------------------------------

	/**
	 * Saves relationship of folder and site in database, and returns the
	 * updated Google Drive Configuration json to the browser.
	 */
	private void linkGoogleFolder(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData)
	throws IOException
	{
		String folderId = request.getParameter(PARAM_FILE_ID);
		TcSiteToGoogleLink newLink = new TcSiteToGoogleLink(
				tcSessionData.getContextId(),
				tcSessionData.getUserEmailAddress(),
				tcSessionData.getUserId(),
				folderId);
		TcSiteToGoogleStorage.addLink(newLink);
		response.getWriter().print(
				GoogleConfigJsonWriter.getGoogleDriveConfigJson(tcSessionData));
	}


	/**
	 * Removes relationship of folder and site from the database, and returns
	 * the updated Google Drive Configuration json to the browser.
	 */
	private void unlinkGoogleFolder(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData)
	throws IOException
	{
		String folderId = request.getParameter(PARAM_FILE_ID);
		if (TcSiteToGoogleStorage
				.removeLink(tcSessionData.getContextId(), folderId))
		{
			response.getWriter().print(GoogleConfigJsonWriter
					.getGoogleDriveConfigJson(tcSessionData));
		}
	}

	/**
	 * This records TcSessionData holding LTI session specific data, and returns
	 * it for sharing it's unique key with the browser, ensuring requests coming
	 * from the browser carrying this key are for the correct LTI session.
	 * 
	 * @param request HttpServletRequest holding the session
	 */
	private TcSessionData lockInSession(HttpServletRequest request) {
		// Store TC data in session.
		TcSessionData result = new TcSessionData(request);
		if (getIsEmpty(result.getUserEmailAddress())) {
			throw new IllegalStateException(
					"Google Drive LTI was opened by user without email address:"
					+ " please verify the tool is configured with"
					+ " imsti.releaseemail = 'on' for course (context_id) '"
					+ result.getContextId()
					+ "'");
		}
		request.setAttribute(SESSION_ATTR_TC_DATA, result);
		request.getSession().setAttribute(SESSION_ATTR_TC_DATA, result);
		return result;
	}

	/**
	 * 
	 * @param request HttpServletRequest holding the session
	 * @return TcSessionData for this session; null if there is none
	 */
	private TcSessionData retrieveLockFromSession(HttpServletRequest request) {
		TcSessionData result = (TcSessionData)request
				.getSession()
				.getAttribute(SESSION_ATTR_TC_DATA);
		request.setAttribute(SESSION_ATTR_TC_DATA, result);
		return result;
	}

	private boolean verifyGet(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData,
			String requestedAction)
	throws ServletException, IOException 
	{
		boolean result = false;
		if (tcSessionData != null) {
			String requestTpId = request.getParameter(PARAM_TP_ID);
			if (
					!getIsEmpty(requestTpId)
					&& tcSessionData.matchTpId(requestTpId))
			{
				result = true;
			} else {
				M_log.warning("A request \""
						+ requestedAction
						+ "\" was made to Google Drive LTI with unmatched "
						+ " authority key: given \""
						+ requestTpId
						+ "\", expected \""
						+ tcSessionData.getId()
						+ "\".");
				doError(
						request,
						response,
						"The server failed to match the authority key for "
						+ "this request.");
			}
		} else {
			M_log.warning("A request \""
					+ requestedAction
					+ "\" was made to Google Drive LTI, and there is no "
					+ "data in the session from a post made by TC.");
			doError(
					request,
					response,
					"No action taken: the request could not be verified in "
					+ "this session.");
		}
		return result;
	}

	/**
	 * This verifies the post has expected parameters from valid LTI client, and
	 * the request has matching signature.  If not verified, this sends error to
	 * client; caller should not attempt to proceed.
	 */
	private boolean verifyPost(
			HttpServletRequest request,
			HttpServletResponse response)
	throws ServletException, IOException 
	{
		// 1 - verify the expected parameters exist
		boolean result = 
				EXPECTED_LTI_MESSAGE_TYPE.equals(
						request.getParameter("lti_message_type"))
				&& EXPECTED_LTI_VERSION.equals(
						request.getParameter("lti_version"))
				&& !getIsEmpty(request.getParameter("oauth_consumer_key"))
				&& !getIsEmpty(request.getParameter("resource_link_id"));
		if (!result) {
			doError(
					request,
					response,
					"Post apparently not from LTI Consumer, as parameters are "
					+ "missing or invalid.");
		}
		// 2 - verify signature
		result = RequestSignatureUtils.verifySignature(
				request,
				request.getParameter("oauth_consumer_key"),
				"secret");
		if (!result) {
			doError(
					request,
					response,
					"Request signature is invalid.");
		}
		return result;
	}

	/**
	 * @return true if the given object is null or trimmed = ""
	 */
	private boolean getIsEmpty(String item) {
		return (item == null) || (item.trim() == "");
	}

	/**
	 * This puts the Google Configuration object into the request, for inserting
	 * onto the HTML page.
	 * 
	 * @param request
	 */
	private void retrieveGoogleDriveConfigFromSession(
			TcSessionData tcSessionData,
			HttpServletRequest request)
	throws IOException
	{
		request.setAttribute(
				JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON,
				GoogleConfigJsonWriter
						.getGoogleDriveConfigJsonScript(tcSessionData));
	}

	private void insertRosterPermissions(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData)
	throws ServletException, IOException 
	{
		List<String> emailAddresses = getRoster(request, tcSessionData);
		int count = insertPermissions(
				request,
				response,
				tcSessionData,
				emailAddresses);
		// Title set in request by insertPermissions: get and clear it
		String folderTitle = (String)request.getAttribute("folderTitle");
		request.removeAttribute("folderTitle");
		StringBuilder sbResponse = new StringBuilder();
		sbResponse.append("Added permissions for folder \"")
				.append(folderTitle)
				.append("\" to ")
				.append(count)
				.append((count == 1) ? " person" : " people")
				.append(" in the roster");
		if (tcSessionData.getIsInstructor()) {
			sbResponse.append(" (you already have permissions)");
		}
		sbResponse.append(".");
		response.getWriter().print(sbResponse.toString());
	}

	private void insertCurrentUserPermissions(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData)
	throws ServletException, IOException 
	{
		String emailAddress = tcSessionData.getUserEmailAddress();
		if (getIsEmpty(emailAddress)) {
			logError(
					response,
					"Error: unable to handle permissions - the TC did "
					+ "not sent the current user's email address.");
			return;
		}
		List<String> emailAddresses = new ArrayList<String>();
		emailAddresses.add(emailAddress);
		if (insertPermissions(request, response, tcSessionData, emailAddresses)
				== 1)
		{
			response.getWriter().print("SUCCESS");
		}
	}

	/**
	 * Gives people with the given email addresses read-only access to the given
	 * folder.  The instructor is expected to be the owner of the folder and
	 * their permissions are not touched.
	 * 
	 * If people already have higher permissions, this will not affect that.
	 * This would be the case because the instructor already gave them those
	 * permissions.
	 * 
	 * @return Number of permissions that were successfully inserted
	 */
	private int insertPermissions(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData,
			List<String> emailAddresses)
	throws ServletException, IOException 
	{
		int result = 0;
		try {
			if (!validatePermissionsRequiredParams(
					request,
					response,
					tcSessionData))
			{
				return 0;
			}
			FolderPermissionsHandler handler =
					getHandler(request, response, tcSessionData);
			File file = handler.getFile();
			if (file == null) {
				logError(
						response,
						"Error: unable to modify Google Folder permissions, as "
						+ "the folder was not retrieved from Google Drive.");
				return 0;	// Quick return to simplify code
			}
			// Ugly way to pass title to the calling method
			request.setAttribute("folderTitle", file.getTitle());
			boolean sendNotificationEmails = Boolean.parseBoolean(
					request.getParameter(PARAM_SEND_NOTIFICATION_EMAILS));
			// Insert permission for each given person
			for (
					int rosterIdx = 0;
					rosterIdx < emailAddresses.size();
					rosterIdx++)
			{
				String userEmailAddress = emailAddresses.get(rosterIdx);
				// TODO: consider doing formal check this is valid email address
				if (
						!getIsEmpty(userEmailAddress)
						&& !handler.getIsInstructor(userEmailAddress))
				{
					// If result not null, the user has permission >= inserted
					if (null != handler.insertPermission(
							userEmailAddress,
							sendNotificationEmails))
					{
						result++;
					}
				}
			}
		} catch (Exception err) {
			M_log.warning("Error insertPermissions():");
			err.printStackTrace();
		}
		return result;
	}

	private FolderPermissionsHandler getHandler(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData)
	throws ServletException, IOException 
	{
		FolderPermissionsHandler result = null;
		String siteId = tcSessionData.getContextId();
		String fileId = request.getParameter(PARAM_FILE_ID);
		TcSiteToGoogleLinks links =
				TcSiteToGoogleStorage.getLinkedGoogleFolders(siteId);
		TcSiteToGoogleLink link = null;
		if (links != null) {
			link = links.getLinkForFolder(fileId);
			if (link == null) {
				link = links.getRemovedLinkForFolder(fileId);
			}
			if (link == null) {
				M_log.warning(
						"Error: cannot modify permissions to folder #"
						+ fileId
						+ " - did not find link with course #"
						+ tcSessionData.getContextId());
				logError(
						response,
						"Server failed to find link to this Google folder.");
				return null;
			}
		} else {
			if (link == null) {
				M_log.warning(
						"Error: cannot modify permissions to folder #"
						+ fileId
						+ " - did not find any links with course #"
						+ tcSessionData.getContextId());
				logError(
						response,
						"Server failed to find link to this Google folder.");
				return null;
			}
		}
		String instructorEmailAddress = link.getUserEmailAddress();
		GoogleCredential googleCredential = null;
		if (instructorEmailAddress.equalsIgnoreCase(
				tcSessionData.getUserEmailAddress()))
		{
			// Logged in user is instructor: use their access token
			googleCredential = getGoogleCredential(request);
		} else {
			// This is unlikely to happen for whole roster, but will be
			// useful for code modifying a single student's permissions
			googleCredential = GoogleSecurity.authorize(
					getGoogleServiceAccount(),
					instructorEmailAddress);
		}
		Drive drive = GoogleSecurity.getGoogleDrive(googleCredential);
		result = new FolderPermissionsHandler(link, drive, fileId);
		return result;
	}

	/**
	 * Removes read-only access to the given folder to people in the roster.
	 * Permissions for owners of the folder, and for the instructor, are not
	 * touched.
	 * 
	 * This uses a workaround to modify permissions, as there is no way to
	 * request permissions per file and user.  Inserting a permission returns
	 * the existing permission if the user already has permissions, so inserting
	 * and deleting permission is the simplest method available for removing a
	 * person's permissions to the file.
	 */
	private void removePermissions(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData)
	throws ServletException, IOException 
	{
		try {
			if (!validatePermissionsRequiredParams(
					request,
					response,
					tcSessionData))
			{
				return;
			}
			FolderPermissionsHandler handler =
					getHandler(request, response, tcSessionData);
			File file = handler.getFile();
			if (file == null) {
				logError(
						response,
						"Error: unable to modify Google Folder permissions, as "
						+ "the folder was not retrieved from Google Drive.");
				return;	// Quick return to simplify code
			}
			// Get credential for the instructor owning the folder
			boolean sendNotificationEmails = Boolean.parseBoolean(
					request.getParameter(PARAM_SEND_NOTIFICATION_EMAILS));
			// Insert permission for each person in the roster
			List<String> roster = getRoster(request, tcSessionData);
			int updateCount = 0;
			for (int rosterIdx = 0; rosterIdx < roster.size(); rosterIdx++) {
				String userEmailAddress = roster.get(rosterIdx);
				// TODO: consider doing formal check this is valid email address
				if (
						!getIsEmpty(userEmailAddress)
						&& !handler.getIsInstructor(userEmailAddress))
				{
					// If result not null, the user has permission >= inserted
					Permission permission = handler.insertPermission(
							userEmailAddress,
							sendNotificationEmails);
					if (permission != null) {
						if (handler.removePermission(permission.getId())) {
							updateCount++;
						}
					}
				}
			}
			response.getWriter().print(
					"Removed permissions for folder \""
					+ file.getTitle()
					+ "\" to "
					+ updateCount
					+ ((updateCount == 1) ? " person" : " people")
					+ " in the roster (you already have permissions).");
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

	/**
	 * Returns access token for Google Drive and the given user to the browser.
	 */
	private void getGoogleAccessToken(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData)
	throws IOException
	{
		String userEmailAddress =
				tcSessionData.getUserEmailAddress();
		if (getIsEmpty(userEmailAddress)) {
			logError(
					response,
					"Error: unable to get access token - the TP server does "
					+ "not know the user's email address.");
			return;
		}
		String accessToken = GoogleSecurity.getGoogleAccessToken(
				getGoogleServiceAccount(),
				userEmailAddress);
		if (accessToken != null) {
			response.getWriter().print(accessToken);
		} else {
			logError(
					response,
					"Error: unable to get access token.");
		}
	}

	private GoogleServiceAccount getGoogleServiceAccount() {
		return new GoogleServiceAccount(GOOGLE_SERVICE_ACCOUNT_PROPS_PREFIX);
	}

	private boolean validatePermissionsRequiredParams(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData)
	throws IOException
	{
		boolean result = true;
		if (getIsEmpty(tcSessionData.getUserEmailAddress())) {
			logError(
					response,
					"Error: unable to handle permissions - the request did "
					+ "not specify the instructor.");
			result = false;
		}
		if (getIsEmpty(request.getParameter(PARAM_ACCESS_TOKEN))) {
			logError(
					response,
					"Error: unable to handle permissions - the request did "
					+ "not include valid access token.");
			result = false;
		}
		if (getIsEmpty(PARAM_FILE_ID)) {
			logError(
					response,
					"Error: unable to insert permissions, as no file "
					+ "ID was included in the request.");
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
	private List<String> getRoster(
			HttpServletRequest request,
			TcSessionData tcSessionData)
	throws ServletException, IOException 
	{
		return RosterClientUtils.getRoster(tcSessionData);
	}

	private void logError(HttpServletResponse response, String message)
	throws IOException
	{
		M_log.warning(message);
		response.getWriter().print(message);
	}

	/**
	 * Overload that gets JSP page to open from parameter in the request.
	 */
	private void loadJspPage(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData)
	throws ServletException, IOException
	{
		String pageName = request.getParameter(PARAM_OPEN_PAGE_NAME);
		loadJspPage(
				request,
				response,
				tcSessionData,
				JspPage.valueOf(pageName));
	}

	/**
	 * Forwards the request to open owner (container) JSP /view/root.jsp,
	 * loading the given JSP page as container's contents.
	 * 
	 * @param request HttpServletRequest storing the JSP page for use by owner
	 * @param response HttpServletResponse for forward
	 * @param tcSessionData 
	 * @param jspPage JspPage enum containing page-specific settings
	 * @throws ServletException
	 * @throws IOException
	 */
	private void loadJspPage(
			HttpServletRequest request,
			HttpServletResponse response,
			TcSessionData tcSessionData,
			JspPage jspPage)
	throws ServletException, IOException
	{
		if (jspPage.verifyAllowedRoles(tcSessionData.getUserRoleArray())) {
			request.setAttribute("jspPage", jspPage);
			retrieveGoogleDriveConfigFromSession(tcSessionData, request);
			getServletContext()
					.getRequestDispatcher("/view/root.jsp")
					.forward(request, response);
		} else {
			M_log.warning(
					"Unauthorized attempt to acces JSP page "
					+ jspPage
					+ " requiring roles "
					+ Arrays.toString(jspPage.getRoles())
					+ " by "
					+ tcSessionData.getUserNameFull()
					+ " <"
					+ tcSessionData.getUserEmailAddress()
					+ "> with roles "
					+ Arrays.toString(tcSessionData.getUserRoleArray()));
			loadJspPage(request, response, tcSessionData, JspPage.Home);
		}
	}


	// Inner classes ------------------------------------------------


	private class FolderPermissionsHandler {
		// Instance variables ---------------------------------------

		private TcSiteToGoogleLink link;
		private Drive drive;
		private String fileId;


		// Constructors ---------------------------------------------

		FolderPermissionsHandler(
				TcSiteToGoogleLink link,
				Drive drive,
				String fileId)
		{
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

		private Permission insertPermission(
				String userEmailAddress,
				boolean sendNotificationEmails)
		{
			Permission result = null;
			try {
				Permission newPermission = new Permission();
				newPermission.setValue(userEmailAddress);
				newPermission.setType("user");
				newPermission.setRole("reader");
				result =
						getDrive()
								.permissions()
								.insert(getFileId(), newPermission)
						.setSendNotificationEmails(sendNotificationEmails)
						.execute();
			} catch (Exception err) {
				M_log.warning("Failed to insert permission for user \""
						+ getLink().getUserEmailAddress()
						+ "\" on file \""
						+ getFileId()
						+ "\"");
						err.printStackTrace();
			}
			return result;
		}

		private boolean removePermission(String permissionId)
		{
			boolean result = false;
			try {
				getDrive()
						.permissions()
						.delete(getFileId(), permissionId).execute();
				// No errors indicates this operation succeeded
				result = true;
			} catch (Exception err) {
				M_log.warning(
						"Failed to remove permission "
						+ permissionId
						+ " for user \""
						+ getLink().getUserEmailAddress()
						+ "\" on file \""
						+ getFileId()
						+ "\"");
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
