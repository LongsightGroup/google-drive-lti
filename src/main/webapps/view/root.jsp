<%--
This is root file, managing structure of the web pages.  It includes the header,
contents, and footers.
--%>
<%@page isELIgnored="false"%>
<%@page import="edu.umich.its.lti.TcSessionData"%>
<%@page import="edu.umich.its.google.oauth.GoogleAccessToken"%>
<%TcSessionData data = (TcSessionData)request.getAttribute("TcSessionData");
String linkFolderButton =(String)request.getAttribute("linkFolderButton");
String unlinkFolderButton =(String)request.getAttribute("unlinkFolderButton");
String unlinkFolderAlert =(String)request.getAttribute("unlinkFolderAlert");
String linkFolderAlert =(String)request.getAttribute("linkFolderAlert");
String deleteButton =(String)request.getAttribute("deleteButton");
String deleteFilePrompt =(String)request.getAttribute("deleteFilePrompt");
String deleteFolderPrompt =(String)request.getAttribute("deleteFolderPrompt");
String deleteItemPromptHeader =(String)request.getAttribute("deleteItemPromptHeader");
String createItemPromptHeader =(String)request.getAttribute("createItemPromptHeader");
String createItemPrompt =(String)request.getAttribute("createItemPrompt");
String createItemPromptError =(String)request.getAttribute("createItemPromptError");
String createItemAlert =(String)request.getAttribute("createItemAlert");
String deleteItemAlert =(String)request.getAttribute("deleteItemAlert");
String linkFolderErrorAlert =(String)request.getAttribute("linkFolderErrorAlert");
String sendEmailPrompt =(String)request.getAttribute("sendEmailPrompt");
String sendEmailPromptHeader =(String)request.getAttribute("sendEmailPromptHeader");
String buttonYes =(String)request.getAttribute("buttonYes");
String buttonNo =(String)request.getAttribute("buttonNo");
String buttonOk =(String)request.getAttribute("buttonOk");
String buttonCreate =(String)request.getAttribute("buttonCreate");
String buttonDelete =(String)request.getAttribute("buttonDelete");
String contextUrl =(String)request.getAttribute("contextUrl");
String userEmailAddress =(String)request.getAttribute("userEmailAddress");
String deleteFileErrorAlert =(String)request.getAttribute("deleteFileErrorAlert");
String deleteFolderErrorAlert =(String)request.getAttribute("deleteFolderErrorAlert");
String errorMsg404 =(String)request.getAttribute("errorMsg404");
String errorMessageIe8 =(String)request.getAttribute("errorMessageIe8");
String contextLabel =(String)request.getAttribute("contextLabel");
String token=null;
Long tokenTimeStamp=null;
GoogleAccessToken accessToken=(GoogleAccessToken)request.getSession().getAttribute("accessToken");
if(accessToken!=null){
 token=accessToken.getToken();
 tokenTimeStamp=accessToken.getTimeTokenCreated();
} 
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
    var linkFolderAlert='<%=linkFolderAlert %>';
    var unlinkFolderAlert='<%=unlinkFolderAlert %>';
    var deleteFolderButton='<%=deleteButton %>';
    var deleteFilePrompt='<%=deleteFilePrompt %>';
    var deleteFolderPrompt='<%=deleteFolderPrompt %>';
    var deleteItemPromptHeader='<%=deleteItemPromptHeader %>';
    var createItemPromptHeader='<%=createItemPromptHeader %>';
    var createItemPrompt='<%=createItemPrompt %>';
    var createItemPromptError='<%=createItemPromptError %>';
    var createItemAlert='<%=createItemAlert %>';
    var deleteItemAlert='<%=deleteItemAlert %>';
    var linkFolderErrorAlert='<%=linkFolderErrorAlert %>';
    var sendEmailPromptHeader='<%=sendEmailPromptHeader %>';
    var sendEmailPrompt='<%=sendEmailPrompt %>';
    var buttonYes='<%=buttonYes %>';
    var buttonNo='<%=buttonNo %>';
    var buttonOk='<%=buttonOk %>';
    var buttonCreate='<%=buttonCreate %>';
    var buttonDelete='<%=buttonDelete %>';
    var contextUrl='<%=contextUrl%>';
    var userEmailAddress='<%=userEmailAddress%>';
    var deleteFileErrorAlert='<%=deleteFileErrorAlert %>';
    var deleteFolderErrorAlert='<%=deleteFolderErrorAlert %>';
    var accessTokenHandler='<%=token %>';
    var accessTokenTime='<%=tokenTimeStamp %>';
    </script>
<script type="text/javascript" src="js/jquery.1.10.0.min.js"></script>
<script src="js/jquery-plugin-xdr.js" type="text/javascript"></script>
<script src="js/bootbox.4.1.0.min.js"></script>
<script src="js/underscore-min.js"></script>

<link rel="stylesheet" href="jstree/themes/default/style.min.css" />
<script type="text/javascript" src="jstree/jstree.min.js"></script>

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
				<a class="brand"><%=contextLabel%></a>
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
						<li><a href="view/pages/help/about.html" target="GDLHelp">${about}</a></li>
						<li><a href="view/pages/help/help.html" target="GDLHelp">${help}</a></li>
					</ul>
				</div>
			</div>
		</div>
	</div>
	<div class="container-fluid">
		<div class="row-fluid">
			<div class="span12">
				<div id="spinner" style="display: none"></div>
				<!--[if IE 8]><div class="alert alert-error">${errorMessageIe8}</div><![endif]-->
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
			<div id="par4" class="hide alert alert-error">${errorMsg404}</div>
			<div id="permissionUpdate" class="hide alert alert-error">${permissionUpdate}</div>
			<div id="par5"></div>
		</footer>
	</div>
<script src="bootstrap/js/bootstrap.js"></script>
</body>
</html>
