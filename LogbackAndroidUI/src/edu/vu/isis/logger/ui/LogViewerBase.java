package edu.vu.isis.logger.ui;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import edu.vu.isis.logger.R;
import edu.vu.isis.logger.util.LogElementAdapter;
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

	protected LogElementAdapter mAdapter;
	protected ListView mListView;
	protected final AtomicBoolean isPaused = new AtomicBoolean(false);
	protected boolean isConfigChanging = false;

	/* Menu constants */
	protected static final int TOGGLE_MENU = Menu.NONE + 0;

	/* Configuration instance array constants */
	protected static final int LOG_READER_INDEX = 0;
	protected static final int ADAPTER_INDEX = 1;

	protected final Logger logger = LoggerFactory
			.getLogger("ui.logger.logviewer");
	protected LogReader mLogReader;

	public static final String EXTRA_NAME = "source";

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.log_viewer);
		mListView = getListView();

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
		this.logger.warn("Log reader was never initialized!");
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		menu.clear();

		menu.add(Menu.NONE, TOGGLE_MENU, Menu.NONE,
				(this.isPaused.get() ? "Play" : "Pause"));

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
