package edu.vu.isis.logger.test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * Main class for this test app. We simply create several Logger objects so that
 * we can test whether they show up in LAUI and if we can see/edit their levels.
 * There is also a Logger that will write messages to Logcat very quickly. This
 * allows us to test the Logcat reader.
 * 
 * @author Nick King
 * 
 */
public class Main extends ListActivity {

	private static final Logger spamLogger = (Logger) LoggerFactory
			.getLogger("laui.test.spam");
	private ScheduledExecutorService ex;

	private static final Runnable SPAM_RUNNABLE = new Runnable() {

		@Override
		public void run() {
			spamLogger.trace("My level is: {}\tCurrent time: {}", spamLogger
					.getLevel().toString(), System.currentTimeMillis());
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		for (char i = 'a'; i < 'g'; i++) {
			for (char j = 'h'; j < 'j'; j++) {
				for (int k = 1; k <= 4; k++) {
					LoggerFactory.getLogger(i + "." + j + "." + k);
				}
			}
		}

		List<Logger> loggerList = ((LoggerContext) LoggerFactory
				.getILoggerFactory()).getLoggerList();

		ArrayAdapter<Logger> adapter = new ArrayAdapter<Logger>(this,
				android.R.layout.simple_list_item_1, loggerList);
		setListAdapter(adapter);

		spamLogger.setLevel(Level.ERROR);
	}

	@Override
	public void onStart() {
		super.onStart();
		ex = Executors.newSingleThreadScheduledExecutor();
		ex.scheduleAtFixedRate(SPAM_RUNNABLE, 10, 10, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onPause() {
		super.onPause();
		ex.shutdown();
	}

}
