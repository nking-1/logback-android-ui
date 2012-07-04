package edu.vu.isis.logger.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

/**
 * A log reader that reads from the Logcat output stream. A LogcatLogReader
 * sends all new LogElements read to a Handler through Messages tagged with the
 * constant CONCAT_DATA_MSG. These messages will be sent with a fixed period.
 * <p>
 * If an invalid regular expression is given to this log reader, then a Message
 * tagged with the constant NOTIFY_INVALID_REGEX_MSG will be sent to the
 * Handler.
 * <p>
 * It is extremely important to call the terminate method on any
 * LogcatLogReaders that have been started. This allows the log readers to
 * terminate their threads and stop caching new messages. Failing to call
 * terminate will result in a nasty memory leak and reduced performance from the
 * threads not terminating after their references are lost.
 * 
 * @author Nick King
 * 
 */
public class LogcatLogReader extends LogReader {

	private static final int BUFFER_SIZE = 1024;
	private static final long SEND_DELAY = 10;

	public static final int CONCAT_DATA_MSG = 0;
	public static final int NOTIFY_INVALID_REGEX_MSG = 1;

	private final BufferedReader mReader;
	private final ArrayList<LogElement> mLogCache = new ArrayList<LogElement>();
	private Pattern mPattern;

	private final ScheduledExecutorService scheduler = Executors
			.newSingleThreadScheduledExecutor();

	public LogcatLogReader(Context context, Handler handler, String regex)
			throws IOException {

		this.mContext = context;
		this.mHandler = handler;
		setRegex(regex);
		String command = "logcat";
		final Process logcatProcess = Runtime.getRuntime().exec(command);
		this.mReader = new BufferedReader(new InputStreamReader(
				logcatProcess.getInputStream()), BUFFER_SIZE);

	}

	/**
	 * Sends the current log cache and clears it if sending has not been paused
	 * and the log cache is not empty
	 */
	private void sendCacheAndClear() {
		// checkValidState();
		// both methods check the state
		if (!this.mLogCache.isEmpty()) {
			sendCacheMsg();
			clearCache();
		}
	}

	/**
	 * Sends the log cache to the attached Handler
	 */
	private void sendCacheMsg() {

		checkValidState();

		if (!this.isPaused.get()) {
			final Message msg = Message.obtain();
			msg.what = CONCAT_DATA_MSG;

			synchronized (this.mLogCache) {
				msg.obj = this.mLogCache.clone();
			}

			msg.setTarget(mHandler);
			msg.sendToTarget();
		}

	}

	/**
	 * Starts this LogcatLogReader's reading and sending threads. It is
	 * extremely important that after this method is called, the terminate
	 * method is called when this object is no longer needed.
	 */
	@Override
	public void start() {

		super.start();

		resume();

		this.myReadThread = new ReadThread();
		this.myReadThread.start();
		this.scheduler.scheduleWithFixedDelay(this.updateRunnable, SEND_DELAY,
				SEND_DELAY, TimeUnit.MILLISECONDS);
	}

	/**
	 * Allows this LogcatLogReader to terminate its thread. It is extremely
	 * important to call this method when this object is no longer needed.
	 */
	@Override
	public void terminate() {
		super.terminate();
		this.myReadThread.cancel();
		this.scheduler.shutdown();
		this.mLogCache.clear();
		try {
			this.mReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// TODO: Ensure that synchronization is actually necessary for pause()
	// and resume(). Current thoughts: what happens if pause() and resume()
	// are called at nearly the same time by different threads? Could the
	// object end up in a state in which it is paused but still reading or
	// not reading but unpaused?

	@Override
	public synchronized void pause() {
		super.pause();
		stopReading();
	}

	@Override
	public synchronized void resume() {
		super.resume();
		resumeReading();
	}

	/**
	 * Pauses reading from LogCat, but has no effect on whether cache messages
	 * are sent.
	 */
	private void stopReading() {
		this.isReading.set(false);
	}

	/**
	 * Resumes reading from LogCat, but has no effect on whether cache messages
	 * are sent.
	 */
	private void resumeReading() {
		this.isReading.set(true);
	}

	/**
	 * Clears the log cache
	 */
	private void clearCache() {
		checkValidState();
		synchronized (mLogCache) {
			mLogCache.clear();
		}
	}

	public void setRegex(String newRegex) {
		try {
			mPattern = Pattern.compile(newRegex);
		} catch (PatternSyntaxException pe) {
			mPattern = Pattern.compile("");
			mHandler.sendEmptyMessage(NOTIFY_INVALID_REGEX_MSG);
		}
	}

	private final AtomicBoolean isReading = new AtomicBoolean(false);

	private ReadThread myReadThread;

	private class ReadThread extends Thread {

		@Override
		public void run() {

			while (!Thread.currentThread().isInterrupted()) {
				if (isReading.get()) {
					try {
						String nextLine = mReader.readLine();
						Matcher matcher = mPattern.matcher(nextLine);
						if (!matcher.find())
							continue;
						final LogLevel level = getCorrespondingLevel(nextLine);
						synchronized (mLogCache) {
							mLogCache.add(new LogElement(level, nextLine));
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}

		public void cancel() {
			Thread.currentThread().interrupt();
		}

	};

	private Runnable updateRunnable = new Runnable() {

		private LogcatLogReader parent = LogcatLogReader.this;

		@Override
		public void run() {

			if (!parent.isPaused.get()) {
				parent.sendCacheAndClear();
			}

		}

	};

}
