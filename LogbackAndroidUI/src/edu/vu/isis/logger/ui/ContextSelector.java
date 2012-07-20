package edu.vu.isis.logger.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import edu.vu.isis.logger.R;

public class ContextSelector extends ListActivity {

	private List<AppHolder> mAppList = new ArrayList<AppHolder>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.context_selector);
	}

	@Override
	public void onStart() {
		super.onStart();
		findContentProviders();
		setListAdapter(new AppHolderAdapter());
	}

	private void findContentProviders() {
		mAppList.clear();

		final PackageManager pm = getPackageManager();
		final List<PackageInfo> packageList = pm
				.getInstalledPackages(PackageManager.GET_PROVIDERS);

		for (PackageInfo pkg : packageList) {
			if (pkg.applicationInfo == null || pkg.providers == null)
				continue;
			for (ProviderInfo info : pkg.providers) {
				if (info.authority.endsWith("LauiContentProvider")) {
					AppHolder holder = new AppHolder();
					Drawable icon = pkg.applicationInfo.loadIcon(pm);
					CharSequence label = pkg.applicationInfo.loadLabel(pm);

					holder.cpAuthority = info.authority;
					holder.icon = icon;
					holder.label = label;
					mAppList.add(holder);
				}
			}
		}
	}

	public void viewLogcat(View v) {
		Intent intent = new Intent();
		intent.setClass(this, LogcatLogViewer.class);
		startActivity(intent);
	}
	
	public void refreshList(View v) {
		findContentProviders();
	}

	@Override
	protected void onListItemClick(ListView parent, View row, int position,
			long id) {
		Intent intent = new Intent();
		intent.setClass(this, LoggerEditor.class);
		intent.putExtra(LoggerEditor.EXTRA_NAME,
				mAppList.get(position).cpAuthority);

		startActivity(intent);
	}

	private static class AppHolder {

		private Drawable icon;
		private CharSequence label;
		private String cpAuthority;

	}

	private static class ViewHolder {
		private TextView tv;
	}

	private class AppHolderAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mAppList.size();
		}

		@Override
		public Object getItem(int position) {
			return mAppList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View row;
			if (convertView == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.context_selector_row, parent,
						false);
			} else {
				row = convertView;
			}

			TextView tv;
			if (row.getTag() == null) {
				tv = (TextView) row.findViewById(R.id.context_selector_row_tv);
				row.setTag(new ViewHolder().tv = tv);
			} else {
				ViewHolder vHolder = (ViewHolder) row.getTag();
				tv = vHolder.tv;
			}

			AppHolder aHolder = mAppList.get(position);
			tv.setText(aHolder.label);
			tv.setCompoundDrawablesWithIntrinsicBounds(aHolder.icon, null,
					null, null);

			return row;
		}

	}

}
