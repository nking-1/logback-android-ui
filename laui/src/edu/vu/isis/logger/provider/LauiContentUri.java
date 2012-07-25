package edu.vu.isis.logger.provider;

import android.net.Uri;
import edu.vu.isis.logger.lib.LauiContentProvider.AppenderTable;
import edu.vu.isis.logger.lib.LauiContentProvider.LoggerTable;

/**
 * Allows the generation of the content URIs for the tables in the LAUI Content
 * Provider. This class is necessary because each application's LAUI Content
 * Provider has a different authority based on its package name.
 * 
 * @author Nick King
 * 
 */
public class LauiContentUri {

	private Uri mLoggerTableContentUri;
	private Uri mAppenderTableContentUri;

	public LauiContentUri(String authority) {
		mLoggerTableContentUri = Uri.parse("content://" + authority + "/"
				+ LoggerTable.PATH_MULTIPLE);
		mAppenderTableContentUri = Uri.parse("content://" + authority + "/"
				+ AppenderTable.PATH_MULTIPLE);
	}

	public Uri getLoggerTableContentUri() {
		return mLoggerTableContentUri;
	}

	public Uri getAppenderTableContentUri() {
		return mAppenderTableContentUri;
	}

}
