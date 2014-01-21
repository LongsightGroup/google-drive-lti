
[ PURPOSE ]
===========
This project will run as LTI interacting with Google Drive and Sakai LTI client, and may also operate well with other LTI compliant systems.

This gives a course's instructor the ability to associate a Google Drive folder with the Sakai site, and to give other people in the roster read-only access to the folder.

These permissions work for the folder's contents, so documents, sub folders, and their descendants will be accessible in the same manner.

The Google Drive LTI run on its own instance than the Sakai instance


         
[ SETUP ]
=========
1. svn co https://source.sakaiproject.org/contrib/umich/lti-utils 

2. svn co https://source.sakaiproject.org/contrib/umich/google/google-drive-lti

3. Google service account creation at https://code.google.com/apis/console/. This is account is created and every body will be using the same account properties and .p12file
   - Create a public/private key and download private key file (p12 file)
   
4a.Replace the googleServiceAccounts.properties.template file with googleServiceAccounts.properties

4b. Define the following properties to googleServiceAccounts.properties
   ## client.id and email.address are created as part of Google service account initial creation. just populate with those values
   googleDriveLti.service.account.client.id=
   googleDriveLti.service.account.email.address=
   googleDriveLti.service.account.scopes=https://www.googleapis.com/auth/drive
   
   ## LTI key/password property
   googleDriveLti.service.account.lti.secret=
   googleDriveLti.service.account.lti.key=

4c. Deploy google private key within web application (cp p12-file to src/main/java/secure/)

    ## Update googleServiceAccounts.properties
    googleDriveLti.service.account.private.key.file.classpath=true
    googleDriveLti.service.account.private.key.file=/secure/<filename>


4f. OR// Deploy google private key external to web application (cp p12-file to directory accessible by webapp)

    ## Update googleServiceAccounts.properties
    googleDriveLti.service.account.private.key.file.classpath=false
    googleDriveLti.service.account.private.key.file=<absolute file location>
    
4g. If the googleServiceAccounts.properties file is in the $TOMCAT directory then follow the procedures to do this
     1.cd $TOMCAT
     2.create a directory named "google" and add the properties file into it $TOMCAT/google/googleServiceAccounts.properties
     3.Add properties to JAVA_OPTS for Tomcat:
        -DgoogleServicePropsPath=$TOMCAT/google/googleServiceAccounts.properties
        
5.   GoogleDriveLTI if deployed to AWS vs local/deluxe servers. 

      googleDriveLti.context=google-drive-lti/googledrivelti -Local/deluxe
      OR
      googleDriveLti.context=googledrivelti  - AWS   
        
   

6. Enable Basic LTI in sakai.properties
   
   #set the variable to Server URL of sakai instance( and not Google-Drive instance) including Protocol eg. http://localhost:8080
   
   sakai.lti.serverUrl=

   basiclti.provider.enabled=true
   basiclti.provider.allowedtools=sakai.announcements:sakai.singleuser:sakai.assignment.grades:blogger:sakai.dropbox:sakai.mailbox:sakai.forums:sakai.gradebook.tool:sakai.podcasts:sakai.poll:sakai.resources:sakai.schedule:sakai.samigo:sakai.rwiki
   basiclti.provider.lmsng.school.edu.secret=secret
   basiclti.roster.enabled=true
   basiclti.outcomes.enabled=true


[ BUILD & DEPLOY ]
==================
1. cd lti-utils; mvn install
2. cd google-drive-lti; mvn install
3. cp target/google-drive-lti.war $TOMCAT2/webapps
4. do a regular sakai build and deploy to $TOMCAT1 Instance

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

HOW TO RUN SECOND TOMCAT INSTANCE FOR LOCAL DEVELOPMENT
1. untar the TC2 to your favorite location
2. open TC2/bin/startup.sh in a VI editor.... and  add below 2 lines at the beginning.
        export TOMCAT_HOME=TC2 DIRECTORY PATH eg./user/tomcat
        export CATALINA_HOME=$TOMCAT_HOME
3. open server.xml and change the  below tags something different from the TC1
        <Connector port for protocol="HTTP/1.1"> && <Server port > && <Connector port="8009" protocol="AJP/1.3" >
 


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


