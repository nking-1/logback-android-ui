logback-android-application user interface (LAUI)
==========================================

The logback-android project provides an excellent logging framework.
However, it lacks a means for the operator to view or configure the logging behavior..  
This project fills that gap adding a user inteface to logback-android.
The name of this project is .LAUI (pronounced like Maui).

This project provides a stand alone application for viewing logs managed by logback-android.
It expects a set of android applications with logback enabled and a content provider wrapping their specific configurations.
The application provides both configuration of loggers and viewing of the logs.
All interaction with logback is through the content provider, thus there is very little (hopefully none) calls to logback present in this application.

Quick Start
-----------

Clone this repository.

This project is built with maven.

mvn clean install

And to install on all adb connected phones...

cd laui

mvn android:deploy
 
This provides a means for looking at the android logcat log.
If an android application has been outfitted with Laui the
applications loggers can be configured dynamically.

 
