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

		Field spinnerPopup = findField(Spinner.class.getDeclaredFields(),
				"mPopup");
		Class<?>[] innerClasses = Spinner.class.getDeclaredClasses();
		Class<?> spinnerPopupClass = null;
		try {
			spinnerPopupClass = spinnerPopup.get(this).getClass();
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalAccessException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (spinnerPopupClass == null)
			return;

		for (Class<?> clazz : innerClasses) {
			if (clazz.getName().equals("android.widget.Spinner$DialogPopup")
					&& spinnerPopupClass.equals(clazz)) {
				setupAlertDlgField(clazz);
			} else if (clazz.getName().equals(
					"android.widget.Spinner$DropdownPopup")
					&& spinnerPopupClass.equals(clazz)) {
				try {
					Field field = clazz.getField("mItemClickListener");
					field.setAccessible(true);
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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

	private void setupAlertDlgField(Class<?> clazz) {
		final Field[] fields = clazz.getDeclaredFields();
		Field dialogPopupAlertDlg = findField(fields, "mPopup");
		dialogPopupAlertDlg.setAccessible(true);
		spinnerPopup.setAccessible(true);
		try {
			// Get this Spinner's mPopup, then get the wrapped AlertDialog
			AlertDialog dlg = (AlertDialog) dialogPopupAlertDlg
					.get(spinnerPopup.get(this));
			dlg.setOnDismissListener(new OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					mListener.onSpinnerDialogClick(0);
				}

			});
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Field findField(Field[] fields, String name) {
		for (Field field : fields) {
			if (field.getName().equals(name)) {
				return field;
			}
		}
		return null;
	}

}
