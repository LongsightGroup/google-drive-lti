package edu.umich.its.lti;

/**
 * This holds information regarding one relationship between a TC Site and a
 * Google folder, including details needed to manage the relationship (i.e.,
 * linking/unlinking and modifying permissions to the Google folder).
 * 
 * @author ranaseef
 *
 */
public class TcSiteToGoogleLink
implements Comparable<TcSiteToGoogleLink>
{
	// Instance variables -------------------------------------------

	private String siteId;
	private String folderId;
	private String userId;
	private String userEmailAddress;


	// Constructors -------------------------------------------------

	public TcSiteToGoogleLink() {
	}


	// Public methods -----------------------------------------------

	public int compareTo(TcSiteToGoogleLink other) {
		int result = 0;
		if (other == null) {
			result = 1;
		} else {
			result = getSiteId().compareTo(other.getSiteId());
			if (result == 0) {
				result = getFolderId().compareTo(other.getFolderId());
			}
			if (result == 0) {
				result = getUserId().compareTo(other.getUserId());
			}
			if (result == 0) {
				result = getUserEmailAddress().compareTo(
						other.getUserEmailAddress());
			}
		}
		return 0;
	}

	public boolean equals(Object other) {
		if (other instanceof TcSiteToGoogleLink) {
			return (compareTo((TcSiteToGoogleLink)other) == 0);
		} else {
			return false;
		}
	}

	public String getSiteId() {
		return siteId;
	}

	public void setSiteId(String value) {
		siteId = value;
	}

	public String getFolderId() {
		return folderId;
	}

	public void setFolderId(String value) {
		folderId = value;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String value) {
		userId = value;
	}

	public String getUserEmailAddress() {
		return userEmailAddress;
	}

	public void setUserEmailAddress(String value) {
		userEmailAddress = value;
	}

	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(encodeField(getSiteId()))
				.append(",")
				.append(encodeField(getUserId()))
				.append(",")
				.append(encodeField(getUserEmailAddress()))
				.append(",")
				.append(encodeField(getFolderId()));
		return result.toString();
	}


	// Private methods ----------------------------------------------

	private static String encodeField(String value) {
		return value.replaceAll(",", "%2C")
				.replaceAll("\n", " ");
	}
}
