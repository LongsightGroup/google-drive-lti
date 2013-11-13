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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;

public class GoogleSecurity {
	// Constants ----------------------------------------------------

	private static final Logger M_log =
			Logger.getLogger(GoogleSecurity.class.toString());

	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	private static final String APPLICATION_NAME = "GoogleDriveLti";


	// Static public methods ----------------------------------------

	/** Authorizes the service account to access user's protected data. */
	static public GoogleCredential authorize(
			GoogleServiceAccount serviceAccount,
			String emailAddress)
	{
		if (serviceAccount == null) {
			M_log.warning("GoogleServiceAccount must not be null");
			return null;
		}
		if (emailAddress == null) {
			M_log.warning("User's email address must not be null");
			return null;
		}
		GoogleCredential result = null;
		try {
			// check for valid setup
			String filePath = serviceAccount.getPrivateKeyFilePath();
			// If this path is in classpath, get the file's path from it
			if (serviceAccount.getPrivateKeyFileClasspath()) {
				filePath = GoogleSecurity.class
						.getClassLoader()
						.getResource(filePath)
						.getFile();
			}
			File privateKeyFile = new File(filePath);
			// Get service account credential
			String[] scopes = serviceAccount.getScopesArray();
			result = new GoogleCredential.Builder()
					.setTransport(HTTP_TRANSPORT)
					.setJsonFactory(JSON_FACTORY)
					.setServiceAccountId(serviceAccount.getEmailAddress())
					.setServiceAccountScopes(scopes)
					.setServiceAccountPrivateKeyFromP12File(
							privateKeyFile)
					.setServiceAccountUser(emailAddress)
					.build();
		} catch (Exception err) {
			M_log.warning("Failed to Google Authorize " + emailAddress);
			err.printStackTrace();
		}
		return result;
	}

	/**
	 * Creates calendar client from GoogleCredential and opens the calendar with
	 * the given ID, returning accessToken for that.
	 * 
	 * @param userEmailAddress	User's full email address
	 * @param gcalId	Google ID for the calendar to open
	 * @return
	 */
	static public String getGoogleAccessToken(
			GoogleServiceAccount serviceAccount,
			String userEmailAddress)
	{
		String result = null;
		try {
			GoogleCredential credential =
					authorize(serviceAccount, userEmailAddress);
			credential.refreshToken();
			result = credential.getAccessToken();
		} catch (Exception err) {
			M_log.log(
					Level.ALL,
					"Failed to get access token for user \""
					+ userEmailAddress
					+ "\" and service account "
					+ serviceAccount,
					err);
		}
		return result;
	}

	static public Drive getGoogleDrive(GoogleCredential credential) {
		Drive result = new Drive
				.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME)
				.build();
		return result;
	}
}
