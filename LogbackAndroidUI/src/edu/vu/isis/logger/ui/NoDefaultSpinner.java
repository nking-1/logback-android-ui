package edu.vu.isis.logger.ui;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

/**
 * A modified Spinner that doesn't automatically select the first entry in the
 * list.
 * 
 * Shows the prompt if nothing is selected.
 * 
 * Limitations: does not display prompt if the entry list is empty.
 * 
 * <p>
 * This class comes from an answer by emmby on StackOverflow:
 * http://stackoverflow .com/questions/867518/how-to-make-an-android-spinner
 * -with-initial-text-select-one
 * <p>
 * Go upvote his answer if you like this workaround.
 * 
 * @author emmby, Nick King
 */
public class NoDefaultSpinner extends Spinner {

	private Method m;
	private Method n;

	public NoDefaultSpinner(Context context) {
		super(context);
		initMethods();
	}

	public NoDefaultSpinner(Context context, AttributeSet attrs) {
		super(context, attrs);
		initMethods();
	}

	public NoDefaultSpinner(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initMethods();
	}

	@Override
	public void setAdapter(SpinnerAdapter orig) {
		final SpinnerAdapter adapter = newProxy(orig);
		super.setAdapter(adapter);
		setHintSelected();
	}

	/**
	 * Sets the Spinner's selection text to the hint
	 */
	public void setHintSelected() {
		try {
			m.invoke(this, -1);
			n.invoke(this, -1);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		requestLayout();
		invalidate();
	}

	private void initMethods() {
		try {
			m = AdapterView.class.getDeclaredMethod(
					"setNextSelectedPositionInt", int.class);
			m.setAccessible(true);

			n = AdapterView.class.getDeclaredMethod("setSelectedPositionInt",
					int.class);
			n.setAccessible(true);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected SpinnerAdapter newProxy(SpinnerAdapter obj) {
		return (SpinnerAdapter) java.lang.reflect.Proxy.newProxyInstance(obj
				.getClass().getClassLoader(),
				new Class[] { SpinnerAdapter.class }, new SpinnerAdapterProxy(
						obj));
	}

	/**
	 * Intercepts getView() to display the prompt if position < 0
	 */
	protected class SpinnerAdapterProxy implements InvocationHandler {

		protected SpinnerAdapter obj;
		protected Method getView;

		protected SpinnerAdapterProxy(SpinnerAdapter obj) {
			this.obj = obj;
			try {
				this.getView = SpinnerAdapter.class.getMethod("getView",
						int.class, View.class, ViewGroup.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		public Object invoke(Object proxy, Method m, Object[] args)
				throws Throwable {
			try {
				return m.equals(getView) && (Integer) (args[0]) < 0 ? getView(
						(Integer) args[0], (View) args[1], (ViewGroup) args[2])
						: m.invoke(obj, args);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		protected View getView(int position, View convertView, ViewGroup parent)
				throws IllegalAccessException {
			if (position < 0) {
				final TextView v = (TextView) ((LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(android.R.layout.simple_spinner_item, parent,
								false);
				v.setText(getPrompt());
				return v;
			}

			return obj.getView(position, convertView, parent);
		}

	}

}
