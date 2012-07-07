package com.nbk.logbackcontentprovtest;

import java.util.List;

import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ArrayAdapter;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class MainActivity extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		for (char i = 'a'; i < 'g'; i++) {
			for (char j = 'h'; j < 'j'; j++) {
				for (int k = 1; k <= 4; k++) {
					LoggerFactory.getLogger(i + "." + j + "." + k);
				}
			}
		}

		List<Logger> loggerList = ((LoggerContext) LoggerFactory
				.getILoggerFactory()).getLoggerList();
		
		ArrayAdapter<Logger> adapter = new ArrayAdapter<Logger>(this, android.R.layout.simple_list_item_1, loggerList);
		setListAdapter(adapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
