package edu.umich.its.lti;

import java.util.HashMap;
import java.util.Map;

public class GoogleCache {
	private static final GoogleCache INSTANCE=new GoogleCache();
	
	public static GoogleCache getInstance() {
		return INSTANCE;
	}
	
	private Map<String, TcSiteToGoogleLink> siteI2LinkMap=new HashMap<String, TcSiteToGoogleLink>();
	
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
