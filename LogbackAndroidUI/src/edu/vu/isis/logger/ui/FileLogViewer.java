package edu.vu.isis.logger.ui;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.TextView;
import android.widget.Toast;
import ch.qos.logback.classic.Level;
import edu.vu.isis.logger.R;
import edu.vu.isis.logger.util.FileLogReader;
import edu.vu.isis.logger.util.LogElement;
import edu.vu.isis.logger.util.LogElementAdapter;

/**
 * A log viewer designed to display logs from files. This class is not
 * responsible for reading from the file. It is only responsible for displaying
 * the contents of the file as read by its log reader.
 * 
 * An important optimization that has been made in this class is that it does
 * not load all of the contents of a file at once. Because only a portion of a
 * file can be viewed at a time on a small screen, this log viewer only loads
 * enough of the file to allow the user to read a file smoothly. When the user
 * scrolls to the edge of the currently loaded portion of the file, more data is
 * loaded in the direction of the scrolling, and some of the data is cleared in
 * the other direction. This behavior allows us to read files of extremely large
 * size without blocking the UI thread or running out of memory.
 * 
 * @author Nick King
 * 
 */
public class FileLogViewer extends LogViewerBase {

	private FileLogReader mLogReader;

	private int numEntriesToSave;
	private long lastToastTime = 0;

	private static final int JUMP_TOP_MENU = Menu.NONE + 1;
	private static final int JUMP_BOTTOM_MENU = Menu.NONE + 2;

	public final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			mAdapter.remove(mAdapter.getItem(0));
			mAdapter.notifyDataSetChanged();
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		final Display display = getWindowManager().getDefaultDisplay();
		@SuppressWarnings("deprecation")
		final int width = display.getWidth();
		@SuppressWarnings("deprecation")
		final int height = display.getHeight();
		final int largestDimension = Math.max(width, height);

		final LayoutInflater inflater = getLayoutInflater();
		final View row = inflater.inflate(R.layout.log_display_row, null);
		final TextView tv = (TextView) row.findViewById(R.id.log_display_row);

		final float textSize = tv.getTextSize();
		final int numLinesOnScreen = (int) (largestDimension / textSize);
		numEntriesToSave = 2 * numLinesOnScreen;
		final int numLines = 2 * (numLinesOnScreen + numEntriesToSave);

		String filepath = processIntent();
		if (filepath == null) {
			logger.error("Received intent without String extra for filepath");
			Toast.makeText(this, "Received invalid Intent", Toast.LENGTH_LONG);
			return;
		}

		try {
			super.mLogReader = this.mLogReader = new FileLogReader(this,
					mHandler, filepath, numLines);
		} catch (FileNotFoundException e) {
			String errorMsg = "Could not find file: " + filepath;
			Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
			logger.error(errorMsg);
			e.printStackTrace();
			return;
		} catch (IOException e) {
			String errorMsg = "Error reading from file: " + filepath;
			Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
			logger.error(errorMsg);
			e.printStackTrace();
			return;
		}

		mLogReader.start();
		setupColoringFromPrefs("colored_file_logs");
		mAdapter = new LogElementAdapter(this, R.layout.log_display_row);
		mListView.setAdapter(mAdapter);
		mAdapter.addAll(mLogReader.fillDown());

		// TODO: Take out this level setting
		((ch.qos.logback.classic.Logger) logger).setLevel(Level.DEBUG);
		logger.debug("Adapter count: {}  Spread limit: {}",
				mAdapter.getCount(), mLogReader.getSpreadLimit());
		assert (mAdapter.getCount() == mLogReader.getSpreadLimit());

		mListView.setOnScrollListener(new FileOnScrollListener());

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean returnValue = true;
		switch (item.getItemId()) {
		case JUMP_BOTTOM_MENU:
			mLogReader.jumpToEndOfFile();
			mAdapter.clear();
			mAdapter.addAll(mLogReader.fillUp());
			setScrollToBottom();
			break;
		case JUMP_TOP_MENU:
			mLogReader.jumpToBeginningOfFile();
			mAdapter.clear();
			mAdapter.addAll(mLogReader.fillDown());
			setScrollToTop();
			break;
		case OPEN_PREFS_MENU:
			mLogReader.pause();
			Intent intent = new Intent().setClass(this, LogViewerPreferences.class);
			startActivityForResult(intent, 0);
			break;
		default:
			returnValue = false;
		}
		return returnValue || super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		boolean wasColored = mLogReader.isColored();
		setupColoringFromPrefs("colored_file_logs");
		if(wasColored != mLogReader.isColored()) {
			recolorLogsInAdapter();
			mListView.invalidateViews();
		}
		mLogReader.resume();
	}

	/**
	 * Helper method to get the extra out of the intent
	 * 
	 * @return -- the String extra in the intent
	 */
	private String processIntent() {
		Object obj = getIntent().getExtras().get(EXTRA_NAME);
		if (obj instanceof String) {
			return (String) obj;
		} else {
			return null;
		}
	}

	/**
	 * Loads lines downwards to display new data from the file after the user
	 * has reached the bottom of the listview. For each line added, a line is
	 * cleared from the screen so that we uphold our log reader's contract that
	 * the first and last lines in our adapter are exactly within its top and
	 * bottom line markers.
	 * 
	 * @param firstVisiblePosition
	 *            -- the position of the first line on screen
	 * @param lastVisiblePosition
	 *            -- the position of the last line on screen
	 */
	private void loadDown(int firstVisiblePosition, int lastVisiblePosition) {

		// If we were already at the end of the file, notify the user, then
		// get out
		if (mLogReader.atEndOfFile()) {
			// Don't make a toast unless it's been at least 2 seconds since the
			// last one
			if (!enoughTimePassed())
				return;
			Toast.makeText(this, "End of file", Toast.LENGTH_SHORT).show();
			return;
		}

		final Object topMarker = mListView
				.getItemAtPosition(firstVisiblePosition - numEntriesToSave);
		final LogElement firstVisible = (LogElement) mListView
				.getItemAtPosition(firstVisiblePosition);

		// This is our first fencepost
		Object firstInList = mListView.getItemAtPosition(0);
		LogElement nextElement = mLogReader.scrollDown();

		// We loop until we reach the end marker line or until we reach
		// the beginning of the file.
		while ((firstInList != topMarker)
				&& (nextElement != FileLogReader.END_OF_FILE)) {
			mAdapter.remove((LogElement) firstInList);
			mAdapter.add(nextElement);
			firstInList = mListView.getItemAtPosition(0);
			nextElement = mLogReader.scrollDown();
		}

		// This is our last fencepost. We don't want to show an end of file
		// indicator, so we only do the final add/remove if we didn't reach
		// the end of the file
		if (nextElement != FileLogReader.END_OF_FILE) {
			mAdapter.remove((LogElement) firstInList);
			mAdapter.add(nextElement);
		}

		logger.debug("Adapter count: {}  Spread limit: {}",
				mAdapter.getCount(), mLogReader.getSpreadLimit());

		assert (mAdapter.getCount() == mLogReader.getSpreadLimit());

		mListView.setSelection(mAdapter.getPosition(firstVisible));

	}

	/**
	 * Loads lines upwards to display new data from the file after the user has
	 * reached the top of the listview. For each line added, a line is cleared
	 * from the screen so that we uphold our log reader's contract that the
	 * first and last lines in our adapter are exactly within its top and bottom
	 * line markers.
	 * 
	 * @param firstVisiblePosition
	 *            -- the position of the first line on screen
	 * @param lastVisiblePosition
	 *            -- the position of the last line on screen
	 */
	private void loadUp(int firstVisiblePosition, int lastVisiblePosition) {

		// If we were already at the beginning of the file, notify the user,
		// then get out
		if (mLogReader.atBegOfFile()) {
			// Don't make a toast unless it's been at least 2 seconds since the
			// last one
			if (!enoughTimePassed())
				return;
			Toast.makeText(this, "Beginning of file", Toast.LENGTH_SHORT)
					.show();
			return;
		}

		final Object endMarker = mListView
				.getItemAtPosition(lastVisiblePosition + numEntriesToSave);
		final LogElement firstVisible = (LogElement) mListView
				.getItemAtPosition(firstVisiblePosition);

		// This is our first fencepost
		Object lastInList = mListView
				.getItemAtPosition(mAdapter.getCount() - 1);
		LogElement nextElement = mLogReader.scrollUp();

		// We loop until we reach the end marker line or until we reach
		// the beginning of the file.
		while ((lastInList != endMarker)
				&& (nextElement != FileLogReader.BEG_OF_FILE)) {
			mAdapter.remove((LogElement) lastInList);
			mAdapter.insert(nextElement, 0);
			lastInList = mListView.getItemAtPosition(mAdapter.getCount() - 1);
			nextElement = mLogReader.scrollUp();
		}

		// This is our last fencepost. We don't want to show an end of file
		// indicator, so we only do the final add/remove if we didn't reach
		// the end of the file
		if (nextElement != FileLogReader.BEG_OF_FILE) {
			mAdapter.remove((LogElement) lastInList);
			mAdapter.insert(nextElement, 0);
		}

		logger.debug("Adapter count: {}  Spread limit: {}",
				mAdapter.getCount(), mLogReader.getSpreadLimit());
		assert (mAdapter.getCount() == mLogReader.getSpreadLimit());

		mListView.setSelection(mAdapter.getPosition(firstVisible));

	}

	/**
	 * Determines if enough time has passed since our last toast to display a
	 * new toast. This is to prevent us from making too many toasts when the
	 * user is at the beginning or end of a file.
	 * 
	 * @return
	 */
	private boolean enoughTimePassed() {
		final long now = System.currentTimeMillis();
		if (now - lastToastTime < 2000)
			return false;
		lastToastTime = now;
		return true;
	}

	/**
	 * This inner clas allows us to monitor the user's scrolling and determine
	 * when we need to load more lines from our log reader
	 * 
	 * @author Nick King
	 * 
	 */
	protected class FileOnScrollListener implements OnScrollListener {

		private FileLogViewer parent = FileLogViewer.this;

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			final int firstVisiblePosition = parent.mListView
					.getFirstVisiblePosition();
			final int totalItemCount = parent.mAdapter.getCount();
			final int lastVisiblePosition = parent.mListView
					.getLastVisiblePosition();

			final boolean atEndOfList = (lastVisiblePosition + 1 == totalItemCount);
			final boolean atBeginningOfList = (firstVisiblePosition == 0);

			if (atEndOfList) {
				parent.loadDown(firstVisiblePosition, lastVisiblePosition);
			} else if (atBeginningOfList) {
				parent.loadUp(firstVisiblePosition, lastVisiblePosition);
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			// Do nothing
		}

	}

}
