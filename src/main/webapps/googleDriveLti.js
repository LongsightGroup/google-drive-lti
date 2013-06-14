/* JavaScript library for working with Google Drive.  This library is intended
 * for use in Basic LTI, and includes code written in other JavaScript library,
 * google-drive.js in sibling project google-integration-prototype here in
 * google-sandbox.
 *
 * What is written as of May 15, 2013:
 * A - Function to lookup and show a Google Folder by its title
 * B - Opens the folder into another window/tab when clicked on
 *
 * It relies upon the following Libraries:
 * - <script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js" type="text/javascript"></script>
 * - <script src="/google-integration-prototype/google-integration-prototype.js" type="text/javascript"></script>
 * - <link href="/google-integration-prototype/google-integration-prototype.css" rel="stylesheet" type="text/css">
 * - <script> containing the following JSON object:
 * 	googleDriveConfig = {
 * 		"tp_id" : ""
 * 		"user" : { "name" : "", "roles" : [ "", "" ]}
 *  ,	"folder" : { "title" : "" },
 *  ,	"course_id" : "",
 * 
 *  }
 *
 * Requests to LTI need to include "tp_id" as parameter, so the server can
 * verify the request.
 *
 * ISSUES:
 * 
 *  A - Google files are only added to display once each, avoiding duplicate
 *      entries in the display.  This can create inconsistent view, as the
 *      winning location depends on timing of AJAX calls retrieving file tree
 *      from Google.  This happens as each folder runs a async query to get its
 *      direct children, so many threads may be running at the same time.
 *
 * @author: Raymond Louis Naseef
 */
if (typeof(verifyAllArgumentsNotEmpty) === 'undefined') {
	if ((typeof(console) !== 'undefined') && (typeof(console.log) === 'function')) {
		console.log('WARNING: googleDriveLti.js relies upon sibling library google-drive-utils.js');
	}
}
if (typeof(verifyAllArgumentsNotEmpty) === 'undefined') {
	if ((typeof(console) !== 'undefined') && (typeof(console.log) === 'function')) {
		console.log('WARNING: google-drive-utils.js relies upon sibling library utils.js');
	}
}

var DEBUG_MODE = true;
var GOOGLE_AUTHORIZE_URL = '/google-integration-prototype/googleLinks';
var FILTER_FOR_FOLDERS = 'mimeType = \'application/vnd.google-apps.folder\'';
var SELECTED_FOLDER_INPUT_NAME = 'folderSelectRadio';
// number of px is multiplied by the file's depth to pad the title
var FILE_DEPTH_PADDING_PX = 20;

var accessTokenHandler = {
		"accessToken" : null,
	};

function clearGoogleControls() {
	getDriveViewTopList().empty();
	getDriveFolderController().hide();
	getDriveFolderSelectionButtonsDiv().hide();
}

/**
 * Retrieves and displays folders linked with this site, with functions
 * allowing the user to interact with the Google resources.
 * 
 * The results of the query are handled by callback showGoogleFoldersCallback().
 * If there are no folders linked with the site, the callback handles that
 * based on the user's position in the site:
 *   For non-instructors:
 * - The page is left blank.
 * 
 *   For instructors:
 * - The page allows the instructor to link an existing folder with the
 * site.
 * - The page allows the instructor to create a new folder, linked with the
 * site, and as child of "My Drive" or any other folder they own.
 */
function showLinkedGoogleFolders() {
	var googleDriveFolder = googleDriveConfig.folder;
	if (googleDriveFolder != null) {
		var query = 'fullText contains \'' + getConfigCourseId() + '\'';
		// Filter to only get a folder
		query = query + ' AND ' + FILTER_FOR_FOLDERS;
		queryDriveFilesNotTrashed(
				getGoogleAccessToken(),
				query,
				function(data) {
					// Linked folders are all depth 0 (no parents)
					showLinkedGoogleFoldersCallback(data, 0);
				});
	}
}

/**
 * Displays the given folders on the page, so the user can open them in another
 * window.
 * 
 * See showLinkedGoogleFolders() for description of this function's responsibilities.
 */
function showLinkedGoogleFoldersCallback(data, depth) {
	if (data && (typeof(data.items) !== 'undefined') && (data.items.length > 0)) {
		var files = sortFilesByTitle(data.items);
		for (var fileIdx in files) {
			var file = files[fileIdx];
			if (!findFileInFileTreeTable(file.id)) {
				addFileToFileTreeTable(file, null, file.id, depth);
				getFoldersChildren(file, file.id, depth);
			}
		}
	}
}

/**
 * This finds any direct children of the given folder, and that I have the right
 * to view, and displays them on the page.  This is recursive, and will find the
 * tree of files I have access to in the folder tree.  It will not find
 * a folder's grand-children if I do not have read access to its sub-folders,
 * even if I have rights to see the grand-children.
 * 
 * @param folder	The parent folder to search
 * @param linkedFolderId ID of the linked folder this file belongs to
 * @param $parentList		The <ul> on the page for the parent folder
 */
function getFoldersChildren(folder, linkedFolderId, depth) {
	var query = '\'' + folder.id + '\' in parents';
	queryDriveFilesNotTrashed(getGoogleAccessToken(), query, function(data) {
		getFoldersChildrenCallback(data, folder, linkedFolderId, depth);
	});
}

/**
 * This handles results for query run by getFoldersChildren(); see that function
 * for explanation.
 * 
 * @param data	Child files for the parent folder
 * @param parentFolder Google object for the parent folder
 * @param linkedFolderId ID of the linked folder this file belongs to
 * @param parentDepth # of parents from this file's parent folder to linked folder
 */
function getFoldersChildrenCallback(data, parentFolder, linkedFolderId, parentDepth) {
	var childDepth = parentDepth + 1;
	if ((data != null) && (typeof(data.items) !== 'undefined')) {
		var files = sortFilesByTitle(data.items);
		for (var fileIdx in files) {
			var file = files[fileIdx];
			if (!findFileInFileTreeTable(file.id)) {
				addFileToFileTreeTable(file, parentFolder.id, linkedFolderId, childDepth);
				// If folder, search for its children (recursively)
				if (file.mimeType === 'application/vnd.google-apps.folder') {
					getFoldersChildren(file, linkedFolderId, childDepth);
				}
			}
		}
	}
}

/**
 * Gets folders I own and displays them on the screen, so I can select one as
 * the folder for this course, or as the parent for a new folder.
 */
function listUnlinkedFoldersOwnedByMe() {
	var query = '\'me\' in owners AND ' + FILTER_FOR_FOLDERS;
// This was not working, I check folders on the callback
//			+ ' AND NOT fullText contains \'' + getConfigCourseId() + '\'';
	queryDriveFilesNotTrashed(getGoogleAccessToken(), query,
			function(data) {
				listUnlinkedFoldersOwnedByMeCallback(data, getConfigCourseId());
			});
}

/**
 * When called as result of AJAX call, this displays the folders on the screen:
 * see listUnlinkedFoldersOwnedByMe() for details.
 */
function listUnlinkedFoldersOwnedByMeCallback(data, courseId) {
	if (data && (typeof(data.items) !== 'undefined') && (data.items.length > 0))
	{
		// Show span that explains purpose of selecting an existing folder
		$('#MessageForFolderSelect').show();
		var files = sortFilesByTitle(data.items);
		for (var fileIdx in files) {
			var file = files[fileIdx];
			// Filter out folders that are already linked
			if ((typeof(file.description) != 'undefined') && (file.description.indexOf(courseId) == -1)) {
				addFolderToLinkFolderTable(file);
			}
		}
	} else {
		$('#MessageForFolderNoParents').show();
	}
	getDriveFolderSelectionButtonsDiv().show();
}

function getButtonsActingOnSelectedFolder() {
	return $('button.actingOnSelectedFolder');
}

function enableButtonsActingOnSeectedFolder() {
	getButtonsActingOnSelectedFolder().prop('disabled', false);
}

/**
 * Unchecks all folders, to clear the selection.
 */
function clearFolderSelection() {
	$('input[name="' + SELECTED_FOLDER_INPUT_NAME + '"]:checked').prop('checked', false);
	getButtonsActingOnSelectedFolder().prop('disabled', true);
}

/**
 * 
 */
function unlinkFolderFromSite(folderId, folderTitle) {
	if ($.trim(folderId) !== '') {
		if (confirm('Please confirm unlinking folder "' + folderTitle + ' from the course.'))
		{
			saveCourseIdInFolder(folderId, getConfigCourseId(), true);
		}
	}
}

/**
 * This gets the file's description and adds or removes the course's ID to it.
 * 
 * @param folderId Google ID for the folder
 * @param courseId Sakai ID for the site
 * @param unlink  false = link; true = unlink
 */
function saveCourseIdInFolder(folderId, courseId, unlink) {
	var accessToken = getGoogleAccessToken();
	if (!verifyAllArgumentsNotEmpty(accessToken, folderId, courseId)) {
		return;	// Quick return to simplify code
	}
	getDriveFile(accessToken, folderId, function(folder) {
		saveCourseIdInFolderCallback(accessToken, folder, courseId, unlink);
	});
}

/**
 * Callback for saveCourseIdInFolder(), completes its operation with the given
 * Google object representing the folder to be updated.
 * 
 * @param folder Google Folder
 */
function saveCourseIdInFolderCallback(accessToken, folder, courseId, unlink) {
	var description = $.trim(folder.description);
	if (!unlink) {
		// Linking this folder to the site
		if (description === '') {
			description = courseId;
		} else {
			description = description + ' ' + courseId;
		}
	} else {
		// Unlinking this folder from the site
		var re = new RegExp(courseId, 'g');
		description = description.replace(re, '');
	}
	var requestData = '{ \
		"description" : "' + escapeJson(description) + '" \
	}';
	putDriveFileChanges(accessToken, folder.id, requestData, function(data) {
			notifyUserSiteLinkChangedWithFolder(data, false, unlink);
		});
}

/**
 * Creates new folder with "My Drive" as its parent. 
 */
function assignNewFolder() {
	createFile(getGoogleAccessToken(),
			'',
			getConfigFolderTitle(),
			getConfigCourseId(),
			'application/vnd.google-apps.folder',
			function(data) {
				notifyUserSiteLinkChangedWithFolder(data, true, false);
			});
}

/**
 * Notifies the user the folder has been associated with the given folder, and
 * gives permissions to people in the roster if the user wants that done.
 * 
 * @param folderData
 * @param newFolder
 * @param unlinked  true = folder was unlinked from site; false = folder was
 * linked with the site
 */
function notifyUserSiteLinkChangedWithFolder(folderData, newFolder, unlinked) {
	if (typeof(folderData) === 'undefined') {
		// Do nothing, as the response does not show association succeeded
		return;	// Quick return to simpify code
	}
	if (!unlinked) {
		var sendNotificationEmails = confirm('Site linked to folder "'
				+ folderData.title
				+ '", and will give the roster access.  '
				+ 'Send email to notify people about their new permissions?');
		giveRosterReadOnlyPermissions(folderData, sendNotificationEmails);
		removeLinkedFolderFromLinkingTable(folderData.id);
	} else {
		removeRosterPermissions(folderData);
		removeUnlinkedFileTreeFromTable(folderData.id);
		alert('Folder "'
				+ folderData.title + '" was unlinked from the site: '
				+ 'permissions were updated in Sakai, and are being removed in '
				+ 'Google Drive.');
	}
}

function getConfigCourseId() {
	return googleDriveConfig.course_id;
}

function getConfigFolderTitle() {
	return googleDriveConfig.folder.title;
}

function getConfigTpId() {
	return googleDriveConfig.tp_id;
}

function getUserName() {
	return googleDriveConfig.user.name;
}

function getIsInstructor() {
	var result = false;
	var roles = googleDriveConfig.user.roles;
	var roleCount = roles.length;
	for (var roleIdx = 0; !result && (roleIdx < roleCount); roleIdx++) {
		if (roles[roleIdx] === 'Instructor') {
			result = true;
		}
	}
	return result;
}

/**
 * Returns access token, retrieved from GoogleLinks server if the token is null.
 */
function getGoogleAccessToken() {
	if ($.trim(accessTokenHandler.accessToken) === '') {
		accessTokenHandler.accessToken = requestGoogleAccessToken();
	}
	return accessTokenHandler.accessToken;
}

/**
 * Removes permissions for people in the roster to the given folder.
 * Permissions for the instructor and owners of the folder are not affected.
 */
function requestGoogleAccessToken() {
	var result = null;
	result = $.ajax({
		url: '/google-drive-lti/service',
		async: false,
		type: 'GET',
		data: {
			"requested_action" : "getAccessToken",
			"tp_id" : getConfigTpId()
		}
	}).responseText;
	return result;
}

/**
 * Sends request to TP to give people in the roster read-only access to the
 * given folder (people with higher permissions are not affected by this call).
 */
function giveRosterReadOnlyPermissions(folderData, sendNotificationEmails) {
	$.ajax({
		url: '/google-drive-lti/service',
		type: 'GET',
		data: getUpdateRosterParams(
				folderData,
				"giveRosterAccessReadOnly",
				sendNotificationEmails),
		success: function(data) {
			alert(data);
		}
	});
}

/**
 * Removes permissions for people in the roster to the given folder.
 * Permissions for the instructor and owners of the folder are not affected.
 */
function removeRosterPermissions(folderData) {
	$.ajax({
		url: '/google-drive-lti/service',
		type: 'GET',
		data: getUpdateRosterParams(
				folderData,
				"removeRosterAccess",
				false),
		success: function(data) {
			alert(data);
		}
	});
}

/**
 * Returns URL parameters (without leading "?" or "&") sent to the TLI Producer
 * to modify rosters' permissions with the given file.  
 * 
 * @param folderData Google folder the request will act upon
 * @param requestedAction action to take (can be "giveRosterAccessReadOnly")
 * @param sendNotificationEmails Boolean indicate if server will email people of
 * changes to their permissions.
 * @returns {String}
 */
function getUpdateRosterParams(folderData, requestedAction, sendNotificationEmails) {
	return 'access_token=' + getGoogleAccessToken()
			+ '&requested_action=' + requestedAction
			+ '&send_notification_emails=' + sendNotificationEmails
			+ '&file_id=' + escapeUrl(folderData.id)
			+ '&tp_id=' + escapeUrl(getConfigTpId());
}

function openDialogToCreateFile(fileType, parentFolderId, linkedFolderId, depth)
{
	var title = prompt('Please enter title for the new ' + fileType, '');
	if ($.trim(title) === '') {
		return;	// Quick return to simplify code
	}
	createFile(getGoogleAccessToken(),
			parentFolderId,
			title,
			'',
			'application/vnd.google-apps.' + fileType,
			function(file) {
				addFileToFileTreeTable(file, parentFolderId, linkedFolderId, depth + 1);
			});
}

function openFile(title, sourceUrl, googleFileMimeType, inDialog) {
	// Folders do not open in iframe, due to SAMEORIGIN rule: open in new tab/window
	if (!inDialog) {
		window.open(sourceUrl, '_blank');
		return;
	}
	var $dialogRoot = $('#JqueryDialogRoot');
	if (!$dialogRoot.is(':data(dialog)')) {
		$dialogRoot.dialog({
			bgiframe: false,
			autoOpen: false,
			modal: true,
			height: '90%',
			width: '90%',
			closeOnEscape: false,
			position: 'center',
			close: function() {
					$('#DialogIframe').remove();
			}
		});
	}
	$dialogRoot.dialog('option', 'title', title);
	$dialogRoot.dialog('open');
	create_iframe($dialogRoot.attr('id'), 'DialogIframe', 'dialogIframe', sourceUrl);
}

function openFolder(sourceUrl) {
	window.open(sourceUrl, '_blank');
}

function create_iframe(parentId, iframeId, iframeName, sourceUrl) {
	return $('<iframe class="dialog"></iframe>')
			.appendTo('#' + parentId)
			.attr('id', iframeId)
			.attr('name', iframeName)
			.attr('src', sourceUrl);
}

function logToConsole() {
	// Doing nothing now: main purpose is to allow code copied here to function
	// without modifying the code
}

/**
 * Sets the page's URL to one with query asking for a particular page.  Doing
 * this while async() AJAX calls are in operation may cause AJAX calls and/or
 * this function to fail.
 * 
 * @param page
 */
function openPage(page) {
	document.location.href = getPageUrl()
			+ '?requested_action=' + page
			+ '&tp_id=' + escapeUrl(getConfigTpId());
}

/**
 * Returns the page's URL with query trimmed off.
 */
function getPageUrl() {
	var queryIdx = document.location.href.indexOf('?');
	if (queryIdx > 0) {
		return document.location.href.substr(0, queryIdx);
	} else {
		return document.location.href;
	}
}

var LINK_FOLDER_TABLE_ROW_TEMPLATE = '<tr id="[TrFolderId]"> \
	<td><a onclick="[OpenFileCall]"> \
	  <img src="[GoogleIconLink]" width="16" height="16" alt="Folder">&nbsp;[FolderTitle] \
	</a></td> \
    <td> \
      <a class="btn btn-primary btn-small" onclick="linkFolder(\'[FolderId]\');">Link Folder</a> \
    </td> \
	</tr>';

/**
 * Adds the given Google folder to table row, allowing the user to link it to
 * the site.
 * 
 * @param folder Google folder available for linking
 */
function addFolderToLinkFolderTable(folder) {
	var newEntry = LINK_FOLDER_TABLE_ROW_TEMPLATE
			.replace(/\[TrFolderId\]/g, escapeSingleQuotes(getLinkingTableRowIdForFolder(folder.id)))
			.replace(/\[FolderId\]/g, escapeSingleQuotes(folder.id))
			.replace(/\[FolderTitle\]/g, folder.title)
			.replace(/\[GoogleIconLink\]/g, folder.iconLink)
			.replace(/\[OpenFileCall\]/g, getFunctionCallToOpenFile(folder));
	$(newEntry).appendTo('#LinkFolderTableTbody');
}

/**
 * Returns JavaScript to caller that can be used (usually during onclick) to
 * open the given Google file.
 */
function getFunctionCallToOpenFile(file) {
	return "openFile("
			+ "'" + escapeSingleQuotes(file.title)
			+ "', '" + escapeSingleQuotes(file.alternateLink)
			+ "', '" + escapeSingleQuotes(file.mimeType)
			+ "');";
}

/**
 * Links the given folder to the site.
 * 
 * @param folderId
 */
function linkFolder(folderId) {
	saveCourseIdInFolder(folderId, getConfigCourseId(), false);
}

var FILE_TREE_TABLE_ROW_TEMPLATE = '<tr id="[FileId]" class="[ClassSpecifyParentAndDepth] [LinkedFolderId]"> \
	<td><a style="[FileIndentCss]" onclick="[OpenFileCall]"> \
		<img src="[GoogleIconLink]" width="16" height="16" alt="Folder">&nbsp;[FileTitle] \
	</a></td> \
	<td>[DropdownTemplate]</td> \
	<td>[ActionTemplate]</td> \
	<td>[LastModified]</td> \
	</tr>';

var ACTION_BUTTON_TEMPLATE = '<a class="btn btn-primary btn-small" onclick="[ActionOnClick]">[ActionTitle]</a>';

/**
 * Displays the given Google file on the screen, allowing the user to click on
 * the file to open it.  It also gives instructor options to unlink a linked
 * folder or to create subfolder or documents in the folder.
 * 
 * @param file Google file being added to the table
 * @param parentFolderId ID of the folder this file is child of
 * @param linkedFolderId ID of the linked folder this file belongs to; null when
 * this file is the linked folder
 * @param treeDepth	Number of parents of this file including the linked folder
 */
function addFileToFileTreeTable(file, parentFolderId, linkedFolderId, treeDepth)
{
	var fileIndentCss = '';
	if (treeDepth > 0) {
		fileIndentCss = 'padding-left: ' + (treeDepth * FILE_DEPTH_PADDING_PX) + 'px;';
	}
	var dropdownTemplate = '';
	var isFolder = (file.mimeType === 'application/vnd.google-apps.folder');
	if (getIsInstructor() && isFolder) {
		dropdownTemplate = $('#FolderDropdownTemplate').html();
		dropdownTemplate = dropdownTemplate
				.replace(/\[FolderId\]/g, escapeSingleQuotes(file.id))
				.replace(/\[LinkedFolderId\]/g, escapeSingleQuotes(linkedFolderId))
				.replace(/\[FolderDepth\]/g, treeDepth);
	}
	var actionTitle = null;
	var actionOnClick = '';
	if (getIsInstructor() && isFolder && (treeDepth === 0)) {
		actionTitle = 'Unlink';
		actionOnClick = 'unlinkFolderFromSite(\'' + escapeSingleQuotes(file.id) + '\', \'' + escapeSingleQuotes(file.title) + '\');';
	} else {
	}
	var actionTemplate = '';
	if (actionTitle !== null) {
		actionTemplate = ACTION_BUTTON_TEMPLATE
				.replace(/\[ActionTitle\]/g, actionTitle)
				.replace(/\[ActionOnClick\]/g, actionOnClick);
	}
	// Using trim so null/undefined id is empty string ('')
	var childOfParentId = 'child-of-' + $.trim(parentFolderId);
	var newEntry = FILE_TREE_TABLE_ROW_TEMPLATE
			.replace(/\[FileId\]/g, getTableRowIdForFile(file.id))
			.replace(/\[ClassSpecifyParentAndDepth\]/g, childOfParentId)
			.replace(/\[LinkedFolderId\]/g, getClassForLinkedFolder(linkedFolderId))
			.replace(/\[FileTitle\]/g, file.title)
			.replace(/\[DropdownTemplate\]/g, dropdownTemplate)
			.replace(/\[ActionTemplate\]/g, actionTemplate)
			.replace(/\[GoogleIconLink\]/g, file.iconLink)
			.replace(/\[FileIndentCss\]/g, fileIndentCss)
			.replace(/\[OpenFileCall\]/g, getFunctionCallToOpenFile(file))
			.replace(/\[LastModified\]/g, getFileLastModified(file));
	if (parentFolderId) {
		var $parentRow = $('#' + getTableRowIdForFile(parentFolderId));
		if ($parentRow.length > 0) {
			var $parentLastChild = $parentRow.siblings('.' + childOfParentId + ':last');
			if ($parentLastChild.length === 1) {
				$parentLastChild.after(newEntry);
			} else {
				$parentRow.after(newEntry);
			}
		}
	} else {
		$(newEntry).appendTo('#FileTreeTableTbody');
	}
}

/**
 * 
 * @param file
 * @returns {String}
 */
function getFileLastModified(file) {
	var result = '';
	if (file) {
		result = getGoogleDateOrTime(file.modifiedDate)
				+ ' <span class="modified_by">'
				+ file.lastModifyingUserName
				+ "</span>";	
	}
	return result;
}

var MONTHS = [
              'January',
              'February',
              'March',
              'April',
              'May',
              'June',
              'July',
              'August',
              'September',
              'October',
              'November',
              'December'
		];

/**
 * Returns given date/time as time of day for today, "May 5" if today is not
 * "May 5".
 * 
 * @param googleDateIso
 * @returns {String}
 */
function getGoogleDateOrTime(googleDateIso) {
	var result = '';
	if (googleDateIso) {
		try {
			var googleDate = Date.fromISOString(googleDateIso);
			var todayStart = clearTime(new Date());
			// Getting next midnight by adding 1 day in milliseconds, ignoring leap
			// second
			var todayEndTimeMs = todayStart.getTime() + ((86400000) - 10);
			if ((googleDate.getTime() >= todayStart.getTime())
					&& (googleDate.getTime() <= todayEndTimeMs))
			{
				// Today, show hh:mm AM/PM
				var hour = googleDate.getHours();
				var min = googleDate.getMinutes();
				var am_pm = (hour >= 12) ? "pm" : "am";
				if (hour > 12) {
					hour = hour - 12;
				} else if (hour === 0) {
					hour = 12;
				}
				result = hour + ':' + padNumber(min, 2) + ' ' + am_pm;
			} else {
				// Not today, show "Month ##"
				result = MONTHS[googleDate.getMonth() + 1]
						+ ' '
						+ googleDate.getDate();
			}
		} catch (err) {
			logToConsole(err);
		}
	}
	return result;
}

/**
 * Returns true if table row for exists in the table for file with the given ID.
 * 
 * NOTE: this is being used to ensure the same folder is not included in the
 * table multiple times, to keep display simple & to avoid duplicating "unique"
 * table-row IDs.  If people really want folder to show 2+ times, it may be best
 * to add rows that act as links where clicking on that row scrolls to show the
 * folder's tree in the table.
 * 
 * @param fileId Id of Google File
 * @returns {Boolean} true = table row exists
 */
function findFileInFileTreeTable(fileId) {
	return $('#' + getTableRowIdForFile(fileId)).length > 0;
}

/**
 * Removes the newly linked folder from the linking table, so it does not look
 * still unlinked.
 */
function removeLinkedFolderFromLinkingTable(linkedFolderId) {
	$('#' + getLinkingTableRowIdForFolder(linkedFolderId)).remove();
}

/**
 * Removes the unlinked folder and all its descendants from the table.
 */
function removeUnlinkedFileTreeFromTable(unlinkedFolderId) {
	$('#FileTreeTableTbody').find('.' + getClassForLinkedFolder(unlinkedFolderId)).remove();
	$('#' + getTableRowIdForFile(unlinkedFolderId)).remove();
}

/**
 * Returns the class to be put into each <tr> showing a file that belongs to the
 * linked folder with the given ID.
 */
function getClassForLinkedFolder(linkedFolderId) {
	return 'linkedGoogleFolder' + linkedFolderId;
}

/**
 * Returns ID of the <tr> for the file with the given ID.
 */
function getTableRowIdForFile(fileId) {
	return 'FileTreeTableTrGoogleFile' + fileId;
}

/**
 * Returns ID of the linking table's <tr> for the folder with the given ID
 */
function getLinkingTableRowIdForFolder(folderId) {
	return 'LinkedFolderTrGoogleFolder' + folderId;
}
