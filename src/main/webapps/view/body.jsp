<%@page isELIgnored="false" %>

  <body>

    <%@include file="navigation.jsp" %>

    <div class="container-fluid">
      <div class="row-fluid">
        <div class="span12">
          <div class="grey_container">
          	<h2>${requestScope.jspPage.pageTitle}</h2>
          	<p>Share Google Files With Your Site</p>

          	<jsp:include page="${requestScope.jspPage.pageFileUrl}" flush="false"/>
          </div>  <!-- .grey_container -->
        </div>  <!-- .span12 -->
      </div>  <!-- .row_fluid -->
      <jsp:include page="content-footer.jsp" flush="false"/>
    </div>  <!-- .container_fluid -->

  </body>