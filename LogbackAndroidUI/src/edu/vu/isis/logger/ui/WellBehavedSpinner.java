package edu.vu.isis.logger.ui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * This class extends a Spinner to support an OnSpinnerDialogClickListener. The
 * purpose of this is that the usual method of setting a Spinner listener with
 * an OnItemSelectedListener can produce unexpected results. For instance,
 * programmatically calling setSelection on a Spinner to change its current
 * selection also causes the OnItemSelectedListener to be called. Additionally,
 * if the currently selected item on a Spinner is selected a second time, the
 * OnItemSelectedListener will not be called. By using an
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

		Class<?>[] innerClasses = Spinner.class.getDeclaredClasses();
		Field dialogPopupAlertDlg = null;
		Field dropdownPopupOnItemClickListener = null;

		for (int i = 0; i < innerClasses.length; i++) {
			if (innerClasses[i].getName().equals("android.widget.Spinner$DialogPopup")) {
				Field[] fields = innerClasses[i].getDeclaredFields();
				for(Field field : fields) {
					if(field.getName().equals("mPopup")) {
						dialogPopupAlertDlg = field;
					}
				}
				dialogPopupAlertDlg.setAccessible(true);
				AlertDialog dlg = null;
				try {
					dlg = (AlertDialog) dialogPopupAlertDlg
							.get(innerClasses[i]);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (dlg != null) {
					dlg.setOnDismissListener(new OnDismissListener() {

						@Override
						public void onDismiss(DialogInterface dialog) {
							mListener.onSpinnerDialogClick(0);
						}

					});
				}
			} else if (innerClasses[i].getName().equals("android.widget.Spinner$DropdownPopup")) {
//				try {
//					dropdownPopupOnItemClickListener = innerClasses[i]
//							.getField("mItemClickListener");
//				} catch (NoSuchFieldException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				dropdownPopupOnItemClickListener.setAccessible(true);
			}
		}

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
