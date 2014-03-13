
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
				<button type="button" class="btn" id="searchButton" onclick="searchUnlinkedFoldersOwnedByMe();">${requestScope.search}</button>
			</div>
		</form> 
	</div>
</div>

<div id="alertContainer" class="clearfix"></div>

<table class="table table-striped table-bordered table-hover"
	width="100%">
	<tbody id="FileTreeTableTbody">
	</tbody>
</table>

<script type="text/javascript">
	$(document).ready(function() {
		if (getGoogleAccessToken() === 'ERROR') {
			$('#par3').show();
		}

		var rootFolderId = 'root';  // GD's special ID for root of user's drive
		var foldersOnly = true;
		showLinkedGoogleFolder(rootFolderId, foldersOnly);
		checkBackButtonHit();
	});
</script>