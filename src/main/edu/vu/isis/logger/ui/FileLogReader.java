package edu.vu.isis.logger.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.FileObserver;
import android.os.Handler;

public class FileLogReader extends LogReader {
	
	public static final LogElement BEG_OF_FILE = new LogElement(LogLevel.None, ByteBuffers.BEG_OF_TEXT_STR);
	public static final LogElement END_OF_FILE = new LogElement(LogLevel.None, ByteBuffers.END_OF_TEXT_STR);
	
	private final MyFileObserver mObserver;
	private final ScrollingFileReader mReader;
	
	public FileLogReader(Context context, Handler handler, String filepath,
			int spreadLimit) throws IOException, FileNotFoundException {

		this(context, handler, new File("/sdcard/lined.log"), spreadLimit);

	}
	
	
	public FileLogReader(Context context, Handler handler, File file,
			int spreadLimit) throws IOException, FileNotFoundException {
		mReader = new ScrollingFileReader(file, spreadLimit);
		mContext = context;
		mHandler = handler;
		mObserver = new MyFileObserver(file.getAbsolutePath(), FileObserver.MODIFY);
	}
	

	@Override
	public void start() {
		// TODO: Start the FileObserver
		//mObserver.startWatching();
	}
	
	@Override
	public void resume() {
		// TODO: Resume the FileObserver
	}
	
	
	@Override
	public void pause() {
		// TODO: Pause the FileObserver
	}
	
	@Override
	public void terminate() {
		// TODO: Stop the FileObserver
	}
	
	
	/**
	 * Scroll down through the log file, getting the next corresponding
	 * LogElement.
	 * @return -- the next LogElement, or END_OF_FILE if the end of the file was reached
	 */
	public LogElement scrollDown() {
		String str = mReader.stepForward();
		if(str.equals(ByteBuffers.END_OF_TEXT_STR)) return END_OF_FILE;
		if(str.endsWith(ByteBuffers.END_OF_TEXT_STR)) {
			// Trim off the EOF character given to us by ByteBuffers
			str = str.substring(0, str.length()-1);
		}
		return new LogElement(getCorrespondingLevel(str), str);
	}
	
	
	/**
	 * Scroll up through the log file, getting the next corresponding
	 * LogElement.
	 * @return -- the next LogElement, or BEG_OF_FILE if the beginning of the file was reached
	 */
	public LogElement scrollUp() {
		String str = mReader.stepBackward();
		if(str.equals(ByteBuffers.BEG_OF_TEXT_STR)) return BEG_OF_FILE;
		if(str.startsWith(ByteBuffers.BEG_OF_TEXT_STR)) {
			// Trim off the BOF character given to us by ByteBuffers
			str = str.substring(1);
		}
		return new LogElement(getCorrespondingLevel(str), str);
	}
	
	
	public boolean atEndOfFile() {
		final String str = mReader.peekForward();
		return str == ByteBuffers.END_OF_TEXT_STR;
	}
	
	
	public boolean atBegOfFile() {
		final String str = mReader.peekBackward();
		return str == ByteBuffers.BEG_OF_TEXT_STR;
	}
	

	public List<LogElement> fillDown() {
		
		final String[] str = mReader.leapForward();
		final List<LogElement> logList = new ArrayList<LogElement>();
		
		for(int i=0; i<str.length; i++) {
			// Return early if we reach the end of the file
			if(str[i].equals(ByteBuffers.END_OF_TEXT_STR)) return logList;
			LogElement element = new LogElement(getCorrespondingLevel(str[i]), str[i]);
			logList.add(element);
		}
		
		return logList;
		
	}
	
	
	public int getSpreadLimit() {
		return mReader.getSpreadLimit();
	}
	
	
	public List<LogElement> fillUp() {
		
		final String[] str = mReader.leapBackward();
		final List<LogElement> logList = new ArrayList<LogElement>();
		
		for(int i=0; i<str.length; i++) {
			// Return early if we reach the end of the file
			if(str[i].equals(ByteBuffers.END_OF_TEXT_STR)) return logList;
			LogElement element = new LogElement(getCorrespondingLevel(str[i]), str[i]);
			logList.add(element);
		}
		
		return logList;
		
	}
	
	
	
//	class ReadFileTask extends AsyncTask<Void, Void, Void> {
//
//		private FileLogReader parent = FileLogReader.this;
//		
//		@Override
//		protected Void doInBackground(Void... unused) {
//			
//			parent.bufferNewData();
//			parent.sendCacheAndClear();
//			parent.observer.startWatching();
//			
//			return null;
//		}
//		
//	}
	
	
	/**
	 * Private inner class to notify us of file events
	 */
	private class MyFileObserver extends FileObserver {
		
		private FileLogReader parent = FileLogReader.this;
		
		public MyFileObserver(String path) {
			super(path);
		}

		public MyFileObserver(String path, int mask) {
			super(path, mask);
		}

		@Override
		public void onEvent(int event, String path) {
//			parent.bufferNewData();
//			parent.sendCacheAndClear();
		}

	}

}
