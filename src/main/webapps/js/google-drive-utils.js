if (typeof(verifyAllArgumentsNotEmpty) === 'undefined') {
	if (getHasConsoleLogFunction()) {
		console.log('WARNING: google-drive-utils.js relies upon sibling library utils.js');
	}
}

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

	var requestData = {
		'access_token' : accessToken,
		'description' : fileDescription,
		'mimeType' : googleFileMimeType,
		'title' : fileTitle,
	};

	if ($.trim(parentFolderId) !== '') {
		requestData['parents'] = [ {
			'id' : parentFolderId,
		} ];
	}

	$.ajax({
		url: _getGoogleDriveUrl(),
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Content-Type', 'application/json');
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		type : 'POST',
		data : JSON.stringify(requestData),
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
 * Gets Google File Object retrieved by AJAX query to get the file by its ID,
 * and gives the file to the given callback.
 * 
 * Using async AJAX with callback, as synchronous AJAX call fails in IE8.
 */
function deleteDriveFile(accessToken, fileId, callback, errorCallback, completeCallback) {
	// Return if parameters for Google AJAX request are not valid
	if (!verifyAllArgumentsNotEmpty(accessToken, fileId)) {
		return;
	}
	$.ajax({
		timeout: 10000,	// Timeout (in ms) = 10sec
		url: _getGoogleDriveUrl(fileId),
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		type: 'DELETE',
		dataType: 'json',
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
function getDriveFile(accessToken, fileId, callback, errorCallback, completeCallback) {
	// Return if parameters for Google AJAX request are not valid
	if (!verifyAllArgumentsNotEmpty(accessToken, fileId)) {
		return null;
	}
	
	var ajaxRequest = $.ajax({
		timeout: 10000,	// Timeout (in ms) = 10sec
		url: _getGoogleDriveUrl(fileId),
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		type: 'GET',
		dataType: 'json',
		cache:false});
	
	
	if (typeof(callback) === 'function') {
	    ajaxRequest.done(
	            function(data, textStatus, jqXHR) {
	                callback(data, textStatus, jqXHR);
	            });
	}

	if (typeof(errorCallback) === 'function') {
		ajaxRequest.fail(
		        function(jqXHR, textStatus, errorThrown) {
		            errorCallback(jqXHR, textStatus, errorThrown);
		        });
	}

    if (typeof (completeCallback) === 'function') {
        ajaxRequest.always(
                function(jqXHR, textStatus) {
                    completeCallback(jqXHR, textStatus);
                });
    }
    
    return ajaxRequest;
}

function getIsFolder(fileMimeType) {
	return (fileMimeType === 'application/vnd.google-apps.folder');
}

function getItemTypeFromMimeType(itemMimeType) {
	return (itemMimeType + '').replace('application/vnd.google-apps.','');
}

/**
 * Returns one page of data from Google.  This uses Google paging to continue
 * getting data after Google's initial response.
 * 
 * @param accessToken	Google Access Token for deleting this permission
 * @param pageUrl URL containing data to retrieve from Google
 * 		* this is likely data.nextLink from prior request
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
 * Returns list of Google File Object's parents retrieved by AJAX query to list
 * from the child file's ID, and gives the data to the given callback.
 * 
 * Using async AJAX with callback, as synchronous AJAX call fails in IE8.
 */
function listDriveFileParents(accessToken, fileId, callback, errorCallback, completeCallback) {
	// Return if parameters for Google AJAX request are not valid
	if (!verifyAllArgumentsNotEmpty(accessToken, fileId)) {
		return;
	}
	$.ajax({
		timeout: 10000,	// Timeout (in ms) = 10sec
		url: _getGoogleDriveUrl(fileId) +'/parents',
		beforeSend: function(xhr) {
			xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
		},
		type: 'GET',
		dataType: 'json',
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
 * This will send given updates to a Google file (was used by googleDriveLti.js
 * v83984 2013-06-14, to modify description of a folder to include site's ID, to
 * persist link of Google folders with TC sites in that way).
 * 
 * Example requestData JSON:
 * <pre>
 * 	{ 
 * 		"description" : "new description"
 * 	}
 * </pre>
 */
function putDriveFileChanges(accessToken, fileId, requestData, callback, completeCallback)
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
    var completeQuery = 'trashed = false';
    
	if ($.trim(query) !== '') {
	    completeQuery += ' and ' + query;
	}
	
	_queryDriveFiles(accessToken, completeQuery, callback);
}

/**
 * Returns array of files sorted by title (case-insensitive).
 * 
 * @param files Array of files to sort
 * @returns {Array}
 */
function sortFilesByTitle(files) {
	var result = [];
	if (files) {
		for (var fileIdx in files) {
			var file = files[fileIdx];
			var added = false;
			for (var resultIdx in result) {
				var resultFile = result[resultIdx];
				if (file.title.toLowerCase() <= resultFile.title.toLowerCase()) {
					result.splice(resultIdx, 0, file);
					added = true;
					break;
				}
			}
			if (!added) {
				result.push(file);
			}
		}
	}
	return result;
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
	var hostName = '../'; // relative path to proxy
	var googleDriveUrl = hostName + contextUrl + '/drive/v2/files/';
	
	if ($.trim(fileId) !== '') {
		googleDriveUrl += fileId;
		
		// Adding when permissionId is '', so URL is correct for all permissions
		if (permissionId != null) {
			googleDriveUrl += '/permissions/' + permissionId;
		}
	}
	return googleDriveUrl;
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
