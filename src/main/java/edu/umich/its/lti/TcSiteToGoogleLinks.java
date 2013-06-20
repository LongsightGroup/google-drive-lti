package edu.umich.its.lti;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author ranaseef
 *
 */
public class TcSiteToGoogleLinks implements Iterable<TcSiteToGoogleLink> {
	// Constants ----------------------------------------------------

	// When unlinked list exceeds this size, the oldest entries are removed.
	private static int MAX_UNLINKED_LIST_SIZE = 10;


	// Instance variables -------------------------------------------

	private long lastUpdatedTimeMs;
	private List<TcSiteToGoogleLink> links;
	// This is used to help keep links around for a little while after they are
	// unlinked, so permissions can be modified for removed links.
	private List<TcSiteToGoogleLink> unlinkedLinkList;


	// Constructors -------------------------------------------------

	TcSiteToGoogleLinks(List<TcSiteToGoogleLink> links, long lastUpdatedTimeMs)
	{
		setLinks(links);
		setLastUpdatedTimeMs(lastUpdatedTimeMs);
	}


	// Public methods -----------------------------------------------

	/**
	 * Returns true if the storage file was modified after its last modified
	 * time this was updated (the last timestamp the file was modified, not the
	 * timestamp it was read).
	 * 
	 * @param storageFile File that is used for this record's site
	 * @return true = storage file has updates; this is out of sync
	 */
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

	/**
	 * Returns link for the given folder, if it is linked.  If not linked, the
	 * result is null.
	 * 
	 * @param folderId	Google file ID of the linked folder
	 * @return TcSiteToGoogleLink (or null)
	 */
	public TcSiteToGoogleLink getLinkForFolder(String folderId) {
		for (TcSiteToGoogleLink link : links) {
			if (folderId.equals(link.getFolderId()))
			{
				// Found - we are done: return immediately
				return link;
			}
		}
		// Not found: return null
		return null;
	}

	/**
	 * Returns link that was recently unlinked for the given folder.  If there
	 * are multiple links removed for this folder, the one being returned may be
	 * inconsistent over repeated calls.
	 */
	public TcSiteToGoogleLink getRemovedLinkForFolder(String folderId) {
		TcSiteToGoogleLink result = null;
		List<TcSiteToGoogleLink> linkList = getUnlinkedLinkList(false);
		if (linkList != null) {
			for (TcSiteToGoogleLink link : linkList) {
				if (link.getFolderId().equals(folderId)) {
					result = link;
					break;
				}
			}
		}
		return result;
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

	TcSiteToGoogleLink removeLink(String siteId, String folderId) {
		TcSiteToGoogleLink result = null;
		List<TcSiteToGoogleLink> links = getLinks();
		for (int linkIdx = 0; linkIdx < links.size(); linkIdx++) {
			TcSiteToGoogleLink link = links.get(linkIdx);
			if (
					siteId.equals(link.getSiteId())
					&& folderId.equals(link.getFolderId()))
			{
				result = link;
				links.remove(linkIdx);
				addRemovedLink(link);
				break;
			}
		}
		return result;
	}


	// Private methods ----------------------------------------------

	private void addRemovedLink(TcSiteToGoogleLink link) {
		List<TcSiteToGoogleLink> linkList = getUnlinkedLinkList(true);
		linkList.add(link);
		while (linkList.size() > MAX_UNLINKED_LIST_SIZE) {
			linkList.remove(0);
		}
	}

	private List<TcSiteToGoogleLink> getUnlinkedLinkList(
			boolean create)
	{
		if ((unlinkedLinkList == null) && create) {
			unlinkedLinkList = new ArrayList<TcSiteToGoogleLink>();
		}
		return unlinkedLinkList;
	}

	private List<TcSiteToGoogleLink> getLinks() {
		return links;
	}

	private void setLinks(List<TcSiteToGoogleLink> value) {
		links = value;
	}
}
