package edu.vu.isis.logger.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * This class extends a Spinner to support an OnSpinnerDialogClickListener.
 * The purpose of this is that the usual method of setting a Spinner listener
 * with an OnItemSelectedListener can produce unexpected results.  For instance,
 * programmatically calling setSelection on a Spinner to change its current
 * selection also causes the OnItemSelectedListener to be called.  Additionally,
 * if the currently selected item on a Spinner is selected a second time, the
 * OnItemSelectedListener will not be called.  By using an 
 * OnSpinnerDialogClickListener instead, the callback will be made iff the user
 * manually selects an item from the Spinner's AlertDialog popup.
 * 
 * @author Nick King
 *
 */
public class WellBehavedSpinner extends Spinner {
	
	private OnSpinnerDialogClickListener mListener;
	
	public WellBehavedSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
	
	public void setOnSpinnerDialogClickListener(OnSpinnerDialogClickListener l) {
		this.mListener = l;
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
        
		super.onClick(dialog, which);
        mListener.onSpinnerDialogClick(which);
        
    }
	
	
}
