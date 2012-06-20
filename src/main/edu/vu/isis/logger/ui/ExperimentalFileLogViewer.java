package edu.vu.isis.logger.ui;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;
import edu.vu.isis.ammo.core.R;

public class ExperimentalFileLogViewer extends Activity {

	private FileLogReader mLogReader;
	private final Logger logger = LoggerFactory.getLogger(ExperimentalFileLogViewer.class);
	private TextView mTextView;
	
	public final Handler mHandler = new Handler() {
		
		@Override
		public void handleMessage(Message msg) {
			// Do nothing
		}
		
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.experimental_file_log_viewer);
		
		final Display display = getWindowManager().getDefaultDisplay();
		final int width = display.getWidth();
		final int height = display.getHeight();
		final int largestDimension = Math.max(width, height);
		
		mTextView = (TextView) findViewById(R.id.exptv);
		
		final float textSize = mTextView.getTextSize();
		final int numLines = (int) (largestDimension / textSize);
		
		String filepath = processIntent();
		if(filepath == null) {
			logger.error("Received intent without String extra for filepath");
			Toast.makeText(this, "Received invalid Intent", Toast.LENGTH_LONG);
			return;
		}
		
		try {
			this.mLogReader = new FileLogReader(this,
					mHandler, filepath, numLines);
		} catch (FileNotFoundException e) {
			logger.error("Could not find file: {}", filepath);
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("Error reading from file: {}", filepath);
			e.printStackTrace();
		}
		
		StringBuilder sb = new StringBuilder();
		
		for(LogElement e : mLogReader.fillDown()) {
			sb.append(e.toString()).append('\n');
		}
		
		mTextView.setText(sb.toString());
		
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		Log.d("ExpFileLogViewer", e.getAction() + " " + e.getPressure() + " " + e.toString());
		return true;
	}
	
	
	private String processIntent() {
		Object obj = getIntent().getExtras().get(LogViewerBase.EXTRA_NAME);
		if(obj instanceof String) {
			return (String) obj;
		} else {
			return null;
		}
	}
	
	
}
