package edu.umich.its.lti;

import java.util.HashMap;
import java.util.Map;

/**
 * This holds Site to Google folder links in memory, so TP can retrieve them
 * without reading the file each time.
 * 
 * @author ranaseef
 *
 */
public class TcSiteToGoogleCache {
	// Constants ----------------------------------------------------

	private static final TcSiteToGoogleCache INSTANCE =
			new TcSiteToGoogleCache();


	// Static public methods ----------------------------------------

	public static TcSiteToGoogleCache getInstance() {
		return INSTANCE;
	}


	// Instance variables -------------------------------------------

	private Map<String, TcSiteToGoogleLinks> siteId2LinksMap =
			new HashMap<String, TcSiteToGoogleLinks>();


	// Constructors -------------------------------------------------

	private TcSiteToGoogleCache() {
	}


	// Public methods -----------------------------------------------

	public void clearLinksForSite(String siteId) {
		siteId2LinksMap.remove(siteId);
	}

	public TcSiteToGoogleLinks getLinksForSite(String siteId) {
		return siteId2LinksMap.get(siteId);
	}

	public void setLinksForSite(String siteId, TcSiteToGoogleLinks links) {
		siteId2LinksMap.put(siteId, links);
	}
}
