<%@page isELIgnored="false" %>

  <body>

    <%@include file="navigation.jsp" %>

    <div class="container-fluid">
      <div class="row-fluid">
        <div class="span12">
          <div class="grey_container">
          	<h2>Link Google Drive</h2>
          	<p>Share Google Files With Your Site</p>

          	<jsp:include page="${requestScope.pageFile}" flush="false"/>
          </div>  <!-- .grey_container -->
        </div>  <!-- .span12 -->
      </div>  <!-- .row_fluid -->
      <jsp:include page="content-footer.jsp" flush="false"/>
    </div>  <!-- .container_fluid -->

  </body>