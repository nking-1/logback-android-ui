package edu.vu.isis.logger.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import edu.vu.isis.logger.R;

public class LogViewerPreferences extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.log_viewer_preferences);
	}
	
}
