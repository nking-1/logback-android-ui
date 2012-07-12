package edu.vu.isis.logger.lib;

import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

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
	 * Stores a reference to the map of appenders created by Joran.  This method
	 * may only be called once per classloader.
	 * @param map The map of appenders
	 * @throws IllegalStateException if this method has already been called.
	 */
	public static synchronized void storeReference(Map<String, Appender<ILoggingEvent>> map) {
		if (instance != null) {
			throw new IllegalStateException(
					"A reference has already been stored.");
		}
		System.out.println("Reference to appenderBag stored");
		instance = new AppenderStore(map);
	}

	/**
	 * Gets the map of appenders created by Joran.
	 * @return the map of appenders
	 * @throws IllegalStateException if no reference to the map has yet been stored
	 */
	public static synchronized Map<String, Appender<ILoggingEvent>> getAppenderMap() {
		if (instance == null) {
			throw new IllegalStateException(
					"No reference has yet been stored.");
		}
		return instance.getAppenderBag();
	}

	private Map<String, Appender<ILoggingEvent>> getAppenderBag() {
		return appenderBag;
	}

}
