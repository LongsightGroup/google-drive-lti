package edu.umich.its.lti.google;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;
import net.oauth.signature.OAuthSignatureMethod;

/**
 * This handles security for requests, signing outgoing request, and ensuring
 * that incoming requests are proper.
 * 
 * @author ranaseef
 *
 */
public class RequestSignatureManager {
	// Constants ----------------------------------------------------

	private static final Logger M_log =
			Logger.getLogger(RequestSignatureManager.class.toString());


	// Static public methods ----------------------------------------

	/**
	 * This signs the parameters, and returns the updated map to the caller.
	 * This is a copy of code
	 * org.imsglobal.basiclti.BasicLTIUtil.signParameters().
	 * 
	 * Note, the given map is not updated; the caller needs to use the returned
	 * map.
	 * 
	 * @param parameters Map<String, String> of the parameter keys & values
	 * @param url        URL of the request to be made
	 * @param method     Request method (POST, GET)
	 * @param oauth_consumer_key Consumer's key
	 * @param oauth_consumer_secret Consumer's secret
	 * @return Map with oauth_signature to validate this request
	 */
	public static Map<String, String> signParameters(
			Map<String, String> parameters,
			String url,
			String method,
			String oauth_consumer_key,
			String oauth_consumer_secret)
	{
		Map<String, String> result = null;
		OAuthMessage oam = new OAuthMessage(method, url, parameters.entrySet());
		OAuthConsumer cons = new OAuthConsumer(
				"about:blank",
				oauth_consumer_key,
				oauth_consumer_secret,
				null);
		OAuthAccessor acc = new OAuthAccessor(cons);
		try {
			oam.addRequiredParameters(acc);
			List<Map.Entry<String, String>> params = oam.getParameters();
			result = new HashMap<String, String>();
			// Convert to Map<String, String>
			for (final Map.Entry<String, String> entry : params) {
				result.put(entry.getKey(), entry.getValue());
			}
			return result;
		} catch (net.oauth.OAuthException e) {
			M_log.warning("BasicLTIUtil.signProperties OAuth Exception "
					+ e.getMessage());
			throw new Error(e);
		} catch (java.io.IOException e) {
			M_log.warning("BasicLTIUtil.signProperties IO Exception "
					+ e.getMessage());
			throw new Error(e);
		} catch (java.net.URISyntaxException e) {
			M_log.warning("BasicLTIUtil.signProperties URI Syntax Exception "
					+ e.getMessage());
			throw new Error(e);
		}
	}

	/**
	 * Verifies the incoming request is valid request from client with the given
	 * key, and the request contains the matching signature.
	 * 
	 * This is copy of code org.sakaiproject.blti.ServiceServlet.doPostForm()
	 * 
	 * @param request HttpServletRequest made by the client
	 * @param oauth_consumer_key The client's key, included in the request
	 * @param oauth_consumer_secret THe client's secret, known locally and not
	 * included in the request.
	 * @return true if the request is valid
	 */
	public static boolean verifySignature(
			HttpServletRequest request,
			String oauth_consumer_key,
			String oauth_consumer_secret)
	{
		boolean result = false;
		OAuthMessage oam = OAuthServlet.getMessage(request, null);
		OAuthValidator oav = new SimpleOAuthValidator();
		OAuthConsumer cons = new OAuthConsumer(
				"about:blank",
				oauth_consumer_key,
				oauth_consumer_secret,
				null);
		OAuthAccessor acc = new OAuthAccessor(cons);
		try {
			oav.validateMessage(oam, acc);
			// If no error thrown, the message is valid
			result = true;
		} catch (Exception e) {
			String errMsg = "Failed to validate message";
			M_log.log(Level.WARNING, errMsg, e);
			try {
				String base_string = OAuthSignatureMethod.getBaseString(oam);
				if (base_string != null) {
					M_log.warning(base_string);
				}
			} catch (Exception err) {
				M_log.log(
						Level.WARNING,
						"Failed get get BaseString; this is for debugging - "
						+ "look at prior error \""
						+ errMsg
						+ "\"",
						err);
			}
		}
		return result;
	}
}
