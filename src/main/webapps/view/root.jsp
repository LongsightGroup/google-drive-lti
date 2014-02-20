<%--
This is root file, managing structure of the web pages.  It includes the header,
contents, and footers.
--%>
<%@page isELIgnored="false"%>
<%@page import="edu.umich.its.lti.TcSessionData"%>
<%TcSessionData data = (TcSessionData)request.getAttribute("TcSessionData");
String linkFolderButton =(String)request.getAttribute("linkFolderButton");
String unlinkFolderButton =(String)request.getAttribute("unlinkFolderButton");
String deleteButton =(String)request.getAttribute("deleteButton");
String undoneCopy =(String)request.getAttribute("undoneCopy");
String deleteUndoneFolderCopy =(String)request.getAttribute("deleteUndoneFolderCopy");
String createItemPrompt =(String)request.getAttribute("createItemPrompt");
String createItemPromptError =(String)request.getAttribute("createItemPromptError");
String sendEmailCopy =(String)request.getAttribute("sendEmailCopy");
String buttonYes =(String)request.getAttribute("buttonYes");
String buttonNo =(String)request.getAttribute("buttonNo");
String contextUrl =(String)request.getAttribute("contextUrl");
%>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Bootstrap, from Twitter</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="description" content="">
<meta name="author" content="">
<meta http-equiv="Cache-control" content="no-cache">

<!-- Le styles -->
<link href="bootstrap/css/bootstrap.css" rel="stylesheet">
<link href="bootstrap/css/bootstrap-responsive.css" rel="stylesheet">
<link href="css/google-drive-lti.css" rel="stylesheet">
<script>
      ${requestScope.GoogleDriveConfigJson}
    </script>
<script type="text/javascript">
    var linkFolderButton='<%=linkFolderButton%>';
    var unlinkFolderButton='<%=unlinkFolderButton %>';
    var deleteFolderButton='<%=deleteButton %>';
    var undoneCopy='<%=undoneCopy %>';
    var deleteUndoneFolderCopy='<%=deleteUndoneFolderCopy %>';
    var createItemPrompt='<%=createItemPrompt %>';
    var createItemPromptError='<%=createItemPromptError %>';
    var sendEmailCopy='<%=sendEmailCopy %>';
    var buttonYes='<%=buttonYes %>';
    var buttonNo='<%=buttonNo %>';
    var contextUrl='<%=contextUrl%>';
    </script>
<script type="text/javascript" src="js/jquery.1.10.0.min.js"></script>
<script src="js/jquery-plugin-xdr.js" type="text/javascript"></script>
<script src="js/bootbox.4.1.0.min.js"></script>
<script src="js/utils.js" type="text/javascript"></script>
<script src="js/google-drive-utils.js" type="text/javascript"></script>
<script src="js/googleDriveLti.js" type="text/javascript"></script>
</head>
<body>
	<div class="navbar navbar-inverse navbar-fixed-top">
		<div class="navbar-inner">
			<div class="container-fluid">
				<button type="button" class="btn btn-navbar" data-toggle="collapse"
					data-target=".nav-collapse">
					<span class="icon-bar"></span> <span class="icon-bar"></span> <span
						class="icon-bar"></span>
				</button>
				<a class="brand">${requestScope.jspPage.pageTitle}</a>
				<div class="nav-collapse collapse">
					<p class="navbar-text pull-right">
						<%
	String username = data.getUserNameFull();
	if (username == null) {
		username = data.getUserSourceDid();
	}
	if (username != null) {
%>
						${loggedMsg} <a href="#" class="navbar-link">&nbsp;<%=username%></a>
						<%
	}
%>
					</p>
					<ul class="nav">
						<%
	if (data.getIsInstructor()) {
%>
						<li class="active"><a>${header2}</a></li>
						<%
	}
%>
						<li><a href="#aboutModal"  role="button" data-toggle="modal">${about}</a></li>
						<li><a href="#helpModal"  role="button" data-toggle="modal">${help}</a></li>
					</ul>
				</div>
			</div>
		</div>
	</div>
	<div class="container-fluid">
		<div class="row-fluid">
			<div class="span12">
				<div id="spinner" style="display: none"></div>
				<div class="grey_container">
					<jsp:include page="${requestScope.jspPage.pageFileUrl}"
						flush="false" />
				</div>
			</div>
		</div>
		<footer>
			<%
				String role = data.getUserRoles();
				if (role.equals("Learner")) {
			%>
			<div id="par1" class="hide">${studentAccessMsg}.</div>
			<div id="par2" class="hide alert alert-info">${studentNoFolderAccessMsg}.</div>
			<%} %>
			<div id="par3" class="hide alert alert-error">${invalidAccountMsg}</div>
			<div id="permissionUpdate" class="hide alert alert-error">${permissionUpdate}</div>
		</footer>
	</div>
<script src="bootstrap/js/bootstrap.js"></script>
</body>

<div id="aboutModal" class="modal hide fade" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
  <div class="modal-header">
    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
    <h3 id="myModalLabel">About</h3>
  </div>
  <div class="modal-body">
	<p>Drive LTI provides an easy way to share Google Drive folders and resources with CTools or any other 
		Sakai-based learning management system.  Google Drive folders, subfolders, documents, 
		drawings and other Google items can be shared with all members of a CTools site. Drive 
		LTI automatically accesses the roster of students or participant list in your site.</p>
	<p>Drive LTI, which was developed by the University of Michigan ITS Teaching & Learning Team, 
		uses the IMS Learning Tools Interoperability (LTI) standard. It is licensed under the terms 
		of the <a href="http://opensource.org/licenses/ecl2.php" target="_blank">Educational Community License, version 2.0</a>, 
		so it is free for you to download and run yourself.  Source code is available at: <a href="http://source.sakaiproject.org/viewsvn/umich/google/google-drive-lti/?root=contrib" target="_blank">http://source.sakaiproject.org/viewsvn/umich/google/google-drive-lti/?root=contrib</a></p>

	<h4>Setup Requirements (for Admins)</h4>
	<ul>
		<li>Google Apps for Education instance</li>
		<li>LTI 1.0 compatible LMS</li>
		<li>Java/Tomcat hosting environment</li>
	</ul>
  </div>
  <div class="modal-footer">
    <button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
  </div>
</div>


<div id="helpModal" class="modal hide fade" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true" style="width:80%;margin-left:-40%">
  <div class="modal-header">
    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
    <h3 id="myModalLabel">Using Drive LTI</h3>
  </div>
  <div class="modal-body">
      <h4>Linking to an Existing Google Drive Folder  [this is one time per site]</h4>
      <p>This only needs to be done one time for each site.</p>
      <ol>
          <li>Click <strong>Google Drive</strong> in the left-hand menubar.</li>
          <li>Click the <strong>Share Folder</strong> button to the right of the folder you wish to link.</li>
          <li>Select Email Notification Preference.</li>
          <li>If you would like to send email notifications to all members of your site, click <strong>OK</strong>.  If not, click <strong>Cancel.</strong></li>
      </ol>
      <img class="img-polaroid" src="images/help-share-1.png" alt="Sharing Google Drive Folder" />
      <h4>Linking to a New Google Drive Folder</h4>
      <ol>
          <li>Click <strong>Drive LTI</strong> in the left-hand menubar.</li>
          <li>Select the <strong>Create & Share</strong> Folder button.
              <p>
                  (You can also access this folder in your Google Drive.  Your Google Drive must be open, and you will need to click <strong>Open in Drive</strong> in the upper right-hand corner.)
              </p>
          </li>
          <li>Select Email Notification Preference.</li>
          <li>If you would like to send email notifications to all members of your site, click <strong>OK.</strong>  If not, click <strong>Cancel.</strong></li>
      </ol>
      <img class="img-polaroid" src="images/help-share-2.png" alt="Sharing Google Drive Folder" />
      <h4>Adding a Subfolder</h4>
      <ol>
          <li>In the "Add" dropdown menu next to the folder to which you would like to add a subfolder, select <strong>Add Folder.</strong></li>
          <li>Name the folder.</li>
          <li>Click <strong>OK.</strong></li>
      </ol>
      <img class="img-polaroid" src="images/help-add-3.png" alt="Adding Google Drive Folder" />
      <h4>Adding Documents, Spreadsheets, Presentations, and Drawings</h4>
      <ol>
          <li>In the "Add" dropdown menu next to the folder to which you would like to add an item, select the type (Document, Presentation, etc.).</li>
          <li>Name the item.</li>
          <li>Click <strong>OK.</strong></li>
      </ol>
      <img class="img-polaroid" src="images/help-add-4.png" alt="Adding Documents, Spreadsheets, Presentations, and Drawings" />
      <h4>Delete Folders and Other Items</h4>
      <ol>
          <li>Click the <strong>Delete</strong> button to the right of the item you wish to delete.</li>
          <li>Click <strong>OK</strong> to confirm the deletion.
            <p>NOTE: This cannot be undone. It will delete the item in Google Drive.  Deleting a folder will also delete its contents.s</p>
          </li>
      </ol>
      <img class="img-polaroid" src="images/help-delete-5.png" alt="Delete Folders and Other Items" />
      <h4>Unsharing a Google Drive Folder</h4>
      <p>Click the <strong>Unshare</strong> button to the right of the the [top??] folder</p>
      <p>You will now be able to share a new or existing Google Drive Folder</p>
      <p>NOTE: Clicking <strong>Unshare</strong> unlinks the folder from CTools, but the user will continue to be able to access the folder and its contents directly from the Google Drive until you turn off the sharing in the Google Drive interface.</p>
        <img class="img-polaroid" src="images/help-unshare-6.png" alt="Unsharing a Google Drive Folder" />
  </div>
  <div class="modal-footer">
    <button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
  </div>
</div>


</html>
