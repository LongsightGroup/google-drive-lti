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
	private static final String POST = "POST";
	private static final String SETTING_SERVICE_VALUE_IN_SESSION = "SettingValue";
	private static final String HOME = "Home";
	private static final String LINK_FOLDER = "LinkFolder";

	/**
	 * Setting string in the format
	 * <site_id>,<user_id>,<user_email_address>,<google-folder-id> to setting
	 * service with the a Folder is shared/linked with the site.
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
				request.getSession().setAttribute("SettingValue", linking.toString());
			}else {
				M_log.error("Setting service call is unsuccessful and putting the Settings value in session failed");
			}

		return state;

	}

	/**
	 * This will set empty string when a folder is unshared/Unlinked to the
	 * setting service
	 * @throws IOException 
	 * @throws ServletException 
	 * 
	 * */

	public synchronized static Boolean setUnLinkingToSettingService(
			TcSessionData tcSessionData, HttpServletRequest request) throws ServletException, IOException  {
		Boolean state = false;

		TcSiteToGoogleLink linkBeforeDeletion = null;
		linkBeforeDeletion = getLinkingFromSettingService(tcSessionData,request);
		state = SettingsClientUtils.setSetting(tcSessionData, "");
		if (state) {
			GoogleCache.getInstance().setLinkForSite(
					tcSessionData.getContextId(), linkBeforeDeletion);
			request.getSession().setAttribute("SettingValue", null);
		}else {
			M_log.error("Setting service call is unsuccessful and putting the Settings value in session failed");
		}
		return state;

	}

	/**
	 * Getting the shared/linked folder from the setting service
	 * 
	 * @throws ServletException
	 * 
	 * */
	public synchronized static TcSiteToGoogleLink getLinkingFromSettingService(
			TcSessionData tcSessionData,HttpServletRequest request) throws IOException, ServletException {
		TcSiteToGoogleLink result = null;
		String linkedGoogleFolder = SettingsClientUtils
				.getSettingString(tcSessionData);
		if(request.getMethod()!=POST) {
			String action = request.getParameter(GoogleLtiServlet.PARAMETER_ACTION);
		if(linkedGoogleFolder==null) {
			if(action.equals(GoogleLtiServlet.PARAM_ACTION_GIVE_ROSTER_ACCESS)) {
				linkedGoogleFolder=(String)request.getSession().getAttribute(SETTING_SERVICE_VALUE_IN_SESSION);
			}
			if(action.equals(GoogleLtiServlet.PARAM_ACTION_OPEN_PAGE)) {
				String pageName = request.getParameter(GoogleLtiServlet.PARAM_OPEN_PAGE_NAME);
				if(pageName.equals(HOME)) {
				linkedGoogleFolder=(String)request.getSession().getAttribute(SETTING_SERVICE_VALUE_IN_SESSION);
				}
			}
			if(action.equals(GoogleLtiServlet.PARAM_ACTION_UNLINK_GOOGLE_FOLDER)) {
				linkedGoogleFolder=(String)request.getSession().getAttribute(SETTING_SERVICE_VALUE_IN_SESSION);
			}
		}else {
			if(action.equals(GoogleLtiServlet.PARAM_ACTION_REMOVE_ROSTER_ACCESS)) {
				linkedGoogleFolder=(String)request.getSession().getAttribute(SETTING_SERVICE_VALUE_IN_SESSION);
			}
			if(action.equals(GoogleLtiServlet.PARAM_ACTION_OPEN_PAGE)) {
				String pageName = request.getParameter(GoogleLtiServlet.PARAM_OPEN_PAGE_NAME);
				if(pageName.equals(LINK_FOLDER)) {
				linkedGoogleFolder=(String)request.getSession().getAttribute(SETTING_SERVICE_VALUE_IN_SESSION);
				}
				
			}
			if(action.equals(GoogleLtiServlet.PARAM_ACTION_CHECK_BACK_BUTTON)) {
				linkedGoogleFolder=(String)request.getSession().getAttribute(SETTING_SERVICE_VALUE_IN_SESSION);
			}
		}
		}
		
		if (linkedGoogleFolder != null) {
			result = parseLink(linkedGoogleFolder);
		}
		return result;
	}

	/**
	 * Parse the given line into a link. The line is in format:
	 * 
	 * <site_id>,<user_id>,<user_email_address>,<google-folder-id>
	 * 
	 */
	private static TcSiteToGoogleLink parseLink(String line) {
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
