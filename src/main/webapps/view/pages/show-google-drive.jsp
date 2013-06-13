<div style="display: none;">
  <div id="FolderDropdownTemplate">
       <div class="dropdown">
         <a class="dropdown-toggle btn btn-primary btn-small" id="dLabel" role="button" data-toggle="dropdown" data-target="#" href="/page.html">Add<b class="caret"></b>
         </a>
         <ul class="dropdown-menu" role="menu" aria-labelledby="dLabel">
           <li><a tabindex="-1" onclick="openDialogToCreateFile('folder', '[FolderId]', '[LinkedFolderId]', [FolderDepth]);">Add Folder</a></li>
           <li class="divider"></li>
           <li><a tabindex="-1" onclick="openDialogToCreateFile('document', '[FolderId]', '[LinkedFolderId]', [FolderDepth]);">Add Document</a></li>
           <li><a tabindex="-1" onclick="openDialogToCreateFile('presentation', '[FolderId]', '[LinkedFolderId]', [FolderDepth]);">Add Presentation</a></li>
           <li><a tabindex="-1" onclick="openDialogToCreateFile('spreadsheet', '[FolderId]', '[LinkedFolderId]', [FolderDepth]);">Add Spreadsheet</a></li>
           <li><a tabindex="-1" onclick="openDialogToCreateFile('drawing', '[FolderId]', '[LinkedFolderId]', [FolderDepth]);">Add Drawing</a></li>
         </ul>
       </div>
  </div>
</div>
            <p><a href="#" class="btn btn-primary btn-large" onclick="openPage('linkFolder');">Link Google Folder</a></p>
            <hr />
            
             <table class="table table-striped table-bordered table-hover" width="100%">
              <tbody id="FileTreeTableTbody">
             </tbody>
            </table>
           
      <hr>
	  <br />
<script type="text/javascript">

// On startup: display Google resources on the screen
$(document).ready(function() {
	showLinkedGoogleFolders();
});

</script>

