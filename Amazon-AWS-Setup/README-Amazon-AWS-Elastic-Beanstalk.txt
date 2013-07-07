This is README.txt for running project google-drive-lti on Amazon AWS Elastic Beanstalk.


[ Purpose ]
===========

This document has instructions for building and deploying Google Drive LTI to Amazon Beanstalk services.  This requires you have an existing account with Amazon with Beanstalk.



[ Terminology ]
===============
- Amazon Beanstalk => Amazon AWS Elastic Beanstalk



[ Issues ]
==========
- Running WAR on Amazon Beanstalk does not appear to give development direct access to the server's hard drive, all though the WAR file does have such access.  Due to this, the easiest way to upload the Google Service .p12 file is to include it in the WAR file.
	+ Google Drive LTI includes:
		++ Classpath package "src/main/java/secure" that can be used for holding the .p12 file
		++ Default properties file for Google security: "src/main/java/edu/umich/its/google/oauth/googleServiceProps.properties"

- Deploying updates to Amazon Beanstalk: this can be done by:
	1. Opening the application at "https://console.aws.amazon.com/elasticbeanstalk/home?region=us-west-2#"
	2. Press "Actions > Deploy a Different Version" (even to update the current version)
	3. Select target WAR file to upload.
	4. If you want to update existing version, click on "Deploy an existing version" and select the version to update.
	!!! I did this with WAR containing modified JavaScript library 2013-07-06, and the changes did not show up on browsers until I deployed this using "Upload and deploy a new version", instead of "Deploy an existing version"



[ Build ]
=========
1. svn co https://source.sakaiproject.org/contrib/umich/google/google-drive-lti
2. cp [p12-file] src/main/java/secure/gdlti-google-private-key.p12
3. vim src/main/java/edu/umich/its/google/oauth/googleServiceProps.properties
	* Enter the following properties (lines starting with '-')
		** Remove '- '
		** Change "505339004686-6u695emetjek7bca8gvr7q14lbp5jbc4" with your Google Service Client ID: that number is not the same as the .p12 file name - see field "Client ID" in box titled "Service account" in area "API Access" at "https://code.google.com/apis/console/")
- googleDriveLti.service.account.client.id=505339004686-6u695emetjek7bca8gvr7q14lbp5jbc4.apps.googleusercontent.com
- googleDriveLti.service.account.email.address=505339004686-6u695emetjek7bca8gvr7q14lbp5jbc4@developer.gserviceaccount.com
- googleDriveLti.service.account.private.key.file=/secure/gdlti-google-private-key.p12
- googleDriveLti.service.account.private.key.file.classpath=true
- googleDriveLti.service.account.scopes=https://www.googleapis.com/auth/drive
4. mvn clean install



[ DEPLOY ]
==========
1. Login to Amazon Beanstalk
2. At "Elastic Beanstalk Management Console" (https://console.aws.amazon.com/elasticbeanstalk/home?region=us-west-2#), press "Create New Application"
3. Follow screen shots 1-4 in this document's sibling folder "1 Create New Application"
	* On screen shot 1, upload "target\google-drive-lti.war"
4. Press "Launch New Environment"
5. Follow screen shots 1-3 in this document's sibling folder "2 Launch New Environment"
	* On screen shot 2, enter an appropriate email address for admin responsible for managing this service
	! there are errors stating I am not authorized to perform "iam:ListInstanceProfiles" - this did not seem to have negative affect: the service is running fine as I write this, and this issue has not been investigated
* Should be good to go, once Amazon has started the new environment:
* To Run This: Setup LTI in Sakai with "Remote Tool URL" (a.k.a. "imsti.launch"): "http://ctools-google-drive-lti.elasticbeanstalk.com/service"


