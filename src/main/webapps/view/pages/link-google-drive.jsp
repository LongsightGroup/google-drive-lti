
<div class="clearfix">
	<div class="pull-left">
		<h3>${requestScope.jspPage.pageTitle}</h3>
	</div>
	<div class="pull-right">
		<br> <a href="#" class="btn btn-primary btn-small"
			onclick="assignNewFolder(); return false;">${createAndLinkButton}</a>
	</div>
</div>

<div class="clearfix">
	<div class="pull-left">
		<p class="muted">${linkingViewInfo}</p>
	</div>
	<div class="pull-right hidden-phone hidden-xs">
		<form class="form-search" onsubmit="return false;"> 
			<div class="input-append" style="margin-right: 73px">
				<input id="fileTree_search" type="text" class="span12 search-query">
				<button type="button" class="btn" id="fileTree_searchButton">${requestScope.search}</button>
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
