This is README.txt for project google-drive-lti
Developed by Raymond Naseef (developer) under leadership of John Johnston, Catherine Crouch, Chris Kretler, Beth Kirschner, and the entire CTools Development Team, for University of Michigan.


[ PURPOSE ]
===========
This project will run as LTI interacting with Google Drive and Sakai LTI client, and may also operate well with other LTI compliant systems.

This gives a course's instructor the ability to associate a Google Drive folder with the Sakai site, and to give other people in the roster read-only access to the folder.

These permissions work for the folder's contents, so documents, sub folders, and their descendants will be accessible in the same manner.



[ KNOWN ISSUES ]
==========
- This LTI service will not run in a cluster environment, due to the way it stores links. The TcSiteToGoogleStorage class maps google folders & sakai sites in a single flat file. This could be resolved by assigning server-specific URL to each site's LTI "Remote Tool URL" (a.k.a. imsti.launch). For example, university could handle Google Drive LTI for all Engineering courses in URL https://ourEgrDomain/google-drive-lti/service, and all Math courses in URL https://ourMathDomain/google-drive-lti/service.  If each URL points to a single server, handling storage will not be complex.

- This LTI service may have issues if multiple clients access the server, due to the way it stores links. The TcSiteToGoogleStorage class maps google folders & sakai sites in a single flat file.  

- Users may not be able to view folders from the Google Drive LTI client that they can view from Google Drove. For the user to see files in the LTI, they need to have read access to all subfolders to see those files in LTI. 
  Examples:
	[A] User will NOT see the file:
	+ Root folderi (user does have access)
		- Subfolder (user does NOT have access)
			+ File   (user does have access)

	[B] User will see the file:
	+ Root folder  (user does have access)
		+ Subfolder (user does have access)
			+ File   (user does have access)


[ BUILDING ]
============
1. svn co https://source.sakaiproject.org/contrib/umich/google/lti-utils
2. cd lti-utils; mvn install
3. svn co https://source.sakaiproject.org/contrib/umich/google/sandbox/google-drive-lti
4. cd google-drive-lti; mvn install
5. cp target/google-drive-lti.war $TOMCAT/webapps



[ Deploying ]
=============
Do each of the following:
	A - Enabling Basic LTI
	B - Installing LTI into a (new) Site
	C - Setting up Google Authorization & Site-to-Google Link Storage


	[ A - Enabling Basic LTI ]
	--------------------------
These properties must exist in sakai.properties.  If not, accessing "/imsblis/service" will deny requests, such as request for the course's roster.

Using these properties works with my tests of "Google Drive LTI" in Sakai Trunk 2013-06-03, but not sure they are correct;.  Some entries may not affect this LTI, and there may be missing properties that should be set.

Better documentation on these may exist in Sakai module "basiclti" at "basiclti-docs/resources/docs".

sakai.properties
----------------
webservices.allowlogin=true
webservices.allow = .*
webservices.log-allowed=true
webservices.log-denied=true

basiclti.provider.enabled=true
basiclti.provider.allowedtools=sakai.announcements:sakai.singleuser:sakai.assignment.grades:blogger:sakai.dropbox:sakai.mailbox:sakai.forums:sakai.gradebook.tool:sakai.podcasts:sakai.poll:sakai.resources:sakai.schedule:sakai.samigo:sakai.rwiki
basiclti.provider.lmsng.school.edu.secret=secret
basiclti.roster.enabled=true
basiclti.outcomes.enabled=true



	[ B - Installing LTI into a Site ]
	----------------------------------
For adding to an existing site, continue from step "!!! 13."
For adding to a new site, start with step 1.

1. Start Tomcat with Sakai Trunk and this project's WAR file.
  * Sakai and google-drive-lti can be run on different servers
  * this example works with Sakai demo by adding "sakai.demo=true" to "JAVA_OPTS" in setenv.sh or the environment
2. Open Sakai in browser (this was done in Google Chrome 26)
3. Login as admin
4. Go to "My Workspace" (this is the default site, so you may already be there)
5. Click on "Worksite Setup" (left-side menu)
6. Press button "New" near top of the page
  * "My Workspace: Worksite Setup" page opens
7. Select "course site" and specify the "Academic term" for the site.
8. Press button "Continue"
  * Page opens with "Course/Section Information"
9. Select section to create (we used "SMPL101 Lecture") and press "Continue"
  * Page opens with "Course Site Information"
10. Enter details on this page (none are required) and press "Continue"
  * Page opens with "Course Site Tools"
11. Press button "Continue"
  * Page opens with "Course Site Access"
  - Default settings are good for test system ("Publish Site" & "Limited to whom I add manually...")
12. Press button "Continue" and press button "Create Site" on the next page
  * Page opens with list of sites
!!! 13. Click on "Sites" (left-side menu)
  ! If step number !!! ... changes, change note at top of this installation step to match this step's correct number
14. Click on ID for the site
  * Page opens with editor for site's information
15. Click on button "Pages"
  * Page opens with list of site's pages
16. Press button "New Page" at top of the page
  * Page opens with editor for the new page
17. Enter fields
	- Set title "Google Drive LTI"
	- Check box to use "Custom Title"
18. Press button "Tools" near bottom of the page
  * Page opens with "There are no tools defined in this page."
19. Press button "New Tool" at the top of the page
  * Page opens with list of different tools that can be added to the page
20. Select "External Tool (sakai.basiclti)"
  * Page shows list of fields for the LTI
21. Enter fields
	- imsti.releaseemail: on
	- imsti.allowroster:  on
	- imsti.secret: secret
	- imsti.releasename: on
	- imsti.launch: https://localhost:8080/google-drive-lti/service
	* The URL is to Sakai's service providing course rosters, and "https://localhost:8080" may be incorrect
	* NOTE: must enter an entire URL, or the server will fail to load the tool with an NPE
	- imsti.key: 12345
22. Press button "Save"
  * Page opens with list of sites



	[ C - Setting up Google Authorization & Site-to-Google Link Storage ]
	---------------------------------------------------------------------
1. cd $TOMCAT
2. touch googleServiceAccounts.properties
3. Add the following properties to googleServiceAccounts.properties
googleDriveLti.service.account.client.id=505339004686-6u695emetjek7bca8gvr7q14lbp5jbc4.apps.googleusercontent.com
googleDriveLti.service.account.email.address=505339004686-6u695emetjek7bca8gvr7q14lbp5jbc4@developer.gserviceaccount.com
googleDriveLti.service.account.private.key.file=/Users/ranaseef/googleKeys/e3f47d43c771825722360e4042cab76e4a3f9dd6-privatekey.p12
googleDriveLti.service.account.scopes=https://www.googleapis.com/auth/drive
* you will need to create service account with permissions to edit Google Drive, and to download .p12 file for it
* Replace /Users/ranaseef/...p12 with filename for your service account
* Replace 505*.apps.googleusercontent.com and 505*@developer.gserviceaccount.com with entries for your Service Account.
* You can find the matching entries at https://code.google.com/apis/console/
4. Add properties to JAVA_OPTS for Tomcat:
	-DgoogleServicePropsPath=$TOMCAT/google/googleServiceAccounts.properties
	-DDgoogleServiceStoragePath=$TOMCAT/google/ltidb



[ NOTES - Storage ]
===================
This currently stores linkings of Google folders to Sites in files.  Here is the configuration for that:

- File name: "google-drive-lti-<context_id>.txt"
- File location: retrieved from System property "googleDriveLtiDataFilesFolder=<path-to-folder>"
- Contents:
<site_id>,<user_id>,<user_email_address>,<google-folder-id>

* Note: newlines are replaced with single space ' ', so each link remains on one line of the file
* <user_id> is owner of the folder, and instructor that linked the folder to the TC site.

- Example of Contents for site with 2 linked folders:

c5e9c736-e8a0-41dc-aa28-d09676d044fe,769b4f4f-d302-449c-b614-9a759f1e37c0,test@collab.its.umich.edu,0B_G6RWXM0arpSlg3b2M4SU50ZUE,2 - Course with Google Drive
c5e9c736-e8a0-41dc-aa28-d09676d044fe,769b4f4f-d302-449c-b614-9a759f1e37c0,test@collab.its.umich.edu,0B_G6RWXM0arpQ3JmOThzVFBtVFE,LTI Folders Parent

