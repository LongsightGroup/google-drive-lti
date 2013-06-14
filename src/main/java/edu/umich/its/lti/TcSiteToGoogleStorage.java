package edu.umich.its.lti;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

	private static String STORAGE_FOLDER_PROPERTY = "googleServiceStoragePath";


	// Static variables ---------------------------------------------

	private static String storageFolder = null;


	// Static public methods ----------------------------------------

	public synchronized static void addLink(TcSiteToGoogleLink newLink)
	throws IOException
	{
		String siteId = newLink.getSiteId();
		TcSiteToGoogleLinks links = loadLinks(newLink.getSiteId());
		// Replace or add the new link
		boolean added = false;
		for (int linkIdx = 0; !added && (linkIdx < links.size()); linkIdx++) {
			TcSiteToGoogleLink link = links.getLink(linkIdx);
			// Check folder ID for match (site ID must be same)
			if (link.getFolderId().equals(newLink.getFolderId())) {
				// Already in the file: let's only continue if they differ
				if (newLink.equals(link)) {
					return;	// Quick return as there is no need to duplicate
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
			TcSiteToGoogleCache.getInstance().setLinksForSite(siteId, result);
		}
		return result;
	}

	public static File getStorageFile(String siteId) {
		return new File(getStorageFileName(siteId));
	}

	public static String getStorageFileName(String siteId) {
		return getStorageFolder() + siteId + ".lti.txt";
	}

	// TODO: File path needs to be retrieved from properties
	public static String getStorageFolder() {
		if (storageFolder == null) {
			storageFolder = System.getProperty(STORAGE_FOLDER_PROPERTY);
		}
		return "/Users/ranaseef/googleLtiDb/";
	}


	// Static private methods ---------------------------------------

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
					err.printStackTrace();
				}
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception err) {
					// TODO: Log the error
					err.printStackTrace();
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
		try {
			writer = new BufferedWriter(
					new FileWriter(getStorageFileName(siteId)));
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
