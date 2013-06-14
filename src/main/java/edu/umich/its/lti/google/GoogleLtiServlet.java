package edu.umich.its.lti.google;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;

import edu.umich.its.google.oauth.GoogleSecurity;
import edu.umich.its.google.oauth.GoogleServiceAccount;
import edu.umich.its.lti.TcSessionData;


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
		Home("pages/show-google-drive.jsp", "Google Drive"),
		// Link Folder page shows instructor folders they own, so they can link
		// 1+ folders to the site
		LinkFolder("pages/link-google-drive.jsp", "Link Google Drive");


		// Instance variables -----------------------------

		private String pageFileUrl;
		private String pageTitle;


		// Constructors -----------------------------------

		private JspPage(String pageFileUrlValue, String pageTitleValue) {
			pageTitle = pageTitleValue;
			pageFileUrl = pageFileUrlValue;
		}


		// Public methods ---------------------------------

		public String getPageFileUrl() {
			return pageFileUrl;
		}

		public String getPageTitle() {
			return pageTitle;
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
	private static final String PARAM_ACTION_GIVE_ROSTER_ACCESS_READ_ONLY =
			"giveRosterAccessReadOnly";
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
		if (PARAM_ACTION_GIVE_ROSTER_ACCESS_READ_ONLY.equals(requestedAction)) {
			insertPermissions(request, response, tcSessionData);
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
			if (tcSessionData.getIsInstructor()) {
				// Pasting in notice so JSP can act differently for Instructor
				request.setAttribute("Instructor", "true");
			}
			String googleConfigJson = tcSessionData.getGoogleDriveConfigJson();
			request.setAttribute(
					JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON,
					googleConfigJson);
			request.getSession().setAttribute(
					JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON,
					googleConfigJson);
			loadJspPage(request, response, tcSessionData, JspPage.Home);
		}
	}

	/**
	 * This records data specific to this instance in the session, and returns
	 * a unique key to be shared with the browser, ensuring requests coming from
	 * the browser are for the correct instance.
	 * 
	 * @param request HttpServletRequest holding the session
	 */
	private TcSessionData lockInSession(HttpServletRequest request) {
		// Store TC data in session.
		TcSessionData result = new TcSessionData(request);
		request.getSession().setAttribute(SESSION_ATTR_TC_DATA, result);
		return result;
	}

	/**
	 * 
	 * @param request HttpServletRequest holding the session
	 * @return TcSessionData for this session; null if there is none
	 */
	private TcSessionData retrieveLockFromSession(HttpServletRequest request) {
		return (TcSessionData)request
				.getSession()
				.getAttribute(SESSION_ATTR_TC_DATA);
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
				M_log.log(
						Level.FINER,
						"A request \""
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
			M_log.log(
					Level.FINER,
					"A request \""
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
		result = RequestSignatureManager.verifySignature(
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
			HttpServletRequest request)
	{
		request.setAttribute(
				JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON,
				request
						.getSession()
						.getAttribute(JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON));
	}

	/**
	 * Gives people in the roster read-only access to the given folder.  The
	 * instructor will be owner of the folder and their permissions are not
	 * touched.
	 * 
	 * If people already have higher permissions, this will not affect that.
	 * This would be the case because the instructor already gave them those
	 * permissions.
	 */
	private void insertPermissions(
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
			String instructorEmailAddress =
					tcSessionData.getUserEmailAddress();
			GoogleCredential googleCredential = getGoogleCredential(request);
			String fileId = request.getParameter(PARAM_FILE_ID);
			Drive drive = GoogleSecurity.getGoogleDrive(googleCredential);
			File file = getFile(drive, fileId);
			if (file == null) {
				logError(
						response,
						"Error: unable to modify Google Folder permissions, as "
						+ "the folder was not retrieved from Google Drive.");
				return;	// Quick return to simplify code
			}
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
						&& !instructorEmailAddress.equalsIgnoreCase(
								userEmailAddress))
				{
					// If result not null, the user has permission >= inserted
					if (insertPermission(
							drive,
							fileId,
							userEmailAddress,
							sendNotificationEmails) != null)
					{
						updateCount++;
					}
				}
			}
			response.getWriter().print(
					"Added permissions for folder \""
					+ file.getTitle()
					+ "\" to "
					+ updateCount
					+ ((updateCount == 1) ? " person" : " people")
					+ " in the roster (you already have permissions).");
		} catch (Exception err) {
			M_log.log(Level.FINER, "Error insertPermissions()", err);
		}
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
			String instructorEmailAddress =
					tcSessionData.getUserEmailAddress();
			GoogleCredential googleCredential = getGoogleCredential(request);
			String fileId = request.getParameter(PARAM_FILE_ID);
			Drive drive = GoogleSecurity.getGoogleDrive(googleCredential);
			File file = getFile(drive, fileId);
			if (file == null) {
				logError(
						response,
						"Error: unable to modify Google Folder permissions, as "
						+ "the folder was not retrieved from Google Drive.");
				return;	// Quick return to simplify code
			}
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
						&& !instructorEmailAddress.equalsIgnoreCase(
								userEmailAddress))
				{
					// If result not null, the user has permission >= inserted
					Permission permission = insertPermission(
							drive,
							fileId,
							userEmailAddress,
							sendNotificationEmails);
					if (permission != null)
					{
						if (removePermission(
								drive,
								fileId,
								userEmailAddress,
								permission.getId()))
						{
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
			M_log.log(Level.FINER, "Error insertPermissions()", err);
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
		if (!tcSessionData.getIsInstructor()) {
			logError(
					response,
					"Error: the server failed to confirm you are the "
					+ "instructor.");
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

	private File getFile(Drive drive, String fileId) throws IOException {
		return drive.files().get(fileId).execute();
	}

	/**
	 * Makes direct server-to-server request to get the site's roster, and
	 * displays the result in the server's log.
	 */
	private List<String> getRoster(
			HttpServletRequest request,
			TcSessionData tcSessionData)
	throws ServletException, IOException 
	{
		List<String> result = new ArrayList<String>();
		String sourceUrl = tcSessionData.getMembershipsUrl();
		try {
			// Make post to get resource
			HttpPost httpPost = new HttpPost(sourceUrl);
			Map<String, String> ltiParams =
					getLtiRosterParameters(request, tcSessionData, sourceUrl);
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			for (Map.Entry<String, String> parameter : ltiParams.entrySet()) {
				addParameter(nvps, parameter.getKey(), parameter.getValue());
			}
	        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpClient client = ClientSslWrapper.wrapClient(new DefaultHttpClient());
			HttpResponse httpResponse = client.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			if (httpEntity != null) {
				// See: http://www.mkyong.com/java/how-to-read-xml-file-in-java-dom-parser/
				DocumentBuilderFactory dbFactory =
						DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
				Document doc = docBuilder.parse(httpEntity.getContent());
				//optional, but recommended
				//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
				doc.getDocumentElement().normalize();
				NodeList nodes = doc
						.getElementsByTagName("person_contact_email_primary");
				for (int nodeIdx = 0; nodeIdx < nodes.getLength(); nodeIdx++) {
					Node node = nodes.item(nodeIdx);
					result.add(node.getTextContent());
				}
			}
		} catch (Exception err) {
			err.printStackTrace();
		}
		return result;
	}

	private Permission insertPermission(
			Drive drive,
			String fileId,
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
					drive.permissions().insert(fileId, newPermission)
					.setSendNotificationEmails(sendNotificationEmails)
					.execute();
		} catch (Exception err) {
			M_log.log(
					Level.FINER,
					"Failed to insert permission for user \""
					+ userEmailAddress
					+ "\" on file \""
					+ fileId
					+ "\"",
					err);
		}
		return result;
	}

	private boolean removePermission(
			Drive drive,
			String fileId,
			String userEmailAddress,
			String permissionId)
	{
		boolean result = false;
		try {
			drive.permissions().delete(fileId, permissionId).execute();
			// No errors indicates this operation succeeded
			result = true;
		} catch (Exception err) {
			M_log.log(
					Level.FINER,
					"Failed to remove permission "
					+ permissionId
					+ " for user \""
					+ userEmailAddress
					+ "\" on file \""
					+ fileId
					+ "\"",
					err);
		}
		return result;
	}

	/**
	 * Creates map of the request's parameters, including a signature the client
	 * server will verify matches with the request.
	 * 
	 * @param request Incoming request containing some of the ID of the client's
	 * site, so that roster may be retrieved.
	 * @param sourceUrl Client server's URL for requesting rosters.
	 * @return
	 */
	private Map<String, String> getLtiRosterParameters(
			HttpServletRequest request,
			TcSessionData tcSessionData,
			String sourceUrl)
	{
		Map<String, String> result = new HashMap<String, String>();
		result.put("id", tcSessionData.getMembershipsId());
		result.put("lti_message_type", "basic-lis-readmembershipsforcontext");
		result.put("lti_version", "LTI-1p0");
		result.put("oauth_callback", "about:blank");
		result = RequestSignatureManager.signParameters(
				result,
				sourceUrl,
				"POST",
				tcSessionData.getConsumerKey(),
				"secret");
		return result;
	}

	private void logError(HttpServletResponse response, String message)
	throws IOException
	{
		M_log.warning(message);
		response.getWriter().print(message);
	}

	private void addParameter(
			List<NameValuePair> nvps,
			String name,
			String value)
	{
		nvps.add(new BasicNameValuePair(name, value));
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
		request.setAttribute("jspPage", jspPage);
		retrieveGoogleDriveConfigFromSession(request);
		getServletContext()
				.getRequestDispatcher("/view/root.jsp")
				.forward(request, response);
	}
}
