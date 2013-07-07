package edu.umich.its.google.oauth;
//FROM: package org.sakaiproject.googleservice.impl;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
* Google Service Account information for authorizing with Google.
* 
* The properties for authorizing include:
* <ul>
* 	<li>Service Account's email address</li>
* 	<li>Service Account's private key file path (.p12)</li>
* 	<li>
* 		Scopes the service account will need
* 	</li>
* </ul>
* 
* @author ranaseef
*
*/
public class GoogleServiceAccount {
	// Constants ----------------------------------------------------

	// TODO: Replace this with different log (am not seeing these in Tomcat log)
	private static final Log M_log =
			LogFactory.getLog(GoogleServiceAccount.class);

	private static final String PROPERTY_SUFFIX_CLIENT_ID =
			".service.account.client.id";
	private static final String PROPERTY_SUFFIX_EMAIL_ADDRESS =
			".service.account.email.address";
	private static final String PROPERTY_SUFFIX_PRIVATE_KEY_FILE_PATH =
			".service.account.private.key.file";
	private static final String PROPERTY_SUFFIX_PRIVATE_KEY_FILE_CLASSPATH =
			".service.account.private.key.file.classpath";
	private static final String PROPERTY_SUFFIX_SCOPES =
			".service.account.scopes";
	// These constants are used for loading properties from system files
	private static final String SYSTEM_PROPERTY_FILE_PATH =
			"googleServicePropsPath";
	private static final String SYSTEM_PROPERTY_FILE_XML =
			"googleServicePropsXml";
	private static final String SYSTEM_PROPERTY_FILE_DEFAULT_NAME =
			"googleServiceProps.properties";


	// Static methods -----------------------------------------------

	static {
		// Get properties from system
		initProperties();
	}

	static private void initProperties() {
		String propertiesFilePath =
				System.getProperty(SYSTEM_PROPERTY_FILE_PATH);
		boolean propertiesFileXml =
				Boolean.getBoolean(SYSTEM_PROPERTY_FILE_XML);
		InputStream in = null;
		try {
			if (!isEmpty(propertiesFilePath)) {
				in = new FileInputStream(propertiesFilePath);
			} else {
				// Use default file, sibling with this class in classpath.
				String packagePath =
						GoogleServiceAccount.class.getPackage().getName()
						.replace(".", "/");
				in = GoogleServiceAccount.class.getClassLoader()
						.getResourceAsStream(
								packagePath
								+ "/"
								+ SYSTEM_PROPERTY_FILE_DEFAULT_NAME);
				if (in == null) {
					M_log.info(
							"GoogleServiceAccount Properties resource \""
							+ SYSTEM_PROPERTY_FILE_DEFAULT_NAME
							+ "\" not located.");
				}
			}
			if (in != null) {
				if (propertiesFileXml) {
					System.getProperties().loadFromXML(in);
				} else {
					System.getProperties().load(in);
				}
			}
		} catch (Exception err) {
			M_log.warn(
					"Failed to load system properties for GoogleServiceAccount",
					err);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception err) {
					// Do nothing
				}
			}
		}
	}

	static private boolean isEmpty(String value) {
		return (value == null) || (value.trim() == "");
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
	private String propertiesPrefix;


	// Constructors -------------------------------------------------

	/**
	 * Use this in production, getting configuration for this service account
	 * from system properties.
	 * 
	 * @param propertiesPrefix Prefix for properties, critical to keep
	 * properties separate for each service account.
	 */
	public GoogleServiceAccount(String propertiesPrefix) {
		setPropertiesPrefix(propertiesPrefix);
	}

	/**
	 * Constructor setting properties directly; this is for unit testing only.
	 * If this method is called from anywhere else, that is an error.
	 * 
	 * @param clientId	Service Account's client ID.
	 * @param emailAddress	Service Account's email address.
	 * @param privateKeyFilePath Pathname for account's .p12 file.
	 */
	public GoogleServiceAccount(
			String clientId,
			String emailAddress,
			String privateKeyFilePath)
	{
		M_log.error(
				"This GoogleServiceAccount constructor is for unit testing and "
				+ "not proper in production.");
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

	public String[] getScopesArray() {
		String[] result;
		if (getScopes() == null) {
			result = new String[]{};
		} else {
			result = getScopes().split(",");
		}
		return result;
	}

	public String toString() {
		return
				"GoogleServiceAccount [propPrefix=\""
				+ getPropertiesPrefix()
				+ "\", clientId=\""
				+ getClientId()
				+ "\", emailAddress=\""
				+ getEmailAddress()
				+ "\", p12FilePath=\""
				+ getPrivateKeyFilePath()
				+ "\", scopes=\""
				+ getScopes()
				+ "\"]";
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


	// Private methods ----------------------------------------------

	/**
	 * Sets prefix used to get values from properties.  This automatically gets
	 * those values immediately
	 */
	private void setPropertiesPrefix(String value) {
		if (isEmpty(value)) {
			throw new IllegalArgumentException(
					"Property prefix for GoogleServiceAccount must not be "
					+ "empty.");
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
		setPrivateKeyFilePath(
				getStringProperty(PROPERTY_SUFFIX_PRIVATE_KEY_FILE_PATH));
		setPrivateKeyFileClasspath(
				getBooleanProperty(PROPERTY_SUFFIX_PRIVATE_KEY_FILE_CLASSPATH));
		setScopes(getStringProperty(PROPERTY_SUFFIX_SCOPES));
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
