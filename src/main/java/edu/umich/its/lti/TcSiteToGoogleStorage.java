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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.umich.its.lti.utils.SettingsClientUtils;

/**
 * This class manages persistence of relationships between TC (Tool Consumer)sites and Google
 * folders. Storage is done in one file for each TC(Tool Consumer) site, listing its linked
 * Google folders.
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
	 * Adding google linked folder to setting service
	 * 
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public synchronized static Boolean setLinkingToSettingService(
			TcSessionData tcSessionData, TcSiteToGoogleLink linking)
					throws IOException, Exception {
		Boolean state = false;
		state = SettingsClientUtils.setSetting(tcSessionData,
				linking.toString());

		return state;

	}

	public synchronized static Boolean setLinkingToSettingServiceWithNoLinking(
			TcSessionData tcSessionData) throws IOException, ServletException {
		Boolean state = false;

		TcSiteToGoogleLink linkBeforeDeletion = null;
		linkBeforeDeletion = getLinkingFromSettingService(tcSessionData);
		state = SettingsClientUtils.setSetting(tcSessionData, "");
		if (state) {
			GoogleCache.getInstance().setLinkForSite(
					tcSessionData.getContextId(), linkBeforeDeletion);
		}
		return state;

	}

	/**
	 * Getting the linked folder from the setting service
	 * 
	 * @throws ServletException
	 * 
	 * */
	public synchronized static TcSiteToGoogleLink getLinkingFromSettingService(
			TcSessionData tcSessionData) throws IOException, ServletException {
		TcSiteToGoogleLink result = null;
		String linkedGoogleFolder = SettingsClientUtils
				.getSettingString(tcSessionData);
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
					"Data line storing link of TC Site to Google Folder is "
							+ "invalid: " + line);
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
