package edu.vu.isis.logger.ui;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

public class AppenderStore {

	private static AppenderStore singleton = null;
	private final ArrayList<Appender<ILoggingEvent>> appenderList =
			new ArrayList<Appender<ILoggingEvent>>();
	
	// Hide the default constructor to prevent instantiation
	private AppenderStore() {}
	
	public static AppenderStore getInstance() {
		if(singleton == null) {
			singleton = new AppenderStore();
		}
		return singleton;
	}
	
	
	public synchronized void addAppender(Appender<ILoggingEvent> appender) {
		if(!appenderList.contains(appender)) appenderList.add(appender);
	}
	
	
	public synchronized void removeAppender(Appender<ILoggingEvent> appender) {
		appenderList.remove(appender);
	}
	
	
	public synchronized List<Appender<ILoggingEvent>> getAppenders() {
		return appenderList;
	}
	
	
	public synchronized void clearStore() {
		appenderList.clear();
	}
	
	
}
