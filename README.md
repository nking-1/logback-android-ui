Logback Android User Interface (LAUI)
==========================================

The logback-android project provides an excellent logging framework.  However, it lacks a means for the operator to conveniently view or configure the logging behavior at runtime.  This project fills that gap by adding a user inteface to logback-android.  The name of this project is Logback Android UI, or LAUI for short (pronounced like Maui).

##Introduction

This project allows a user to graphically view the hierarchy of loggers in an application.  Loggers are displayed in a tree-like list to reflect their inheritance, with an icon to the left indicating the level of the logger.  An icon that is inheriting a level from its parent gets a circular icon, while a logger that has an explicitly set level gets a downward triangle.  The appenders to which each logger writes can also be seen in the list if desired.  A logger that has an appender configuration which differs from its parent gets a rounded square icon to its right.

Loggers in the list can be selected to be configured.  Both the levels and the appenders attached to a logger can be changed.  When a logger's configuration is changed, a Content Provider that is contained in the LAUI library of your project is notified, and this Content Provider is what actually carries out the reconfiguration of your loggers.  The LAUI application is a standalone app which will scan for these Content Providers at runtime to determine which applications are using the LAUI library.  In order for this to work, you must simply add the LAUI library to your project and add a new Content Provider declaration to your Android Manifest file.  The specific instructions are in the Quick Start.

###Other Laui features
LAUI provides a basic Logcat reader and a reader for each of your file-based appenders that it finds.  LAUI can also save your logger level and appender configurations to an XML file.  This file can be parsed by LAUI at a later time to quickly reconfigure your loggers.  Note, however, that this is not quite the same as having Logback configure itself through XML.  LAUI is limited to only reconfiguring the settings of your loggers.

##Quick Start

Clone this repository.

This project is built with maven.

mvn clean install

And to install on all adb connected phones...

cd laui

mvn android:deploy
 
This provides a means for looking at the android logcat log.
If an android application has been outfitted with Laui the
applications loggers can be configured dynamically.

 
