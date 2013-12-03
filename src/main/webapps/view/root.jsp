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
String deleteFileFolderCopy =(String)request.getAttribute("deleteFileFolderCopy");
String createFolderCopy =(String)request.getAttribute("createFolderCopy");
String createFileCopy =(String)request.getAttribute("createFileCopy");
String sendEmailCopy =(String)request.getAttribute("sendEmailCopy");
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
    var deleteFileFolderCopy='<%=deleteFileFolderCopy %>';
    var createFolderCopy='<%=createFolderCopy %>';
    var createFileCopy='<%=createFileCopy %>';
    var sendEmailCopy='<%=sendEmailCopy %>';
    
    </script>
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min.js"
	type="text/javascript"></script>
<script src="js/jquery-plugin-xdr.js" type="text/javascript"></script>
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
						<li><a href="#about">${about}</a></li>
						<li><a href="#contact">${help}</a></li>
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
		<%
     String role= data.getUserRoles();
      %>
		<%if(role.equals("Learner")){%>
		<footer>
			<p>${studentAccessMsg}.</p>
		</footer>
		<%} %>
	</div>
	<script src="bootstrap/js/bootstrap.js"></script>
</body>
</html>