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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is used to store the unlinked folder in a map 
 * just before the deletion from the LTI setting service 
 * and used later during the deletion of permissions in google drive
 * **/

public class GoogleCache {
	private static final GoogleCache INSTANCE=new GoogleCache();
	
	public static GoogleCache getInstance() {
		return INSTANCE;
	}
	
	//used Concurrent Map for thread safety
	private Map<String, TcSiteToGoogleLink> siteI2LinkMap =new ConcurrentHashMap<String,TcSiteToGoogleLink>(); 
	
	private GoogleCache() {
		
	}
	
   public void clearLinkForSite(String siteId) {
	   siteI2LinkMap.remove(siteId);
	   
   }
   
   public TcSiteToGoogleLink getLinkForSite(String siteId) {
	   return siteI2LinkMap.get(siteId);
   }
   
   public void setLinkForSite(String siteId, TcSiteToGoogleLink link) {
	   siteI2LinkMap.put(siteId, link);
   }
}
