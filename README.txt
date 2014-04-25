
[ PURPOSE ]
===========
This is an LTI 2.0 compliant tool that integrates Google Drive with an LMS such as Sakai. Because this tool relies on some LTI 2.0 features, it can only be run on Sakai 10 or greater.

This tool allows an instructor to associate a Google Drive folder with a Sakai site, which grants everyone in that site's roster read-only access to the folder and it's contents. Once granted, this access is valid both from within the Google Drive LTI tool as well as from the Googie Drive UI itself.


         
[ SETUP ]
=========
1. svn co https://source.sakaiproject.org/contrib/umich/lti-utils/tags/<find-latest-version> lti-utils

2. svn co https://source.sakaiproject.org/contrib/umich/google/google-drive-lti/tags/<find-latest-version> google-drive-lti

3. Create a google service account specific to your institution as described at https://code.google.com/apis/console/. Create a public/private key and download the private key file (p12 file).

4. Configure your googleServiceProps.properties file as follows:
   
4a. Copy the googleServiceProps.properties.template file with googleServiceProps.properties

4b. Define the following properties in googleServiceProps.properties
   ## client.id and email.address are created as part of Google service account initial creation. just populate with those values
   googleDriveLti.service.account.client.id=
   googleDriveLti.service.account.email.address=
   googleDriveLti.service.account.scopes=https://www.googleapis.com/auth/drive
   
   ## LTI key,secret launch URL property should match with the properites configuring the google Drive LTI tool in LMS
   googleDriveLti.lti.secret=
   googleDriveLti.lti.key=
   LTI launch URL should be defined, especially if the HttpServletRequest.getRequestURL().toString() does not return a valid value 
   googleDriveLti.lti.launchUrl=

4c. Deploy google private key within web application (cp p12-file to src/main/java/secure/)

    ## Update googleServiceProps.properties
    googleDriveLti.service.account.private.key.file.classpath=true
    googleDriveLti.service.account.private.key.file=/secure/<filename>


    OR// Deploy google private key external to web application (cp p12-file to directory accessible by webapp)

    ## Update googleServiceProps.properties
    googleDriveLti.service.account.private.key.file.classpath=false
    googleDriveLti.service.account.private.key.file=<absolute file location>
    
4d. The googleServiceProps.properties file may optionally be deployed external to the war file:
    ## Update JAVA_OPTS for Tomcat to include:
    -DgoogleServicePropsPath=<fully-qualified-directory-name>/googleServiceProps.properties
        
4e. This is the default context configuration (deploy google-drive-lti.war web application):

    googleDriveLti.context=google-drive-lti/googledrivelti

    OR// This is an alternate configuration (deployed web application is ROOT application, such as is done by Amazon AWS Elastic Beanstalk):

    googleDriveLti.context=googledrivelti

6. Enable Basic LTI in sakai.properties
   
   #set the variable to Server URL of sakai instance (not Google-Drive instance) including Protocol eg. http://localhost:8080
   
   sakai.lti.serverUrl=<server-url>

   basiclti.provider.enabled=true
   basiclti.provider.allowedtools=sakai.announcements:sakai.singleuser:sakai.assignment.grades:blogger:sakai.dropbox:sakai.mailbox:sakai.forums:sakai.gradebook.tool:sakai.podcasts:sakai.poll:sakai.resources:sakai.schedule:sakai.samigo:sakai.rwiki
   basiclti.provider.lmsng.school.edu.secret=<configurable-secret>
   basiclti.roster.enabled=true
   basiclti.outcomes.enabled=true


[ BUILD & DEPLOY ]
==================
1. cd lti-utils; mvn install
2. cd google-drive-lti; mvn install
3. cp target/google-drive-lti.war $TOMCAT/webapps

PLEASE REMEMBER TO REVERT THE BELOW CHANGE BEFORE CHECKING IN TO SVN
HINT: For local development in pom.xml add the <plugin> tag under the <plugins> tag, this will automatically deploy to tomcat. while deploy the project use this build command "mvn clean install sakai:deploy -Dmaven.tomcat.home=$TOMCAT_HOME".
<plugin>
            <groupId>org.sakaiproject.maven.plugins</groupId>
            <artifactId>sakai</artifactId>
            <version>1.4.0</version>
            <extensions>true</extensions>
            <inherited>true</inherited>
            <configuration>
                <deployDirectory>${maven.tomcat.home}</deployDirectory>
                <warSourceDirectory>${basedir}/src/main/webapps</warSourceDirectory>
            </configuration>
</plugin>



[Configuring Google-Drive-LTI in Sakai ]
========================================

The following LTI (aka External Tool) settings must be set
	- Privacy Settings: Please check all the below options
              Send User Names to External Tool
              Send Email Addresses to External Tool
              Provide Roster to External Tool
              Allow External Tool to store setting data
      
	- Launch Secret: 
	- Launch URL: ## eg. http://locahost:8081/google-drive-lti/service
	- Launch Key: 


[Known Problems]
================

The current implementation assumes that the Sakai instance (or LTI Consumer) 
sufficiently secures a user's account information and email address.
Otherwise, if a user is able to change the email address sent to the 
Google LTI provider, they will have (add/delete) access to that user's 
Google Drive resources. 
