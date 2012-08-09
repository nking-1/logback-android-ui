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
package edu.vu.isis.logger.util;

import edu.vu.isis.logger.R;
import android.content.Context;

/**
 * Represents the corresponding names and color values of all possible log levels
 * @author Nick King
 *
 */
public enum LogLevel {
	
	Verbose(R.string.verbose_text_name, R.color.verbose_text_color),
	Trace(R.string.trace_text_name, R.color.trace_text_color),
	Debug(R.string.debug_text_name, R.color.debug_text_color),
	Info(R.string.info_text_name, R.color.info_text_color),
	Warn(R.string.warn_text_name, R.color.warn_text_color),
	Error(R.string.error_text_name, R.color.error_text_color),
	Fail(R.string.fail_text_name, R.color.fail_text_color),
	None(R.string.none_text_name, R.color.none_text_color);
	
	
	private int mColorResId;
	private int mNameResId;
	
	private LogLevel(int nameResId, int colorResId) {
		this.mNameResId = nameResId;
		this.mColorResId = colorResId;
	}
	
	public String getName(Context context) {
		return (String) context.getResources().getText(this.mNameResId);
	}
	
	public int getColor(Context context) {
		return context.getResources().getColor(this.mColorResId);
	}
	
}
