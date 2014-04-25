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

//FROM: package org.sakaiproject.googleservice.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Google Service Account information for authorizing with Google.
 * 
 * The properties for authorizing include:
 * <ul>
 * <li>Service Account's email address</li>
 * <li>Service Account's private key file path (.p12)</li>
 * <li>
 * Scopes the service account will need</li>
 * </ul>
 * 
 * @author ranaseef
 * 
 */
public class GoogleServiceAccount {
	// Constants ----------------------------------------------------

	// TODO: Replace this with different log (am not seeing these in Tomcat log)
	private static final Log M_log = LogFactory
			.getLog(GoogleServiceAccount.class);

	private static final String PROPERTY_SUFFIX_CLIENT_ID = ".service.account.client.id";
	private static final String PROPERTY_SUFFIX_EMAIL_ADDRESS = ".service.account.email.address";
	private static final String PROPERTY_SUFFIX_PRIVATE_KEY_FILE_PATH = ".service.account.private.key.file";
	private static final String PROPERTY_SUFFIX_PRIVATE_KEY_FILE_CLASSPATH = ".service.account.private.key.file.classpath";
	private static final String PROPERTY_SUFFIX_SCOPES = ".service.account.scopes";
	private static final String PROPERTY_SUFFIX_LTI_SECRET = ".lti.secret";
	private static final String PROPERTY_SUFFIX_LTI_KEY = ".lti.key";
	private static final String PROPERTY_SUFFIX_LTI_URL = ".lti.launchUrl";
	// These constants are used for loading properties from system files
	private static final String SYSTEM_PROPERTY_FILE_PATH = "googleServicePropsPath";
	private static final String SYSTEM_PROPERTY_FILE_DEFAULT_NAME = "googleServiceProps.properties";
	private static final String PROPERTY_SUFFIX_GOOGLE_CONTEXT = ".context";

	// Static methods -----------------------------------------------

	static {
		// Get properties from system
		initProperties();
	}

	static private void initProperties() {
		String propertiesFilePath = System
				.getProperty(SYSTEM_PROPERTY_FILE_PATH);

		InputStream in = null;
		try {
			// loads the file from the tomcat directory
			if (!isEmpty(propertiesFilePath)) {
				in = new FileInputStream(propertiesFilePath);
			} else {
				// loads the file from inside of the war
				String packagePath = GoogleServiceAccount.class.getPackage()
						.getName().replace(".", File.separator);
				in = GoogleServiceAccount.class.getClassLoader()
						.getResourceAsStream(
								packagePath + File.separator
								+ SYSTEM_PROPERTY_FILE_DEFAULT_NAME);
				if (in == null) {
					StringBuilder sb = new StringBuilder();
					sb.append("GoogleServiceAccount Properties resource \"");
					sb.append(SYSTEM_PROPERTY_FILE_DEFAULT_NAME);
					sb.append("\" not located.");
					M_log.error(sb.toString());
				}
			}
			if (in != null) {
				System.getProperties().load(in);
			}
		} catch (Exception err) {
			M_log.error(
					"Failed to load system properties(googleServiceProps.properties) for GoogleServiceAccount",
					err);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception err) {
					M_log.error("Failed to close the InputStream");
				}
			}
		}
	}

	static private boolean isEmpty(String value) {
		return (value == null) || (value.trim().equals(""));
	}

	// Instance variables -------------------------------------------

	private String clientId;
	private String emailAddress;
	private String privateKeyFilePath;
	// true = the file path is in classpath; false = file path is computer's
	// file path
	private boolean privateKeyFileClasspath = false;
	// Called SCOPES as this will be changed into String[] listing all the
	// scopes for the service account
	private String scopes;
	private String ltiSecret;
	private String ltiKey;

	private String contextURL;
	private String ltiUrl;
	
	private String propertiesPrefix;

	// Constructors -------------------------------------------------

	/**
	 * Use this in production, getting configuration for this service account
	 * from system properties.
	 * 
	 * @param propertiesPrefix
	 *            Prefix for properties, critical to keep properties separate
	 *            for each service account.
	 */
	public GoogleServiceAccount(String propertiesPrefix) {
		setPropertiesPrefix(propertiesPrefix);
	}

	/**
	 * Constructor setting properties directly; this is for unit testing only.
	 * If this method is called from anywhere else, that is an error.
	 * 
	 * @param clientId
	 *            Service Account's client ID.
	 * @param emailAddress
	 *            Service Account's email address.
	 * @param privateKeyFilePath
	 *            Pathname for account's .p12 file.
	 */
	public GoogleServiceAccount(String clientId, String emailAddress,
			String privateKeyFilePath) {
		M_log.error("This GoogleServiceAccount constructor is for unit testing and not proper in production.");
		setClientId(clientId);
		setEmailAddress(emailAddress);
		setPrivateKeyFilePath(privateKeyFilePath);
	}

	// Public methods -----------------------------------------------

	public String getClientId() {
		return clientId;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public String getPrivateKeyFilePath() {
		return privateKeyFilePath;
	}

	public boolean getPrivateKeyFileClasspath() {
		return privateKeyFileClasspath;
	}

	public String getPropertiesPrefix() {
		return propertiesPrefix;
	}

	public String getScopes() {
		return scopes;
	}

	public String getLtiSecret() {
		return ltiSecret;
	}
	public String getLtiKey() {
		return ltiKey;
	}
	
	public String getLtiUrl() {
		return ltiUrl;
	}

	public String getContextURL() {
		return contextURL;
	}

	public String[] getScopesArray() {
		String[] result;
		if (getScopes() == null) {
			result = new String[] {};
		} else {
			result = getScopes().split(",");
		}
		return result;
	}
	

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("GoogleServiceAccount [propPrefix=\"");
		sb.append(getPropertiesPrefix());
		sb.append("\", clientId=\"");
		sb.append(getClientId());
		sb.append("\", emailAddress=\"");
		sb.append(getEmailAddress());
		sb.append("\", p12FilePath=\"");
		sb.append(getPrivateKeyFilePath());
		sb.append("\", ltiUrl=\"");
		sb.append(getLtiUrl());
		sb.append("\", ltiKey=\"");
		sb.append(getLtiKey());
		sb.append("\", scopes=\"");
		sb.append(getScopes());
		sb.append("\"]");
		return sb.toString();
	}

	// Protected methods --------------------------------------------

	protected void setClientId(String value) {
		clientId = value;
	}

	protected void setEmailAddress(String value) {
		emailAddress = value;
	}

	protected void setPrivateKeyFilePath(String value) {
		privateKeyFilePath = value;
	}

	private void setPrivateKeyFileClasspath(boolean value) {
		privateKeyFileClasspath = value;
	}

	protected void setScopes(String value) {
		scopes = value;
	}

	protected void setLtiSecret(String value) {
		ltiSecret = value;
	}
	
	protected void setLtiKey(String ltiKey) {
		this.ltiKey = ltiKey;
	}
	
	public void setLtiUrl(String ltiUrl) {
		this.ltiUrl = ltiUrl;
	}
	public void setContextURL(String context) {
		this.contextURL = context;
	}

	// Private methods ----------------------------------------------

	/**
	 * Sets prefix used to get values from properties. This automatically gets
	 * those values immediately
	 */
	private void setPropertiesPrefix(String value) {
		if (isEmpty(value)) {
			throw new IllegalArgumentException(
					"Property prefix for GoogleServiceAccount must not be empty.");
		}
		propertiesPrefix = value;
		loadProperties();
	}

	/**
	 * Get account service's properties and store them internally.
	 */
	private void loadProperties() {
		setClientId(getStringProperty(PROPERTY_SUFFIX_CLIENT_ID));
		setEmailAddress(getStringProperty(PROPERTY_SUFFIX_EMAIL_ADDRESS));
		setPrivateKeyFilePath(getStringProperty(PROPERTY_SUFFIX_PRIVATE_KEY_FILE_PATH));
		setPrivateKeyFileClasspath(getBooleanProperty(PROPERTY_SUFFIX_PRIVATE_KEY_FILE_CLASSPATH));
		setScopes(getStringProperty(PROPERTY_SUFFIX_SCOPES));
		setLtiSecret(getStringProperty(PROPERTY_SUFFIX_LTI_SECRET));
		setLtiKey(getStringProperty(PROPERTY_SUFFIX_LTI_KEY));
		setLtiUrl(getStringProperty(PROPERTY_SUFFIX_LTI_URL));
		setContextURL(getStringProperty(PROPERTY_SUFFIX_GOOGLE_CONTEXT));
	}

	/**
	 * This method is responsible for getting boolean properties from system for
	 * this service account, using Boolean.parseBoolean(), defaulting in false.
	 */
	private boolean getBooleanProperty(String suffix) {
		String result = System.getProperty(getPropertiesPrefix() + suffix);
		return (result == null) ? false : Boolean.parseBoolean(result);
	}

	/**
	 * This method is responsible for getting properties from system for this
	 * service account.
	 */
	private String getStringProperty(String suffix) {
		return System.getProperty(getPropertiesPrefix() + suffix);
	}
}
