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
