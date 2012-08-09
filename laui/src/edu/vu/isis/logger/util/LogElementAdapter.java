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

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * An adapter for LogElements. Automatically colors each line based on the
 * LogLevel's level.
 * 
 * @author Nick King
 * 
 */
public class LogElementAdapter extends ArrayAdapter<LogElement> {

	private Context mContext;
	private int maxNumLines = 0; // 0 means unlimited lines

	public LogElementAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
		this.mContext = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View view = super.getView(position, convertView, parent);

		final TextView tv = (TextView) view;
		final LogElement element = super.getItem(position);
		final LogLevel level = element.getLogLevel();

		tv.setText(element.getMessage());
		tv.setTextColor(level.getColor(mContext));
		return view;

	}

	public void addAll(List<LogElement> elemList) {
		synchronized (elemList) {
			for (LogElement e : elemList) {
				super.add(e);
				if (this.maxNumLines != 0
						&& this.maxNumLines < super.getCount()) {
					// Remove the first item in the list if we have exceeded the
					// max number of lines allowed
					super.remove(super.getItem(0));
				}
			}
		}
	}

	/**
	 * Sets the max number of elements that will be held by this adapter. The
	 * adapter will remove all elements that exceed the new max automatically.
	 * 
	 * @param newMax
	 */
	public void setMaxLines(int newMax) {
		this.maxNumLines = newMax;
		reduceCacheSizeToNewMax();
	}

	private void reduceCacheSizeToNewMax() {
		if (!(this.maxNumLines < super.getCount()))
			return;
		if (this.maxNumLines == 0)
			return;

		while (this.maxNumLines < super.getCount()) {
			super.remove(super.getItem(0));
		}
	}

}
