package edu.umich.its.lti;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class manages persistence of relationships between TC sites and Google
 * folders.  Storage is done in one file for each TC site, listing its linked
 * Google folders.
 * 
 * @author ranaseef
 *
 */
public class TcSiteToGoogleStorage {
	// Constants ----------------------------------------------------

	private static final Log M_log =
			LogFactory.getLog(TcSiteToGoogleStorage.class);

	private static String STORAGE_FOLDER_PROPERTY = "googleServiceStoragePath";


	// Static variables ---------------------------------------------

	private static String storageFolder = null;


	// Static public methods ----------------------------------------

	/**
	 * Adds link of site with Google folder, iff there is no otherLink where
	 * link.equals(otherLink).
	 * 
	 * @param newLink Link to add
	 * @return TcSiteToGoogleLinks holding links for the new link's site
	 * @throws IOException If saving to storage file fails
	 */
	public synchronized static TcSiteToGoogleLinks addLink(
			TcSiteToGoogleLink newLink)
	throws IOException
	{
		TcSiteToGoogleLinks result = loadLinks(newLink.getSiteId());
		if (result != null) {
			addLink(result, newLink);
		} else {
			List<TcSiteToGoogleLink> linkList =
					new ArrayList<TcSiteToGoogleLink>();
			linkList.add(newLink);
			// Sets time -1, as the storage file DNE
			result = new TcSiteToGoogleLinks(linkList, -1);
			saveLinks(newLink.getSiteId(), result);
			TcSiteToGoogleCache.getInstance().setLinksForSite(
					newLink.getSiteId(), 
					result);
		}
		return result;
	}

	/**
	 * Returns holder of all links for a given site.
	 * 
	 * @param siteId
	 * @return
	 * @throws IOException
	 */
	public static TcSiteToGoogleLinks getLinkedGoogleFolders(
			String siteId)
	throws IOException
	{
		// Get from cache
		File storageFile = getStorageFile(siteId);
		// Get from file if cache has null or out-dated copy
		TcSiteToGoogleLinks result = 
				TcSiteToGoogleCache.getInstance().getLinksForSite(siteId);
		if ((result == null) || result.getIsStorageFileNewer(storageFile)) {
			result = loadLinks(siteId);
			if (result != null) {
				TcSiteToGoogleCache
						.getInstance()
						.setLinksForSite(siteId, result);
			}
		}
		return result;
	}

	public static File getStorageFile(String siteId) {
		return new File(getStorageFolder(), siteId + ".lti.txt");
	}

	public static String getStorageFolder() {
		if (storageFolder == null) {
			storageFolder = System.getProperty(STORAGE_FOLDER_PROPERTY);
		}
		return storageFolder;
	}

	/**
	 * Removes link for the given site and folder.  Returns false if not removed
	 * (e.g., the site has no links).
	 * 
	 * @param siteId
	 * @param folderId
	 * @return
	 * @throws IOException
	 */
	public synchronized static boolean removeLink(
			String siteId,
			String folderId)
	throws IOException
	{
		boolean result = false;
		TcSiteToGoogleLinks links = loadLinks(siteId);
		TcSiteToGoogleLink trashed = links.removeLink(siteId, folderId);
		if (trashed != null) {
			try {
				saveLinks(siteId, links);
				TcSiteToGoogleCache
						.getInstance()
						.setLinksForSite(siteId, links);
				result = true;
			} catch (Exception err) {
				// Failed to unlink: for consistency, putting the link back into
				// the cache
				links.addLink(trashed);
				throw new RuntimeException(
						"Failed to unlink folder #"
						+ folderId
						+ " from site #"
						+ siteId,
						err);
			}
		}
		return result;
	}


	// Static private methods ---------------------------------------

	private static void addLink(
			TcSiteToGoogleLinks links,
			TcSiteToGoogleLink newLink)
	throws IOException
	{
		String siteId = newLink.getSiteId();
		// Replace or add the new link
		boolean added = false;
		for (int linkIdx = 0; !added && (linkIdx < links.size()); linkIdx++) {
			TcSiteToGoogleLink link = links.getLink(linkIdx);
			// Check folder ID for match (site ID must be same)
			if (link.getFolderId().equals(newLink.getFolderId())) {
				// Already in the file: let's only continue if they differ
				if (newLink.equals(link)) {
					// The link already exists: consider done
					added = true;
					break;
				} else {
					// Replace the old entry for the folder
					links.addLink(linkIdx, newLink);
					added = true;
				}
			}
		}
		if (!added) {
			links.addLink(newLink);
		}
		saveLinks(siteId, links);
		TcSiteToGoogleCache.getInstance().setLinksForSite(siteId, links);
	}

	/**
	 * Reads and returns all the links from the site's storage file.
	 * 
	 * @param siteId TC ID for the site
	 * @return TcSiteToGoogleLinks if there are links; null = there storage file
	 * DNE (links will be returned if storage file exists with no links)
	 * @throws IOException If reading the file fails
	 */
	private static TcSiteToGoogleLinks loadLinks(String siteId)
	throws IOException
	{
		File storageFile = getStorageFile(siteId);
		TcSiteToGoogleLinks result = null;
		List<TcSiteToGoogleLink> linksList =
				new ArrayList<TcSiteToGoogleLink>();
		BufferedReader reader = null;
		// Do nothing if the file does not exist
		if (!storageFile.exists()) {
			return null;	// Quick return to simplify code
		}
		try {
			reader = new BufferedReader(new FileReader(storageFile));
			String line = reader.readLine();
			// Read 2nd line to skip the header
			line = reader.readLine();
			while (line != null) {
				try {
					TcSiteToGoogleLink link = parseLink(line);
					// If stored in one file per site, this is not needed.
					if (siteId.equals(link.getSiteId())) {
						linksList.add(link);
						line = reader.readLine();
					}
				} catch (Exception err) {
					M_log.error(
							"Failed to parse link for site "
							+ siteId
							+ ": "
							+ line,
							err);
				}
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception err) {
					M_log.error(
							"Warning: reader failed to close properly; may be "
							+ "due to another IOException.",
							err);
				}
			}
		}
		result = new TcSiteToGoogleLinks(linksList, storageFile.lastModified());
		return result;
	}

	private synchronized static void saveLinks(
			String siteId,
			TcSiteToGoogleLinks links)
	throws IOException
	{
		BufferedWriter writer = null;
		File storageFile = getStorageFile(siteId);
		try {
			writer = new BufferedWriter(new FileWriter(storageFile));
			writer.write(getHeaderLine());
			writer.newLine();
			for (TcSiteToGoogleLink link : links) {
				writer.write(assembleLink(link));
				writer.newLine();
			}
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception err) {
					err.printStackTrace();
				}
			}
		}
		// Set last updated time after the write has been closed.
		links.setLastUpdatedTimeMs(storageFile.lastModified());
	}

	private static String getHeaderLine() {
		return "site_id,user_id,user_email_address,google_folder_id";
	}

	private static String assembleLink(TcSiteToGoogleLink link) {
		return link.toString();
	}

	/**
	 * Parse the given line into a link.  The line is in format:
	 * 
	 * <site_id>,<user_id>,<user_email_address>,<google-folder-id>
	 * 
	 */
	private static TcSiteToGoogleLink parseLink(String line) {
		TcSiteToGoogleLink result = new TcSiteToGoogleLink();
		String[] fields = line.split(",");
		if (fields.length != 4) {
			throw new IllegalArgumentException(
					"Data line storing link of TC Site to Google Folder is "
					+ "invalid: "
					+ line);
		}
		result.setSiteId(decodeField(fields[0]));
		result.setUserId(decodeField(fields[1]));
		result.setUserEmailAddress(decodeField(fields[2]));
		result.setFolderId(decodeField(fields[3]));
		return result;
	}

	private static String decodeField(String value) {
		return value.replaceAll("%2C", ",");
	}
}
