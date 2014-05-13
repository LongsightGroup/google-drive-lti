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
package edu.umich.its.google.oauth;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Clock;
import com.google.api.services.drive.Drive;

public class GoogleSecurity {
	// Constants ----------------------------------------------------

	private static final Log M_log = LogFactory.getLog(GoogleSecurity.class);

	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	private static final String APPLICATION_NAME = "GoogleDriveLti";



	// Static public methods ----------------------------------------

	/** Authorizes the service account to access user's protected data. */
	static public GoogleCredential authorize(
			GoogleServiceAccount serviceAccount, String emailAddress) {
		if (serviceAccount == null) {
			M_log.warn("GoogleServiceAccount must not be null");
			return null;
		}
		if (emailAddress == null) {
			M_log.warn("User's email address must not be null");
			return null;
		}
		GoogleCredential result = null;
		try {
			// check for valid setup
			String filePath = serviceAccount.getPrivateKeyFilePath();
			// If this path is in classpath, get the file's path from it
			if (serviceAccount.getPrivateKeyFileClasspath()) {
				filePath = GoogleSecurity.class.getClassLoader()
						.getResource(filePath).getFile();
			}
			File privateKeyFile = new File(filePath);
			// Get service account credential
			String[] scopes = serviceAccount.getScopesArray();
			List<String> scopesCollection = Arrays.asList(scopes);
			result = new GoogleCredential.Builder()
			.setTransport(HTTP_TRANSPORT).setJsonFactory(JSON_FACTORY)
			.setServiceAccountId(serviceAccount.getEmailAddress())
			.setServiceAccountScopes(scopesCollection)
			.setServiceAccountPrivateKeyFromP12File(privateKeyFile)
			.setServiceAccountUser(emailAddress).build();
		} catch (Exception err) {
			M_log.warn("Failed to Google Authorize " + emailAddress);
			err.printStackTrace();
		}
		return result;
	}

	/**
	 * Generate the access token that is need to Authorizes the service account
	 * to access user's protected data for user requests
	 * 
	 * 
	 * @param userEmailAddress
	 *            User's full email address
	 * @param serviceAccount
	 *            Google Service Account for authorizing with Google
	 * @return
	 */
	static public String getGoogleAccessToken(
			GoogleServiceAccount serviceAccount, String userEmailAddress) {
		String result = null;
		try {
			GoogleCredential credential = authorize(serviceAccount,
					userEmailAddress);
			credential.refreshToken();
			result = credential.getAccessToken();
		} catch (Exception err) {
			StringBuilder sb = new StringBuilder();
			sb.append("Failed to get access token for user \"");
			sb.append(userEmailAddress);
			sb.append("\" and service account ");
			sb.append(serviceAccount);
			M_log.error(sb.toString(), err);
		}
		return result;
	}
	static public GoogleAccessToken getGoogleAccessTokenWithTimeStamp(
			GoogleServiceAccount serviceAccount, String userEmailAddress) {
		GoogleAccessToken googleAccessToken=null;
		try {
			GoogleCredential credential = authorize(serviceAccount,
					userEmailAddress);
			credential.refreshToken();
			String accessToken = credential.getAccessToken();
			Clock clock = credential.getClock();
			//Returns the current time in milliseconds since midnight, January 1, 1970 UTC.
			long currentTimeMillis = clock.currentTimeMillis();
			googleAccessToken = new GoogleAccessToken(accessToken,currentTimeMillis);
		} catch (Exception err) {
			StringBuilder sb = new StringBuilder();
			sb.append("Failed to get access token for user \"");
			sb.append(userEmailAddress);
			sb.append("\" and service account ");
			sb.append(serviceAccount);
			M_log.error(sb.toString(), err);
		}
		return googleAccessToken;
	}

	/**
	 * This creates the Drive object that is useful to interact with the Google
	 * Drive tool for doing various activities like inserting the permission and
	 * so. More info in below link
	 * https://developers.google.com/resources/api-libraries
	 * /documentation/drive/v2/java/latest/
	 * 
	 * @param credential
	 * @return
	 */
	static public Drive getGoogleDrive(GoogleCredential credential) {
		Drive result = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				credential).setApplicationName(APPLICATION_NAME).build();
		if (result == null) {
			M_log.error("Failed to Instantiate the Drive object, this might lead to error in permissions");
		}
		return result;
	}
}
