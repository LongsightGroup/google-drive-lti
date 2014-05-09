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
/**
 * This class holds the Access token generated for an user and  the time stamp the Access 
 * token generated. This Object is kept in the Session and passed to the JSP page for accessing
 * in Javascript. 
 * 
 * With The launch of the LTI tool(POST) Every time  Access token is generated.
 * The Access token life time as google says seems to be 1 hour. But google agrees that they don't
 * always expires the access token in an a hour. So they recommend to watch out for a 401-unauthorised error.
 * More on this https://code.google.com/p/google-oauth-java-client/wiki/OAuth2#FAQ.
 *   
 * @author pushyami
 *
 */

public class GoogleAccessToken {

	private String token;
	private long timeTokenCreated;
	

	public GoogleAccessToken(String token, long currentTimeMillis) {
		this.token=token;
		this.timeTokenCreated=currentTimeMillis;
	}
	
	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public long getTimeTokenCreated() {
		return timeTokenCreated;
	}

	public void setTimeTokenCreated(long timeTokenCreated) {
		this.timeTokenCreated = timeTokenCreated;
	}


}
