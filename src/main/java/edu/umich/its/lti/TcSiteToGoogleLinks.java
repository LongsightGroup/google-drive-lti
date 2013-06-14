package edu.umich.its.lti;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author ranaseef
 *
 */
public class TcSiteToGoogleLinks implements Iterable<TcSiteToGoogleLink> {
	// Instance variables -------------------------------------------

	private long lastUpdatedTimeMs;
	private List<TcSiteToGoogleLink> links;


	// Constructors -------------------------------------------------

	TcSiteToGoogleLinks(List<TcSiteToGoogleLink> links, long lastUpdatedTimeMs)
	{
		setLinks(links);
		setLastUpdatedTimeMs(lastUpdatedTimeMs);
	}


	// Public methods -----------------------------------------------

	public boolean getIsStorageFileNewer(File storageFile) {
		return storageFile.lastModified() > lastUpdatedTimeMs;
	}

	public long getLastUpdatedTimeMs() {
		return lastUpdatedTimeMs;
	}

	public void setLastUpdatedTimeMs(long value) {
		lastUpdatedTimeMs = value;
	}

	public int size() {
		return (getLinks() == null) ? 0 : getLinks().size();
	}

	public TcSiteToGoogleLink getLink(int idx) {
		return (getLinks() == null) ? null : getLinks().get(idx);
	}

	public Iterator<TcSiteToGoogleLink> iterator() {
		return (getLinks() == null) ? null : getLinks().iterator();
	}


	// Friendly methods ---------------------------------------------

	void addLink(TcSiteToGoogleLink link) {
		getLinks().add(link);
	}

	void addLink(int index, TcSiteToGoogleLink link) {
		getLinks().add(index, link);
	}


	// Private methods ----------------------------------------------

	private List<TcSiteToGoogleLink> getLinks() {
		return links;
	}

	private void setLinks(List<TcSiteToGoogleLink> value) {
		links = value;
	}
}
