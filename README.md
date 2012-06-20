logback-android-application
===========================

The logback-android project provides an excellent logging framework but it provides not operator viewing or configuration.  This project fills that gap.

This project provides a stand alone application for viewing logs managed by logback-android.
It expects a set of android applications with logback enabled and a content provider wrapping their specific configurations.
The application provides both configuration of loggers and viewing of the logs.
All interaction with logback is through the content provider, thus there is very little (hopefully none) calls to logback present in this application.

Quick Start
===========

git clone

This project is built with maven.

mvn clean install

