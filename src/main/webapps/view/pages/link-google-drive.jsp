<%@page import="java.util.Map"%>
<%Map<String, String> applicationProperties =(Map<String, String>)request.getAttribute("applicationProperties"); %>

<div class="clearfix">
	<div class="pull-left">
		<h3>${requestScope.jspPage.pageTitle}</h3>
	</div>
	<div class="pull-right">
		<br> <a href="#" class="btn btn-primary btn-small"
			onclick="assignNewFolder(); return false;"><%=applicationProperties.get("gd.create.link.button") %></a>
	</div>
</div>

<div class="clearfix">
	<div class="pull-left">
		<p class="muted"><%=applicationProperties.get("gd.linking.view.info") %></p>
	</div>
	<div class="pull-right hidden-phone hidden-xs">
		<form class="form-search" onsubmit="return false;"> 
			<div class="input-append" style="margin-right: 73px">
                <label for="fileTree_search" class="sr-only"><%=applicationProperties.get("gd.search") %></label>
				<input id="fileTree_search" type="text" class="span12 search-query">
				<button type="button" class="btn" id="fileTree_searchButton"><%=applicationProperties.get("gd.search") %></button>
			</div>
		</form> 
	</div>
</div>

<div id="alertContainer" class="clearfix"></div>

<div id="fileTree"></div>

<script type="text/javascript">
	$(document).ready(function() {
		if (getGoogleAccessToken() === 'null') {
			$('#par3').show();
		}else{

		initializeFileTree('#fileTree', {
			'onlyOwnedFolders' : true
		});

		checkBackButtonHit();
		}
	});
</script>
