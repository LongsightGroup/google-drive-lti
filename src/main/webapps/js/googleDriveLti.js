/**
 * JavaScript library for working with Google Drive.  This library is intended
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
 * 
 * @author lsloan@umich.edu (Lance E Sloan)
 */

if (typeof(jQuery) === 'undefined') {
	console.log('ERROR: googleDriveLti.js requires jQuery');
}

if (typeof(bootbox) === 'undefined') {
	console.log('ERROR: googleDriveLti.js requires bootbox');
}

if (typeof(getDriveFile) === 'undefined') {
	if (getHasConsoleLogFunction()) {
		console.log('ERROR: googleDriveLti.js requires google-drive-utils.js');
	}
}

if (typeof(verifyAllArgumentsNotEmpty) === 'undefined') {
	if (getHasConsoleLogFunction()) {
		console.log('ERROR: googleDriveLti.js requires utils.js');
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
var EXPAND_TEXT = '+ <span class="sr-only">Expand this folder</span>';
var SHRINK_TEXT = '- <span class="sr-only">Collapse this folder</span>';

var accessTokenHandler = {
		"accessToken" : null
};

bootbox.setDefaults({
	closeButton : false
});

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
		showLinkedGoogleFolder(folders[0]);
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
function showLinkedGoogleFolder(folderId, foldersOnly) {	
	foldersOnly = (foldersOnly === true);
	
	getDriveFile(
		getGoogleAccessToken(),
		folderId,
		function(data) {
			// Linked folders are all depth 0 (no parents)
			if(!data.labels.trashed){
				showLinkedGoogleFolderCallback(data, 0, foldersOnly);
			}
			else{
				handleUnlinkingFolder(folderId);
			}
		},
		function(data, textStatus, jqXHR) {
			if (data.status === 404) {
				checkSharedFolderDeletionStatus(folderId);
			}
		}
	);
}

var fileTree = null;

/**
 * Displays the given folder on the page, so the user can open it in another
 * window, and runs query for getting its children.
 * 
 * See showLinkedGoogleFolders() for description of this function's responsibilities.
 */
function showLinkedGoogleFolderCallback(file, depth, foldersOnly) {
	foldersOnly = (foldersOnly === true);

	if ((typeof(file) === 'object') && (file !== null) && (typeof(file.id) === 'string')) {
		if (!findFileInFileTreeTable(file.id)) {
			addFileToFileTreeTable(file, null, file.id, depth, foldersOnly);
		}

		getFoldersChildren(file.id, file.id, depth, foldersOnly);
	}
}

/**
 * This finds any direct children of the given parent folder ID and displays
 * them on the page. If "foldersOnly" is set to `true`, the list of children is
 * restricted to folders owned by the current user. This is recursive, and will
 * find the tree of files to which the user has access in the folder tree. It
 * will not find a folder's grandchildren if the user doesn't have read access
 * to its subfolders, even if the user has rights to see the grandchildren.
 * 
 * @param parentFolderId
 *            String ID of the parent folder to search
 * @param linkedFolderId
 *            String ID of the linked folder this file belongs to
 * @param depth
 *            Integer number of indentation levels
 * @param foldersOnly
 *            Boolean indicating whether only folders should be queried
 */
function getFoldersChildren(parentFolderId, linkedFolderId, indentationDepth, foldersOnly) {
	foldersOnly = (foldersOnly === true);

	var queryFolderId = parentFolderId;

	if (queryFolderId == null) {
		queryFolderId = 'root';
	}

	var query = "'" + queryFolderId + "' in parents";

	if (foldersOnly) {
		query += " and mimeType = 'application/vnd.google-apps.folder' and 'me' in owners";
	}

	queryDriveFilesNotTrashed(getGoogleAccessToken(), query, function(data) {
		var childIndentationDepth = indentationDepth;

		if (parentFolderId != null) {
			childIndentationDepth++;
		}

		if ((data != null) && (typeof (data.items) !== 'undefined')) {
			$.each(data.items, function(key, file) {
				if (!findFileInFileTreeTable(file.id)) {
					addFileToFileTreeTable(file, parentFolderId,
							linkedFolderId, childIndentationDepth, foldersOnly);
				}

				// If folder, search for its children (recursively)
				if (file.mimeType === 'application/vnd.google-apps.folder') {
					getFoldersChildren(file.id, linkedFolderId, childIndentationDepth,
							foldersOnly);
				}
			});
		}
	});
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

function AutoClick(e) {
	var carriageReturnKeyCode = 13;

	if (e.keyCode == carriageReturnKeyCode) {
		searchItems();
	}
}

/**
 * Apply a highlight color to table rows that match the search field. Also
 * expand all collapsed ancestors and mark them to be displayed as part of the
 * search results.
 */
function searchItems() {
	var searchValue = $('#UnlinkedFolderSearchInput').val().trim();

	// The Bootstrap error class gives a good highlight color for table rows
	var searchResultClass = 'error';

	// Remove highlight from any previous matches
	$('tbody#FileTreeTableTbody tr').removeClass(
			searchResultClass).removeClass('searchResult').removeClass(
			'searchNonResult');

	if (searchValue !== '') {
		var matchedItems = $('tbody#FileTreeTableTbody tr').has(
				'td a span.title:contains("' + searchValue + '")');

		if (matchedItems) {
			matchedItems.addClass(searchResultClass).addClass('searchResult');

			matchedItems.each(function() {
				expandAndIncludeParent($(this));
			});

			$('tbody#FileTreeTableTbody tr').not('.searchResult').addClass('searchNonResult');
		}
	}
}

function expandAndIncludeParent(item) {
	var itemClasses = item.attr('class');

	if (itemClasses) {
		$.each(itemClasses.split(/\s+/), function(index, itemClass) {
			if (itemClass.search('child-of-') == 0) {
				var parentId = itemClass.substring(9);

				expand(parentId);

				var parentNode = $('tbody#FileTreeTableTbody tr#'
						+ getTableRowIdForFile(parentId));
				if (parentNode) {
					parentNode.addClass('searchResult');
					expandAndIncludeParent(parentNode);
				} else {
					return;
				}
			}
		});
	}
}

/**
 * When called as result of AJAX call, this displays the folders on the screen:
 * see searchItems() for details.
 */
function searchItemsCallback(data, courseId) {
	if (data && (typeof(data.items) !== 'undefined') && (data.items.length > 0))
	{
		var folders = sortFilesByTitle(data.items);
		for (var folderIndex in folders) {
			var folder = folders[folderIndex];
			if (!getIsFolderLinked(folder)) {
				// Get folder's ancestors (specifying root=false, as Google response does not indicate).
				listAncestorsForFileRecursive(folder.id, folder.id, false);
				// Add to the table
				addFolderToLinkFolderTable(folder, null, 0);
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

function deleteGoogleFile(fileId, fileTitle, fileMimeType, role) {
	var isFolder = getIsFolder(fileMimeType);
	var itemType = getItemTypeFromMimeType(fileMimeType);

	if (role === "writer") {
		$.ajax({
			url : getPageUrl(),
			type : 'GET',
			data : getUpdateLtiParams(fileId, "getOwnerToken", false),
			success : function(ownerToken) {
				var deleteMsg = null;
				if ($.trim(ownerToken) == 'ERROR') {
					if (isFolder) {
						deleteMsg = sprintf(deleteFolderErrorAlert,
								escapeHtml(fileTitle));
					} else {
						deleteMsg = sprintf(deleteFileErrorAlert,
								escapeHtml(fileTitle));
					}
					bootbox.alert({
						  message: deleteMsg,
						  buttons: {
						    ok: {
						      label: buttonOk,
						    }
						  }
						});
				} else {
					var deleteConfirmationMessage = null;
					if (isFolder) {
						deleteConfirmationMessage = sprintf(
								deleteFolderPrompt, escapeHtml(fileTitle));
					} else {
						deleteConfirmationMessage = sprintf(deleteFilePrompt, itemType,
								escapeHtml(fileTitle));
					}
					bootbox.confirm({
						title: sprintf(deleteItemPromptHeader, itemType),
						message : deleteConfirmationMessage,
						buttons : {
							confirm : {
								label : buttonDelete
							}
						},
						callback : function(userConfirmed) {
							if (userConfirmed === true) {
								deleteDriveFile(ownerToken, fileId, function() {
									removeFileTreeFromTable(fileId);
									showInfo($('#alertContainer'), sprintf(deleteItemAlert, itemType, escapeHtml(fileTitle)));
								});
							}
						}
					});
				}
			}
		})
	} else if (role === "owner") {
		var deleteConfirmationMessage = null;
		if (isFolder) {
			deleteConfirmationMessage = sprintf(deleteFolderPrompt,
					escapeHtml(fileTitle));
		} else {
			deleteConfirmationMessage = sprintf(deleteFilePrompt, itemType,
					escapeHtml(fileTitle));
		}

		bootbox.confirm({
			title: sprintf(deleteItemPromptHeader, itemType),
			message : deleteConfirmationMessage,
			buttons : {
				confirm : {
					label : buttonDelete
				}
			},
			callback : function(userConfirmed) {
				if (userConfirmed === true) {
					deleteDriveFile(getGoogleAccessToken(), fileId, function() {
						removeFileTreeFromTable(fileId);
						showInfo($('#alertContainer'), sprintf(deleteItemAlert, itemType, escapeHtml(fileTitle)));
					});
				}
			}
		});
	}
}

/**
 * Prompt user for an item title. If an empty string is entered, prompt again.
 * 
 * @param itemType
 *            'folder', 'document', 'drawing', etc.
 * @param defaultTitle
 *            default value for title shown in dialog
 * @returns non-empty title string or null if user selected "Cancel"
 */
function itemTitlePromptDialog(itemType, defaultTitle, validResponseCallback) {
	var defaultItemPrompt = sprintf(createItemPrompt, itemType.toLowerCase());
	var itemPrompt = defaultItemPrompt;
	var itemTitle = defaultTitle;

	if (defaultTitle === null) {
		itemTitle = '';
	}

	var displayPrompt = function() {
		var promptDialog = bootbox.prompt({
			inputType: 'text',
			title : sprintf(createItemPromptHeader, itemType),
			value : itemTitle,
			buttons : {
				confirm : {
					label : buttonCreate
				}
			},
			callback : function(itemTitle) {
				if (itemTitle !== null) {
					itemTitle = $.trim(itemTitle);

					if (itemTitle === '') {
						// Empty responses are invalid. Prompt again
						// with error message.
						itemPrompt = defaultItemPrompt + '<br/><em>'
								+ createItemPromptError + '</em>';
						displayPrompt();
						return;
					}
				}

				if (typeof (validResponseCallback) === 'function') {
					validResponseCallback(itemTitle);
				}
			}
		});

		// Bootbox doesn't support the "message" property for prompt dialogs, so
		// wedge it in.
		$('form.bootbox-form', promptDialog).prepend($('<label>', {
			html : itemPrompt
		}));
	};

	displayPrompt();
}

/**
 * Creates new folder with "My Drive" as its parent.
 */
function assignNewFolder() {
	itemTitlePromptDialog('folder', getConfigCourseTitle(), function(folderTitle) {
		// User clicked "Cancel" button
		if (folderTitle === null) {
			return;
		}

		// This would be the place to avoid duplicate existing file names.

		// Empty string causes parent folder to be "My Drive".
		var parentFolderId = '';

		createFile(
				getGoogleAccessToken(),
				parentFolderId,
				folderTitle,
				getConfigCourseId(),
				'application/vnd.google-apps.folder',
				function(data) {
					var folderId = '';
					var folderTitle = '';
					if ((typeof (data) !== 'undefined') && ($.trim(data.id) !== '')) {
						folderId = data.id;
						folderTitle = data.title
					}

					linkFolderToSite(folderId, function() {
						notifyUserSiteLinkChangedWithFolder(folderId, folderTitle,
								true, false);
					});
				});
	});
}

/**
 * Notifies the user the folder has been associated with the given folder, and
 * gives permissions to people in the roster if the user wants that done.
 * 
 * @param folderData
 * @param newFolder
 * @param unlinked
 *            true = folder was unlinked from site; false = folder was linked
 *            with the site
 */
function notifyUserSiteLinkChangedWithFolder(folderId, folderTitle, newFolder, unlinked) {
	if ($.trim(folderId) === '') {
		// Do nothing, because empty folderId shows the association failed
		return;
	}

	if (!unlinked) {
		bootbox.confirm({
			title : sendEmailPromptHeader,
			message : sendEmailPrompt,
			buttons : {
				confirm : {
					label : buttonYes
				},
				cancel : {
					label : buttonNo
				}
			},
			callback : function(sendNotificationEmails) {
				giveRosterPermissions(folderId,
						sendNotificationEmails);
				removeLinkedFolderFromLinkingTable(folderId);
				showInfo($('#alertContainer'), sprintf(linkFolderAlert, escapeHtml(folderTitle)));
			}
		});
	} else {
		removeRosterPermissions(folderId);
		removeUnlinkedFileTreeFromTable(folderId);
		showInfo($('#alertContainer'), sprintf(unlinkFolderAlert, escapeHtml(folderTitle)));
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
 * Returns access token, retrieved from Google if we don't have it already.
 */
function getGoogleAccessToken() {
	if ($.trim(accessTokenHandler.accessToken) === '') {
		accessTokenHandler.accessToken = requestGoogleAccessToken();
	}
	
	return accessTokenHandler.accessToken;
}

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
 * This case help to determines if the  404 error occurs while showing a shared folder is due to user 
 * don't has permission or shared folder has been deleted from the google drive interface. 
 * This function call check to get the instructor email address from the setting service and generates a token
 * and with generated token check if the shared folder exist or not. If the folder don't exist we are simply 
 * unlinking the shared folder from the site. If their is error occurs in generation of instructor token,
 * we are displaying the error message as this case is not very likely to happen. 
 */

function checkSharedFolderDeletionStatus(sharedFolderId){
	$.ajax({
		url: getPageUrl(),
		type: 'GET',
		data: getUpdateLtiParams(
				sharedFolderId,
				"getIntructorTokenSS",
				false),
				success: function(token) {
					if($.trim(token) ==='ERROR'){
						$('#par1').hide();
						$('#par4').show();
					}else{
					getDriveFile(
							token,
							sharedFolderId,
							function(data) {
								if(!data.labels.trashed){
								giveCurrentUserPermissions(sharedFolderId);
								}
								else{
									handleUnlinkingFolder(sharedFolderId);
								}
							},
						    function(data, textStatus, jqXHR) {
								if (data.status === 404) {
									handleUnlinkingFolder(sharedFolderId);
								} 
								
							}
					);
					}
				}
	});
}


function handleUnlinkingFolder(folderId){
	unlinkFolderToSite(folderId,function() {
		actionAfterChecking();
	});
}

function actionAfterChecking(){
	if (getIsInstructor() ){
		openPage('LinkFolder');
		}
	else{
		$('#par1').hide();
		$('#par2').show();
	}
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
				false)
	}).done(function(data){
					if ((typeof(data) === 'object') && (data !== null)) {
						if ($.trim(data.tp_id) !== '') {
							googleDriveConfig = data;
						}
						callback(data);
					} 
	}).fail(function(data){
		bootbox.alert({
			  message: data.responseText,
			  buttons: {
			    ok: {
			      label: buttonOk,
			    }
			  }
			});
	})
				
}

/**
 * Unlink the given Google Folder from the site
 */
function unlinkFolderToSite(folderId, callback) {
	$.ajax({
		url: getPageUrl(),
		type: 'GET',
		dataType: 'json',
		data: getUpdateLtiParams(
				folderId,
				"unlinkGoogleFolder",
				false)
	}).done(function(data) {
					if (typeof(data) === 'object' && (data !== null)) {
						if ($.trim(data.tp_id) !== '') {
							googleDriveConfig = data;
						}
						callback(data);
					} 
	}).fail(function(data){
		bootbox.alert({
			  message: data.responseText,
			  buttons: {
			    ok: {
			      label: buttonOk,
			    }
			  }
			});
	})			
}

/**
 * Sends request to TP to give people in the roster read-only access to the
 * to students for the given shared folder. Multiple instructors in the roster 
 * who are not owner of the shared folder are given can edit access.
 */
function giveRosterPermissions(folderId, sendNotificationEmails) {
	$.ajax({
		url: getPageUrl(),
		type: 'GET',
		data: getUpdateLtiParams(
				folderId,
				"giveRosterAccess",
				sendNotificationEmails)
	}).done(function(data) {
			openPage('Home');
						
	})
				
	
}

/**
 * Gives access to the currently logged in user.  This is called when request to
 * get linked folder fails due to 404 error; hat may occur when student/or instructor is
 * added to the class roster after the folder has been linked.
 * 
 * @param folderId Google folder's ID
 */
function giveCurrentUserPermissions(folderId) {
	$.ajax({
		url: getPageUrl(),
		type: 'GET',
		data: getUpdateLtiParams(
				folderId,
				"giveCurrentUserAccess",
				false)
	}).done(function(data) {
					if ($.trim(data) === 'SUCCESS') {
						$('#permissionUpdate').show();
						getDriveFile(
								getGoogleAccessToken(),
								folderId,
								function(data) {
									// Linked folders are all depth 0 (no parents)
									// TODO: can we call getFoldersChildren() instead, using folderId?
									showLinkedGoogleFolderCallback(data, 0);
								});
					}
	}).fail(function(data){
		$('#par5').addClass('alert alert-error').html(data.responseText);
	})
	
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
				false)
	}).done(function(data) {
					if ($.trim(data) === 'SUCCESS') {
						openPage('LinkFolder');
					}
					
	}).fail(function(data){
		bootbox.alert({
			  message: data.responseText,
			  buttons: {
			    ok: {
			      label: buttonOk,
			    }
			  },
			  callback:function(){
				  openPage('LinkFolder');
			  }
			});
					
	})
	
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

function openDialogToCreateFile(fileType, parentFolderId, linkedFolderId, depth) {
	itemTitlePromptDialog(fileType, null, function(title) {
		// User clicked "Cancel" button
		if (title === null) {
			return;
		}

		var fileTypeLowerCase = fileType.toLowerCase();
		// Files are not associated with courses, use empty ID string
		var courseId = '';

		createFile(
				getGoogleAccessToken(),
				parentFolderId,
				title,
				courseId,
				'application/vnd.google-apps.' + fileTypeLowerCase,
				function(file) {
					addFileToFileTreeTable(file, parentFolderId, linkedFolderId,
							depth + 1);
				});
		
		showInfo($('#alertContainer'), sprintf(createItemAlert, fileTypeLowerCase, escapeHtml(title)));
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
	<td><a href="#" class="itemLink" onclick="[OpenFileCall]" title="[FolderTitle]"> \
	<img src="[GoogleIconLink]" width="16" height="16" alt="Folder">&nbsp;[FolderTitle] \
	</a></td> \
	<td> \
	<a href="#" class="btn btn-small" onclick="linkFolder(\'[FolderIdOnclickParam]\', \'[FolderTitleOnclickParam]\');">'+linkFolderButton+'</a> \
	</td> \
	</tr>';

/**
 * Adds the given Google folder to table row, allowing the user to link it to
 * the site.
 * 
 * @param folder Google folder available for linking
 */
function addFolderToLinkFolderTable(folder, parentFolderId, indentationLevel) {	
	// Only add the row if row for same file DNE
	if ($('#' + getLinkingTableRowIdForFolder(folder.id)).length > 0) {
		return;
	}

	var indentCss = '';
	if (indentationLevel > 0) {
		indentCss = 'padding-left: ' + (indentationLevel * FILE_DEPTH_PADDING_PX)
				+ 'px;';
	}

	var newEntry = LINK_FOLDER_TABLE_ROW_TEMPLATE
		.replace(/\[TrFolderId\]/g,
			getLinkingTableRowIdForFolder(folder.id))
		.replace(
			/\[FolderIdOnclickParam\]/g, escapeAllQuotes(folder.id))
		.replace(
			/\[FolderTitle\]/g, escapeDoubleQuotes(escapeHtml(folder.title)))
		.replace(/\[FolderTitleOnclickParam\]/g,
			escapeAllQuotes(folder.title))
		.replace(
			/\[GoogleIconLink\]/g, folder.iconLink)
		.replace(
			/\[OpenFileCall\]/g, getFunctionCallToOpenFile(folder))
		.replace(/\[indentCss\]/g, indentCss);

	$(newEntry).appendTo('#LinkFolderTableTbody');
	
}

/**
 * Returns JavaScript to caller that can be used (usually during onclick) to
 * open the given Google file.
 */
function getFunctionCallToOpenFile(file) {
	return "openFile('" + escapeAllQuotes(escapeHtml(file.title)) + "', '"
			+ escapeAllQuotes(file.alternateLink) + "', '"
			+ escapeAllQuotes(file.mimeType) + "');";
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
 * @param folderTitle
 * @param reason
 */
function notifyUserFolderCannotBeLinked(folderTitle, reason) {
	bootbox.alert(sprintf(linkFolderErrorAlert, escapeHtml(folderTitle), reason));
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
	<td style="[FileIndentCss]">[ExpandShrink]<a href="#" class="itemLink" onclick="[OpenFileCall]" title="[FileTitle]"> \
	<img src="[GoogleIconLink]" width="16" height="16" alt="Folder">&nbsp;<span class="title">[FileTitle]</span> \
	</a></td> \
	<td>&nbsp;[DropdownTemplate]</td> \
	<td>&nbsp;[ActionTemplate]</td> \
	<td>[LastModified]</td> \
	</tr>';

var ACTION_BUTTON_TEMPLATE = '<a href="#" class="btn btn-small" onclick="[ActionOnClick]">[ActionTitle]</a>';

var FOLDER_TREE_TABLE_ROW_TEMPLATE = '<tr id="[FileId]" class="[ClassSpecifyParentAndDepth] [LinkedFolderId]"> \
	<td style="[FileIndentCss]">[ExpandShrink]<a class="itemLink" onclick="[OpenFileCall]" title="[FileTitle]"> \
	<img src="[GoogleIconLink]" width="16" height="16" alt="Folder">&nbsp;<span class="title">[FileTitle]</span> \
	</a></td> \
	[ShareColumn] \
	</tr>';

var FOLDER_TREE_SHARE_COLUMN_BUTTON_TEMPLATE = '\
	<td> \
	<a class="btn btn-small" onclick="linkFolder(\'[FolderIdOnclickParam]\', \'[FolderTitleOnclickParam]\');">' + linkFolderButton + '</a> \
	</td> \
	';

var FOLDER_TREE_SHARE_COLUMN_EMPTY_TEMPLATE = '\
	<td style="border-left: 0px;"> \
	&nbsp; \
	</td> \
	';

// Node types supported for sorting purposes
var NODE_TYPE_FOLDER = 'NODE_TYPE_FOLDER';
var NODE_TYPE_NONFOLDER = 'NODE_TYPE_NONFOLDER';

function initializeFileTree(fileTreeDivSelector) {
	var newFileTree = $(fileTreeDivSelector).first();

	if (newFileTree.length == 1) {
		// check_callback: allow all tree changes
		fileTree = $(fileTreeDivSelector).first().jstree(
				{
					core : {
						check_callback : true
					},
					plugins : [ 'sort', 'types' ],
					sort : function(nodeIdA, nodeIdB) {
						var returnValue;

						// sort by type, then by text
						if (this.get_type(nodeIdA) === this.get_type(nodeIdB)) {
							returnValue = this.get_text(nodeIdA).localeCompare(
									this.get_text(nodeIdB));
						} else {
							returnValue = this.get_type(nodeIdA) >= this
									.get_type(nodeIdB);
						}

						return returnValue;
					},
					types : {
						NODE_TYPE_FOLDER : {},
						NODE_TYPE_NONFOLDER : {},
					}
				}).jstree(true);
	} else {
		fileTree = null;
	}

	// return to allow chain-ability
	return fileTree;
}

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
function addFileToFileTreeTable(file, parentFolderId, linkedFolderId, treeDepth, foldersOnly)
{
	foldersOnly = (foldersOnly === true);
	
//	console.log('addFileToFileTreeTable');
//	console.log(file);
//	console.log(parentFolderId);
//	console.log(linkedFolderId);
//	console.log(treeDepth);
//	console.log(foldersOnly);

	var isFolder = getIsFolder(file.mimeType);

	if (fileTree) {
		var escapedTitle = escapeDoubleQuotes(escapeHtml(file.title));
		fileTree.create_node((parentFolderId == null) ? '#' : parentFolderId, {
			text : escapedTitle,
			type : (isFolder) ? NODE_TYPE_FOLDER : NODE_TYPE_NONFOLDER,
			id : file.id,
			icon: file.iconLink
		});
	}

	var fileIndentCss = '';
	if (treeDepth > 0) {
		fileIndentCss = 'padding-left: ' + (treeDepth * FILE_DEPTH_PADDING_PX) + 'px;';
	}
	var dropdownTemplate = '';
	var expandShrinkOption = '';
	if (isFolder) {
		expandShrinkOption = '<a href="#" class="expandShrink" onclick="toggleExpandShrink(\'' + file.id + '\');"><em></em><span class="expandShringFolderTitle  sr-only">' + escapeAllQuotes(escapeHtml(file.title)) + '</span>&nbsp;</a>';
	}
	// Add text to parent folder for expanding/shrinking functionality
	if ($.trim(parentFolderId) !== '') {
	    var folderTitle =  $('#' + getTableRowIdForFile(parentFolderId)).find('span.title').text();
		$('#' + getTableRowIdForFile(parentFolderId)).find('a.expandShrink:not(.shrinkable)').addClass('shrinkable').html("<em>" + SHRINK_TEXT + '</em><span class="expandShringFolderTitle sr-only">' + escapeAllQuotes(escapeHtml(folderTitle)) + '</span>');
	}
	if (!foldersOnly && getIsInstructor() && isFolder) {
		dropdownTemplate = $('#FolderDropdownTemplate').html();
		dropdownTemplate = dropdownTemplate
		.replace(/\[FolderIdParam\]/g, escapeAllQuotes(file.id))
		.replace(/\[LinkedFolderIdParam\]/g, escapeAllQuotes(linkedFolderId))
		.replace(/\[FolderDepthParam\]/g, treeDepth);
	}
	var actionTitle = null;
	var actionOnClick = '';
	if (getIsInstructor() && isFolder && (treeDepth === 0)) {
		actionTitle = unlinkFolderButton+' <span class ="sr-only">'+escapeAllQuotes(escapeHtml(file.title))+'</span>';
		actionOnClick = 'unlinkFolderFromSite(\'' + escapeAllQuotes(file.id) + '\', \'' + escapeAllQuotes(file.title) + '\');return false;';
	} else {
		if (getIsInstructor()) {
			actionTitle = deleteFolderButton+' <span class ="sr-only">'+escapeAllQuotes(escapeHtml(file.title))+'</span>';
			actionOnClick = 'deleteGoogleFile(\'' + escapeAllQuotes(file.id) + '\', \'' + escapeAllQuotes(file.title) + '\', \'' + escapeAllQuotes(file.mimeType) + '\', \'' + escapeAllQuotes(file.userPermission.role) + '\');';
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

	if (!foldersOnly) {
		newEntry = FILE_TREE_TABLE_ROW_TEMPLATE
		.replace(/\[FileId\]/g, getTableRowIdForFile(file.id))
		.replace(/\[ClassSpecifyParentAndDepth\]/g, childOfParentId)
		.replace(/\[LinkedFolderId\]/g, getClassForLinkedFolder(linkedFolderId))
		.replace(/\[FileTitle\]/g,  escapeDoubleQuotes(escapeHtml(file.title)))
		.replace(/\[DropdownTemplate\]/g, dropdownTemplate)
		.replace(/\[ActionTemplate\]/g, actionTemplate)
		.replace(/\[GoogleIconLink\]/g, file.iconLink)
		.replace(/\[ExpandShrink\]/g, expandShrinkOption)
		.replace(/\[FileIndentCss\]/g, fileIndentCss)
		.replace(/\[OpenFileCall\]/g, getFunctionCallToOpenFile(file))
		.replace(/\[LastModified\]/g, getFileLastModified(file));
	} else {
		var shareColumn = ((!jQuery.isEmptyObject(file.parents)) ? FOLDER_TREE_SHARE_COLUMN_BUTTON_TEMPLATE: FOLDER_TREE_SHARE_COLUMN_EMPTY_TEMPLATE)
			.replace(/\[FolderIdOnclickParam\]/g, escapeAllQuotes(file.id))
			.replace(/\[FolderTitleOnclickParam\]/g, escapeAllQuotes(file.title));
		
		newEntry = FOLDER_TREE_TABLE_ROW_TEMPLATE
		.replace(/\[FileId\]/g, getTableRowIdForFile(file.id))
		.replace(/\[ClassSpecifyParentAndDepth\]/g, childOfParentId)
		.replace(/\[LinkedFolderId\]/g, getClassForLinkedFolder(linkedFolderId))
		.replace(/\[FileTitle\]/g,  escapeDoubleQuotes(escapeHtml(file.title)))
		.replace(/\[ActionTemplate\]/g, actionTemplate)
		.replace(/\[GoogleIconLink\]/g, file.iconLink)
		.replace(/\[ExpandShrink\]/g, expandShrinkOption)
		.replace(/\[FileIndentCss\]/g, fileIndentCss)
		.replace(/\[OpenFileCall\]/g, getFunctionCallToOpenFile(file))
		.replace(/\[ShareColumn\]/g, shareColumn);
	}

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

function expand(folderId) {
	if ($('#' + getTableRowIdForFile(folderId)).find('a.expandShrink.shrinkable').hasClass('shrunk'))
		toggleExpandShrink(folderId);
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
	if ($expandShrinkSpan.hasClass('shrunk')) {
		$expandShrinkSpan.removeClass('shrunk');
		$expandShrinkSpan.find('em').html(SHRINK_TEXT);
		expandOrShrinkChildren(folderId, false);
	} else {
		$expandShrinkSpan.addClass('shrunk');
		$expandShrinkSpan.find('em').html(EXPAND_TEXT);
		expandOrShrinkChildren(folderId, true);
	}
}

/**
 * Shows/Hides the children rows for the current folder, recursively closing
 * their children (call is made for non-folders: that should have no effect
 * since those will not have children).
 * 
 * FIXME: This shouldn't expand collapsed children...
 * 
 * Assume folder B is collapsed within folder A, which is also collapsed. When
 * folder A is expanded, it should not expand folder B by default.
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
				result = MONTHS[googleDate.getMonth()]
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

