/*Copyright (C) 2010-2012 Institute for Software Integrated Systems (ISIS)
This software was developed by the Institute for Software Integrated
Systems (ISIS) at Vanderbilt University, Tennessee, USA for the 
Transformative Apps program under DARPA, Contract # HR011-10-C-0175.
The United States Government has unlimited rights to this software. 
The US government has the right to use, modify, reproduce, release, 
perform, display, or disclose computer software or computer software 
documentation in whole or in part, in any manner and for any 
purpose whatsoever, and to have or authorize others to do so.
 */
package edu.vu.isis.logger.lib;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * This class allows us to hold a reference to Joran's bag of Appenders. This is
 * necessary since the bag is garbage collected after Joran finishes its job and
 * Logback does not seem to keep a copy of all the Appenders instantiated by
 * Joran. If those Appenders were not attached to any Loggers, then they are
 * garbage collected, and we have no way of obtaining them again. It is
 * important that we have references to all of the Appenders in order to allow
 * the user to attach/detach appenders from loggers at runtime.
 * 
 * @author Nick King
 * 
 */
public class AppenderStore {

	private final Map<String, Appender<ILoggingEvent>> appenderBag;
	private static AppenderStore instance = null;

	private AppenderStore() {
		appenderBag = null;
		throw new AssertionError("This constructor should never be called");
	}

	private AppenderStore(Map<String, Appender<ILoggingEvent>> map) {
		appenderBag = map;
	}

	/**
	 * Stores a reference to the map of appenders created by Joran. This method
	 * may only be called once per classloader.
	 * 
	 * @param map
	 *            The map of appenders
	 * @throws IllegalStateException
	 *             if this method has already been called.
	 */
	public static synchronized void storeReference(
			Map<String, Appender<ILoggingEvent>> map) {
		if (instance != null) {
			throw new IllegalStateException(
					"A reference has already been stored.");
		}
		System.out.println("Reference to appenderBag stored");
		instance = new AppenderStore(map);
	}

	/**
	 * Gets the map of appenders created by Joran.
	 * 
	 * @return the map of appenders
	 * @throws IllegalStateException
	 *             if no reference to the map has yet been stored
	 */
	public static synchronized Map<String, Appender<ILoggingEvent>> getAppenderMap() {
		if (instance == null) {
			throw new IllegalStateException("No reference has yet been stored.");
		}
		return instance.getAppenderBag();
	}

	private Map<String, Appender<ILoggingEvent>> getAppenderBag() {
		return appenderBag;
	}

}
