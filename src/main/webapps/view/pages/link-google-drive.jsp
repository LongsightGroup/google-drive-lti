            <p>
            	<a href="#" class="btn btn-primary btn-large" onclick="assignNewFolder();">New Google Folder</a>
            	<a class="btn btn-primary btn-large" onclick="openPage('Home');">Back</a>
            </p>
            
 			<p class="large-text">or</p>
            
            <h3>Select a folder below to link</h3>
           
            <div class="row">
              <div class="span5 offset1">
              <br />
              	<form class="form-search">
                  <div class="input-append">
                    <input id="UnlinkedFolderSearchInput" type="text" class="span12 search-query">
                    <button type="button" class="btn" onclick="searchUnlinkedFoldersOwnedByMe();">Search</button>
                  </div>
                </form>
              </div>
              <!-- TODO: Remove this div : page layout will need to be fixed -->
              <div class="span5 offset1" style="visibility: hidden;">
              	<div class="pagination">
                  <ul>
                    <li><a href="#">Prev</a></li>
                    <li><a href="#">1</a></li>
                    <li><a href="#">2</a></li>
                    <li><a href="#">3</a></li>
                    <li><a href="#">Next</a></li>
                  </ul>
            	</div>
              </div>
            </div>

            
             <table class="table table-striped table-bordered table-hover" width="100%">
               <tbody id="LinkFolderTableTbody">
               </tbody>
            </table>
           

<script type="text/javascript">

	$(document).ready(function() {
		getAncestorsForLinkedFolders();
		searchUnlinkedFoldersOwnedByMe();
		$(window).scroll(function(e) {
			handleNeedToGetMoreUnlinkedFolders();
		});
	});

</script>