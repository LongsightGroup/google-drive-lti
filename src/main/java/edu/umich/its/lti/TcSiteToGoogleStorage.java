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
package edu.umich.its.lti;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.umich.its.lti.google.GoogleLtiServlet;
import edu.umich.its.lti.utils.SettingsClientUtils;

/**
 * This class manages persistence of relationships between TC (Tool
 * Consumer)sites and Google folders. Storage is done in one file for each
 * TC(Tool Consumer) site, listing its linked Google folders.
 * 
 * @author ranaseef
 * 
 */
public class TcSiteToGoogleStorage {
	// Constants ----------------------------------------------------

	private static final Log M_log = LogFactory
			.getLog(TcSiteToGoogleStorage.class);

	// Static public methods ----------------------------------------

	/**
	 * Setting string in the format
	 * <site_id>,<user_id>,<user_email_address>,<google-folder-id> to setting
	 * service with the a Folder is shared/linked with the site and also storing the setting value in the session
	 * for accessing later calls(as Setting service seems to be buggy sometime)
	 * @throws Exception 
	 * @throws ServletException 
	 * 
	 */
	public synchronized static Boolean setLinkingToSettingService(
			TcSessionData tcSessionData, TcSiteToGoogleLink linking, HttpServletRequest request) throws Exception {
		Boolean state = false;
			state = SettingsClientUtils.setSetting(tcSessionData,
					linking.toString());
			if(state) {
				setSettingToSession(request, linking.toString());
			}else {
				M_log.error("Setting service call is unsuccessful and putting the Settings value in session failed");
			}

		return state;

	}

	/**
	 * This will set empty string when a folder is unshared/Unlinked to the
	 * setting service and storing the setting value in the session for accessing later calls(as Setting service call seems to be buggy sometime).
	 * @throws IOException 
	 * @throws ServletException 
	 * 
	 * */

	public synchronized static Boolean setUnLinkingToSettingService(
			TcSessionData tcSessionData, HttpServletRequest request) throws ServletException, IOException  {
		Boolean state = false;
		TcSiteToGoogleLink result = null;
		String linkedGoogleFolder=(String)request.getSession().getAttribute(GoogleLtiServlet.SETTING_SERVICE_VALUE_IN_SESSION);
		if (linkedGoogleFolder != null) {
			result = parseLink(linkedGoogleFolder);
		}
		state = SettingsClientUtils.setSetting(tcSessionData, "");
		if (state) {
			GoogleCache.getInstance().setLinkForSite(
					tcSessionData.getContextId(), result);
			setSettingToSession(request, null);
		}else {
			M_log.error("Setting service call is unsuccessful and putting the Settings value in session failed");
		}
		return state;

	}

	/**
	 * Getting the shared/linked folder from the setting service. 
	 * During the launch of the LTI tool we only get the value from setting service as Intermittently some times the get call to setting service is not fetching the correct value.  
	 * So after getting the value from Setting Service we are storing this value in session for accessing it for later calls that need this value. 
	 * @throws ServletException
	 * @throws IOException
	 * */
	public synchronized static TcSiteToGoogleLink getLinkingFromSettingService(
			TcSessionData tcSessionData,HttpServletRequest request) throws IOException, ServletException {
		TcSiteToGoogleLink result = null;
		String linkedGoogleFolder = SettingsClientUtils.getSettingString(tcSessionData);
		setSettingToSession(request, linkedGoogleFolder);

		if (linkedGoogleFolder != null) {
			result = parseLink(linkedGoogleFolder);
		}
		return result;
	}
	/**
	 * Helper method to set the  value <site_id>,<user_id>,<user_email_address>,<google-folder-id> to the session 
	 * @param request
	 * @param linkedGoogleFolder
	 */

	private static void setSettingToSession(HttpServletRequest request,
			String linkedGoogleFolder) {
		request.getSession().setAttribute(GoogleLtiServlet.SETTING_SERVICE_VALUE_IN_SESSION, linkedGoogleFolder);
	}

	/**
	 * Parse the given line into a link. The line is in format:
	 * 
	 * <site_id>,<user_id>,<user_email_address>,<google-folder-id>
	 * 
	 */
	public static TcSiteToGoogleLink parseLink(String line) {
		TcSiteToGoogleLink result = new TcSiteToGoogleLink();
		String[] fields = line.split(",");
		if (fields.length != 4) {
			throw new IllegalArgumentException(
					"Data line storing link of TC Site to Google Folder is invalid: "
							+ line);
		}
		result.setSiteId(decodeComma(fields[0]));
		result.setUserId(decodeComma(fields[1]));
		result.setUserEmailAddress(decodeComma(fields[2]));
		result.setFolderId(decodeComma(fields[3]));
		return result;
	}

	private static String decodeComma(String value) {
		return value.replaceAll("%2C", ",");
	}
}
