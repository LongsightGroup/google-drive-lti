package edu.umich.its.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletRequest;

/**
 * Static utility with some methods for helping developer's walk through data in
 * requests.
 *
 * @author Raymond Naseef
 *
 **/
public class ServletRequestUtil {
	// Constants -----------------------------------------------------

	private static final MapKeySorter MAP_KEY_SORTER = new MapKeySorter();


	// Static public methods -----------------------------------------

	public static String showParametersInTable(
			ServletRequest request,
			String tableAttributes)
	{
		// Begin Table
		StringBuilder result = new StringBuilder("<table ");
		if (tableAttributes != null) {
			result.append(tableAttributes);
		}
		result.append(">");
		// Enter Table Header
		result.append(
				"<thead><tr><th>Parameter</th><th>Values</th></tr></thead>");
		// Begin Table Body
		result.append("<tbody>");
		// Enter Parameters
		addParameterRows(result, request);
		// End Table Body
		result.append("</tbody>");
		// End Table
		result.append("</table>");
		return result.toString();
	}


	// Static private methods ----------------------------------------

	@SuppressWarnings("unchecked")
	private static void addParameterRows(
			StringBuilder result,
			ServletRequest request)
	{
		Set<Map.Entry<String, String[]>> paramsSet =
			new TreeSet<Entry<String, String[]>>(MAP_KEY_SORTER);
		paramsSet.addAll(request.getParameterMap().entrySet());
		for (Map.Entry<String, String[]> paramEntry : paramsSet) {
			result.append("<tr><th>")
					.append(paramEntry.getKey())
					.append("</th>");
			result.append("<td>")
					.append(getValuesHtml(paramEntry.getValue()))
					.append("</td></tr>");
		}
	}

	private static String getValuesHtml(String[] values) {
		if ((values == null) || (values.length == 0)) {
			return "";
		} else if (values.length == 1) {
			return values[0];
		}
		StringBuilder result = new StringBuilder("<ul>");
		for (String value : values) {
			result.append("<li>")
					// Removed encoding as this project uses commons-lang-2.6.jar
					.append(value)
					.append("</li>");
		}
		return result.toString();
	}


	// Constructors --------------------------------------------------

	private ServletRequestUtil() {
	}


	// Inner classes ------------------------------------------------

	/**
	 * Compares map entries to case-insensitive sort by their keys, with null
	 * at the end.
	 */
	private static class MapKeySorter
	implements Comparator<Entry<String, String[]>>
	{
		public int compare(Entry<String, String[]> a, Entry<String, String[]> b)
		{
			if (a == null) {
				return (b == null) ? 0 : 1;
			} else if (b == null) {
				return -1;
			} else {
				String keya = a.getKey();
				String keyb = b.getKey();
				if (keya == null) {
					return (keyb == null) ? 0 : 1;
				} else if (keyb == null) {
					return -1;
				} else {
					return keya.toLowerCase().compareTo(keyb.toLowerCase());
				}
			}
		}
	}
}
