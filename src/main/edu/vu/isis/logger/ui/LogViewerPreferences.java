package edu.vu.isis.logger.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import edu.vu.isis.ammo.R;

public class LogViewerPreferences extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.log_viewer_preferences);
	}
	
}
