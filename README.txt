This is README.txt for project google-drive-lti
Developed by Raymond Naseef (developer) under leadership of John Johnston, Catherine Crouch, Chris Kretler, Beth Kirschner, and the entire CTools Development Team, for University of Michigan.



[ PURPOSE ]
===========
This project will run as LTI interacting with Google Drive and Sakai LTI client, and may also operate well with other LTI compliant systems.

This gives a course's instructor the ability to associate a Google Drive folder with the Sakai site, and to give other people in the roster read-only access to the folder.

These permissions work for the folder's contents, so documents, sub folders, and their descendants will be accessible in the same manner.



[ ISSUES ]
==========
- Internet Explorer [ IE8/IE9 ]:
	-- The page loads script "/library/htmlarea/jquery-plugin-xdr.js".  Instructions to deploy this JS library are in Sakai server module "google-service".  The instructions place the library into sub-module "reference/library", and this project is dependent upon Sakai module "reference/library" being deployed to the same domain.
	-- IE is currently failing to associate a folder with the course, and failing to let instructor to detach the folder from the course.

- If LTI is configured with "Send Email Addresses to the External Tool" FALSE (unchecked), the page will open with nothing showing on it, as it is unable to verify the user and request an access token without their email address.  The request for access token is being made to a local prototype service at URL "/google-integration-prototype/googleLinks".

- Opening the LTI when logged in as a user that is not in the roster will open with "Google Drive LTI" and an empty box, and nothing more showing on the page.  The key is the user's email address is known by LTI

- Requesting roster may fail with error: net.oauth.OAuthProblemException: timestamp_refused.  The issue is that the roster will be requested when the instructor associates the course with a folder, and that may be quite some time from when the page is loaded.  The timestamp for the request is set when the browser loads the page.  Sakai imsblis uses SimpleOauthValidator to validate the request, and uses a "time window" to ensure the request is recent.  The default "time window" is 5 minutes, and sending the request after then will fail.

- When permissions are being given to people in the roster, this is being done on the browser in the background.  If the instructor leaves the page before that completes, the permissions stop.  TODO: add check when leaving the page, to ask the instructor to wait if permissions are still being added.

- For the user to see files they have access to in the LTI, they need to have read access to all subfolders to see those files in LTI.  For example, if "my" permissions are:
Keys:
	+ I have read access to the file
	- I do not have access to the file
Cases:
	[A] NO GOOD, I will not see that file:
	+ Root folder
		- Subfolder
			+ File I have access to

	[B] GOOD, I will see the file:
	+ Root folder
		+ Subfolder
			+ File I have access to

* This issue does not affect what "I" can see in Google Drive.  "I" should be able to see the file in both case [A] & case [B].



[ BUILDING ]
============
1. svn co https://source.sakaiproject.org/contrib/umich/google/sandbox/google-drive-lti
2. cd google-drive-lti
3. mvn clean install sakai:deploy



[ INSTALLING ]
==============
Do each of the following:
	A - Enabling Basic LTI
	B - Installing LTI into a Site
	C - Setting up Google Authorization


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
	--------------------------------
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
14. Click on ID for the new site
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



	[ C - Setting up Google Authorization ]
	---------------------------------------
As written 2013-06-01, the JavaScript file in this service makes AJAX request to URL for sibling project "google-integration-prototype".  If this sibling project is deployed to same server as this project, it will be able to handle these requests.  To build and install that:

1. svn co https://source.sakaiproject.org/contrib/umich/google/sandbox/google-integration-prototype/
2. cd google-integration-prototype
3. mvn clean install sakai:deploy
4. Create Google Service Account with permissions to edit Google Drive, and download .p12 file for it
5. Add the following properties to JAVA_OPTS:
	 i. google.prototype.p12=<PathOnTheServerToP12File>
	ii. google.prototype.service.account.email=<GoogleServiceAccountEmailAddress>
  * Example properties as written on java command-line:
	-Dgoogle.prototype.p12=/usr/local/ctools/m+google/e3f47d43c771825722360e4042cab76e4a3f9dd6-privatekey.p12
	-Dgoogle.prototype.service.account.email=505339004686-6u695emetjek7bca8gvr7q14lbp5jbc4@developer.gserviceaccount.com
6. Start Tomcat


