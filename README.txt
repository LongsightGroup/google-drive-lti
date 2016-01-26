
[ PURPOSE ]
===========
This is an LTI 1.2+ compliant tool that integrates Google Drive with an LMS such as Sakai. Plans are in place to implement LTI 2.0 compliance, but until then, this tool can only be run on Sakai 2.9 or greater (relying on LTI 1.1 + settings service).

This tool allows an instructor to associate a Google Drive folder with a Sakai site, which grants everyone in that site's roster read-only access to the folder and it's contents. Once granted, this access is valid both from within the GDrive tool as well as from the Googie Drive UI itself.

[ REQUIREMENTS ]
================
* Google Apps for Education instance
* LTI 1.0 compatible LMS
* Java/Tomcat hosting environment


[ SETUP ]
=========
1. git clone https://github.com/tl-its-umich-edu/lti-utils

2. git clone https://github.com/LongsightGroup/google-drive-lti

3. Create a google service account specific to your institution as described at https://code.google.com/apis/console/. Create a public/private key and download the private key file (p12 file).

4. Configure your googleServiceProps.properties file as follows:
   
4a. Copy the googleServiceProps.properties.template file with googleServiceProps.properties

4b. Define the following properties in googleServiceProps.properties
   ## client.id and email.address are created as part of Google service account initial creation. just populate with those values
   googleDriveLti.service.account.client.id=
   googleDriveLti.service.account.email.address=
   googleDriveLti.service.account.scopes=https://www.googleapis.com/auth/drive
   
   ## LTI key,secret launch URL property should match with the properites configuring the GDrive tool in LMS
   googleDriveLti.lti.secret=
   googleDriveLti.lti.key=
   # LTI launch URL should be defined, especially if the HttpServletRequest.getRequestURL().toString() does not return a valid value 
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
    
4f. Google Sharing a folder/file limit. Making this limit configurable in future if google changes its mind . Here is the link to google support page https://support.google.com/drive/answer/2494827?hl=en
    googleDriveLti.google.sharing.limit.size=200

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
4. Optional: Set up a script to populate src/main/webapps/build.txt with information about the current build (SVN revision, build time, etc.).
    If using Jenkins to build the application, the following script placed in the "Execute Shell" part of the "Pre Steps" section would do
    the job:
    
    cd src/main/webapps
    if [ -f "build.txt" ]; then
      echo "build.txt found."
      rm build.txt
      echo "Existing build.txt file removed."
    else
      echo "No existing build.txt file found."
    fi
    touch build.txt

    echo "$JOB_NAME | Build: $BUILD_NUMBER | $SVN_URL | $SVN_REVISION | $BUILD_ID" >> build.txt


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

5. Optional logging: Create or update the log4j.properties as follows to enable application logging:

#log4j.logger.edu.umich=INFO
log4j.logger.edu.umich=DEBUG

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

1.  The current implementation assumes that the Sakai instance (or LTI Consumer) 
sufficiently secures a user's account information and email address.
Otherwise, if a user is able to change the email address sent to the 
Google LTI provider, they will have (add/delete) access to that user's 
Google Drive resources. 

2.  Searches only match the start of "words" in folder names.  That is, if the user has
a folder named "my testfest", a search for "fest" will not return the folder.  However, a search 
for "test" will.  This is the way the searches work in Google Drive itself, therefore, GDrive's
search is similar.

3.  Expanding folders in search results do not display sub-folders because they do not
match the search text.  For example, a site owner has a complex directory structure in Google Drive.
Nested several layers deep they have a folder named "photos" and within it is another folder
they want to share with their site.  A search for "photos" returns that folder as expected. 
However, expanding that folder reveals nothing, because the nested folders don't match the search
text.
