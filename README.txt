
[ PURPOSE ]
===========
This project will run as LTI interacting with Google Drive and Sakai LTI client, and may also operate well with other LTI compliant systems.

This gives a course's instructor the ability to associate a Google Drive folder with the Sakai site, and to give other people in the roster read-only access to the folder.

These permissions work for the folder's contents, so documents, sub folders, and their descendants will be accessible in the same manner.


[ KNOWN ISSUES ]
================
- This LTI service will not run in a cluster environment, due to the way it stores links. The TcSiteToGoogleStorage class maps google folders & sakai sites in a single flat file. This could be resolved by assigning server-specific URL to each site's LTI "Remote Tool URL" (a.k.a. imsti.launch). For example, university could handle Google Drive LTI for all Engineering courses in URL https://ourEgrDomain/google-drive-lti/service, and all Math courses in URL https://ourMathDomain/google-drive-lti/service.  If each URL points to a single server, handling storage will not be complex.

- This LTI service may have issues if multiple clients access the server, due to the way it stores links. The TcSiteToGoogleStorage class maps google folders & sakai sites in a single flat file.  




         
[ SETUP ]
=========
1. svn co https://source.sakaiproject.org/contrib/umich/google/lti-utils

2. svn co https://source.sakaiproject.org/contrib/umich/google/google-drive-lti

3. Create a google service account at https://code.google.com/apis/console/
   - Create a public/private key and download private key file (p12 file)
   - Configure the Service Account with the LTI application
   - Configure the LTI 'secret' in a <tbd> properties file

3. Define the following properties to googleServiceAccounts.properties
   googleDriveLti.service.account.client.id=
   googleDriveLti.service.account.email.address=
   googleDriveLti.service.account.scopes=https://www.googleapis.com/auth/drive

4a. Deploy google private key within web application (cp p12-file to src/main/java/secure/)

    ## Update googleServiceAccounts.properties
    googleDriveLti.service.account.private.key.file.classpath=true
    googleDriveLti.service.account.private.key.file=/secure/<filename>


4b. OR// Deploy google private key external to web application (cp p12-file to directory accessible by webapp)

    ## Update googleServiceAccounts.properties
    googleDriveLti.service.account.private.key.file.classpath=false
    googleDriveLti.service.account.private.key.file=<absolute file location>
    
4. Add environment variables to JAVA_OPTS for Tomcat:
	-DgoogleServicePropsPath=$TOMCAT/google/googleServiceAccounts.properties
	-DDgoogleServiceStoragePath=$TOMCAT/google/ltidb

5. Enable Basic LTI in sakai.properties

   basiclti.provider.enabled=true
   basiclti.provider.allowedtools=sakai.announcements:sakai.singleuser:sakai.assignment.grades:blogger:sakai.dropbox:sakai.mailbox:sakai.forums:sakai.gradebook.tool:sakai.podcasts:sakai.poll:sakai.resources:sakai.schedule:sakai.samigo:sakai.rwiki
   basiclti.provider.lmsng.school.edu.secret=secret
   basiclti.roster.enabled=true
   basiclti.outcomes.enabled=true


[ BUILD & DEPLOY ]
==================
1. cd lti-utils; mvn install
2. cd google-drive-lti; mvn install
3. cp target/google-drive-lti.war $TOMCAT/webapps
4. cd lti-proxy; mvn install
5. cp target/lti-proxy.war $TOMCAT/webapps
4. Deploy Sakai's library.war file to $TOMCAT/webapps (until this dependency is resolved)


[Configuring Google-Drive-LTI in Sakai ]
========================================

The following LTI (aka External Tool) settings must be set
	- imsti.releaseemail: on
	- imsti.allowroster:  on
	- imsti.releasename: on
	- imsti.secret: TBD
	- imsti.launch: TBD
	- imsti.key: TBD


[ IMPLEMENTATION NOTES ]
========================
This tool maps Google folders to Sites in files as follows:

* File name: "google-drive-lti-<context_id>.txt"
* File location: retrieved from System property "googleDriveLtiDataFilesFolder=<path-to-folder>"
* File format: <site_id>,<user_id>,<user_email_address>,<google-folder-id>
* Note: newlines are replaced with single space ' ', so each link remains on one line of the file
* <user_id> is owner of the folder, and instructor that linked the folder to the Sakai site
