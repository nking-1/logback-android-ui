package edu.vu.isis.logger.util;

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.Handler;

/**
 * A base class for all LogReaders. All LogReaders are responsible for reading
 * log messages from some source and wrapping them inside LogElement objects.
 * @author Nick King
 * 
 */
public abstract class LogReader {

	/** the handler to which Messages are sent */
	Handler mHandler;

	/** the Context which is using this LogReader */
	Context mContext;

	/** Whether this LogReader has been paused or resumed */
	final AtomicBoolean isPaused = new AtomicBoolean(true);
	final AtomicBoolean hasBeenStarted = new AtomicBoolean(false);
	final AtomicBoolean hasBeenTerminated = new AtomicBoolean(false);

	/**
	 * Indicates whether the logs should be colored
	 */
	final AtomicBoolean isColored = new AtomicBoolean(false);

	/**
	 * Tells this LogReader to start itself
	 */
	public void start() {
		if (this.hasBeenStarted.getAndSet(true)) {
			throw new IllegalStateException(
					"This LogReader has already been started");
		}
	}

	/**
	 * Tells this LogReader to halt all reading and close its streams. This
	 * LogReader may not be used after this method has been called.
	 */
	public synchronized void terminate() {
		checkValidState();
		this.hasBeenTerminated.set(true);
	}

	/**
	 * Checks whether this LogReader has been started and has not been
	 * terminated and throws exceptions if these conditions are not true.
	 */
	synchronized void checkValidState() {
		if (!this.hasBeenStarted.get()) {
			throw new IllegalStateException(
					"This LogReader has not been started");
		}

		if (this.hasBeenTerminated.get()) {
			throw new IllegalStateException(
					"This LogReader has been terminated");
		}
	}

	/**
	 * Tells this LogReader to temporarily pause reading
	 */
	public synchronized void pause() {
		checkValidState();
		this.isPaused.set(true);
	}

	/**
	 * Tells this LogReader to resume reading
	 */
	public synchronized void resume() {
		checkValidState();
		this.isPaused.set(false);
	}

	/**
	 * Attaches a Handler to this LogReader.
	 * 
	 * @param handler
	 *            -- the Handler to which Messages will be sent
	 */
	public synchronized void setHandler(Handler handler) {
		checkValidState();
		this.mHandler = handler;
	}

	public boolean isColored() {
		return isColored.get();
	}

	public void setColored(boolean colored) {
		isColored.set(colored);
	}

	/**
	 * Parses a String to get LogLevel that corresponds to that String. The
	 * default behavior of this method is that the first char in the String will
	 * be used to determine the LogLevel.
	 * <p>
	 * The characters and their corresponding levels are: <list>
	 * <li>V: Verbose
	 * <li>T: Trace
	 * <li>D: Debug
	 * <li>I: Info
	 * <li>W: Warn
	 * <li>E: Error
	 * <li>F: Fail
	 * <li>All others: None </list>
	 * <p>
	 * The characters are case sensitive.
	 * 
	 * @param str
	 *            -- the String to parse
	 * @return the corresponding LogLevel
	 */
	public static LogLevel getCorrespondingLevel(String str) {
		if (str == null)
			return LogLevel.None;
		if (str.length() == 0)
			return LogLevel.None;

		final char firstChar = str.charAt(0);
		switch (firstChar) {
		case 'V':
			return LogLevel.Verbose;
		case 'T':
			return LogLevel.Trace;
		case 'D':
			return LogLevel.Debug;
		case 'I':
			return LogLevel.Info;
		case 'W':
			return LogLevel.Warn;
		case 'E':
			return LogLevel.Error;
		case 'F':
			return LogLevel.Fail;
		default:
			return LogLevel.None;
		}
	}

	/**
	 * Convenience method to get a LogLevel for a String only if we have been
	 * set to color logs
	 * 
	 * @param str
	 *            -- the String to parse
	 * @return the corresponding LogLevel for str if isColored is true. If
	 *         isColored is false, LogLevel.None will be returned.
	 */
	LogLevel getCorrespondingLevelIfIsColored(String str) {
		if (isColored.get()) {
			return getCorrespondingLevel(str);
		} else {
			return LogLevel.None;
		}
	}

}
