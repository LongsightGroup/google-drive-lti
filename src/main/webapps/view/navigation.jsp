    <%@page import="edu.umich.its.lti.TcSessionData"%>
<div class="navbar navbar-inverse navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container-fluid">
          <button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
          <a class="brand" onclick="openPage('Home');">${requestScope.jspPage.pageTitle}</a>
          <div class="nav-collapse collapse">
            <p class="navbar-text pull-right">
<%
	TcSessionData data = (TcSessionData)request.getAttribute("TcSessionData");
	String username = data.getUserNameFull();
	if (username == null) {
		username = data.getUserSourceDid();
	}
	if (username != null) {
%>
              Logged in as <a href="#" class="navbar-link"><%=username%></a>
<%
	}
%>
            </p>
            <ul class="nav">
<%
	if (data.getIsInstructor()) {
%>
              <li class="active"><a onclick="openPage('LinkFolder');">Link Folder</a></li>
<%
	}
%>
              <li><a href="#about">About</a></li>
              <li><a href="#contact">Help</a></li>
            </ul>
          </div><!--/.nav-collapse -->
        </div>
      </div>
    </div>
