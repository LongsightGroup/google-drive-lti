if (typeof(verifyAllArgumentsNotEmpty) === 'undefined') {
	if ((typeof(console) !== 'undefined') && (typeof(console.log) === 'function')) {
		console.log('WARNING: google-drive-utils.js relies upon sibling library utils.js');
	}
}
//$.getScript('utils.js');

var MAX_RESULTS_PER_PAGE = 100;


/**
 * Adds permissions for the given file to the given user.  This will also work
 * for groups, domain, and anyone.
 * 
 * @param accessToken	Google Access Token for inserting this permission
 * @param fileTitle File's title (not used)
 * @param fileId Google File's ID
 * @param permissionRole 'owner', 'writer', or 'reader'
 * @param permissionType 'user', 'group', 'domain', or 'anyone'
 * @param permissionValue identity for that type: e.g., the user's email address
 * @param callback function called when result is success
 * @param completeCallback function called when this AJAX call completes
 */
function addPermissionsToFile(accessToken, fileTitle, fileId, permissionRole, permissionType, permissionValue, callback, completeCallback) {
	// Wait until any action on permissions are completed, as Google Drive does
	// not appear to work well with multiple modifications for single folder running simulatenously
	if (!verifyAllArgumentsNotEmpty(accessToken, fileId, permissionRole, permissionType, permissionValue)) {
		return;	// Quick return to simplify code
	}
	var url = _getGoogleDriveUrl(fileId, '');
	var requestData = '{ \
			"access_token" : "' + escapeJson(accessToken) + '", \
			"role": "' + escapeJson(permissionRole) + '", \
			"type": "' + escapeJson(permissionType) + '", \
			"value" : "' + escapeJson(permissionValue) + '" \
		}';
	$.ajax({
		url: url,
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		type : 'POST',
		data : requestData,
		dataType : 'json',
		success: function(data, textStatus, jqXHR) {
			if (typeof(callback) === 'function') {
				callback(data, textStatus, jqXHR);
			}
		},
		complete: function(jqXHR, textStatus) {
			if (typeof(completeCallback) === 'function') {
				completeCallback(jqXHR, textStatus);
			}
		}
	});
}

/**
 * This creates a new folder associated to the site, with the given parent
 * folder (default parent is Google's default, the user's "My Drive").
 * 
 * @param accessToken	Google Access Token for inserting this permission
 * @param parentFolderId	Google File ID for parent folder; null creates file
 * 	in default location (My Drive)
 * @param fileTitle	File's Title
 * @param fileDescription	File's description
 * @param googleFileMimeType File's Mime Type
 * @param callback function called when result is success
 * @param completeCallback function called when this AJAX call completes
 */
function createFile(accessToken, parentFolderId, fileTitle, fileDescription, googleFileMimeType, callback, completeCallback) {
	if (!verifyAllArgumentsNotEmpty(accessToken, fileTitle)) {
		return;	// Quick return to simplify code
	}
	// Some credit for making this work belongs to answers at: http://stackoverflow.com/questions/4159701/jquery-posting-valid-json-in-request-body
	var requestData = null;
	if ($.trim(parentFolderId) !== '') {
		requestData = '{ \
			"access_token" : "' + escapeJson(accessToken) + '", \
			"title": "' + escapeJson(fileTitle) + '", \
			"description": "' + escapeJson(fileDescription) + '", \
			"parents" : [{"id":"' + escapeJson(parentFolderId) + '"}], \
			"mimeType": "' + escapeJson(googleFileMimeType) + '" \
		}';
	} else {
		requestData = '{ \
			"access_token" : "' + escapeJson(accessToken) + '", \
			"title": "' + escapeJson(fileTitle) + '", \
			"description": "' + escapeJson(fileDescription) + '", \
			"mimeType": "' + escapeJson(googleFileMimeType) + '" \
		}';
	}
	$.ajax({
		url: _getGoogleDriveUrl(),
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		type : 'POST',
		data : requestData,
		dataType : 'json',
		success: function(data, textStatus, jqXHR) {
			if (typeof(callback) === 'function') {
				callback(data, textStatus, jqXHR);
			}
		},
		complete: function(jqXHR, textStatus) {
			if (typeof(completeCallback) === 'function') {
				completeCallback(jqXHR, textStatus);
			}
		}
	});
}

/**
 * Deletes the given permission for a single file with the given fileId.
 * 
 * @param accessToken	Google Access Token for deleting this permission
 * @param fileId Google File's ID
 * @param permissionId Google Permission's ID (should be permission that applies
 * 	to this File, entered for the file or on of its ancestors)
 * @param callback function called when result is success
 * @param errorCallback function called when request fails
 * @param completeCallback function called when this AJAX call completes
 */
function deleteFilePermission(accessToken, fileId, permissionId, callback, errorCallback, completeCallback) {
	if (!verifyAllArgumentsNotEmpty(accessToken, fileId, permissionId)) {
		return; // Quick return to simplify code
	}
	var url = _getGoogleDriveUrl(fileId, permissionId);
	$.ajax({
		url: url,
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		type : 'DELETE',
		dataType : 'json',
		success: function(data, textStatus, jqXHR) {
			if (typeof(callback) === 'function') {
				callback(data, textStatus, jqXHR);
			}
		},
		error: function(data, textStatus, jqXHR) {
			if (typeof(errorCallback) === 'function') {
				errorCallback(data, textStatus, jqXHR);
			}
		},
		complete: function(jqXHR, textStatus) {
			if (typeof(completeCallback) === 'function') {
				completeCallback(jqXHR, textStatus);
			}
		}
	});
}

/**
 * Gets Google File Object retrieved by AJAX query to get the file by its ID,
 * and gives the file to the given callback.
 * 
 * Using async AJAX with callback, as synchronous AJAX call fails in IE8.
 */
function getDriveFile(accessToken, fileId, callback, completeCallback) {
	// Return if parameters for Google AJAX request are not valid
	if (!verifyAllArgumentsNotEmpty(accessToken, fileId)) {
		return;
	}
	$.ajax({
		async: true,
		timeout: 10000,	// Timeout (in ms) = 10sec
		url: _getGoogleDriveUrl(fileId),
		dataType: 'json',
		data: {
			'access_token' : accessToken
		},
		success: function(data, textStatus, jqXHR) {
			if (typeof(callback) === 'function') {
				callback(data, textStatus, jqXHR);
			}
		},
		complete: function(jqXHR, textStatus) {
			if (typeof(completeCallback) === 'function') {
				completeCallback(jqXHR, textStatus);
			}
		}
	});
}

/**
 * Returns one page of data from Google.  This uses Google paging to continue
 * getting data after Google's initial response.
 * 
 * @param accessToken	Google Access Token for deleting this permission
 * @param pageUrl URL containing data to retrieve from Google
 * 		* this is likely data.nextPage from prior request
 * @param callback function called when result is success
 * @param errorCallback function called when request fails
 * @param completeCallback function called when this AJAX call completes
 */
function getResponsePage(accessToken, pageUrl, callback, errorCallback, completeCallback) {
	$.ajax({
		url: pageUrl,
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		dataType: 'json',
		data: {
			'access_token' : accessToken
		},
		success: function(data, textStatus, jqXHR) {
			if (typeof(callback) === 'function') {
				callback(data, textStatus, jqXHR);
			}
		},
		complete: function(jqXHR, textStatus) {
			if (typeof(completeCallback) === 'function') {
				completeCallback(jqXHR, textStatus, accessToken, query, callback);
			}
		}
	});
}

function listCurrentFilePermissions(accessToken, fileId, fileTitle, callback, completeCallback) {
	if (!verifyAllArgumentsNotEmpty(accessToken, fileId)) {
		return;	// Quick return to simplify code
	}
	var url = _getGoogleDriveUrl(fileId, '');
	url = _keySourceUrl(url, accessToken);
	$.ajax({
		url: url,
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		type : 'GET',
		dataType : 'json',
		success: function(data, textStatus, jqXHR) {
			if (typeof(callback) === 'function') {
				callback(data, textStatus, jqXHR);
			}
		},
		complete: function(jqXHR, textStatus) {
			if (typeof(completeCallback) === 'function') {
				completeCallback(jqXHR, textStatus);
			}
		}
	});
}

/**
 * 
 */
function putDriveFileChanges(accessToken, folderId, requestData, callback, completeCallback)
{
	$.ajax({
		url : _getGoogleDriveUrl(folderId),
		type : 'PUT',
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		dataType : 'json',
		data : requestData,
		success: function(data, textStatus, jqXHR) {
			if (typeof(callback) === 'function') {
				callback(data, textStatus, jqXHR);
			}
		},
		complete: function(jqXHR, textStatus) {
			if (typeof(completeCallback) === 'function') {
				completeCallback(jqXHR, textStatus);
			}
		}
	});
}

/**
 * Calls _queryDriveFiles() with query exactly as written, and does not filter
 * trashed files.  If the given query does not filter trashed files, the results
 * will contain NotTrashed and Trashed files.
 * 
 * See _queryDriveFiles() for parameters.
 */
function queryDriveFilesAll(accessToken, query, callback) {
	_queryDriveFiles(accessToken, query, callback);
}

/**
 * Calls _queryDriveFiles() with query, filtering to get only trashed files.
 * 
 * See _queryDriveFiles() for parameters.
 */
function queryDriveFilesTrashed(accessToken, query, callback) {
	if ($.trim(query) === '') {
		query = 'trashed = true';
	} else {
		query = query + ' AND trashed = true';
	}
	_queryDriveFiles(accessToken, query, callback);
}

/**
 * Calls _queryDriveFiles() with query, filtered to remove trashed files.
 * 
 * See _queryDriveFiles() for parameters.
 */
function queryDriveFilesNotTrashed(accessToken, query, callback) {
	if ($.trim(query) === '') {
		query = 'trashed = false';
	} else {
		query = query + ' AND trashed = false';
	}
	_queryDriveFiles(accessToken, query, callback);
}

/**
 * Saves a current permission's local changes
 * 
 * @param accessToken	Google Access Token for deleting this permission
 * @param fileId Google File's ID
 * @param permissionId Google Permission's ID (should be permission that applies
 * 	to this File, entered for the file or on of its ancestors)
 * @param newRole 'owner', 'writer', or 'reader'
 * @param callback function called when result is success
 * @param errorCallback function called when request fails
 * @param completeCallback function called when this AJAX call completes
 */
function updateFilePermission(accessToken, fileId, permissionId, newRole, callback, errorCallback, completeCallback) {
	var url = _getGoogleDriveUrl(fileId, permissionId)
			+ '?transferOwnership=false';
	url = keySourceUrl(url);
	var requestData = '{ \
			"role" : "' + newRole + '" \
	}';
	$.ajax({
		url: url,
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		type : 'PUT',
		data : requestData,
		dataType : 'json',
		success: function(data, textStatus, jqXHR) {
			if (typeof(callback) === 'function') {
				callback(data, textStatus, jqXHR);
			}
		},
		error: function(data, textStatus, jqXHR) {
			if (typeof(errorCallback) === 'function') {
				errorCallback(data, textStatus, jqXHR);
			}
		},
		complete: function(jqXHR, textStatus) {
			if (typeof(completeCallback) === 'function') {
				completeCallback(jqXHR, textStatus);
			}
		}
	});
}

	// ----------------------------------------------------
	// INNER FUNCTIONS
	// ----------------------------------------------------


/**
 * Updates M+Google menu to show files that were modified after the given time,
 * and not viewed by me since then.
 * 
 * @param accessToken Google Access Token
 * @param query Query to filter files returned
 * @param callback function called when result is success.
 * @param completeCallback function called when call is complete.
 */
function _queryDriveFiles(accessToken, query, callback, completeCallback) {
	// Return if parameters for Google AJAX request are not valid
	if (!verifyAllArgumentsNotEmpty(accessToken)) {
		return;
	}
	$.ajax({
		url: _getGoogleDriveUrl(),
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		dataType: 'json',
		data: {
			'access_token' : accessToken,
			'q':  query,
			// Setting max page results to highest value Google "may" support
			'maxResults' : MAX_RESULTS_PER_PAGE
		},
		success: function(data, textStatus, jqXHR) {
			if (typeof(callback) === 'function') {
				callback(data, textStatus, jqXHR);
			}
		},
		complete: function(jqXHR, textStatus) {
			if (typeof(completeCallback) === 'function') {
				completeCallback(jqXHR, textStatus, accessToken, query, callback);
			}
		}
	});
}

/**
 * Returns Google Drive's URL for accessing and modifying files
 */
function _getGoogleDriveUrl(fileId, permissionId) {
	var result = 'https://www.googleapis.com/drive/v2/files/';
	if ($.trim(fileId) !== '') {
		result = result + fileId;
		// Adding when permissionId is '', so URL is correct for all permissions
		if (permissionId != null) {
			result = result + '/permissions/' + permissionId;
		}
	}
	return result;
}

/**
 * Adds key to url for authentication.
 */
function _keySourceUrl(sourceUrl, accessToken) {
	if ($.trim(accessToken) !== '') {
		if (sourceUrl.indexOf('?') === -1) {
			sourceUrl = sourceUrl
					+ '?access_token=' + accessToken;
		} else {
			sourceUrl = sourceUrl
					+ '&access_token=' + accessToken;
		}
	}
	return sourceUrl
}
