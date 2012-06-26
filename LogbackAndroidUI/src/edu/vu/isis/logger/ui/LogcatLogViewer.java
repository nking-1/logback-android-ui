package edu.vu.isis.logger.ui;

import java.io.IOException;
import java.util.List;

import edu.vu.isis.logger.util.LogElement;
import edu.vu.isis.logger.util.LogcatLogReader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class LogcatLogViewer extends LogViewerBase {
	
	public static final int CONCAT_DATA_MSG = 0;
	
	private static final int OPEN_PREFS_MENU = Menu.NONE + 3;
	
	private SharedPreferences prefs;

	public Handler mHandler = new Handler() {

		LogcatLogViewer parent = LogcatLogViewer.this;

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case CONCAT_DATA_MSG:
				if (msg.obj != null) {
					@SuppressWarnings("unchecked")
					final List<LogElement> elemList = (List<LogElement>) msg.obj;
					refreshList(elemList);
				}
				break;
			default:
				parent.logger.error("Handler received malformed message");
			}

		}

	};
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		//getSharedPreferences("edu.vu.isis.logger_preferences", MODE_PRIVATE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		super.onCreate(savedInstanceState);
		configureMaxLinesFromPrefs();
		String regex = prefs.getString("regular_expression", "");
		
		
		// Our superclass took care of retrieving the log reader after
		// the configuration changed.
		if(mLogReader != null) {
			((LogcatLogReader)mLogReader).setRegex(regex);
			return;
		}
		
		try {
			mLogReader = new LogcatLogReader(this, mHandler, regex);
		} catch (IOException e) {
			final String errorMsg = "Could not read from Logcat";
			logger.error(errorMsg);
			e.printStackTrace();
			Toast.makeText(this, errorMsg, Toast.LENGTH_LONG);
			return;
		}
		
		mLogReader.start();
		
	}
	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		final boolean returnValue = true && super.onPrepareOptionsMenu(menu);
		
        menu.add(Menu.NONE, OPEN_PREFS_MENU, Menu.NONE, "Open preferences");
        
        return returnValue;
        
    }
	
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == OPEN_PREFS_MENU) {
			final Intent intent = new Intent().setClass(this, LogViewerPreferences.class);
        	startActivityForResult(intent, 0);
        	return true;
		}
        return super.onOptionsItemSelected(item);
    }
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		// Pause reading until we're done resetting the max lines
		mLogReader.pause();
		
		configureMaxLinesFromPrefs();
		
		if(!isPaused.get()) mLogReader.resume();
		
	}
	
	
	private void configureMaxLinesFromPrefs() {
		mAdapter.setMaxLines(Math.abs(Integer.parseInt(prefs.getString("logcat_max_lines", "1000"))));
	}
	
	
	private void refreshList(List<LogElement> elemList) {
		
		updateAdapter(elemList);
		if(isAutoJump.get()) {
			setScrollToBottom();
		}
		
	}

	
	private void updateAdapter(List<LogElement> elemList) {
		mAdapter.addAll(elemList);
		mAdapter.notifyDataSetChanged();
	}
	
}
