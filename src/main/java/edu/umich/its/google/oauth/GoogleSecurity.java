package edu.umich.its.google.oauth;

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

	static public Drive getGoogleDrive(GoogleCredential credential) {
		Drive result = new Drive
				.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME)
				.build();
		return result;
	}
}
