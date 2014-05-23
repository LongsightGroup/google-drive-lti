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
 * @author lsloan@umich.edu (Lance E Sloan)
 *
 * @author: Raymond Louis Naseef
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

var FOLDER_MIME_TYPE = 'application/vnd.google-apps.folder';
//Array listing ID of each file's parents, keyed by the child file.  Finding
//all a file's ancestors can be done by finding file's parent, then finding
//entry for each parent's parent.
var googleFileParents = [];

bootbox.setDefaults({
	closeButton : false
});

var fileTree = null;
var fileTreeDiv = null;
var fileTreeSearchTimeout = false;

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
						deleteMsg = sprintf(applicationProperties['gd.delete.folder.error.alert'],
								escapeHtml(fileTitle));
					} else {
						deleteMsg = sprintf(applicationProperties['gd.delete.file.error.alert'],
								escapeHtml(fileTitle));
					}
					bootbox.alert({
						message : deleteMsg,
						buttons : {
							ok : {
								label : applicationProperties['gd.button.ok'],
							}
						}
					});
				} else {
					var deleteConfirmationMessage = null;
					if (isFolder) {
						deleteConfirmationMessage = sprintf(
								applicationProperties['gd.delete.folder.prompt'], escapeHtml(fileTitle));
					} else {
						deleteConfirmationMessage = sprintf(applicationProperties['gd.delete.file.prompt'], itemType,
								escapeHtml(fileTitle));
					}
					bootbox.confirm({
						title: sprintf(applicationProperties['gd.delete.item.prompt.header'], itemType),
						message : deleteConfirmationMessage,
						buttons : {
							confirm : {
								label : applicationProperties['gd.button.delete'],
							}
						},
						callback : function(userConfirmed) {
							if (userConfirmed === true) {
								deleteDriveFile(ownerToken, fileId, function() {
									fileTree.delete_node(fileId);
									showInfo($('#alertContainer'), sprintf(applicationProperties['gd.delete.item.alert'],
									        itemType, escapeHtml(fileTitle)));
								});
							}
						}
					});
				}
			}
		});
	} else if (role === "owner") {
		var deleteConfirmationMessage = null;
		if (isFolder) {
			deleteConfirmationMessage = sprintf(applicationProperties['gd.delete.folder.prompt'],
					escapeHtml(fileTitle));
		} else {
			deleteConfirmationMessage = sprintf(applicationProperties['gd.delete.file.prompt'], itemType,
					escapeHtml(fileTitle));
		}

		bootbox.confirm({
			title: sprintf(applicationProperties['gd.delete.item.prompt.header'], itemType),
			message : deleteConfirmationMessage,
			buttons : {
				confirm : {
					label : applicationProperties['gd.button.delete']
				}
			},
			callback : function(userConfirmed) {
				if (userConfirmed === true) {
					deleteDriveFile(getGoogleAccessToken(), fileId, function() {
						fileTree.delete_node(fileId);
						showInfo($('#alertContainer'), sprintf(applicationProperties['gd.delete.item.alert'], 
						        itemType, escapeHtml(fileTitle)));
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
	var defaultItemPrompt = sprintf(applicationProperties['gd.create.item.prompt'], itemType.toLowerCase());
	var itemPrompt = defaultItemPrompt;
	var itemTitle = defaultTitle;

	if (defaultTitle === null) {
		itemTitle = '';
	}

	var displayPrompt = function() {
		var promptDialog = bootbox.prompt({
			inputType: 'text',
			title : sprintf(applicationProperties['gd.create.item.prompt.header'], itemType),
			value : itemTitle,
			buttons : {
				confirm : {
					label : applicationProperties['gd.button.create'],
				}
			},
			callback : function(itemTitle) {
				if (itemTitle !== null) {
					itemTitle = $.trim(itemTitle);

					if (itemTitle === '') {
						// Empty responses are invalid. Prompt again
						// with error message.
						itemPrompt = defaultItemPrompt + '<br/><em>'
								+ applicationProperties['gd.create.item.prompt.error'] + '</em>';
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
				FOLDER_MIME_TYPE,
				function(data) {
					var folderId = '';
					var folderTitle = '';
					if ((typeof (data) !== 'undefined') && ($.trim(data.id) !== '')) {
						folderId = data.id;
						folderTitle = data.title;
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
			title : applicationProperties['gd.send.email.prompt.header'],
			message : applicationProperties['gd.send.email.prompt'],
			buttons : {
				confirm : {
					label : applicationProperties['gd.button.yes'],
				},
				cancel : {
					label : applicationProperties['gd.button.no'],
				}
			},
			callback : function(sendNotificationEmails) {
				giveRosterPermissions(folderId,
						sendNotificationEmails);
				showInfo($('#alertContainer'), sprintf(applicationProperties['gd.link.folder.alert'],
				        escapeHtml(folderTitle)));
			}
		});
	} else {
		removeRosterPermissions(folderId);
		showInfo($('#alertContainer'), sprintf(applicationProperties['gd.unlink.folder.alert'],
		        escapeHtml(folderTitle)));
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

/**
 * @returns {Array} Array of current site's linked (shared) folders.
 */
function getConfigLinkedFolders() {
	return googleDriveConfig.linkedFolders;
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
 * Returns access token, retrieved from Google.Before using the access token doing a check if token 
 * is nearing the expiration time. Ideally the token expires in 60 minutes, but we are checking it on 59th minute.
 * If the time elapsed is greated than 59 minutes then a get a new access token.
 * The Json response getting accessToken and timeStamp in milliseconds when token is created.
 * {
 * "access_token" : "asdc", 
 * "time_stamp" : "1399634704598"
 * } 
 */
function getGoogleAccessToken() {
	if ($.trim(accessTokenHandler) === ''
			|| $.trim(accessTokenHandler) === 'undefined') {
		accessTokenHandler = 'null';
	}

	var date = new Date();
	var currentTimeInMilliSeconds = date.getTime();
	var fortyFiveMinutesInMilliSeconds = 2700000;
	if ((currentTimeInMilliSeconds - accessTokenTime) > fortyFiveMinutesInMilliSeconds) {
		var tokenResponse = requestGoogleAccessToken();
		if (tokenResponse !== 'ERROR') {
			$.each(JSON.parse(tokenResponse), function(i, result) {
				if (i === "access_token") {
					accessTokenHandler = result;
				} else if (i === "time_stamp") {
					accessTokenTime = result;
				}
			});
		} else {
			accessTokenHandler = 'null';
			accessTokenTime = 'null';
		}
	}

	return accessTokenHandler;
}

function requestGoogleAccessToken() {
	var result = null;

	result = $.ajax({
		url: getPageUrl(),
		dataType: 'json',
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
		data: getUpdateLtiParams(
				folderId,
				"linkGoogleFolder",
				false)
	}).done(function(data){
		if ($.trim(data) === 'SUCCESS') {
						callback(data);
					} 
	}).fail(function(data){
		bootbox.alert({
			message : data.responseText,
			buttons : {
				ok : {
					label : applicationProperties['gd.button.ok'],
				}
			}
		});
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
				false)
	}).done(function(data) {
		if ($.trim(data) === 'SUCCESS') {
						callback(data);
					} 
	}).fail(function(data){
		bootbox.alert({
			message : data.responseText,
			buttons : {
				ok : {
					label : applicationProperties['gd.button.ok'],
				}
			}
		});
	});
}

/**
 * Sends request to TP to give people in the roster read-only access to the to
 * students for the given shared folder. Multiple instructors in the roster who
 * are not owner of the shared folder are given can edit access.
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
						
	});
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
						$(fileTreeDiv).hide();
						getDriveFile(
								getGoogleAccessToken(),
								folderId,
								function(data) {
									// Could add code here to refresh display.
								});
					}
	}).fail(function(data){
		$('#par5').addClass('alert alert-error').html(data.responseText);
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
			      label: applicationProperties['gd.button.ok'],
			    }
			  },
			  callback:function(){
				  openPage('LinkFolder');
			  }
			});
					
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

function openDialogToCreateItem(fileType, parentFolderId, linkedFolderId, depth) {
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
					googleDriveItemCache[file.id] = file;

					var parentFolder = fileTree.get_node(parentFolderId);
					
					// setting false forces parent folder to reload when refreshed
					parentFolder.state.loaded = false;
					
					// (re)-load parent folder's contents
					fileTree.refresh_node(parentFolder);

					// open parent folder, just in case it's closed
					fileTree.open_node(parentFolder);
				});
		
		showInfo($('#alertContainer'), sprintf(applicationProperties['gd.create.item.alert'], 
		        fileTypeLowerCase, escapeHtml(title)));
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
				notifyUserFolderCannotBeLinked(folderTitle, 
                        sprintf(applicationProperties['gd.link.folder.error.reason.specificFolder'], 
                                folderRelationship.type, data.title));
			} else {
				notifyUserFolderCannotBeLinked(folderTitle, 
				        sprintf(applicationProperties['gd.link.folder.error.reason.genericFolder'], 
				                folderRelationship.type));
			}
		},
		function() {
			// Error getting linked folder...
			notifyUserFolderCannotBeLinked(folderTitle, 
			        sprintf(applicationProperties['gd.link.folder.error.reason.genericFolder'], 
			                folderRelationship.type));
		});
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
	bootbox.alert(sprintf(applicationProperties['gd.link.folder.error.alert'],
	        escapeHtml(folderTitle), reason));
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

var googleDriveItemCache = {};
var rootNodeId = null;
var onlyOwnedFolders = false;

// Special ID used by jsTree for its root node.
var JSTREE_ROOT_NODE_ID = '#';

// Node types supported for sorting purposes
var NODE_TYPE_FOLDER = 'NODE_TYPE_FOLDER';
var NODE_TYPE_NONFOLDER = 'NODE_TYPE_NONFOLDER';

/**
 * Compare two file tree nodes for sorting purposes. Sort with folders
 * alphabetically first, followed by files alphabetically.
 * 
 * This is used by the jsTree "sort" plugin and requires the "types" plugin,
 * too.
 * 
 * @param {String}
 *            nodeIdA ID of a tree node to compare.
 * @param {String}
 *            nodeIdB ID of another tree node to compare.
 * @returns {Number} 1 or -1, according to jsTree specifications. It is common
 *          for comparators to return 0 if the items being compared are equal,
 *          but jsTree doesn't support that.
 */
function fileTreeNodeSortComparator(nodeIdA, nodeIdB) {
	var returnValue;

	// sort by type, then by text
	if (this.get_type(nodeIdA) !== this.get_type(nodeIdB)) {
		returnValue = (this.get_type(nodeIdA) > this.get_type(nodeIdB)) ? 1
				: -1;
	} else {
		returnValue = (this.get_text(nodeIdA).localeCompare(
				this.get_text(nodeIdB)) > 0) ? 1 : -1;
	}

	return returnValue;
}

/**
 * When called by jsTree, this method will take a jsTree node object and add
 * other elements (buttons, menus, information) to it before returning the
 * altered node to jsTree for display.
 * 
 * @param node
 *            A jsTree node object
 * @returns A jsTree node object with additional elements
 */
function fileTreeRedrawNode(node) {
	// this bootstrap version uses "btn-mini", new version uses "btn-xs"
	var BUTTON_CLASSES = 'btn btn-xs btn-mini';

	if (node) {
		var nodeData = fileTree.get_node(node);
		var item = googleDriveItemCache[nodeData.id];
		var columnClass = null;
		var newContent = $();
		var isRootNode = (item.id === rootNodeId);

		if (onlyOwnedFolders) {
			// Second column content: Share Folder button if it's not the root folder.
			columnClass = 'shareButtonColumn';
			if (!isRootNode) {
				newContent = newContent.add($('<span>', {
					'class' : columnClass,
				}).append($('<a>', {
					'href' : '#',
					
					'class' : BUTTON_CLASSES,
					'html' : applicationProperties['gd.link.folder.button'] + 
					    ' <span class ="sr-only">' + escapeAllQuotes(escapeHtml(item.title)) + '</span>',
					'onclick' : "linkFolder('" + escapeAllQuotes(item.id) + "', '" +
					    escapeAllQuotes(item.title) + "'); return false;",
				})));
			} else {
				newContent = newContent.add($('<span>', {
					'class' : columnClass,
					'html' : '&nbsp;',
				}));
			}
		} else {
			// Second column content: Instructors see the add menu button.
			columnClass = 'col-md-4 addMenuButtonColumn';
			if (getIsInstructor() && getIsFolder(item.mimeType)) {
				newContent = newContent.add($('<span>', {
					'class' : columnClass,
					'html' : $('#FolderDropdownTemplate').html()
						.replace(/\[FolderIdParam\]/g, escapeAllQuotes(item.id))
						.replace(/\[LinkedFolderIdParam\]/g,escapeAllQuotes(rootNodeId))
						.replace(/\[FolderDepthParam\]/g, 0)
				}));
			} else {
				newContent = newContent.add($('<span>', {
					'class' : columnClass,
					'html' : '&nbsp;',
				}));
			}

			// Third column content: Instructors see unshare or delete button.
			columnClass = 'col-md-4 unshareAndDeleteButtonColumn hidden-phone hidden-xs';
			if (getIsInstructor()){
				if (isRootNode) {
					newContent = newContent.add($('<span>', {
						'class' : columnClass,
						'html' : $('<a>', {
							'href' : '#',
							'class' : BUTTON_CLASSES,
							'html' : applicationProperties['gd.unlink.button'] + 
							    ' <span class ="sr-only">' + escapeAllQuotes(escapeHtml(item.title)) + '</span>',
							'onclick' : "unlinkFolderFromSite('" + escapeAllQuotes(item.id) + "', '" + 
							    escapeAllQuotes(item.title) + "'); return false;",
						})}));
				} else {
					newContent = newContent.add($('<span>', {
						'class' : columnClass,
						'html' : $('<a>', {
							'href' : '#',
							'class' : BUTTON_CLASSES,
							'html' : applicationProperties['gd.delete.button'] + 
							    ' <span class ="sr-only">' + escapeAllQuotes(escapeHtml(item.title)) + '</span>',
							'onclick' : "deleteGoogleFile('" + escapeAllQuotes(item.id) + "', '" + 
							    escapeAllQuotes(item.title) + "', '" + escapeAllQuotes(item.mimeType) + "', '" + 
							    escapeAllQuotes(item.userPermission.role) + "');",
						})}));
				}
			} else {
				newContent = newContent.add($('<span>', {
					'class' : columnClass,
					'html' : '&nbsp;',
				}));
			}
		}

		// Last column (third or fourth depending upon view) content: modification date and user
		columnClass = 'col-md-4 metaData hidden-phone hidden-xs';
		newContent = newContent.add($('<small>', {
			'class' : columnClass,
			'html' : getGoogleDateOrTime(item.modifiedDate) + ' ',
		}).append($('<span>', {
			'class' : 'muted',
			'html' : item.lastModifyingUserName,
		})));
		
		$(node).find('a:first').after($('<span>', {
			'class' : 'extras pull-right row',
			'html' : newContent,
		}));
		
		fileTreeLabelExpansionElement($(node));
	}

	return node;
}


/**
 * When a file tree item is clicked, rather than letting jsTree select it, open
 * the URL stored in its item object's "alternateLink" property.
 * 
 * @param event
 *            The click event
 * @param data
 *            The data (tree node) affected by the click
 */
function fileTreeHandleItemClick(event, data) {
	fileTree.deselect_all(suppressChangedEvent = true);

	window.open(googleDriveItemCache[data.node.id].alternateLink, '_blank');
}

/**
 * Check the search text input element for a non-null string and call the jsTree search method.
 * 
 * @param {String} fileTreeSearchSelector A valid jQuery selector for the search text input element.
 */
function fileTreeSearch(fileTreeSearchSelector) {
	var searchText = $(fileTreeSearchSelector).val().trim();
	
	fileTree.search(searchText);
}

/**
 * Given a file tree node, add an element within its expand/collapse icon
 * element that contains appropriate text for screen readers. If the node has
 * the "jstree-open" class, add text for "Collapse". Otherwise, add text for
 * "Expand".
 * 
 * @param element
 *            jQuery object for the file tree node.
 */
function fileTreeLabelExpansionElement(element) {
    element.find('i.jstree-ocl:first').html($('<span>', {
        'class': 'sr-only',
        'html': element.hasClass('jstree-open') ? applicationProperties['gd.screenReader.label.collapseFolder']
                : applicationProperties['gd.screenReader.label.expandFolder'],
    }));
}

/**
 * @param fileTreeDivSelector
 * @param options
 *            Object containing optional parameters: <blockquote>
 *            <dl>
 *            <dt>appendContentConfig </dt>
 *            <dd>An object containing configuration for the appendContent
 *            plugin of jsTree</dd>
 *            <dt>onlyOwnedFolders</dt>
 *            <dd>Boolean value indicating whether only GD folders owned by
 *            current user will be shown.</dd>
 *            </dl>
 *            </blockquote>
 * @returns Initialized jsTree object
 */
function initializeFileTree(fileTreeDivSelector, options) {
	_.defaults(options, {onlyOwnedFolders: false, folderId: 'root'});
	
	onlyOwnedFolders = (options.onlyOwnedFolders === true);

	fileTreeDiv = $(fileTreeDivSelector).first();

	if (fileTreeDiv.length == 1) {
		
		$.jstree.plugins.appendContent = function(options, parent) {
			this.redraw_node = function(node, deep, isCallback) {
				node = parent.redraw_node.call(this, node, deep, isCallback);
				
				return fileTreeRedrawNode(node);
			};
		};

		fileTree = fileTreeDiv.jstree({
		    'plugins' : [ 'sort', 'types', 'appendContent', 'search', 'wholerow' ],
			'sort' : fileTreeNodeSortComparator,
			'types' : {
				NODE_TYPE_FOLDER : {},
				NODE_TYPE_NONFOLDER : {},
			},
			'search' : {
				'fuzzy' : false,
				'show_only_matches' : true,
			}, 
			'core' : {
				'check_callback' : true, // allow all tree node changes (create, delete, etc.)
				'themes' : {
					'dots' : false,
					'responsive' : false,
				},
				'data' : {
					'url' : function(node) {
						return _getGoogleDriveUrl((node.id == JSTREE_ROOT_NODE_ID) ? options.folderId
								: null);
					},
					'data' : function(node) {
						var queryFolderId = node.id;
						var query = '';
						var data = {
							'access_token' : getGoogleAccessToken(),
						};

						if (queryFolderId != JSTREE_ROOT_NODE_ID) {
							query += "'"
									+ queryFolderId
									+ "' in parents and trashed = false";

							if (onlyOwnedFolders === true) {
								query += " and mimeType = '" + FOLDER_MIME_TYPE + "' and 'me' in owners";
							}

							data.q = query;
						}

						return data;
					},
					'error' : function(data, textStatus, jqXHR) {
						if (data.status === 404) {
							// Get ID of folder that caused error from the GD error message.  
						    // Is there a better way?
							var sharedFolderId = data.responseJSON.error.message.split(' ').pop();
							checkSharedFolderDeletionStatus(sharedFolderId);
						}
					},
					'dataFilter' : function(rawResponseText,
							type) {
						var data = JSON.parse(rawResponseText);
						var nodeData = [];

						/**
						 * @param item
						 * @param isOpenedState
						 * @returns 
						 */
						function itemToNodeData(item, isOpenedState) {
							var isFolder = getIsFolder(item.mimeType);
							var newNodeData = {
								'id' : item.id,
								'text' : escapeHtml(item.title),
								'icon' : item.iconLink,
								'type' : (isFolder) ? NODE_TYPE_FOLDER
										: NODE_TYPE_NONFOLDER,
								'children' : isFolder,
							};

							if (isOpenedState === true) {
								newNodeData.state = {
									'opened' : true,
								};
							}
							
							return newNodeData;
						};

						if (data) {
							if (data.hasOwnProperty('items')) {
								$.each(data.items, 
										function(key, item) {
											nodeData.push(itemToNodeData(item));
											googleDriveItemCache[item.id] = item;
										});
							} else {
								nodeData.push(itemToNodeData(data, true));
								googleDriveItemCache[data.id] = data;
								rootNodeId = data.id;
							}

						}

						return JSON.stringify(nodeData);
					},
				},
			},
		}).jstree(true);
		
		fileTreeDiv.on('select_node.jstree', fileTreeHandleItemClick);
		
		fileTreeDiv.on('close_node.jstree open_node.jstree', function(event, data) {
			fileTreeLabelExpansionElement(fileTree.get_node(data.node, true));
		});
		
		fileTreeDiv.on('after_close.jstree after_open.jstree load_node.jstree', function(event, data) {
			fileTreeDiv.find('li.jstree-node').removeClass('shadedBackground');
			fileTreeDiv.find('li.jstree-node:odd').addClass('shadedBackground');
		});

		var fileTreeSearchSelector = fileTreeDivSelector + '_search';
		
		$(fileTreeSearchSelector).keyup(function() {
			if (fileTreeSearchTimeout) {
				clearTimeout(fileTreeSearchTimeout);
			}
			
			fileTreeSearchTimeout = setTimeout(function() {
				fileTreeSearch(fileTreeSearchSelector);
			}, 250);
		});
		

		$(fileTreeDivSelector + '_searchButton').click(function() {
			fileTreeSearch(fileTreeSearchSelector);
		});
	} else {
		fileTree = null;
	}

	// return to allow chain-ability
	return fileTree;
}

/**
 * Split a comma-delimited string of month names into an array. If there aren't
 * 12 month names in the string, use a list of the English month names by
 * default.
 * 
 * @param monthNames {String} Comma-delimited string of month names
 * @returns {Array} Array of month names
 */
function parseMonthNames(monthNames) {
	var months = monthNames.split(',');

	if (months.length != 12) {
		console.log('ERROR: The string of month names did not contain 12 names.  Using default names instead.');
		months = [ 'January', 'February', 'March', 'April', 'May', 'June',
				'July', 'August', 'September', 'October', 'November',
				'December' ];
	}

	return months;
};

var MONTHS = parseMonthNames(applicationProperties['gd.monthNames']);

/**
 * Return the time if the given date/time string is for today, else return the date.
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
			// Getting next midnight by adding 1 day in milliseconds, ignoring leap second
			var todayEndTimeMs = todayStart.getTime() + ((86400000) - 10);
			if ((googleDate.getTime() >= todayStart.getTime())
					&& (googleDate.getTime() <= todayEndTimeMs))
			{
				// Today, show hh:mm AM/PM
				var hour = googleDate.getHours();
				var min = googleDate.getMinutes();
				var am_pm = (hour >= 12) ? "PM" : "AM";
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
});

