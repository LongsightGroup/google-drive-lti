            <div class="clearfix">
                <div class="pull-left">
                    <h3>${requestScope.jspPage.pageTitle}</h3>
                </div>
                <div class="pull-right">
                    <br>
                    <a href="#" class="btn btn-primary btn-small" onclick="assignNewFolder();">Create & Link Folder</a>
                    <a class="btn btn-primary btn-small" onclick="openPage('Home');">Back</a>
                </div>
            </div>
            
            <div class="clearfix">
                <div class="pull-left">
                    <p class="muted">Create a new Google  folder or select a folder below to link</p>
                </div>   
                <div class="pull-right">
                  	<form class="form-search">
                      <div class="input-append" style="margin-right:73px">
                        <input id="UnlinkedFolderSearchInput" type="text" class="span12 search-query">
                        <button type="button" class="btn" onclick="searchUnlinkedFoldersOwnedByMe();">Search</button>
                      </div>
                    </form>
                  <!-- TODO: Remove this div : page layout will need to be fixed
                  <div class="span5 offset1" style="display:none;">
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
                  -->
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