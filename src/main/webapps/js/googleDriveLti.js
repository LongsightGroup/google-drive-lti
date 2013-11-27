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
 * - <script> containing the JSON object defined in GoogleConfigJsonWriter.java </script>
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
	if (getHasConsoleLogFunction()) {
		console.log('WARNING: googleDriveLti.js relies upon sibling library google-drive-utils.js');
	}
}
if (typeof(verifyAllArgumentsNotEmpty) === 'undefined') {
	if (getHasConsoleLogFunction()) {
		console.log('WARNING: googleDriveLit.js relies upon sibling library utils.js');
	}
}

var DEBUG_MODE = false;
var SAVE_SITE_ID_IN_GOOGLE = false;
var SAVE_LINKS_IN_TP = true;
var FILTER_FOR_FOLDERS = 'mimeType = \'application/vnd.google-apps.folder\'';
var SELECTED_FOLDER_INPUT_NAME = 'folderSelectRadio';
//number of px is multiplied by the file's depth to pad the title
var FILE_DEPTH_PADDING_PX = 30;
//Array listing ID of each file's parents, keyed by the child file.  Finding
//all a file's ancestors can be done by finding file's parent, then finding
//entry for each parent's parent.
var googleFileParents = [];
var EXPAND_TEXT = '+ <span class="hide-text">Expand this folder</span>';
var SHRINK_TEXT = '- <span class="hide-text">Collapse this folder</span>';

var accessTokenHandler = {
		"accessToken" : null,
};

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
	var folders = getConfigLinkedFolders();
	if (typeof(folders) !== 'undefined') {
		for (var idx = 0, count = folders.length; idx < count; idx++) {
			showLinkedGoogleFolder(folders[idx]);
		}
	}
}

/**
 * Shows the linked folder with the given ID.
 * 
 * NOTE: this code needed to be split out from caller function, so folderId is
 * correct for errorCallback (otherwise, may end up with folderId of the last
 * folder being requested).
 * 
 * @param folderId
 */
function showLinkedGoogleFolder(folderId) {
	getDriveFile(
			getGoogleAccessToken(),
			folderId,
			function(data) {
				// Linked folders are all depth 0 (no parents)
				showLinkedGoogleFolderCallback(data, 0);
			},
			function(data, textStatus, jqXHR) {
				// Handle error...
				if (data.status === 404) {
					giveCurrentUserReadOnlyPermissions(folderId)
				}
			});

}

/**
 * Displays the given folder on the page, so the user can open it in another
 * window, and runs query for getting its children.
 * 
 * See showLinkedGoogleFolders() for description of this function's responsibilities.
 */
function showLinkedGoogleFolderCallback(file, depth) {
	if ((typeof(file) === 'object') && (file !== null) && (typeof(file.id) === 'string')) {
		if (!findFileInFileTreeTable(file.id)) {
			addFileToFileTreeTable(file, null, file.id, depth);
			getFoldersChildren(file, file.id, depth);
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
		var files = data.items;
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
 * Looks up ancestors for all the linked folders.  If the googleFileParents
 * array already has keys for folders, it will not request them again.  To
 * refresh, the caller will need to remove those keys from the array.
 */
function getAncestorsForLinkedFolders() {
	var folders = getConfigLinkedFolders();
	if (typeof(folders) !== 'undefined') {
		for (var idx = 0, count = folders.length; idx < count; idx++) {
			var linkedFolderId = folders[idx];
			listAncestorsForFileRecursive(linkedFolderId, linkedFolderId, false);
		}
	}
}

/**
 * 
 * 
 * @param linkedFolderId
 * @param currentFolderId
 * @param rootParent true = the parent folder is known to be root, so no need to make call for its parents
 */
function listAncestorsForFileRecursive(currentFileId, parentFileId, rootParent) {
	// Handle results of last AJAX call (equal ID is request to start looking for parents, so this will make 1st AJAX call)
	if (currentFileId !== parentFileId) {
		var ancestors = null;
		if (!(currentFileId in googleFileParents)) {
			googleFileParents[currentFileId] = [];
		}
		googleFileParents[currentFileId].push(parentFileId);
	}
	// "Recursive" - make call to get parent folder's parents
	// Skip finding grand-parents if they were already retrieved
	if (!rootParent && !(parentFileId in googleFileParents)) {
		listDriveFileParents(getGoogleAccessToken(), parentFileId, function(data) {
			// Find each parent folder's grand-parents
			for (var grandParentIdx = 0, grandParentCount = data.items.length; grandParentIdx < grandParentCount; grandParentIdx++) {
				var grandParent = data.items[grandParentIdx];
				listAncestorsForFileRecursive(parentFileId, grandParent.id, grandParent.isRoot);
			}
		});
	}
}

/**
 * Performs Google search for folders, retrieving all of them if the search
 * value is blank.  When it succeeds, it replaces contents in the table with the
 * results.
 * 
 * Not using local filtering to find all matching Google folders (using "paging"
 * means the page does not have all the matching folders).
 */
function searchUnlinkedFoldersOwnedByMe() {
	var searchValue = $('#UnlinkedFolderSearchInput').val();
	var query = '\'me\' in owners AND ' + FILTER_FOR_FOLDERS;
	if ($.trim(searchValue) !== '') {
		query = query + " AND title contains '" + escapeSingleQuotes(searchValue) + "'";
	}
	queryDriveFilesNotTrashed(getGoogleAccessToken(), query,
			function(data) {
		emptyUnlinkedFoldersTable();
		searchUnlinkedFoldersOwnedByMeCallback(data, getConfigCourseId());
	});
}


/**
 * When called as result of AJAX call, this displays the folders on the screen:
 * see searchUnlinkedFoldersOwnedByMe() for details.
 */
function searchUnlinkedFoldersOwnedByMeCallback(data, courseId) {
	if (data && (typeof(data.items) !== 'undefined') && (data.items.length > 0))
	{
		var files = sortFilesByTitle(data.items);
		for (var fileIdx in files) {
			var file = files[fileIdx];
			if (!getIsFolderLinked(file)) {
				// Get folder's ancestors (specifying root=false, as Google response does not indicate).
				listAncestorsForFileRecursive(file.id, file.id, false);
				// Add to the table
				addFolderToLinkFolderTable(file);
			}
		}

	}
}

/**
 * Removes all folders currently in the table for unlinked folders.
 */
function emptyUnlinkedFoldersTable() {
	$('#LinkFolderTableTbody').empty();
}



/**
 * Returns true if the folder is linked to the site.
 * 
 * @param folder	Google folder
 * @returns {Boolean}
 */
function getIsFolderLinked(folder) {
	var result = false;
	var linked = getConfigLinkedFolders();
	if (linked && linked.length > 0) {
		for (var linkedIdx = 0, count = linked.length; !result && (linkedIdx < count); linkedIdx++) {
			if (linked[linkedIdx] === folder.id) {
				result = true;
			}
		}
	}
	return result;
}

/**
 * 
 */
function unlinkFolderFromSite(folderId, folderTitle) {
	if ($.trim(folderId) !== '') {
		unlinkFolderToSite(folderId, function() {
			notifyUserSiteLinkChangedWithFolder(folderId, folderTitle, false, true);
		});
	}
}

function deleteGoogleFile(fileId, fileTitle, fileMimeType) {
	var msg = deleteFileFolderCopy + fileTitle + undoneCopy;
	var isFolder = getIsFolder(fileMimeType);
	if (isFolder) {
		msg = deleteFileFolderCopy + fileTitle + deleteUndoneFolderCopy;
	}
	if (confirm(msg)) {
		deleteDriveFile(getGoogleAccessToken(), fileId, function() {
			// This is deleted, not unlinked, but the result is essentially the
			// same on the page (nobody can see it any longer)
			removeFileTreeFromTable(fileId);
		});
	}
}

/**
 * Creates new folder with "My Drive" as its parent. 
 */
function assignNewFolder() {
	var folderTitle = prompt(createFolderCopy,getConfigCourseTitle());
	if ($.trim(folderTitle) === '') {
		return;	// Quick return to simplify code
	}
	// This would be the place to avoid duplicate existing file names.

	createFile(getGoogleAccessToken(),
			'',
			folderTitle,
			getConfigCourseId(),
			'application/vnd.google-apps.folder',
			function(data) {
		var folderId = '';
		var folderTitle = '';
		if ((typeof(data) !== 'undefined') && ($.trim(data.id) !== '')) {
			folderId = data.id;
			folderTitle = data.title
		}
		linkFolderToSite(folderId, function() {
			notifyUserSiteLinkChangedWithFolder(folderId, folderTitle, true, false);
		});
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
function notifyUserSiteLinkChangedWithFolder(folderId, folderTitle, newFolder, unlinked) {
	if ($.trim(folderId) === '') {
		// Do nothing, as the response does not show association succeeded
		return;	// Quick return to simpify code
	}

	if (!unlinked) {
		var sendNotificationEmails = confirm(sendEmailCopy);
		giveRosterReadOnlyPermissions(folderId, sendNotificationEmails);
		removeLinkedFolderFromLinkingTable(folderId);

	} else {
		removeRosterPermissions(folderId);
		removeUnlinkedFileTreeFromTable(folderId);
	}
}

function getConfigCourseId() {
	return googleDriveConfig.course.id;
}

function getConfigCourseTitle() {
	return googleDriveConfig.course.title;
}

function getConfigTpId() {
	return googleDriveConfig.tp_id;
}

function getConfigLinkedFolders() {
	return googleDriveConfig.linkedFolders;
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
		url: getPageUrl(),
		async: false,
		type: 'GET',
		data: {
			"requested_action" : "getAccessToken",
			"tp_id" : getConfigTpId()
		}
	}).responseText;
	return result;
}


function checkBackButtonHit(){
	$.ajax({
		url:getPageUrl(),
		type:'GET',
		cache:false,
		data:getUpdateLtiParams(
				"",
				"checkBackButton",
				false),
				success:function(data){
					if ($.trim(data) === 'SUCCESS') {
						openPage('Home');
					}

				}
	});
}

/**
 * Link the given Google Folder to the site
 */
function linkFolderToSite(folderId, callback) {
	$.ajax({
		url: getPageUrl(),
		type: 'GET',
		dataType: 'json',
		data: getUpdateLtiParams(
				folderId,
				"linkGoogleFolder",
				false),
				success: function(data) {
					if ((typeof(data) === 'object') && (data !== null)) {
						if ($.trim(data.tp_id) !== '') {
							googleDriveConfig = data;
						}
						callback(data);
					} else if (typeof(data) === 'string') {
						alert(data);
					}
				}
	});
}

/**
 * Unlink the given Google Folder from the site
 */
function unlinkFolderToSite(folderId, callback) {
	$.ajax({
		url: getPageUrl(),
		type: 'GET',
		data: getUpdateLtiParams(
				folderId,
				"unlinkGoogleFolder",
				false),
				dataType: 'json',
				success: function(data) {
					if (typeof(data) === 'object' && (data !== null)) {
						if ($.trim(data.tp_id) !== '') {
							googleDriveConfig = data;
						}
						callback(data);
					} else if (typeof(data) === 'string') {
						alert(data);
					}
				}
	});
}

/**
 * Sends request to TP to give people in the roster read-only access to the
 * given folder (people with higher permissions are not affected by this call).
 */
function giveRosterReadOnlyPermissions(folderId, sendNotificationEmails) {
	$.ajax({
		url: getPageUrl(),
		type: 'GET',
		data: getUpdateLtiParams(
				folderId,
				"giveRosterAccessReadOnly",
				sendNotificationEmails),
				success: function(data) {
					if ($.trim(data) == 'SUCCESS') {
						openPage('Home');
					}else if ($.trim(data) !== '') {
						alert(data);
					}

				}
	});
}

/**
 * Gives access to the currently logged in user.  This is called when request to
 * get linked folder fails due to 404 error; that may occur when student is
 * added to the roster after the folder has been linked.
 * 
 * @param folderId Google folder's ID
 */
function giveCurrentUserReadOnlyPermissions(folderId) {
	$.ajax({
		url: getPageUrl(),
		type: 'GET',
		data: getUpdateLtiParams(
				folderId,
				"giveCurrentUserAccessReadOnly",
				false),
				success: function(data) {
					if ($.trim(data) === 'SUCCESS') {
						getDriveFile(
								getGoogleAccessToken(),
								folderId,
								function(data) {
									// Linked folders are all depth 0 (no parents)
									showLinkedGoogleFoldersCallback(data, 0);
								});
					}
				}
	});
}

/**
 * Removes permissions for people in the roster to the given folder.
 * Permissions for the instructor and owners of the folder are not affected.
 */
function removeRosterPermissions(folderId) {
	$.ajax({
		url: getPageUrl(),
		type: 'GET',
		data: getUpdateLtiParams(
				folderId,
				"removeRosterAccess",
				false),
				success: function(data) {
					if ($.trim(data) === 'SUCCESS') {
						openPage('LinkFolder');
					}else if ($.trim(data) !== '') {
						alert(data);
					}
					
				}
	});
}

/**
 * Returns URL parameters (without leading "?" or "&") sent to the TLI Producer
 * to modify rosters' permissions with the given file.  
 * 
 * @param folderId ID of folder to act upon
 * @param requestedAction action to take (can be "giveRosterAccessReadOnly")
 * @param sendNotificationEmails Boolean indicate if server will email people of
 * changes to their permissions.
 * @returns {String}
 */
function getUpdateLtiParams(folderId, requestedAction, sendNotificationEmails) {
	return 'access_token=' + getGoogleAccessToken()
	+ '&requested_action=' + requestedAction
	+ '&send_notification_emails=' + sendNotificationEmails
	+ '&file_id=' + escapeUrl(folderId)
	+ '&tp_id=' + escapeUrl(getConfigTpId());
}

function openDialogToCreateFile(fileType, parentFolderId, linkedFolderId, depth)
{
	var title = prompt(createFileCopy + '' + fileType, '');
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
function openPage(pageName) {
	var url = getPageUrl()
	+ '?requested_action=openPage'
	+ '&pageName=' + escapeUrl(pageName)
	+ '&tp_id=' + escapeUrl(getConfigTpId())
	// Adding timestamp to ensure request is sent & result is not cached
	+ "&_=" + new Date().getTime();
	document.location.href = url;
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
	<td><a class="itemLink" onclick="[OpenFileCall]" title="[FolderTitle]"> \
	<img src="[GoogleIconLink]" width="16" height="16" alt="Folder">&nbsp;[FolderTitle] \
	</a></td> \
	<td> \
	<a class="btn btn-small" onclick="linkFolder(\'[FolderIdOnclickParam]\', \'[FolderTitleOnclickParam]\');">'+linkFolderButton+'</a> \
	</td> \
	</tr>';

/**
 * Adds the given Google folder to table row, allowing the user to link it to
 * the site.
 * 
 * @param folder Google folder available for linking
 */
function addFolderToLinkFolderTable(folder) {
	// Only add the row if row for same file DNE
	if ($('#' + getLinkingTableRowIdForFolder(folder.id)).length > 0) {
		return;
	}
	var newEntry = LINK_FOLDER_TABLE_ROW_TEMPLATE
	.replace(/\[TrFolderId\]/g, escapeSingleQuotes(getLinkingTableRowIdForFolder(folder.id)))
	.replace(/\[FolderIdOnclickParam\]/g, escapeAllQuotes(folder.id))
	.replace(/\[FolderTitle\]/g, folder.title)
	.replace(/\[FolderTitleOnclickParam\]/g, escapeAllQuotes(folder.title))
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
 * Links the existing folder to the site.
 * 
 * @param folderId
 */
function linkFolder(folderId, folderTitle) {
	var folderRelationship = getFileRelationshipWithLinkedFolders(folderId);
	if (folderRelationship !== null) {
		getDriveFile(getGoogleAccessToken(), folderRelationship.linked.id, function(data) {
			if ((typeof(data) === 'object') && (typeof(data.title) !== 'undefined')) {
				notifyUserFolderCannotBeLinked(folderTitle, 'this is ' + folderRelationship.type + ' of linked folder ' + data.title + '.');
			} else {
				notifyUserFolderCannotBeLinked(folderTitle, 'this is ' + folderRelationship.type + ' of a linked folder.');
			}
		},
		function() {
			// Error getting linked folder...
			notifyUserFolderCannotBeLinked(folderTitle, 'this is ' + folderRelationship.type + ' of a linked folder.');
		})
	} else 
	{
		linkFolderToSite(folderId, function() {
			notifyUserSiteLinkChangedWithFolder(folderId, folderTitle, false, false);
		});
	}
}

/**
 * 
 * @param reason
 */
function notifyUserFolderCannotBeLinked(folderTitle, reason) {
	alert('Unable to link folder ' + folderTitle + ': ' + reason);
}

/**
 * Returns object, with the following format, specifying the given file is
 * ancestor or descendant of a linked folder; null if there is no relationship.
 * 
 * Returns the first relationship found, and what relationship is returned may
 * be inconsistent if multiple such relationships exist with this file.
 * 
 * result.me.id     (file's ID)
 * result.linked.id (linked folder's ID)
 * result.type      (I am "ascendant" or "descendant" of linked)
 * 
 * @param fileId Google ID of file to check with linked folders
 * @returns {Object} As desribed above; null if there are no such relationships
 */
function getFileRelationshipWithLinkedFolders(fileId) {
	var result = null;
	var linkedFolders = getConfigLinkedFolders();
	if (typeof(linkedFolders) !== 'undefined') {
		for (var idx = 0, count = linkedFolders.length; (result === null) && (idx < count); idx++) {
			var linkedFolderId = linkedFolders[idx];
			if (getLatterIsAncestor(fileId, linkedFolderId)) {
				result = {};
				result.me = {};
				result.me.id = fileId;
				result.type = 'descendant';
				result.linked = {};
				result.linked.id = linkedFolderId;
			} else if (getLatterIsAncestor(linkedFolderId, fileId)) {
				result = {};
				result.me = {};
				result.me.id = fileId;
				result.type = 'ascendant';
				result.linked = {};
				result.linked.id = linkedFolderId;
			}
		}
	}
	return result;
}

/**
 * Performs recursive check of given file's parents, and the parent's parents,
 * until the ancestor is found or all parents have been checked.  It is possible
 * the same grand-parents will be checked multiple times (e.g., if a file has 2
 * parents with same grand-parent).
 * 
 * @param childFileId File to check on its behalf or that of its child
 * @param ancestorFolderId File being checked as ancestor to this child
 * @returns {Boolean}
 */
function getLatterIsAncestor(childFileId, ancestorFolderId) {
	var result = false;
	if (childFileId in googleFileParents) {
		// Check child's parents
		var parentFolders = googleFileParents[childFileId];
		for (var parentIdx = 0, count = parentFolders.length; !result && (parentIdx < count); parentIdx++) {
			var parentFolderId = parentFolders[parentIdx];
			result = (parentFolderId === ancestorFolderId);
		}
		// Recursive check parent's grand-parents
		if (!result) {
			for (var parentIdx = 0, count = parentFolders.length; !result && (parentIdx < count); parentIdx++) {
				var parentFolderId = parentFolders[parentIdx];
				result = getLatterIsAncestor(parentFolderId, ancestorFolderId);
			}
		}
	}
	return result;
}

var FILE_TREE_TABLE_ROW_TEMPLATE = '<tr id="[FileId]" class="[ClassSpecifyParentAndDepth] [LinkedFolderId]"> \
	<td style="[FileIndentCss]">[ExpandShrink]<a class="itemLink" onclick="[OpenFileCall]" title="[FileTitle]"> \
	<img src="[GoogleIconLink]" width="16" height="16" alt="Folder">&nbsp;<span class="title">[FileTitle]</span> \
	</a></td> \
	<td>&nbsp;[DropdownTemplate]</td> \
	<td>&nbsp;[ActionTemplate]</td> \
	<td>[LastModified]</td> \
	</tr>';

var ACTION_BUTTON_TEMPLATE = '<a class="btn btn-small" onclick="[ActionOnClick]">[ActionTitle]</a>';

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
	var isFolder = (getIsFolder(file.mimeType));
	var expandShrinkOption = '<a href="#">&nbsp;</a>';
	if (isFolder) {
		expandShrinkOption = '<a href="#" class="expandShrink" onclick="toggleExpandShrink(\'' + file.id + '\');">&nbsp;</a>';
	}
	// Add text to parent folder for expanding/shrinking functionality
	if ($.trim(parentFolderId) !== '') {
		$('#' + getTableRowIdForFile(parentFolderId)).find('a.expandShrink:not(.shrinkable)').addClass('shrinkable').html(SHRINK_TEXT);
	}
	if (getIsInstructor() && isFolder) {
		dropdownTemplate = $('#FolderDropdownTemplate').html();
		dropdownTemplate = dropdownTemplate
		.replace(/\[FolderIdParam\]/g, escapeAllQuotes(file.id))
		.replace(/\[LinkedFolderIdParam\]/g, escapeAllQuotes(linkedFolderId))
		.replace(/\[FolderDepthParam\]/g, treeDepth);
	}
	var actionTitle = null;
	var actionOnClick = '';
	if (getIsInstructor() && isFolder && (treeDepth === 0)) {
		actionTitle = unlinkFolderButton;
		actionOnClick = 'unlinkFolderFromSite(\'' + escapeAllQuotes(file.id) + '\', \'' + escapeAllQuotes(file.title) + '\');';
	} else {
		if (getIsInstructor()) {
			actionTitle = deleteFolderButton;
			actionOnClick = 'deleteGoogleFile(\'' + escapeAllQuotes(file.id) + '\', \'' + escapeAllQuotes(file.title) + '\', \'' + escapeAllQuotes(file.mimeType) + '\');';
		}
	}
	var actionTemplate = '';
	if (actionTitle !== null) {
		actionTemplate = ACTION_BUTTON_TEMPLATE
		.replace(/\[ActionTitle\]/g, actionTitle)
		.replace(/\[ActionOnClick\]/g, actionOnClick);
	}
	// Using trim so null/undefined id is empty string ('')
	var childOfParentId = getClassForFoldersChildren(parentFolderId);
	var newEntry = FILE_TREE_TABLE_ROW_TEMPLATE
	.replace(/\[FileId\]/g, getTableRowIdForFile(file.id))
	.replace(/\[ClassSpecifyParentAndDepth\]/g, childOfParentId)
	.replace(/\[LinkedFolderId\]/g, getClassForLinkedFolder(linkedFolderId))
	.replace(/\[FileTitle\]/g, file.title)
	.replace(/\[DropdownTemplate\]/g, dropdownTemplate)
	.replace(/\[ActionTemplate\]/g, actionTemplate)
	.replace(/\[GoogleIconLink\]/g, file.iconLink)
	.replace(/\[ExpandShrink\]/g, expandShrinkOption)
	.replace(/\[FileIndentCss\]/g, fileIndentCss)
	.replace(/\[OpenFileCall\]/g, getFunctionCallToOpenFile(file))
	.replace(/\[LastModified\]/g, getFileLastModified(file));
	if (parentFolderId) {
		var $parentRow = $('#' + getTableRowIdForFile(parentFolderId));
		if ($parentRow.length > 0) {
			// There are children: add this one in sorted order
			// NOTE: this may still fail to sort all if there are siblings being
			// added in multiple threads, as is the case with linked folders
			var $parentChildren = $parentRow.siblings('.' + childOfParentId);
			if (!addFileInOrderWithSiblings(newEntry, file.title, $parentChildren)) {
				$parentRow.after(newEntry);
			}
		} else {
			logToConsole('No parent ' + parentFolderId + ' in table for file ' + file.id);
		}
	} else {
		if (!addFileInOrderWithSiblings(newEntry, file.title, $('#FileTreeTableTbody tr'))) {
			$(newEntry).appendTo('#FileTreeTableTbody');
		}
	}
}

/**
 * This is called when user clicks on icon to expand or shrink the folder.  It
 * marks the folder's span with class 'shrunk' and showing text to indicate the
 * change can be reverted.  Then this calls recursive function
 * expandOrShrinkChildren() to update its children.
 * 
 * @param folderId
 */
function toggleExpandShrink(folderId) {
	var $expandShrinkSpan = $('#' + getTableRowIdForFile(folderId)).find('a.expandShrink.shrinkable');
	var expand = false;
	if ($expandShrinkSpan.hasClass('shrunk')) {
		$expandShrinkSpan.removeClass('shrunk');
		expand = true;
		$expandShrinkSpan.html(SHRINK_TEXT);
		expandOrShrinkChildren(folderId, false);
	} else {
		$expandShrinkSpan.addClass('shrunk');
		$expandShrinkSpan.html(EXPAND_TEXT);
		expandOrShrinkChildren(folderId, true);
	}
}

/**
 * Shows/Hides the children rows for the current folder, recursively closing
 * their children (call is made for non-folders: that should have no effect
 * since those will not have children).
 * 
 * @param folderId
 * @param shrinking
 */
function expandOrShrinkChildren(folderId, shrinking) {
	$('tr.' + getClassForFoldersChildren(folderId)).each(function() {
		var $this = $(this);
		if ($this.find('span.shrunk').length === 0) {
			expandOrShrinkChildren(getFileIdFromTableRowId($this.attr('id')), shrinking);
		}
		if (shrinking) {
			$this.fadeOut();
		} else {
			$this.fadeIn();
		}
	});
}

/**
 * Adds the new entry sorted by its title with the given siblings.  If there are
 * no siblings, it will not be added.
 * 
 * @param newEntry  New <tr> for the file
 * @param fileTitle The file's title
 * @param $siblings Array of siblings to sort the new entry with
 * @return added true = added; false = not added
 */
function addFileInOrderWithSiblings(newEntry, fileTitle, $siblings) {
	var result = false;
	var newTitle = fileTitle.toLowerCase();
	for (var idx = 0, count = $siblings.length; !result && (idx < count); idx++) {
		var $sibling = $($siblings[idx]);
		var siblingTitle = $.trim($sibling.find('span.title').text()).toLowerCase();
		if (siblingTitle > newTitle) {
			$sibling.before(newEntry);
			result = true;
		} else if (idx === count - 1) {
			// Last sibling, enter at the end
			$sibling.after(newEntry);
			result = true;
		}
	}
	return result;
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
 * Recursive walk through the table to delete the given file and all of its
 * descendants.
 * 
 * @param fileId
 */
function removeFileTreeFromTable(fileId) {
	$('#FileTreeTableTbody').find('tr.' + getClassForFoldersChildren(fileId)).each(
			function() {
				removeFileTreeFromTable(getFileIdFromTableRowId(this.id));
			});
	$('#' + getTableRowIdForFile(fileId)).remove();
}

/**
 * Removes the unlinked folder and all its descendants from the table.  Simple
 * operation as every trow in the entire linked folder's tree share the same
 * class.
 */
function removeUnlinkedFileTreeFromTable(unlinkedFolderId) {
	$('#FileTreeTableTbody').find('tr.' + getClassForLinkedFolder(unlinkedFolderId)).remove();
}

/**
 * Returns the class to be put into each <tr> showing a file that belongs to the
 * linked folder with the given ID.
 */
function getClassForLinkedFolder(linkedFolderId) {
	return 'linkedGoogleFolder' + linkedFolderId;
}

/**
 * Returns the class put into <tr> for each of the given folder's children (not
 * their grandchildren).
 * 
 * @param parentFolderId
 * @returns {String}
 */
function getClassForFoldersChildren(parentFolderId) {
	return 'child-of-' + $.trim(parentFolderId);
}

/**
 * Returns ID of the <tr> for the file with the given ID.
 */
function getTableRowIdForFile(fileId) {
	return 'FileTreeTableTrGoogleFile' + fileId;
}

function getFileIdFromTableRowId(tableRowId) {
	var match = /FileTreeTableTrGoogleFile(.*)$/.exec(tableRowId);
	if (match) {
		return match[1];
	} else {
		return null;
	}
}

/**
 * Returns ID of the linking table's <tr> for the folder with the given ID
 */
function getLinkingTableRowIdForFolder(folderId) {
	return 'LinkedFolderTrGoogleFolder' + folderId;
}

/*
 show spinner whenever async actvity takes place
 */
$(document).ready(function(){
	$(document).ajaxStart(function(){
		$('#spinner').show();
	});
	$(document).ajaxStop(function(){
		$('#spinner').hide();
	});
})

