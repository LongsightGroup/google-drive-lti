/*
 * Author: Raymond Louis Naseef
 */


function escapeJson(value) {
	return (value == null) ? '' : value.replace(/"/g,'\\"');
}

function escapeUrl(value) {
	return encodeURIComponent(value);
}

function logError(message, textStatus, jqXHR, debugMode) {
	if ((typeof(console) !== 'undefined') && (typeof(console.log) === 'function')) {
		console.log(message + ': ' + textStatus);
	} else if (debugMode == true) {
		alert(message + ': ' + textStatus);
	}
}

/**
 * Checks if arguments are not empty, used by callers to ensure values they
 * require are not undefined, null, or trim to "".
 * 
 * This is critical to avoid sending request to Google with bad access token, as
 * Google will ask the user to authenticate.
 */
function verifyAllArgumentsNotEmpty() {
	var result = true;
	for (var argIdx = 0; result && (argIdx < arguments.length); argIdx++) {
		result = ($.trim(arguments[argIdx]) !== '');
	}
	return result;
}
