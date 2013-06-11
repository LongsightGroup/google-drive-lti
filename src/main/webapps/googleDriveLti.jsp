<%@page isELIgnored="false" %>
<%@page import="edu.umich.its.util.ServletRequestUtil" %>

<html>
	<head>
		<title>Google Drive LTI ${param.context_title}</title>
		<script src="//ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js" type="text/javascript"></script>
		<script src="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.2/jquery-ui.min.js"></script>
		<link href="//ajax.googleapis.com/ajax/libs/jqueryui/1.10.2/themes/south-street/jquery-ui.min.css" rel="stylesheet" type="text/css"></link>
		<script src="/library/htmlarea/jquery-plugin-xdr.js" type="text/javascript"></script>
		<script src="/google-integration-prototype/google-integration-prototype.js" type="text/javascript"></script>
		<link href="/google-integration-prototype/google-integration-prototype.css" rel="stylesheet" type="text/css"></link>
		<script>
			${requestScope.GoogleDriveConfigJson}
		</script>
		<script src="utils.js" type="text/javascript"></script>
		<script src="google-drive-utils.js" type="text/javascript"></script>
		<script src="googleDriveLti.js" type="text/javascript"></script>
	</head>
	<body style="font-family:sans-serif">
		<h1>Google Drive LTI</h1>
		<div id="DriveFolderController" style="display: none;" class="edged">
			<span id="Welcome" class="heavy"></span>
			<p>
				The course does not have a folder assigned to it.  The
				instructor will create this.
			</p>
			<span id="MessageForFolderSelect" style="display: none;">
				Instructor, please do one of the following:
				<ol>
					<li>
						Select a folder and press "Link Selected Folder" to link
						it for this course
					</li>
					<li>
						Select a folder, and Press "Create New Linked Folder"
						to create a new subfolder linked with the course
					</li>
					<li>
						Press "Create New Linked Folder" without selecting a
						parent folder to create this course's folder directly
						inside "My Drive"
					</li>
				</ol>
			</span>
			<span id="MessageForFolderNoParents" style="display: none;">
				<p>
					Please press "Create New Linked Folder" to create this
					course's folder directly inside "My Drive".
				</p>
			</span>
		</div>
		<div id="DriveViewDiv" class="edged driveFileListsDiv">
			<ul id="GoogleDriveTopList" class="driveViewTopList"></ul>
			<div id="DriveFolderSelectionButtonsDiv" style="display: none;">
				<button type="button" onclick="clearFolderSelection();" disabled="disabled" class="actingOnSelectedFolder">Clear Selection</button>
				<button type="button" onclick="assignSelectedFolder();" disabled="disabled" class="actingOnSelectedFolder">
					Link Selected Folder
				</button>
				<button type="button" onclick="assignNewFolder();">
					Create New Linked Folder
				</button>
			</div>
		</div>
<%
	// Only show parameters when request includes "custom_show_parameters=true"
	if ("true".equalsIgnoreCase(request.getParameter("custom_show_parameters")))
	{
%>
		<div class="edged">
			<p>
				Parameters Sent to LTI (this is improper to share in production): 
				<button type="button" onclick="$('#ParamTable').toggle();">Show/Hide</button>
			</p>
			<%=ServletRequestUtil.showParametersInTable(request, " id=\"ParamTable\" style=\"display: none;\"")%>
			<br clear="both"/>
		</div>
<%
	}
%>
		<%-- Include Roster HTML Form (if it exists) --%>
		${requestScope.RosterHtmlForm}
<!-- Wrapper containing menus; those are not to be shown directly on the page -->
<div style="">
			<div id="JqueryDialogRoot" style="display: none;"></div>
			<div id="JqueryCreateFileDialogRoot" style="display: none;">
				<input name="fileMimeType" type="hidden"/>
				<input name="parentFolderId" type="hidden"/>
				<div class="field">
					<label class="heavy">Title:</label>
					<input type="text" name="title" class="fieldInput"/>
				</div>
				<div class="field">
					<label class="heavy">Description:</label>
					<input type="text" name="description" class="fieldInput"/>
				</div>
			</div>
			<div id="JqueryEditPermissionsDialogRoot" style="display: none;">
				<input name="fileId" type="hidden"/>
				<input name="fileTitle" type="hidden"/>
				<div class="edged">
					<h3>Add Permission:</h3>
					<!-- permissionType can be "user", "group", "domain", "anyone" -->
					<input name="permissionType" type="hidden" value="user"/>
					<div class="field">
						<label class="heavy">Email Address:</label>
						<input type="text" name="permissionValue" class="fieldInput"/>
					</div>
					<div class="field">
						<label class="heavy">Access:</label>
						<select id="PermissionRole">
							<option value="owner">Is owner</option>
							<option value="writer">Can edit</option>
							<option value="reader" selected="selected">Can view</option>
						</select>
					</div>
					<button type="button" onclick="addPermission();">Add</button>
				</div>
				<div class="edged">
					<h3>Current Permissions:</h3>
					<div>
						<table class="currentPermissionsTable">
							<tbody>
							</tbody>
						</table>
					</div>
				</div>
			</div>
<%
	// Only show parameters when request includes "custom_show_parameters=true"
	if ("true".equalsIgnoreCase(String.valueOf(request.getAttribute("Instructor"))))
	{
		// Include menus for the Instructor
%>
			<div id="FolderPopupMenu" class="popupMenu">
				<ul>
					<li><a href="#" onclick="popupCancel(this);">Cancel</a></li>
					<li><hr/></li>
					<li><a href="#" onclick="popupEditPermissions(this);">Edit Permissions</a></li>
					<li><a href="#" onclick="popupOpenFile(this, false);">Open This Folder</a></li>
					<li><hr/></li>
					<li><a href="#" onclick="popupCreateFile(this, 'folder');">Create Folder</a></li>
					<li><a href="#" onclick="popupCreateFile(this, 'document');">Create Document</a></li>
					<li><a href="#" onclick="popupCreateFile(this, 'presentation');">Create Presentation</a></li>
					<li><a href="#" onclick="popupCreateFile(this, 'spreadsheet');">Create Spreadsheet</a></li>
					<!-- * did not work: Google Error: "Bad Request" * li href="#" onclick="popupCreateFile('form');">Create Form</li -->
					<li><a href="#" onclick="popupCreateFile('drawing');">Create Drawing</a></li>
					<!-- * did not work: Google Error: "Bad Request" * li href="#" onclick="popupCreateFile('script');">Create Script</li -->
				</ul>
			</div>
			<div id="FilePopupMenu" class="popupMenu">
				<ul>
					<li><a href="#" onclick="popupCancel(this);">Cancel</a></li>
					<li><hr/></li>
					<li><a href="#" onclick="popupEditPermissions(this);">Edit Permissions</a></li>
					<li><a href="#" onclick="popupOpenFile(this, false);">Open This File</a></li>
				</ul>
			</div>
<%
	} else {
		// Include menus for people other than the Instructor
%>
			<div id="FolderPopupMenu" class="popupMenu">
				<ul>
					<li><a href="#" onclick="popupCancel(this);">Cancel</a></li>
					<li><hr/></li>
					<li><a href="#" onclick="popupOpenFile(this, false);">Open This Folder</a></li>
				</ul>
			</div>
			<div id="FilePopupMenu" class="popupMenu">
				<ul>
					<li><a href="#" onclick="popupCancel(this);">Cancel</a></li>
					<li><hr/></li>
					<li><a href="#" onclick="popupOpenFile(this, false);">Open This File</a></li>
				</ul>
			</div>
<%
	}
%>
</div>
	</body>
</html>
