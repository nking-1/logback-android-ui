package edu.vu.isis.logger.ui;

import edu.vu.isis.ammo.R;
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
