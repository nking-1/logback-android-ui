package edu.vu.isis.logger.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class LogcatLogReader extends LogReader {

	private static final int BUFFER_SIZE = 1024;
	private static final long SEND_DELAY = 10;
	
	private final BufferedReader mReader;
	private final ArrayList<LogElement> mLogCache = new ArrayList<LogElement>();
	
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	
	public LogcatLogReader(Context context, Handler handler) throws IOException {

		this.mContext = context;
		this.mHandler = handler;
		final Process logcatProcess = Runtime.getRuntime().exec("logcat");
		this.mReader = new BufferedReader(new InputStreamReader(
				logcatProcess.getInputStream()), BUFFER_SIZE);
		
	}
	
	
	/**
	 * Sends the current log cache and clears it if sending has not been
	 * paused and the log cache is not empty
	 */
	private void sendCacheAndClear() {
		//checkValidState();
		// both methods check the state
		if(!this.mLogCache.isEmpty()) {
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
			msg.what = LogcatLogViewer.CONCAT_DATA_MSG;
			
			synchronized(this.mLogCache) {
				msg.obj = this.mLogCache.clone();
			}

			msg.setTarget(mHandler);
			msg.sendToTarget();
		}
		
	}
	

	@Override
	public void start() {
		
		super.start();
		
		resume();
		
		this.myReadThread = new ReadThread();
		this.myReadThread.start();
		this.scheduler.scheduleWithFixedDelay(this.updateRunnable, SEND_DELAY, SEND_DELAY, TimeUnit.MILLISECONDS);
	}
	
	
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
	// and resume().  Current thoughts:  what happens if pause() and resume()
	// are called at nearly the same time by different threads?  Could the
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
	 * Pauses reading from LogCat, but has no effect on whether cache
	 * messages are sent.
	 */
	private void stopReading() {
		this.isReading.set(false);
	}
	
	
	/**
	 * Resumes reading from LogCat, but has no effect on whether cache
	 * messages are sent.
	 */
	private void resumeReading() {
		this.isReading.set(true);
	}
	
	
	/**
	 * Clears the log cache
	 */
	private void clearCache() {
		checkValidState();
		synchronized(mLogCache) {
			mLogCache.clear();
		}
	}
	
	
	private final AtomicBoolean isReading = new AtomicBoolean(false);
	
    private ReadThread myReadThread;
	
	private class ReadThread extends Thread {

		private LogcatLogReader parent = LogcatLogReader.this;

		@Override
		public void run() {

			while (!Thread.currentThread().isInterrupted()) {
				if (parent.isReading.get()) {
					try {

						String nextLine = parent.mReader.readLine();
						final LogLevel level = getCorrespondingLevel(nextLine);
						synchronized (parent.mLogCache) {
							parent.mLogCache
									.add(new LogElement(level, nextLine));
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
			
				if(!parent.isPaused.get()) {
					parent.sendCacheAndClear();
				}
			
		}
		
	};

}
