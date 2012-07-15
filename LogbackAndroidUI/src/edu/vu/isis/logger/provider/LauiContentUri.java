package edu.vu.isis.logger.provider;

import android.net.Uri;

public class LauiContentUri implements CpConstants {

	private Uri mLoggerTableContentUri;
	private Uri mAppenderTableContentUri;

	public LauiContentUri(String authority) {
		mLoggerTableContentUri = Uri.parse("content://" + authority + "/"
				+ LoggerTable.PATH_MULTIPLE);
		mAppenderTableContentUri = Uri.parse("content://"
				+ authority + "/" + AppenderTable.PATH_MULTIPLE);
	}
	
	public Uri getLoggerTableContentUri() {
		return mLoggerTableContentUri;
	}
	
	public Uri getAppenderTableContentUri() {
		return mAppenderTableContentUri;
	}
	
}
