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
package edu.vu.isis.logger.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

/**
 * Allows a Spinner to show a hint/prompt instead of one of its choices whenever
 * the Spinner is waiting to be clicked.
 * @author Nick King
 */
public class HintSpinner extends Spinner {

	private String mHint;
	private boolean showHintOnce = false;
	private boolean alwaysShowHint = false;
	
	public HintSpinner(Context context) {
		super(context);
		init();
	}

	public HintSpinner(Context context, int mode) {
		super(context, mode);
		init();
	}

	public HintSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public HintSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public HintSpinner(Context context, AttributeSet attrs, int defStyle,
			int mode) {
		super(context, attrs, defStyle, mode);
		init();
	}

	private void init() {
		setHint((String) getPrompt());
	}

	public void setHint(String hint) {
		mHint = hint;
	}
	
	public void setAlwaysShowHint(boolean b) {
		alwaysShowHint = b;
	}
	
	public void setShowHintOnce(boolean b) {
		showHintOnce = b;
	}

	@Override
	public void setAdapter(SpinnerAdapter adapter) {
		super.setAdapter(new HintSpinnerAdapter(adapter));
	}

	private class HintSpinnerAdapter implements SpinnerAdapter {

		private SpinnerAdapter mAdapter;

		public HintSpinnerAdapter(SpinnerAdapter adapter) {
			mAdapter = adapter;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			mAdapter.registerDataSetObserver(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			mAdapter.unregisterDataSetObserver(observer);
		}

		@Override
		public int getCount() {
			return mAdapter.getCount();
		}

		@Override
		public Object getItem(int position) {
			return mAdapter.getItem(position);
		}

		@Override
		public long getItemId(int position) {
			return mAdapter.getItemId(position);
		}

		@Override
		public boolean hasStableIds() {
			return mAdapter.hasStableIds();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = mAdapter.getView(position, convertView, parent);
			
			// Don't simplify with De Morgan's, too confusing
			if(!(showHintOnce || alwaysShowHint)) {
				return view;
			}
			
			// Set this false for next time
			showHintOnce = false;
			
			// It's possible that we're wrapping an adapter that doesn't make
			// TextViews.  If the adapter *is* making TextViews, we'll use their
			// view instead of inflating a new one.  Otherwise, we'll inflate
			// the default spinner item.
			if (!(view instanceof TextView)) {
				view = LayoutInflater.from(getContext()).inflate(
						android.R.layout.simple_spinner_item, parent, false);
			}
			((TextView) view).setText(mHint);
			return view;
		}

		@Override
		public int getItemViewType(int position) {
			return mAdapter.getItemViewType(position);
		}

		@Override
		public int getViewTypeCount() {
			return mAdapter.getViewTypeCount();
		}

		@Override
		public boolean isEmpty() {
			return mAdapter.isEmpty();
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			return mAdapter.getDropDownView(position, convertView, parent);
		}

	}


}
