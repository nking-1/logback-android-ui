package edu.vu.isis.logger.ui;

import java.net.URL;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class LogbackReconfigureForcer {

	/**
	 * Forces Logback to reconfigure itself according to the given file url and
	 * logger context
	 * 
	 * @param url
	 *            -- the url of the logback xml file
	 * @param loggerContext
	 *            -- the context to reconfigure <i>(use
	 *            LoggerFactory.getILoggerFactory() and cast the return value to
	 *            a LoggerContext to get your logger context)</i>
	 */
	public static void forceReconfigure(URL url, LoggerContext loggerContext) {
		
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(loggerContext);
		
		// We want to reset everything before Joran reconfigures the context
		// in order to keep from having duplicate appenders and such
		AppenderStore.getInstance().clearStore();
		loggerContext.reset();
		
		try {
			configurator.doConfigure(url);
		} catch (JoranException e) {
			e.printStackTrace();
		}
		
	}

}
