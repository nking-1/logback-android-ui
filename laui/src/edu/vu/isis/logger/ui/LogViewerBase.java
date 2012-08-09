/*Copyright (C) 2010-2012 Institute for Software Integrated Systems (ISIS)
This software was developed by the Institute for Software Integrated
Systems (ISIS) at Vanderbilt University, Tennessee, USA for the 
Transformative Apps program under DARPA, Contract # HR011-10-C-0175.
The United States Government has unlimited rights to this software. 
The US government has the right to use, modify, reproduce, release, 
perform, display, or disclose computer software or computer software 
documentation in whole or in part, in any manner and for any 
purpose whatsoever, and to have or authorize others to do so.
*/
package edu.vu.isis.logger.ui;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ListActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import edu.vu.isis.logger.R;
import edu.vu.isis.logger.util.LogElement;
import edu.vu.isis.logger.util.LogElementAdapter;
import edu.vu.isis.logger.util.LogLevel;
import edu.vu.isis.logger.util.LogReader;

/**
 * A base class for Activities designed to view logs. This class is meant to be
 * extended and not used directly. It encapsulates behaviors that are common to
 * all log viewers.
 * 
 * All log viewers are coupled with a log reader. The log viewer is strictly
 * responsible for displaying the LogElements from its log reader on screen. The
 * log reader is strictly responsible for reading the log messages from whatever
 * the source may be and wrapping those messages inside LogElement objects.
 * 
 * @author Nick King
 * 
 */
public class LogViewerBase extends ListActivity {

	LogElementAdapter mAdapter;
	ListView mListView;
	final AtomicBoolean isPaused = new AtomicBoolean(false);
	boolean isConfigChanging = false;

	SharedPreferences mPrefs;

	/* Menu constants */
	static final int TOGGLE_MENU = Menu.NONE + 0;
	static final int JUMP_TOP_MENU = Menu.NONE + 1;
	static final int JUMP_BOTTOM_MENU = Menu.NONE + 2;
	static final int OPEN_PREFS_MENU = Menu.NONE + 3;

	/* Configuration instance array constants */
	static final int LOG_READER_INDEX = 0;
	static final int ADAPTER_INDEX = 1;

	static final Logger logger = LoggerFactory.getLogger("ui.logger.logviewer");
	LogReader mLogReader;

	public static final String EXTRA_NAME = "source";

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		setContentView(R.layout.log_viewer);
		mListView = getListView();

		@SuppressWarnings("deprecation")
		Object o = getLastNonConfigurationInstance();

		if (o == null) {
			// We are starting the Activity for the first time and need to do
			// our initial setup
			mAdapter = new LogElementAdapter(this, R.layout.log_display_row);
		} else {
			// We changed configurations, so we need to retrieve our data
			Object[] oArr = (Object[]) o;
			mLogReader = (LogReader) oArr[LOG_READER_INDEX];
			mAdapter = (LogElementAdapter) oArr[ADAPTER_INDEX];
		}

		mAdapter.setNotifyOnChange(true);
		setListAdapter(mAdapter);
		mListView.setDivider(null);

	}

	@Override
	public void onResume() {
		super.onResume();
		if (isLogReaderNull())
			return;
		if (!this.isPaused.get()) {
			this.mLogReader.resume();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (isLogReaderNull())
			return;
		this.mLogReader.pause();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		isConfigChanging = true;
		Object[] saveData = new Object[2];
		saveData[LOG_READER_INDEX] = mLogReader;
		saveData[ADAPTER_INDEX] = mAdapter;
		return saveData;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// If we're just changing configurations then there is no need to
		// terminate our threads
		if (isConfigChanging)
			return;

		if (!isLogReaderNull()) {
			this.mLogReader.terminate();
		}
		this.mAdapter.clear();
	}

	void setupColoringFromPrefs(String key) {
		mLogReader.setColored(mPrefs.getBoolean(key, true));
	}

	void recolorLogsInAdapter() {
		mLogReader.pause();
		if (mLogReader.isColored()) {
			for (int i = 0; i < mAdapter.getCount(); i++) {
				LogElement element = mAdapter.getItem(i);
				element.setLogLevel(LogReader.getCorrespondingLevel(element.getMessage()));
			}
		} else {
			for (int i = 0; i < mAdapter.getCount(); i++) {
				LogElement element = mAdapter.getItem(i);
				element.setLogLevel(LogLevel.None);
			}
		}
		if(!isPaused.get()) {
			mLogReader.resume();
		}
	}

	private boolean isLogReaderNull() {
		if (this.mLogReader == null) {
			warnNullReader();
			return true;
		}
		return false;
	}

	/**
	 * Convenience method for reporting that our log reader is null
	 */
	private void warnNullReader() {
		logger.warn("Log reader was never initialized!");
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		menu.clear();

		menu.add(Menu.NONE, TOGGLE_MENU, Menu.NONE,
				(this.isPaused.get() ? "Play" : "Pause"));
		menu.add(Menu.NONE, JUMP_BOTTOM_MENU, Menu.NONE, "Go to bottom");
		menu.add(Menu.NONE, JUMP_TOP_MENU, Menu.NONE, "Go to top");
		menu.add(Menu.NONE, OPEN_PREFS_MENU, Menu.NONE, "Open preferences");

		return super.onPrepareOptionsMenu(menu);

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean returnValue = true;
		switch (item.getItemId()) {
		case TOGGLE_MENU:
			if (isPaused.get()) {
				play();
			} else {
				pause();
			}
			break;
		default:
			returnValue = false;
		}
		return returnValue;
	}

	/**
	 * Unpauses the log reader, causing it to resume its reading
	 */
	protected void play() {
		this.isPaused.set(false);
		if (mLogReader != null) {
			this.mLogReader.resume();
		}
	}

	/**
	 * Pauses the log reader, causing it to stop reading temporarily.
	 */
	protected void pause() {
		this.isPaused.set(true);
		if (mLogReader != null) {
			this.mLogReader.pause();
		}
	}

	/**
	 * Sets the listview's scroll to the top of the list
	 */
	protected void setScrollToTop() {
		this.mListView.setSelection(0);
	}

	/**
	 * Sets the listview's scroll to the bottom of the list
	 */
	protected void setScrollToBottom() {
		this.mListView.setSelection(this.mAdapter.getCount() - 1);
	}

}
