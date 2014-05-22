<%@page import="edu.umich.its.lti.TcSessionData"%>

<div style="display: none;">
	<div id="FolderDropdownTemplate">
		<div class="dropdown">
			<a class="dropdown-toggle btn btn-xs btn-mini" id="dLabel" role="button"
				data-toggle="dropdown" data-target="#" href="/page.html">${addButton}
				<b class="caret"></b>
			</a>
			<ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
				<%
	// This is list of each file types as it appears on the page, and is also
	// appended to Google MIME type
	// See comments on openDialogToCreateItem() below
	// * '-' is special entry that creates an <li> with a horizontal line
	String[] fileTypes = new String[] {
		"Folder",
		"-",
		"Document",
		"Presentation",
		"Spreadsheet",
		"Drawing"
	};
	for (int fileTypeIdx = 0; fileTypeIdx < fileTypes.length; fileTypeIdx++) {
		String fileType = fileTypes[fileTypeIdx];
		if ("-".equals(fileType)) {
%>
				<li class="divider"></li>
				<%
		} else {
			String fileTypeLowerCase = fileType.toLowerCase();
			/*
				openDialogToCreateItem() Parameters:

				1 - Suffix of the file's Google MIME type appended to 'application/vnd.google-apps.'
				    * folder => application/vnd.google-apps.folder
				    * document => application/vnd.google-apps.document
				2 - [FolderIdParam] is folder that will contain the new file
				3 - [LinkedFolderIdParam] is the linked (root) folder the entire
				    tree of contents on the page belong to.
				4 - [FolderDepthParam] is number of parents from the new file's
				    parent to the linked folder
			*/
%>
				<li><a href="#" tabindex="-1"
					onclick="openDialogToCreateItem('<%=fileType%>', '[FolderIdParam]', '[LinkedFolderIdParam]', [FolderDepthParam]);">${addButton}
						<%=fileType%></a></li>
				<%
		}
	}
%>
			</ul>
		</div>
	</div>
</div>
<%
	if (((TcSessionData)request.getAttribute("TcSessionData")).getIsInstructor()) {
%>
<div class="row-fluid header-controls">
	<h2>${requestScope.jspPage.pageTitle}</h2>
	<p class="muted pull-left">${requestScope.info}</p>
	<p class="pull-right "></p>
</div>
<%
    } else {
%>
<h3>${studentInfo}</h3>

<%
    }
%>

<div id="alertContainer" class="clearfix"></div>

<div id="fileTree" class="showGoogleDrive"></div>

<script type="text/javascript">
	$(document).ready(function() {
		if (getGoogleAccessToken() === 'null') {
			$('#par3').show();
		} else {
			var sharedFolderIds = getConfigLinkedFolders();

			if (sharedFolderIds && sharedFolderIds.length > 0) {
				$('#par1').show();
				initializeFileTree('#fileTree', {
					'folderId' : sharedFolderIds[0]
				});
			} else {
				$('#par2').show();
			}
		}
	});
</script>
