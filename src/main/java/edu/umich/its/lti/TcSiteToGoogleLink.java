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

/**
 * This holds information regarding one relationship between a TC(Tool Consumer) Site and a
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

	public TcSiteToGoogleLink(
			String siteId,
			String userEmailAddress,
			String userId,
			String folderId)
	{
		setSiteId(siteId);
		setUserEmailAddress(userEmailAddress);
		setUserId(userId);
		setFolderId(folderId);
	}


	// Public methods -----------------------------------------------

	@Override
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

	@Override
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

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(encodeComma(getSiteId()))
		.append(",")
		.append(encodeComma(getUserId()))
		.append(",")
		.append(encodeComma(getUserEmailAddress()))
		.append(",")
		.append(encodeComma(getFolderId()));
		return result.toString();
	}


	// Private methods ----------------------------------------------

	private static String encodeComma(String value) {
		return value.replaceAll(",", "%2C")
				.replaceAll("\n", " ");
	}
}
