package edu.vu.isis.logger.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.FileObserver;
import android.os.Handler;

/**
 * A log reader designed to read logs from files. This log reader is designed to
 * allow only much of the file as necessary to be read at one time. It has the
 * capability to read both backwards and forwards through a file. However, it
 * may also be used to read straight through a file in only one direction if
 * desired.
 * <p/>
 * If this log reader will be used to read from a file only once and only in one
 * direction, then no care has to be taken about the handling of the data
 * previously read. However, if this log reader will be used to read back and
 * forth in a file, then it is important to ensure that the following
 * information is kept in mind.
 * <p/>
 * When constructing a FileLogReader, one must specify a parameter known as the
 * spread limit. This is the maximum number of lines that can be between this
 * log reader's top and bottom line markers in the file. When this limit is
 * reached, the FileLogReader shifts both of its line markers forward or
 * backward when the scrollUp() or scrollDown() methods are called. This is to
 * allow client classes to read only new data as it is needed and throw away old
 * data. This is analogous to scrolling through a file on a computer screen. As
 * a user scrolls down through a file, the newest, bottommost line comes on
 * screen and all of the other lines shift upward, causing the oldest, topmost
 * line to be bumped off screen. If the user then decides to scroll upward, the
 * topmost line that was just bumped off screen comes back on screen and the
 * newest, bottommost line is pushed off screen again. This is the model that
 * was in mind for this class to facilitate.
 * <p/>
 * This log reader does not cache previously read data to accomplish this
 * behavior. It only keeps track of the position of the top and bottom lines
 * within its spread limit. It is the responsibility of the client class to
 * manage the lines as they are read and do whatever is necessary to ensure that
 * when scrollUp() or scrollDown() is called, the expected lines are in fact the
 * ones that are returned.
 * 
 * @author Nick King
 * 
 */
public class FileLogReader extends LogReader {

	public static final LogElement BEG_OF_FILE = new LogElement(LogLevel.None,
			ByteBuffers.BEG_OF_TEXT_STR);
	public static final LogElement END_OF_FILE = new LogElement(LogLevel.None,
			ByteBuffers.END_OF_TEXT_STR);

	// private final MyFileObserver mObserver;
	private final File mFile;
	private final ScrollingFileReader mReader;

	/**
	 * @param context
	 *            -- the Context of the Activity using this FileLogReader
	 * @param handler
	 *            -- the Handler to which messages should be posted
	 * @param filepath
	 *            -- the path of the file which will be read
	 * @param spreadLimit
	 *            -- the farthest apart the top and bottom line markers can be
	 *            before they are moved simultaneously when a new line is read
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public FileLogReader(Context context, Handler handler, String filepath,
			int spreadLimit) throws IOException, FileNotFoundException {

		this(context, handler, new File(filepath), spreadLimit);

	}

	/**
	 * @param context
	 *            -- the Context of the Activity using this FileLogReader
	 * @param handler
	 *            -- the Handler to which messages should be posted
	 * @param file
	 *            -- the file which will be read
	 * @param spreadLimit
	 *            -- the farthest apart the top and bottom line markers can be
	 *            before they are moved simultaneously when a new line is read
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public FileLogReader(Context context, Handler handler, File file,
			int spreadLimit) throws IOException, FileNotFoundException {
		mReader = new ScrollingFileReader(file, spreadLimit);
		mFile = file;
		mContext = context;
		mHandler = handler;
		// mObserver = new MyFileObserver(file.getAbsolutePath(),
		// FileObserver.MODIFY);
	}

	/*
	 * The functionality for displaying new file contents as they are written
	 * has not been added yet, so the overidden methods below essentially do
	 * nothing.
	 */

	@Override
	public void start() {
		// mObserver.startWatching();
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
		// mObserver.stopWatching();
	}

	/**
	 * Scroll down through the log file, getting the next corresponding
	 * LogElement.
	 * 
	 * @return -- the next LogElement, or END_OF_FILE if the end of the file was
	 *         reached
	 */
	public LogElement scrollDown() {
		String str = mReader.stepForward();
		if (str.equals(ByteBuffers.END_OF_TEXT_STR))
			return END_OF_FILE;
		if (str.endsWith(ByteBuffers.END_OF_TEXT_STR)) {
			// Trim off the EOF character given to us by ByteBuffers
			str = str.substring(0, str.length() - 1);
		}
		return new LogElement(getCorrespondingLevelIfIsColored(str), str);
	}

	/**
	 * Scroll up through the log file, getting the next corresponding
	 * LogElement.
	 * 
	 * @return -- the next LogElement, or BEG_OF_FILE if the beginning of the
	 *         file was reached
	 */
	public LogElement scrollUp() {
		String str = mReader.stepBackward();
		if (str.equals(ByteBuffers.BEG_OF_TEXT_STR))
			return BEG_OF_FILE;
		if (str.startsWith(ByteBuffers.BEG_OF_TEXT_STR)) {
			// Trim off the BOF character given to us by ByteBuffers
			str = str.substring(1);
		}
		return new LogElement(getCorrespondingLevelIfIsColored(str), str);
	}

	public boolean atEndOfFile() {
		final String str = mReader.peekForward();
		return str == ByteBuffers.END_OF_TEXT_STR;
	}

	public boolean atBegOfFile() {
		final String str = mReader.peekBackward();
		return str == ByteBuffers.BEG_OF_TEXT_STR;
	}

	/**
	 * Scroll down through the file as far as possible until the spread limit is
	 * reached
	 * 
	 * @return a list of all LogElements read
	 */
	public List<LogElement> fillDown() {

		final String[] str = mReader.leapForward();
		final List<LogElement> logList = new ArrayList<LogElement>();

		for (int i = 0; i < str.length; i++) {
			// Return early if we reach the end of the file
			if (str[i].equals(ByteBuffers.END_OF_TEXT_STR))
				return logList;
			LogElement element = new LogElement(
					getCorrespondingLevelIfIsColored(str[i]), str[i]);
			logList.add(element);
		}

		return logList;

	}

	public int getSpreadLimit() {
		return mReader.getSpreadLimit();
	}

	/**
	 * Scroll up through the file as far as possible until the spread limit is
	 * reached
	 * 
	 * @return a list of all LogElements read
	 */
	public List<LogElement> fillUp() {
		final String[] str = mReader.leapBackward();
		final List<LogElement> logList = new ArrayList<LogElement>();

		for (int i = 0; i < str.length; i++) {
			// Return early if we reach the end of the file
			if (str[i].equals(ByteBuffers.END_OF_TEXT_STR))
				return logList;
			LogElement element = new LogElement(
					getCorrespondingLevelIfIsColored(str[i]), str[i]);
			logList.add(element);
		}

		Collections.reverse(logList);

		return logList;
	}

	/**
	 * Jumps to the end of the file. It is important to note that both the
	 * bottom and top line markers are moved to the same position. Therefore, it
	 * is probably desirable to call fillUp() or scrollUp() after calling this
	 * method.
	 */
	public void jumpToEndOfFile() {
		mReader.jumpToEndOfFile();
	}

	/**
	 * Jumps to the beginning of the file. It is important to note that both the
	 * bottom and top line markers are moved to the same position. Therefore, it
	 * is probably desirable to call fillDown() or scrollDown() after calling
	 * this method.
	 */
	public void jumpToBeginningOfFile() {
		mReader.jumpToBeginningOfFile();
	}

	/**
	 * Private inner class to notify us of file events. The plan is to
	 * automatically do whatever is necessary to allow the user to see the new
	 * lines in the file when they are added, but this functionality is not yet
	 * implemented.
	 */
	@SuppressWarnings("unused")
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
			System.err.println(mFile.length());
		}

	}

}
