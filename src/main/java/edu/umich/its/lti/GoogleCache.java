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
