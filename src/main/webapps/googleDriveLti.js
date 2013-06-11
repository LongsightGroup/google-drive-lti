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
 * 		"user" : { "name" : "", "emailAddress" : "", "roles" : [ "", "" ]}
 *  ,	"folder" : { "title" : "" },
 *  ,	"course_id" : "",
 * !,	"rosterRequestUrl" : "",
 * !,	"ltiMembershipsId" : "",
 * !,	"oauthCallback" : "",
 * !,	"oauthConsumerKey" : "",
 * 
 *  }
 *
 * Items with '!' only exist for the instructor
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
var MAX_RESULTS_PER_PAGE = 100;
var GOOGLE_AUTHORIZE_URL = '/google-integration-prototype/googleLinks';
var FILTER_FOR_FOLDERS = 'mimeType = \'application/vnd.google-apps.folder\'';
var SELECTED_FOLDER_INPUT_NAME = 'folderSelectRadio';

var accessTokenHandler = {
		"accessToken" : null,
		"userEmail" : null
	};

$(document).ready(function() {
	getGoogleFolder();
});

function clearGoogleControls() {
	getDriveViewTopList().empty();
	getDriveFolderController().hide();
	getDriveFolderSelectionButtonsDiv().hide();
}

/**
 * Retrieves and displays folders linked with this site, with functions
 * allowing the user to interact with the Google resources.
 * 
 * The results of the query are handled by callback getGoogleFolderCallback().
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
function getGoogleFolder() {
	var googleDriveFolder = googleDriveConfig.folder;
	if (googleDriveFolder != null) {
		var query = 'fullText contains \'' + getConfigCourseId() + '\'';
		// Filter to only get a folder
		query = query + ' AND ' + FILTER_FOR_FOLDERS;
		queryDriveFilesNotTrashed(getGoogleAccessToken(), query, getGoogleFolderCallback);
	}
}

/**
 * Displays the given folders on the page, so the user can open them in another
 * window.
 * 
 * See getGoogleFolder() for description of this function's responsibilities.
 */
function getGoogleFolderCallback(data) {
	if (data && (typeof(data.items) !== 'undefined') && (data.items.length > 0)) {
		var files = data.items;
		for (var fileIdx in files) {
			var file = files[fileIdx];
			var $list = createFileListForFileFromTemplate(getDriveViewTopList(), file, false, true);
			getFoldersChildren(file, $list);
		}
	} else {
		if (getIsInstructor()) {
			$('#Welcome').text('Welcome Instructor ' + getUserName());
			var $folderController = getDriveFolderController();
			$folderController.show();
			listMyFolders();
		} else {
			$('#DriveFolderController').show();
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
 * @param $parentList		The <ul> on the page for the parent folder
 */
function getFoldersChildren(folder, $parentList) {
	var query = '\'' + folder.id + '\' in parents';
	queryDriveFilesNotTrashed(getGoogleAccessToken(), query, function(data) {
		getFoldersChildrenCallback(data, folder, $parentList);
	});
}

/**
 * This handles results for query run by getFoldersChildren(); see that function
 * for explanation.
 * 
 * @param data	Child files for the parent folder
 * @param parentFolder Google object for the parent folder 
 * @param $parentList The <ul> on the page for the parent folder
 */
function getFoldersChildrenCallback(data, parentFolder, $parentList) {
//	alert('Found ' + ((data == null) ? 0 : data.items.length) + ' children.');
	if ((data != null) && (typeof(data.items) !== 'undefined')) {
		var files = data.items;
		for (var fileIdx in files) {
			var file = files[fileIdx];
			var $list = createFileListForFileFromTemplate($parentList, file, false, false);
			// If folder, search for its children (recursively)
			if (file.mimeType === 'application/vnd.google-apps.folder') {
				getFoldersChildren(file, $list);
			}
		}
	}
}

/**
 * Gets folders I own and displays them on the screen, so I can select one as
 * the folder for this course, or as the parent for a new folder.
 */
function listMyFolders() {
	var query = '\'me\' in owners AND ' + FILTER_FOR_FOLDERS;
	queryDriveFilesNotTrashed(getGoogleAccessToken(), query, listMyFoldersCallback);
}

/**
 * When called as result of AJAX call, this displays the folders on the screen:
 * see listMyFolders() for details.
 */
function listMyFoldersCallback(data) {
	if (data && (typeof(data.items) !== 'undefined') && (data.items.length > 0))
	{
		// Show span that explains purpose of selecting an existing folder
		$('#MessageForFolderSelect').show();
		var files = data.items;
		for (var fileIdx in files) {
			var file = files[fileIdx];
			createFileListForFileFromTemplate(getDriveViewTopList(), file, true, false);
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
function assignSelectedFolder() {
	var folderId = getSelectedFolderId();
	if ($.trim(folderId) !== '') {
		saveCourseIdInFolder(folderId, getConfigCourseId(), false);
	}
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
 * 
 */
function assignNewFolder() {
	var parentFolderId = getSelectedFolderId();
	createFile(getGoogleAccessToken(),
			parentFolderId,
			getConfigFolderTitle(),
			getConfigCourseId(),
			'application/vnd.google-apps.folder',
			function(data) {
				notifyUserSiteLinkChangedWithFolder(data, true);
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
		clearGoogleControls();
		getGoogleFolder();
	} else {
		removeRosterPermissions(folderData);
		alert('Folder "'
				+ folderData.title + '" was unlinked from the site: '
				+ 'permissions were updated in Sakai, and are being removed in '
				+ 'Google Drive.');
		clearGoogleControls();
		getGoogleFolder();
	}
}

/**
 * @returns Value of selected folder: the value is the folder's Google ID
 */
function getSelectedFolderId() {
	return $('input[name="' + SELECTED_FOLDER_INPUT_NAME + '"]:checked').val();
}

function getConfigCourseId() {
	return googleDriveConfig.course_id;
}

function getConfigFolderTitle() {
	return googleDriveConfig.folder.title;
}

function getConfigRosterRequestUrl() {
	return googleDriveConfig.rosterRequestUrl;
}

function getConfigLtiMembershipsId() {
	return googleDriveConfig.ltiMembershipsId;
}

function getConfigOAuthCallback() {
	return googleDriveConfig.oauthCallback;
}

function getConfigOAuthCosumerKey() {
	return googleDriveConfig.oauthConsumerKey;
}

function getUserName() {
	return googleDriveConfig.user.name;
}

function getUserEmailAddress() {
	return googleDriveConfig.user.emailAddress;
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
function getGoogleAccessToken(responseType) {
	if ($.trim(accessTokenHandler.accessToken) === '') {
		// Get's email address from the form.
		userEmail = getUserEmailAddress();
		accessTokenHandler.accessToken = requestGoogleAccessToken(userEmail);
		if ($.trim(accessTokenHandler.accessToken) !== '') {
			accessTokenHandler.userEmail = userEmail;
		} else {
			accessTokenHandler.userEmail = '';
		}
	}
	return accessTokenHandler.accessToken;
}

/**
 * Removes permissions for people in the roster to the given folder.
 * Permissions for the instructor and owners of the folder are not affected.
 */
function requestGoogleAccessToken(userEmail) {
	var result = null;
	if ($.trim(userEmail) !== '') {
		result = $.ajax({
			url: '/google-drive-lti/service',
			async: false,
			type: 'GET',
			data: {
				"requested_action" : "getAccessToken",
				"user_email_address" : userEmail
			}
		}).responseText;
	} else {
		// Do nothing, as blank email address cannot be authorized
	}
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
			+ '&ext_ims_lis_memberships_url=' + escapeUrl(getConfigRosterRequestUrl())
			+ '&ext_ims_lis_memberships_id=' + escapeUrl(getConfigLtiMembershipsId())
			+ '&oauth_callback=' + escapeUrl(getConfigOAuthCallback())
			+ '&oauth_consumer_key=' + escapeUrl(getConfigOAuthCosumerKey())
			+ '&user_email_address=' + escapeUrl(getUserEmailAddress());
}

function getDriveFolderController() {
	return $('#DriveFolderController');
}

function getDriveFolderSelectionButtonsDiv() {
	return $('#DriveFolderSelectionButtonsDiv');
}

function getDriveViewTopList() {
	return $('#GoogleDriveTopList');
}

// Template of list for a file (folder): this is <li>...</li> to be inserted into an existing list.
var fileListTemplateFolderSelection = '<input name="' + SELECTED_FOLDER_INPUT_NAME + '" type="radio" value="[fileId]" onclick="enableButtonsActingOnSeectedFolder();"/>';
var fileListTemplateFolderUnassign = '<button type="button" onclick="unlinkFolderFromSite(\'[fileId]\', \'[fileTitle]\');">X</button>';
var fileListTemplate = '<li> \
		<ul class="[fileId]"> \
			<li class="[fileMimeTypeSuffix]">\
				[FolderSelection]\
				[IconImage]\
				<a href="#" onclick="handleFileClick(this, \'[fileMimeType]\', \'[fileId]\', \'[fileUrl]\'); return false; return false;">[fileTitle]</a>\
			</li> \
		</ul> \
	</li>';


/**
 * This is function called when a file retrieved from Google Drive file-list is
 * clicked, and currently opens a pop-up menu for the user to act upon.
 */
function handleFileClick(elTarget, fileMimeType, fileId, fileUrl) {
	// Show pop-up menu to select action to take
	var $popupMenu = getPopupFileMenuFor($(elTarget), fileMimeType);
	$popupMenu.data('fileElTarget', elTarget)
			.data('fileId', fileId)
			.data('fileMimeType', fileMimeType)
			.data('fileUrl', fileUrl);
	if (fileMimeType === 'application/vnd.google-apps.folder') {
		$popupMenu.data('parentFolderId', fileId);
	}
	$popupMenu.show();
}

/**
 * Returns the popup menu for this file positioned to show next to the file.
 *
 * TODO: verify position is visible on the screen, or find better fitting position.
 */
function getPopupFileMenuFor($elTarget, fileMimeType) {
	var $result = getPopupFileMenu(fileMimeType);
	placePopupMenuNearTargetElement($result, $elTarget);
	return $result;
}

function placePopupMenuNearTargetElement($popupMenu, $elTarget) {
	$popupMenu.css('top', $elTarget.offset().top - $(window).scrollTop())
			.css('left', $elTarget.offset().left + $elTarget.width() - $(window).scrollLeft() + 5);
}

/*  ---- BEGIN Functions for items in files' pop-up menus ----  */

function getJqueryEditPermissionsDialogRoot() {
	return $('#JqueryEditPermissionsDialogRoot');
}

function getPermissionsTbody($dialogRoot) {
	return $dialogRoot.find('.currentPermissionsTable > tbody');
}

/**
 * Simply closes the pop-up menu
 */
function popupCancel(me) {
	getParentPopupMenu($(me)).hide();
}

/**
 * Creates a new file with the given type, with the folder this menu was opened for as the file's parent.
 */
function popupCreateFile(me, fileType) {
	var $filePopupMenu = getParentPopupMenu($(me));
	var parentFolderId = $filePopupMenu.data('parentFolderId');
	var $parentList = $($filePopupMenu.data('fileElTarget')).parents('ul:first');
	openDialogToCreateFile($parentList, fileType, parentFolderId);
	$filePopupMenu.hide();
}

/**
 * Opens the file this menu was opened for to allow viewing/editing.  Parameter
 * inDialog true = opens the file in jQuery dialog; false = opens in new tab/window.
 */
function popupOpenFile(me, inDialog) {
	var $filePopupMenu = getParentPopupMenu($(me));
	var elTarget = $filePopupMenu.data('fileElTarget');
	openFileForElement(elTarget, $filePopupMenu.data('fileMimeType'), $filePopupMenu.data('fileUrl'), inDialog);
	$filePopupMenu.hide();
}

/**
 * Opens dialog showing the file's current permissions, and allowing
 * adding/changing/deleting the permissions.
 */
function popupEditPermissions(me) {
	var $filePopupMenu = getParentPopupMenu($(me));
	openDialogToEditPermissions($filePopupMenu.data('fileElTarget'));
	$filePopupMenu.hide();
}

function openDialogToCreateFile($parentList, fileType, parentFolderId) {
	var $dialogRoot = $('#JqueryCreateFileDialogRoot');
	$dialogRoot.find('input[name="fileMimeType"]').val('application/vnd.google-apps.' + fileType);
	$dialogRoot.find('input[name="parentFolderId"]').val(parentFolderId);
	$dialogRoot.find('input[name="title"]').val('New');
	$dialogRoot.find('input[name="description"]').val('');
	if (!$dialogRoot.is(':data(dialog)')) {
		$dialogRoot.dialog({
			bgiframe: false,
			autoOpen: false,
			modal: true,
			height: $(window).height() - 80,
			width: '90%',
			closeOnEscape: false,
			position: 'center',
			buttons: {
				"OK" : function() {
					if ($.trim($(this).find('input[name="title"]').val()) === '') {
						alert('Please enter title.');
					} else {
						createFile(getGoogleAccessToken(),
								$dialogRoot.find('input[name="parentFolderId"]').val(),
								$dialogRoot.find('input[name="title"]').val(),
								$dialogRoot.find('input[name="description"]').val(),
								$dialogRoot.find('input[name="fileMimeType"]').val(),
								function(data) {
									addFileToList(data, $parentList);
								});
						$(this).dialog('close');
					}
				},
				"Cancel" : function() {
					$(this).dialog('close');
				}
			}
		});
	}
	$dialogRoot.dialog('option', 'title', 'Create ' + fileType);
	$dialogRoot.dialog('open');
}

function addFileToList(data, $parentList) {
	if (typeof(data) !== 'undefined') {
		setTimeout(function() { alert('File "' + data.title + '" was created, and should be showing on the page.');}, 10);
		var $list = createFileListForFileFromTemplate($parentList, data, false, false);
	}
}

function openDialogToEditPermissions(elTarget) {
	var $elTarget = $(elTarget);
	var fileTitle = $elTarget.text();
	var fileId = $elTarget.parents('ul:first').attr('class');
	var $dialogRoot = getJqueryEditPermissionsDialogRoot();
	$dialogRoot.find('input[name="fileId"]').val(fileId);
	$dialogRoot.find('input[name="fileTitle"]').val(fileTitle);
	loadCurrentPermissions(fileId, fileTitle, getPermissionsTbody($dialogRoot));
	if (!$dialogRoot.is(':data(dialog)')) {
		$dialogRoot.dialog({
			bgiframe: false,
			autoOpen: false,
			modal: true,
			maxHeight: $(window).height() - 200,
			width: '90%',
			closeOnEscape: false,
			position: 'center',
			buttons: {
				"Close" : function() {
					$(this).dialog('close');
				}
			}
		});
	}
	$dialogRoot.dialog('option', 'title', 'Edit Permissions for ' + fileTitle);
	$dialogRoot.dialog('open');
}

var PermissionsTableRowTemplate = ' \
	<tr id="TrPid-[PermissionId]"> \
		<td> \
			<button type="button" onclick="savePermission(this, \'[FileId]\', \'[PermissionId]\', \'[Name]\');">Save</button> \
			<button type="button" onclick="deletePermission(this, \'[FileId]\', \'[PermissionId]\', \'[Name]\');">Delete</button> \
		</td> \
		<td> \
			[Name] \
		</td> \
		<td> \
			<select class="permissionRole"> \
				<option value="owner">Is owner</option> \
				<option value="writer">Can edit</option> \
				<option value="reader" selected="selected">Can view</option> \
			</select> \
		</td> \
	</tr> \
	';

function loadCurrentPermissions(fileId, fileTitle, $permissionsTbody) {
	var accessToken = getGoogleAccessToken();
	if (!verifyAllArgumentsNotEmpty(accessToken, fileId) || ($permissionsTbody.length === 0)) {
		return;	// Quick return to simplify code
	}
	// 1 - Clear existing entries
	$permissionsTbody.empty();
	// 2 - Query to load entries
	listCurrentFilePermissions(accessToken, fileId, fileTitle, function(data) {
		var items = data.items || new Array();
		for (var itemIdx in items) {
			var item = items[itemIdx];
			addPermissionToTbody(fileId, item, $permissionsTbody);
		}
	});
}

/**
 * Updates/Adds the permission to the table with current settings returned by Google Drive.
 */
function addPermissionToTbody(fileId, item, $permissionsTbody) {
	var $result = null;
	var name = item.name || 'Unknown (may be incorrect email address)';
	var tableRow = PermissionsTableRowTemplate.replace(/\[Name\]/gi, name)
		.replace(/\[FileId\]/gi, fileId)
		.replace(/\[PermissionId\]/gi, item.id);
	// NOTE: using $(new-template-string).appendTo(...) does not work, as jQuery throws an error
	var $existingRow = $('#TrPid-' + item.id);
	if ($existingRow.length === 0) {
		$permissionsTbody.append(tableRow);
	} else {
		$existingRow.replaceWith(tableRow);
	}
	$result = $('#TrPid-' + item.id);
	$result.find('.permissionRole option[value="' + item.role + '"]').attr('selected', 'selected');
	return $result;
}

/**
 * Deletes the given permission for a single file with the given fileId.
 */
function deletePermission(me, fileId, permissionId, name) {
	if (!confirm('Do you want to unlink this permission for ' + name + '?')) {
		return;	// Quick return to simplify code
	}
	var accessToken = getGoogleAccessToken();
	logToConsole('Deleting "' + permissionId + '" for file "' + fileId + '" and person "' + name + '"');
	deleteFilePermission(accessToken, fileId, permissionId,
			function(data) {
				logToConsole('Deleted permission as ' + role + ' for ' + name);
				alert('Permission was deleted.');
				$(me).parents('tr:first').remove();
			},
			function(jqXHR, textStatus, errorThrown) {
				logToConsole('Deleting permission failed: ' + textStatus + ' - ' + errorThrown + '\n\n----\n\n');
				alert('Deleting the permission failed.');
			},
			function(jqXHR, textStatus) {
				logToConsole('Deleted permission as ' + role + ' for ' + name + '\n\n----\n\n');
			});
}

/**
 * Saves a current permission's local changes
 */
function savePermission(me, fileId, permissionId, name) {
	logToConsole('Save "' + permissionId + '"');
	var accessToken = getGoogleAccessToken();
	var newRole = $(me).parents('tr:first').find('.permissionRole').val();
	updateFilePermission(accessToken, fileId, permissionId, newRole,
			function(data) {
				logToConsole('Changed permission for ' + name + ' to ' + role);
				alert('Permission change was saved.');
			},
			function(jqXHR, textStatus, errorThrown) {
				logToConsole('Changed permission failed: ' + textStatus + ' - ' + errorThrown + '\n\n----\n\n');
				alert('The update failed.');
			},
			function(jqXHR, textStatus) {
				logToConsole('Changed permission: ' + jqXHR.responseText + '\n\n----\n\n');
			});
}

function popupListChildrenOfFolder(me, fileStatus) {
	var $filePopupMenu = getParentPopupMenu($(me));
	var fileId = $($filePopupMenu.data('fileElTarget')).parents('ul:first').attr('class');
	$filePopupMenu.hide();
	listFiles(fileStatus, fileId);
}

/**
 * Opens the file belonging to the given element.  If file is a folder, opens in
 * new tab/window, as Google does not allow opening their Drive page in Iframe.
 * Other files are opened for editing in a jQuery dialog.
 */
function openFileForElement(elTarget, googleFileMimeType, sourceUrl, inDialog) {
	var $elTarget = $(elTarget);
	var title = $elTarget.text();
	openFile(title, sourceUrl, googleFileMimeType, inDialog);
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

	/*  ---- END Functions for items in files' pop-up menus ----  */

/**
 * Returns popup menu this <li> belongs to
 */
function getParentPopupMenu(me) {
	return $(me).parents('div.popupMenu:first');
}

/**
 * Returns the popup menu container for this type of file
 */
function getPopupFileMenu(fileMimeType) {
	if (fileMimeType === 'application/vnd.google-apps.folder') {
		return $('#FolderPopupMenu');
	} else {
		return $('#FilePopupMenu');
	}
}


function openFolder(sourceUrl) {
	window.open(sourceUrl, '_blank');
}

/**
 * Convenient pass-through function for createFileListFromTemplate(), setting
 * arguments from the given file object returned from Google request.
 */
function createFileListForFileFromTemplate($parentList, file, selection, root) {
	return createFileListFromTemplate($parentList, file.id, file.title, file.alternateLink, file.iconLink, file.mimeType, selection, root);
}

/**
 * This adds the given file to the list shown on the browser, allowing the user
 * to open the file in another tab/window by clicking on it.
 * 
 * This is used to show resources for use in the site.  When there are no
 * resources linked, it shows existing folders with radio buttons to either
 * [A] select folder to link with the site
 * OR
 * [B] select folder to as parent to new folder being created for the site
 * 
 * @param $parentList jQuery containing <ul> to add this resource to
 * @param fileId      Google's ID for the resource
 * @param fileTitle   Resource's title
 * @param fileUrl     Google URL to open the resource
 * @param iconLink    Google URL for the resource's icon
 * @param mimeType    The resource's MIME type (e.g., "application/vnd.google-apps.folder")
 * @param selection   true = add a radio button for use selection
 * @param root        This is root folder linked with the site
 * @returns jQuery containing the new element's top <ul>
 */
function createFileListFromTemplate($parentList, fileId, fileTitle, fileUrl, iconLink, mimeType, selection, root) {
	var result = null;
	fileUrl = fileUrl.replace(/"/g, '%22');
	fileTitle = fileTitle.replace(/"/g, '&quot;');
	var iconImage = '';
	if ($.trim(iconLink) !== '') {
		iconImage = '<img src="' + iconLink.replace(/"/g, '%22') + '"></img>';
	}
	var mimeTypeSuffix = '';
	if ($.trim(mimeType) !== '') {
		mimeTypeSuffix = 'google-type-' + /\.([^\.]+)$/g.exec(mimeType)[1];
	}
	var folderCommands = '';
	if (selection) {
		folderCommands = fileListTemplateFolderSelection
				.replace(/\[fileId\]/g, fileId);
	} else if (root) {
		if (getIsInstructor()) {
			folderCommands = fileListTemplateFolderUnassign
					.replace(/\[fileId\]/g, fileId);
		}
	}
	var newEntry = fileListTemplate
			.replace(/\[FolderSelection\]/g, folderCommands)
			.replace(/\[fileId\]/g, fileId)
			.replace(/\[fileTitle\]/g, fileTitle)
			.replace(/\[fileUrl\]/g, fileUrl)
			.replace(/\[IconImage\]/g, iconImage)
			.replace(/\[fileMimeType\]/g, mimeType)
			.replace(/\[fileMimeTypeSuffix\]/g, mimeTypeSuffix);
	/**
	 * Placing "My Drive" at top of ths list; this keeps the user's top folder,
	 * "My Drive", at the top of the structure, and may be strange if the user
	 * has folder named "My Drive"
	 */
	if (fileTitle === 'My Drive') {
		result = $(newEntry).prependTo($parentList).find('ul:first');
	} else {
		result = $(newEntry).appendTo($parentList).find('ul:first');
	}
	return result;
}

/**
 * Adds key to url for authentication.  Access token is not required for
 * file.alternateLink, but is for file.selfLink.  Best to not use token, and let
 * Google ensure security with user on the browser.
 */
function keySourceUrl(sourceUrl) {
	if (sourceUrl.indexOf('?') === -1) {
		sourceUrl = sourceUrl
				+ '?access_token=' + getGoogleAccessToken();
	} else {
		sourceUrl = sourceUrl
				+ '&access_token=' + getGoogleAccessToken();
	}
	return sourceUrl
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
