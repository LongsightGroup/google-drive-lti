<%--
This is root file, managing structure of the web pages.  It includes the header,
contents, and footers.
--%>
<%@page import="java.util.Map"%>
<%@page isELIgnored="false"%>
<%@page import="edu.umich.its.lti.TcSessionData"%>
<%@page import="edu.umich.its.google.oauth.GoogleAccessToken"%>
<%TcSessionData data = (TcSessionData)request.getAttribute("TcSessionData");

Map<String, String> applicationProperties =(Map<String, String>)request.getAttribute("applicationProperties");
String applicationPropertiesJson =(String)request.getAttribute("applicationPropertiesJson");
String contextUrl =(String)request.getAttribute("contextUrl");
String userEmailAddress =(String)request.getAttribute("userEmailAddress");
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
    var contextUrl='<%=contextUrl%>';
    var userEmailAddress='<%=userEmailAddress%>';
    var accessTokenHandler='<%=token %>';
    var accessTokenTime='<%=tokenTimeStamp %>';
    var applicationProperties = <%=applicationPropertiesJson %>;
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
	<div class="navbar navbar-inverse">
		<div class="navbar-inner">
			<button type="button" class="btn btn-navbar" data-toggle="collapse"
				data-target=".nav-collapse">
				<span class="icon-bar"></span> <span class="icon-bar"></span> <span
					class="icon-bar"></span>
			</button>
			<a class="brand"><%=contextLabel%></a>
			<div class="nav-collapse collapse">
				<p class="navbar-text pull-right visible-desktop">
					<%
String username = data.getUserNameFull();
if (username == null) {
	username = data.getUserSourceDid();
}
if (username != null) {
%>
						<%=applicationProperties.get("gd.logged.in") %> <a href="#" class="navbar-link"><%=username %></a>
						<%
	}
%>
					</p>
					<ul class="nav">
						<li><a href="view/pages/help/about.html" target="GDLHelp"><%=applicationProperties.get("gd.header3.about") %></a></li>
						<li><a href="view/pages/help/help.html" target="GDLHelp"><%=applicationProperties.get("gd.header4.help") %></a></li>
					</ul>
				</div>
			</div>
		</div>
	</div>
	<div class="container-fluid">
		<div class="row-fluid">
			<div class="span12">
				<div id="spinner" style="display: none"></div>
				<!--[if IE 8]><div class="alert alert-error"><%=applicationProperties.get("gd.error.message.ie8") %></div><![endif]-->
				<div class="grey_container">
					<p class="sr-only"><%=applicationProperties.get("gd.screenReader.help.keyboardNavigation") %></p>
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
			<div id="par1" class="hide"><%=applicationProperties.get("gd.student.view.access.msg") %></div>
			<div id="par2" class="hide alert alert-info"><%=applicationProperties.get("gd.student.view.nofolder.message") %></div>
			<%} %>
			<div id="par3" class="hide alert alert-error"><%=applicationProperties.get("gd.invalid.account.message") %></div>
			<div id="par4" class="hide alert alert-error"><%=applicationProperties.get("gd.error.msg.404") %></div>
			<div id="permissionUpdate" class="hide alert alert-error"><%=applicationProperties.get("gd.student.view.permission.update") %></div>
			<div id="par5"></div>
		</footer>
	</div>
<script src="bootstrap/js/bootstrap.js"></script>
</body>
</html>
