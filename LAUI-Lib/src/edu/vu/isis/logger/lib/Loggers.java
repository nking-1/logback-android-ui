package edu.vu.isis.logger.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.slf4j.LoggerFactory;

import android.util.Log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * This class contains a collection of static convenience methods to use for
 * Logback loggers.
 * 
 * @author Nick King
 * 
 */
public final class Loggers {

	public static final Logger ROOT_LOGGER = getLoggerByName(Logger.ROOT_LOGGER_NAME);

	private static final Comparator<Appender<?>> APPENDER_COMPARATOR = new Comparator<Appender<?>>() {

		@Override
		public int compare(Appender<?> app1, Appender<?> app2) {
			return app1.getName().compareTo(app2.getName());
		}

	};

	// Suppress default constructor to prevent instantiation
	// (See Item 4 in Effective Java, Second Edition)
	private Loggers() {
	}

	/**
	 * Returns a Logback Logger that has the given name. This method is simply a
	 * wrapper of the LoggerFactory.getLogger(String name) method. However, it
	 * hides an ugly type cast that results from the Logback Logger class
	 * implementing the slf4j Logger interface.
	 * 
	 * @param name
	 *            -- the name of the Logger to retrieve
	 * @return -- the Logger whose name was given
	 */
	public static Logger getLoggerByName(final String name) {
		return (Logger) LoggerFactory.getLogger(name);
	}

	/**
	 * Returns the parent Logger of the given Logger
	 * 
	 * @param childLogger
	 *            -- the Logger whose parent to return
	 * @return -- the parent of the given Logger
	 */
	public static Logger getParentLogger(final Logger childLogger) {
		return getLoggerByName(getParentLoggerName(childLogger));
	}

	/**
	 * Returns the name of the parent logger of the given logger
	 * 
	 * @param childLogger
	 *            -- the logger whose parent's name will be found
	 * @return -- the name of the parent logger
	 */
	public static String getParentLoggerName(final Logger childLogger) {
		return getParentLoggerName(childLogger.getName());
	}

	/**
	 * Returns the name of the parent logger of the given child logger name
	 * 
	 * @param childName
	 *            -- the name of the logger whose parent's name will be found
	 * @return -- the name of the parent logger
	 */
	public static String getParentLoggerName(final String childName) {
		final int lastDotIndex = childName.lastIndexOf('.');
		final String parentLoggerName;
		if (lastDotIndex == -1) {
			parentLoggerName = Logger.ROOT_LOGGER_NAME;
		} else {
			parentLoggerName = childName.substring(0, lastDotIndex);
		}
		return parentLoggerName;
	}

	/**
	 * Determines whether a logger has an explicitly set level or if it is
	 * inheriting its level from its parent logger(s).
	 * 
	 * @param logger
	 *            -- the logger to analyze
	 * @return -- true if logger has no explicit level, false if it does.
	 */
	public static boolean isInheritingLevel(final Logger logger) {
		return logger.getLevel() == null;
	}

	public static List<Logger> getChangedLoggers(final Tree<Logger> loggerTree) {
		return findExplicitlySetLoggers(loggerTree, new ArrayList<Logger>());
	}

	private static List<Logger> findExplicitlySetLoggers(
			final Tree<Logger> loggerTree, final List<Logger> loggerList) {

		final Logger logger = loggerTree.getPayload();
		if (!Loggers.isInheritingLevel(logger) || !logger.isAdditive()) {
			loggerList.add(logger);
		}

		for (Tree<Logger> subTree : loggerTree.getSubTrees()) {
			Loggers.findExplicitlySetLoggers(subTree, loggerList);
		}

		return loggerList;
	}

	/**
	 * Determines whether a logger has an attached Appender. Note that this
	 * method only accounts for appenders explicitly attached to the logger;
	 * loggers also inherit appenders from their parents if their additivity
	 * flag is set to true. For instance, in the case that a logger has no
	 * appenders attached to it but its parent has two appenders attached, this
	 * method would still return <b>false</b>.
	 * 
	 * @param logger
	 *            -- the logger to analyze
	 * @return -- true if the logger has an Appender explicitly attached, false
	 *         if not.
	 */
	public static boolean hasAttachedAppender(final Logger logger) {
		return logger.iteratorForAppenders().hasNext();
	}

	/**
	 * Returns all appenders that the logger has explicitly attached to it. Note
	 * that loggers inherit appenders from their parents if their additivity
	 * flag is set to true, but this method does <b>not</b> return the appenders
	 * that this logger is inheriting.
	 * <p>
	 * The List returned will be in alphabetical order by Appender name.
	 * 
	 * @param logger
	 *            -- the Logger whose appenders to retrieve
	 * @return -- the appenders explicitly attached to the Logger
	 */
	public static List<Appender<ILoggingEvent>> getAttachedAppenders(
			final Logger logger) {

		final Iterator<Appender<ILoggingEvent>> it = logger
				.iteratorForAppenders();
		final List<Appender<ILoggingEvent>> appenderList = new ArrayList<Appender<ILoggingEvent>>();

		while (it.hasNext()) {
			appenderList.add(it.next());
		}

		Collections.sort(appenderList, APPENDER_COMPARATOR);

		return appenderList;

	}

	/**
	 * Returns all appenders that currently affect the logger, including the
	 * appenders that the logger is inheriting from its parents, if any.
	 * <p>
	 * The List returned will be in alphabetical order by Appender name.
	 * 
	 * @param logger
	 *            -- the Logger whose appenders to retrieve
	 * @return -- all appenders associated with the logger
	 */
	public static List<Appender<ILoggingEvent>> getEffectiveAppenders(
			final Logger logger) {

		final List<Appender<ILoggingEvent>> appenderList = new ArrayList<Appender<ILoggingEvent>>();
		appenderList.addAll(getAppenderSet(logger,
				new HashSet<Appender<ILoggingEvent>>()));

		Collections.sort(appenderList, APPENDER_COMPARATOR);

		return appenderList;

	}

	/**
	 * We recursively step up through the Logger family until we either reach
	 * the ROOT Logger or reach the closest Logger with its additivity flag set
	 * to false. Each time we step through the family we add the appenders
	 * attached to the current Logger in question to a HashSet, then pass that
	 * set on to the next recursive method call.
	 */
	private static HashSet<Appender<ILoggingEvent>> getAppenderSet(
			final Logger logger, final HashSet<Appender<ILoggingEvent>> soFar) {

		soFar.addAll(getAttachedAppenders(logger));

		if (!logger.isAdditive() || logger.getName() == Logger.ROOT_LOGGER_NAME) {
			return soFar;
		} else {
			return getAppenderSet(getParentLogger(logger), soFar);
		}

	}

	/**
	 * Determines whether an Appender is <i>effectively</i> attached to a Logger
	 * through inheritance. In other words, determines whether an Appender is
	 * affecting a given Logger in any way. Loggers themselves have an
	 * isAttached() method, but it only accounts for appenders that are
	 * explicitly attached to that Logger.
	 * 
	 * @param logger
	 *            -- the Logger to analyze
	 * @param app
	 *            -- the Appender to check for attachment to logger
	 * @return -- whether app is effectively attached to logger
	 */
	public static boolean isAttachedEffective(Logger logger,
			Appender<ILoggingEvent> app) {
		return getEffectiveAppenders(logger).contains(app);
	}

	/**
	 * Determines whether a Logger has the same appenders <i>explicitly</i>
	 * attached to it as its parent. This method does not account for
	 * additivity.
	 * 
	 * @param logger
	 *            -- the child Logger to check
	 * @return -- whether logger has the same attached appenders as its parent
	 */
	public static boolean hasSameAttachedAppendersAsParent(Logger logger) {
		return getAttachedAppenders(logger).equals(
				getAttachedAppenders(getParentLogger(logger)));
	}

	/**
	 * Determines whether a Logger has the same Appenders attached to it as the
	 * Appenders that are affecting its parent.
	 * 
	 * @param logger
	 * @return
	 */
	public static boolean hasSameAppendersAsParent(Logger logger) {
		List<Appender<ILoggingEvent>> attached = getAttachedAppenders(logger);
		List<Appender<ILoggingEvent>> effective = getEffectiveAppenders(getParentLogger(logger));
		Log.v("Loggers.java", "Appenders attached to Logger " + logger.getName() + ": " + attached.toString());
		Log.v("Loggers.java", "Effective Appenders on Logger " + getParentLogger(logger).getName() + ": " + effective.toString());
		return attached.equals(effective);
	}

	/**
	 * Sets the additivity flag for all loggers in the given Tree.
	 * 
	 * @param loggerTree
	 *            -- the Tree containing the loggers
	 * @param additive
	 *            -- what to set the additivity flag to
	 */
	public static void setAdditivityForAll(Tree<Logger> loggerTree,
			boolean additive) {
		final Logger headLogger = loggerTree.getPayload();
		headLogger.setAdditive(additive);
		for (Logger subLogger : loggerTree.getSuccessors(headLogger)) {
			setAdditivityForAll(loggerTree.getTree(subLogger), additive);
		}
	}

	/**
	 * Attaches a given Appender to all loggers in a Tree
	 * 
	 * @param loggerTree
	 *            -- the Tree containing the loggers
	 * @param app
	 *            -- the Appender to attach to all loggers in the Tree
	 */
	public static void addAppenderForAll(Tree<Logger> loggerTree,
			Appender<ILoggingEvent> app) {
		final Logger headLogger = loggerTree.getPayload();
		headLogger.addAppender(app);
		for (Logger subLogger : loggerTree.getSuccessors(headLogger)) {
			addAppenderForAll(loggerTree.getTree(subLogger), app);
		}
	}

	/**
	 * Detaches a given Appender from all loggers in a Tree
	 * 
	 * @param loggerTree
	 *            -- the Tree containing the loggers
	 * @param app
	 *            -- the Appender to detach from all loggers in the Tree
	 */
	public static void detachAppenderForAll(Tree<Logger> loggerTree,
			Appender<ILoggingEvent> app) {
		final Logger headLogger = loggerTree.getPayload();
		headLogger.detachAppender(app);
		for (Logger subLogger : loggerTree.getSuccessors(headLogger)) {
			detachAppenderForAll(loggerTree.getTree(subLogger), app);
		}
	}

	/**
	 * Sets all loggers in a Tree to have the same attached Appender settings as
	 * the Logger at the top of the Tree
	 * 
	 * @param loggerTree
	 *            -- the Tree containing the loggers
	 */
	public static void copyHeadAppenderSettings(Tree<Logger> loggerTree) {

		final Logger headLogger = loggerTree.getPayload();
		final Collection<Appender<ILoggingEvent>> appenders = getAppendersInLoggerTree(loggerTree);

		for (Appender<ILoggingEvent> app : appenders) {
			if (headLogger.isAttached(app)) {
				addAppenderForAll(loggerTree, app);
			} else {
				detachAppenderForAll(loggerTree, app);
			}
		}

	}

	/**
	 * Returns all appenders attached to any Logger in the Tree.
	 * 
	 * @param loggerTree
	 *            -- the Tree containing the loggers
	 * @return -- a Collection of appenders in the Tree
	 */
	public static Collection<Appender<ILoggingEvent>> getAppendersInLoggerTree(
			final Tree<Logger> loggerTree) {
		return getAppendersInLoggerTree(loggerTree,
				new HashSet<Appender<ILoggingEvent>>());
	}

	private static Collection<Appender<ILoggingEvent>> getAppendersInLoggerTree(
			final Tree<Logger> loggerTree,
			final HashSet<Appender<ILoggingEvent>> soFar) {

		final Logger headLogger = loggerTree.getPayload();

		for (Appender<ILoggingEvent> app : getAttachedAppenders(headLogger)) {
			soFar.add(app);
		}

		for (Logger subLogger : loggerTree.getSuccessors(headLogger)) {
			getAppendersInLoggerTree(loggerTree.getTree(subLogger), soFar);
		}

		return soFar;
	}

	public static void clearAppenders(Logger logger) {
		Appender<ILoggingEvent> a = null;
		for (Iterator<Appender<ILoggingEvent>> itr = logger
				.iteratorForAppenders(); itr.hasNext(); a = itr.next()) {
			logger.detachAppender(a);
		}
	}

}
