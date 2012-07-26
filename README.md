#Logback Android User Interface (LAUI)

The logback-android project provides an excellent logging framework.  However, it lacks a means for the operator to conveniently view or configure the logging behavior at runtime.  This project fills that gap by adding a user inteface to logback-android.  The name of this project is Logback Android UI, or LAUI for short (pronounced like Maui).

##Introduction

This project allows a user to graphically view the hierarchy of loggers in an application.  Loggers are displayed in a tree-like list to reflect their inheritance, with an icon to the left indicating the level of the logger.  An icon that is inheriting a level from its parent gets a circular icon, while a logger that has an explicitly set level gets a downward triangle.  The appenders to which each logger writes can also be seen in the list if desired.  A logger that has an appender configuration which differs from its parent gets a rounded square icon to its right.

Loggers in the list can be selected to be configured.  Both the levels and the appenders attached to a logger can be changed.  When a logger's configuration is changed, a Content Provider that is contained in the LAUI library of your project is notified, and this Content Provider is what actually carries out the reconfiguration of your loggers.  The LAUI application is a standalone app which will scan for these Content Providers at runtime to determine which applications are using the LAUI library.  In order for this to work, you must simply add the LAUI library to your project and add a new Content Provider declaration to your Android Manifest file.  The specific instructions are in the Quick Start.

###Other Laui features
LAUI provides a basic Logcat reader and a reader for each of your file-based appenders that it finds.  LAUI can also save your logger level and appender configurations to an XML file.  This file can be parsed by LAUI at a later time to quickly reconfigure your loggers.  Note, however, that this is not quite the same as having Logback configure itself through XML.  LAUI is limited to only reconfiguring the settings of your loggers.

##Quick Start

###Building
Clone this repository.  This project is built with maven.  To build the project, run in a terminal:

    mvn clean install

And to install LAUI on all adb connected devices:

    cd laui
    mvn android:deploy
 
###Using LAUI-Lib in your project
First, set up your project to use logback-android as instructed at https://github.com/tony19/logback-android.  Then, follow the instructions that are relevant to you below.

####For Eclipse users
Import into Eclipse the LAUI-Lib project in the lauilib directory of this repository.  Right click on your project and click Properties.  Click on the Android tab on the left.  In the Library section of the lower part of the window, click the "Add..." button, and select the LAUI-Lib project in the dialog that pops up.  Click Apply and Ok.

Alternatively, after building LAUI-Lib with Maven, you can put the generated jar file in the libs folder of your project.  Android will automatically include it as a dependency in your builds.

####For everyone else
Build this project with Maven and include the generated LAUI-Lib jar file as a dependency in your build scripts.

###Setting up the ContentProvider
LAUI-Lib includes a ContentProvider to allow LAUI to display and edit your loggers.  This ContentProvider must be declared in your application's manifest file in order for it to function.

In your AndroidManifest.xml, include the following lines:

    <provider
            android:name="edu.vu.isis.logger.lib.LauiContentProvider"
            android:authorities="[your application's package name].LauiContentProvider" />
            
As an example, here is a sample manifest file:

    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="edu.vu.isis.logger.test"
        android:versionCode="1"
        android:versionName="1.0" >
    
        <uses-sdk
            android:minSdkVersion="8"
            android:targetSdkVersion="15" />
    
        <application
            android:icon="@drawable/ic_launcher"
            android:label="@string/app_name" >
            <!-- Omitted code -->
    
            <provider
                android:name="edu.vu.isis.logger.lib.LauiContentProvider"
                android:authorities="edu.vu.isis.logger.test.LauiContentProvider" />
        </application>
    
    </manifest>
            
Notice how the package name declared in the manifest tag's package attribute exactly matches the android:authorities attribute inside the provider tag.  <b>This is absolutely necessary since LAUI's ContentProvider generates its authority from your application's package name.</b>  If these do not match, then LAUI will find your loggers.

###Setting up the AppenderStore
Logback uses appender objects to direct the output of loggers.  Appenders are attached to loggers, and the loggers send their logs to their appenders for output.  LAUI allows you to change which appenders are attached to your loggers so, for example, you could change one of your loggers from writing to Logcat to writing to a file.

However, Logback does not, by default, keep track of its instantiated appenders at runtime like it keeps track of its loggers.  In order to allow this, include the following in your Logback XML configuration file:
    
    <newRule pattern="configuration/appender" actionClass="edu.vu.isis.logger.lib.AppenderStoreAction"/>
    
This will allow LAUI-Lib to keep track of your appenders and make them available to LAUI.  This tag should be added inside of your <configuration> tag.  You can still use LAUI if you do not add this to your configuration file.  Here is an example of a properly written configuration file:

    <configuration>
    
        <!-- This new rule causes joran to record each new appender into the appender 
            store. The store is used to display the appenders. -->
        <newRule pattern="configuration/appender"
            actionClass="edu.vu.isis.logger.lib.AppenderStoreAction"/>
            
        <appender name="LOGCAT" class="ch.qos.logback.classic.android.LogcatAppender">
            <checkLoggable>false</checkLoggable>
            <tagEncoder>
                <pattern>laui.%logger{22}</pattern>
    		</tagEncoder>
    		<encoder>
    			<pattern>[%method] %msg%n</pattern>
    		</encoder>
       	</appender>
     
    	<root level="WARN">
    	    <appender-ref ref="LOGCAT" />
    	</root>
    
    </configuration>

To read more about appenders, see http://logback.qos.ch/manual/appenders.html

###Using LAUI
Once you have completed the above steps, your project will be ready for use with LAUI.  Install LAUI on the device and open it.  If your project is configured correctly and installed on the device, you should see it in a list of supported applications.  Simply select your application, and you will be ready to view and edit your loggers.

####Troubleshooting
If the only logger you see is ROOT, then it is because your application has not yet instantiated any loggers.  This is usually caused by not opening your application before opening LAUI.  To fix this, open your application, then go back to LAUI and refresh the logger list.  This can be done by either pressing the menu button and selecting the force refresh button or by exiting the logger editor and reselecting your application from the list.

If you only see a logger called ROOT_NOT_FOUND, then it is probably a sign that you did not configure your application correctly.  Ensure that you have correctly configured logback-android and LAUI-Lib, then try refreshing the list as described above.  If the issue persists, please file a bug report

##Reporting issues/bugs
Please file all issues on Github.  We will address them as soon as we can.