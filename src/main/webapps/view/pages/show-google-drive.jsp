<%@page import="edu.umich.its.lti.TcSessionData"%>
<div style="display: none;">
  <div id="FolderDropdownTemplate">
       <div class="dropdown">
         <a class="dropdown-toggle btn btn-small" id="dLabel" role="button" data-toggle="dropdown" data-target="#" href="/page.html">Add <b class="caret"></b>
         </a>
         <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
<%
	// This is list of each file types as it appears on the page, and is also
	// appended to Google MIME type
	// See comments on openDialogToCreateFile() below
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
				openDialogToCreateFile() Parameters:

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
           <li><a tabindex="-1" onclick="openDialogToCreateFile('<%=fileTypeLowerCase%>', '[FolderIdParam]', '[LinkedFolderIdParam]', [FolderDepthParam]);">Add <%=fileType%></a></li>
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
        <div class="span10">
            <h3>${requestScope.jspPage.pageTitle    }</h3>
            <p class="muted">Share Google Files With Your Site</p>
        </div>
        <div class="span2 controls" >
            <br>
            <a class="btn btn-primary btn-small" onclick="openPage('LinkFolder');">Link Google Folder</a>
        </div>
    </div>
<%
    } else {
%>
            <h3>Google Drive items shared with this site</h3>

<%
    }
%>
            
             <table class="table table-striped table-bordered table-hover" width="100%">
              <tbody id="FileTreeTableTbody">
             </tbody>
            </table>
      <div id="spinner" style="display:none"></div>  
<script type="text/javascript">

// On startup: display Google resources on the screen
$(document).ready(function() {
    /*
     show spinner whenever async actvity takes place
     */
    $(document).ajaxStart(function(){
        $('#spinner').show();
    });
    $(document).ajaxStop(function(){
        $('#spinner').hide();
    });
	showLinkedGoogleFolders();
});

</script>

