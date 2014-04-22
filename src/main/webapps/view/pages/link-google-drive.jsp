
<div class="clearfix">
	<div class="pull-left">
		<h3>${requestScope.jspPage.pageTitle}</h3>
	</div>
	<div class="pull-right">
		<br> <a href="#" class="btn btn-primary btn-small"
			onclick="assignNewFolder();">${createAndLinkButton}</a>
	</div>
</div>

<div class="clearfix">
	<div class="pull-left">
		<p class="muted">${linkingViewInfo}</p>
	</div>
	<div class="pull-right">
		<form class="form-search" onsubmit="return false;"> 
			<div class="input-append" style="margin-right: 73px">
				<input id="UnlinkedFolderSearchInput" type="text" class="span12 search-query" onKeyUp="return AutoClick(event);">
				<button type="button" class="btn" id="searchButton" onclick="searchItems();">${requestScope.search}</button>
			</div>
		</form> 
	</div>
</div>

<div id="alertContainer" class="clearfix"></div>

<!--  button id="removeChildren">Remove Children</button  -->

<div id="jstree" class="unsharedView"></div>

<!-- <table class="table table-striped table-bordered table-hover"
	width="100%">
	<tbody id="FileTreeTableTbody">
	</tbody>
</table> -->

<script type="text/javascript">
	$(document).ready(function() {
		if (getGoogleAccessToken() === 'ERROR') {
			$('#par3').show();
		}

		// A null folder ID refers to the user's GD root folder
		var parentFolderId = null;
		var linkedFolderId = null;
		var depth = 0;
		var foldersOnly = true;

		initializeFileTree('#jstree', {
			'onlyOwnedFolders' : true
		});
		//getFoldersChildren(parentFolderId, linkedFolderId, depth, foldersOnly);

		// TODO: remove call to searchItems() after file tree is obsolete
		searchItems();
		checkBackButtonHit();

	});
</script>
