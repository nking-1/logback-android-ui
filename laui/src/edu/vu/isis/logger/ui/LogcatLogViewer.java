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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;
import edu.vu.isis.logger.util.LogElement;
import edu.vu.isis.logger.util.LogcatLogReader;

/**
 * A log viewer designed specifically for viewing logs from Logcat. The actual
 * reading from Logcat is handled by a LogcatLogReader. This class's
 * responsibility is only to connect the LogcatLogReader to the UI.
 * 
 * @author Nick King
 * 
 */
public class LogcatLogViewer extends LogViewerBase {

	/**
	 * Temporary patch for a bug caused by ListView's transcript mode not quite
	 * working correctly in pre-ICS Android. Without this AtomicBoolean, the
	 * autojump behavior won't work correctly when the user initially opens the
	 * Logcat viewer because so many items are added to the adapter in bulk at
	 * once. There is still a bug with the autojump when lots of items are added
	 * in bulk.
	 */
	private AtomicBoolean alwaysJump = new AtomicBoolean(true);

	public Handler mHandler = new Handler() {

		LogcatLogViewer parent = LogcatLogViewer.this;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LogcatLogReader.CONCAT_DATA_MSG:
				if (msg.obj != null) {
					@SuppressWarnings("unchecked")
					final List<LogElement> elemList = (List<LogElement>) msg.obj;
					final int lastVisiblePos = mListView
							.getLastVisiblePosition();
					final int adapterCount = mAdapter.getCount();
					final boolean jumpDown = (lastVisiblePos == adapterCount - 1)
							|| alwaysJump.get();
					mAdapter.addAll(elemList);
					if (jumpDown)
						mListView.setSelection(mAdapter.getCount() - 1);
				}
				break;
			case LogcatLogReader.NOTIFY_INVALID_REGEX_MSG:
				Toast.makeText(parent, "Syntax of regex is invalid",
						Toast.LENGTH_LONG).show();
				break;
			default:
				LogcatLogViewer.logger
						.error("Handler received malformed message");
			}

		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mListView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				alwaysJump.set(false);
				return false;
			}

		});

		String regex = mPrefs.getString("regular_expression", "");

		openLogReader(regex);
		configureMaxLinesFromPrefs();
		setupColoringFromPrefs("colored_logcat_logs");
		mLogReader.start();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean returnValue = true;
		switch (item.getItemId()) {
		case JUMP_BOTTOM_MENU:
			setScrollToBottom();
			break;
		case JUMP_TOP_MENU:
			setScrollToTop();
			alwaysJump.set(false);
			break;
		case OPEN_PREFS_MENU:
			// Pause reading until we're done resetting the preferences
			mLogReader.pause();
			final Intent intent = new Intent().setClass(this,
					LogViewerPreferences.class);
			startActivityForResult(intent, 0);
			break;
		default:
			returnValue = false;
		}
		return returnValue || super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		configureMaxLinesFromPrefs();
		String regex = mPrefs.getString("regular_expression", "");
		boolean startReader = false;
		if (!regex.equals(((LogcatLogReader) mLogReader).getRegex())) {
			openLogReader(regex);
			// The intended behavior is for the LogReader to resume regardless
			// of whether or not it was previously paused if the regex was
			// changed. We want to reread everything and compare it all
			// against the new regex.
			isPaused.set(false);
			mAdapter.clear();
			startReader = true;
		}

		boolean wasColored = mLogReader.isColored();
		setupColoringFromPrefs("colored_logcat_logs");

		if (wasColored != mLogReader.isColored()) {
			((LogcatLogReader) mLogReader).forceUpdate();
			recolorLogsInAdapter();
			mListView.invalidateViews();
		}

		if (startReader) {
			mLogReader.start();
			return;
		}

		// Resume the LogReader that we paused before we went to the preference
		// screen
		if (!isPaused.get())
			mLogReader.resume();

	}

	private void openLogReader(String regex) {
		try {
			mLogReader = new LogcatLogReader(this, mHandler, regex);
		} catch (IOException e) {
			final String errorMsg = "Could not read from Logcat";
			logger.error(errorMsg);
			e.printStackTrace();
			Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
			return;
		}
	}

	/**
	 * Set our list adapter's max lines as specified in the preferences
	 */
	private void configureMaxLinesFromPrefs() {
		mAdapter.setMaxLines(Math.abs(Integer.parseInt(mPrefs.getString(
				"logcat_max_lines", "1000"))));
	}

}
