package edu.umich.its.lti.google;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

import org.apache.commons.lang3.StringEscapeUtils;
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


/**
 * Servlet with doGet() that will return some Google data to the browser, and
 * using doPost() to open web page that communicates with Google Drive to show
 * resources associated with the client's site.
 * 
 * This is proof-of-concept implementation, and will make requests to the client
 * to retrieve it site's roster, if the client's request includes parameters for
 * that.
 * 
 * Here is list of request parameters handled by this LTI servlet:
 * <table>
 * 	<tr>
 * 		<th>Property</th>
 * 		<th>Purpose</th>
 * 	</tr>
 * 	<tr>
 * 		<td>custom_roster_direct=true</td>
 * 		<td>
 * 			Performs server-2-server request to get site's roster during
 * 			doPost(), reported in this server's log
 * 		</td>
 * 	</tr>
 * 	<tr>
 * 		<td><b>custom_show_parameters=true</b></td>
 * 		<td>
 * 			<b>
 * 			Adds HTML table showing all of the request's parameters to the user.
 * 			<br/>
 * 			! Sharing this with browser is improper in production: SECURITY !
 * 			</b>
 * 		</td>
 * 	</tr>
 * </table>
 *
 * @author Raymond Naseef
 *
 **/
public class GoogleLtiServlet extends HttpServlet {
	// Constants -----------------------------------------------------

	private static final long serialVersionUID = -21239787L;
	private static final Logger M_log =
			Logger.getLogger(GoogleLtiServlet.class.toString());

	private static final String GOOGLE_SERVICE_ACCOUNT_PROPS_PREFIX =
			"googleDriveLti";
	private static final String HTTP_POST_JSP_PAGE = "/googleDriveLti.jsp";
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
	private static final String PARAM_ACCESS_TOKEN = "access_token";
	private static final String PARAM_USER_EMAIL_ADDRESS = "user_email_address";
	private static final String PARAM_FILE_ID = "file_id";
	private static final String PARAM_SEND_NOTIFICATION_EMAILS =
			"send_notification_emails";


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
		if (PARAM_ACTION_GIVE_ROSTER_ACCESS_READ_ONLY.equals(requestedAction)) {
			insertPermissions(request, response);
		} else if (PARAM_ACTION_REMOVE_ROSTER_ACCESS.equals(requestedAction)) {
			removePermissions(request, response);
		} else if (PARAM_ACTION_GET_ACCESS_TOKEN.equals(requestedAction)) {
			getGoogleAccessToken(request, response);
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
			if ("true".equalsIgnoreCase(
					request.getParameter("custom_roster_direct")))
			{
				getRosterDirect(request, response);
			}
			getGoogleDriveConfig(request);
			getServletContext()
					.getRequestDispatcher(HTTP_POST_JSP_PAGE)
					.forward(request, response);
		}
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
	 * This creates JSON with configuration of Google Drive, for use by the
	 * browser to manage the site's Google Resources.
	 */
	private void getGoogleDriveConfig(HttpServletRequest request)
	throws IOException {
		StringBuilder result = new StringBuilder("googleDriveConfig = {");
		String courseId = request.getParameter("context_id");
		if ((courseId == null) || courseId.trim().equals("")) {
			M_log.warning("Google Drive LTI request made without context_id!");
		}
		// 1 - Begin Adding User
		result.append(" \"user\" : {");
		// 1a - full name
		result.append(" \"name\" : '")
				.append(escapeJson(
						request.getParameter("lis_person_name_full")))
				.append("'");
		// 1b - email address
		String userEmailAddress = escapeJson(
				request.getParameter("lis_person_contact_email_primary"));
		if ("".equals(userEmailAddress)) {
			M_log.warning(
					"Google Drive LTI was opened by user without email address:"
					+ " please verify the tool is configured with"
					+ " imsti.releaseemail = 'on' for course (context_id) '"
					+ courseId
					+ "'");
		}
		result.append(", \"emailAddress\" : '")
				.append(userEmailAddress)
				.append("'");
		// 1c - roles
		boolean isInstructor = false;
		String roles = request.getParameter("roles");
		String[] roleArray = roles.split(",");
		result.append(", \"roles\" : [ ");
		for (int idx = 0; idx < roleArray.length; idx++) {
			if (idx > 0) {
				result.append(",");
			}
			if ("Instructor".equals(roleArray[idx])) {
				isInstructor = true;
			}
			result.append("'").append(escapeJson(roleArray[idx])).append("'");
		}
		result.append("]");
		// 1 - End Adding User
		result.append("}");
		// 2 - Begin Adding the folder
		result.append(", \"folder\" : {");
		// 2a - Folder's title
		result.append("\"title\" : \"")
				.append(escapeJson(request.getParameter("context_title")))
				.append("\"");
		// 2 - End Adding the folder
		result.append("}");
		// 3 - Enter course
		result.append(", \"course_id\" : \"");
		result.append(escapeJson(courseId));
		result.append("\"");
		// 4 - Enter parameters for requesting roster
		if (isInstructor) {
			result.append(", \"rosterRequestUrl\" : \"")
					.append(escapeJson(getRosterServerUrl(request)))
					.append("\"");
			result.append(", \"ltiMembershipsId\" : \"")
					.append(escapeJson(
							request.getParameter("ext_ims_lis_memberships_id")))
					.append("\"");
			result.append(", \"oauthCallback\" : \"")
					.append(escapeJson(request.getParameter("oauth_callback")))
					.append("\"");
			result.append(", \"oauthConsumerKey\" : \"")
					.append(escapeJson(
							request.getParameter("oauth_consumer_key")))
					.append("\"");
			// Pasting in notice so JSP can act differently for Instructor
			request.setAttribute("Instructor", "true");
		}
		// End the JSON object
		result.append("}");
		request.setAttribute(
				JSP_VAR_GOOGLE_DRIVE_CONFIG_JSON,
				result.toString());
	}

	/**
	 * Makes direct server-to-server request to get the site's roster, and
	 * displays the result in the server's log.
	 */
	private void getRosterDirect(
			HttpServletRequest request,
			HttpServletResponse response)
	throws ServletException, IOException 
	{
		String sourceUrl = getRosterServerUrl(request);
		try {
			// Make post to get resource
			HttpPost httpPost = new HttpPost(sourceUrl);
			Map<String, String> ltiParams =
					getLtiRosterParameters(request, sourceUrl);
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			for (Map.Entry<String, String> parameter : ltiParams.entrySet()) {
				nvps.add(new BasicNameValuePair(
						parameter.getKey(),
						parameter.getValue()));
			}
	        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpClient client = new DefaultHttpClient();
			HttpResponse httpResponse = client.execute(httpPost);
			HttpEntity httpEntity = httpResponse.getEntity();
			if (httpEntity != null) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(httpEntity.getContent()));
				StringBuilder contents = new StringBuilder();
				String line = reader.readLine();
				while (line != null) {
					contents.append(line).append("\n");
					line = reader.readLine();
				}
				M_log.log(Level.INFO, contents.toString());
			}
		} catch (Exception err) {
			err.printStackTrace();
		}
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
			HttpServletResponse response)
	throws ServletException, IOException 
	{
		try {
			if (!validatePermissionsRequiredParams(request, response)) {
				return;
			}
			String instructorEmailAddress =
					request.getParameter(PARAM_USER_EMAIL_ADDRESS);
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
			List<String> roster = getRoster(request);
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
			HttpServletResponse response)
	throws ServletException, IOException 
	{
		try {
			if (!validatePermissionsRequiredParams(request, response)) {
				return;
			}
			String instructorEmailAddress =
					request.getParameter(PARAM_USER_EMAIL_ADDRESS);
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
			List<String> roster = getRoster(request);
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
			HttpServletResponse response)
	throws IOException
	{
		String userEmailAddress =
				request.getParameter(PARAM_USER_EMAIL_ADDRESS);
		if (getIsEmpty(request.getParameter(PARAM_USER_EMAIL_ADDRESS))) {
			logError(
					response,
					"Error: unable to get access token - the request did no "
					+ "specify the user.");
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
			HttpServletResponse response)
	throws IOException
	{
		boolean result = true;
		if (getIsEmpty(request.getParameter(PARAM_USER_EMAIL_ADDRESS))) {
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

	private File getFile(Drive drive, String fileId) throws IOException {
		return drive.files().get(fileId).execute();
	}

	/**
	 * Makes direct server-to-server request to get the site's roster, and
	 * displays the result in the server's log.
	 */
	private List<String> getRoster(
			HttpServletRequest request)
	throws ServletException, IOException 
	{
		List<String> result = new ArrayList<String>();
		String sourceUrl = getRosterServerUrl(request);
		try {
			// Make post to get resource
			HttpPost httpPost = new HttpPost(sourceUrl);
			Map<String, String> ltiParams =
					getLtiRosterParameters(request, sourceUrl);
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
			String sourceUrl)
	{
		Map<String, String> result = new HashMap<String, String>();
		result.put("id", request.getParameter("ext_ims_lis_memberships_id"));
		result.put("lti_message_type", "basic-lis-readmembershipsforcontext");
		result.put("lti_version", "LTI-1p0");
		result.put("oauth_callback", request.getParameter("oauth_callback"));
		result = RequestSignatureManager.signParameters(
				result,
				sourceUrl,
				"POST",
				request.getParameter("oauth_consumer_key"),
				"secret");
		return result;
	}

	/**
	 * Convenient method to get URL from request coming in from client server.
	 * 
	 * @return URL for server to request rosters.  This is hard-coded, and does
	 * not consider the client server that contacted this LTI.
	 */
	private String getRosterServerUrl(HttpServletRequest request) {
		return request.getParameter("ext_ims_lis_memberships_url");
	}

	/**
	 * Returns the value escaped properly for placement as value in JSON; null
	 * is returned as ''
	 */
	private String escapeJson(String value) {
		return (value == null) ? "" : StringEscapeUtils.escapeEcmaScript(value);
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
}
