package edu.vu.isis.logger.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.PrintStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.Pattern;
import edu.vu.isis.logger.R;
import edu.vu.isis.logger.temp.CpConstants;
import edu.vu.isis.logger.util.LoggerConfigureAction;
import edu.vu.isis.logger.util.Loggers;
import edu.vu.isis.logger.util.SimpleConfigurator;
import edu.vu.isis.logger.util.Tree;
import edu.vu.isis.logger.util.TreeAdapter;

/**
 * This class provides a user interface to edit the Level and Appenders of all
 * Logger objects active in the application.
 * 
 * @author Nick King
 * 
 */

public class LoggerEditor extends ListActivity {

	private static final int READ_MENU = Menu.NONE + 0;
	private static final int TOGGLE_APPENDER_TEXT_MENU = Menu.NONE + 1;
	private static final int RESET_APPENDERS_MENU = Menu.NONE + 2;
	private static final int LOAD_CONFIGURATION_MENU = Menu.NONE + 3;
	private static final int SAVE_CONFIGURATION_MENU = Menu.NONE + 4;

	private static final int APPENDER_REQUEST_CODE = 0;
	private static final int PICKFILE_REQUEST_CODE = 1;

	private static final String DEFAULT_SAVE_DIRECTORY = "/logger/save";

	private LoggerHolder selectedLogger;
	private View selectedView;
	private Tree<LoggerHolder> loggerTree;

	// We use this logger to log for this Activity
	private final Logger personalLogger = Loggers
			.getLoggerByName("ui.logger.editor");

	private TextView selectionText;
	private WellBehavedSpinner levelSpinner;
	private MyOnSpinnerDialogClickListener spinnerListener;
	private ListView mListView;
	private LoggerIconAdapter mAdapter;

	// Storing our LoggerHolders in a Map makes it easier to find our
	// previously created LoggerHolders by name instead of having to search
	// a list every time. This is mainly used when we make the tree of
	// LoggerHolders.
	private Map<String, LoggerHolder> loggerMap = new HashMap<String, LoggerHolder>();

	private LoggerHolder rootHolder;
	// TODO: Appender lists need to work with content providers
	private final List<String> availableAppenders = new ArrayList<String>();

	private final String[] appenderNames = new String[availableAppenders.size()];

	private AtomicBoolean showAppenderText = new AtomicBoolean(true);

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.logger_editor);

		Cursor cursor = getContentResolver().query(
				CpConstants.LoggerTable.CONTENT_URI, null, null, null, null);
		List<LoggerHolder> tempList = new ArrayList<LoggerHolder>();

		if (cursor != null && cursor.getCount() >= 1) {
			while (cursor.moveToNext()) {

				final int nameIndex = cursor
						.getColumnIndexOrThrow(CpConstants.LoggerTable.NAME);
				final int levelIndex = cursor
						.getColumnIndexOrThrow(CpConstants.LoggerTable.LEVEL_INT);
				final int additivityIndex = cursor
						.getColumnIndexOrThrow(CpConstants.LoggerTable.ADDITIVITY);
				final int attachedIndex = cursor
						.getColumnIndexOrThrow(CpConstants.LoggerTable.ATTACHED_APPENDER_NAMES);

				final String name = cursor.getString(nameIndex);
				final int levelInt = cursor.getInt(levelIndex);
				final boolean additivity = cursor.getInt(additivityIndex) == 1;

				// TODO: Quit cheating and actually get the appender names
				final String[] attachedNames = new String[1];
				attachedNames[0] = "Logcat";

				LoggerHolder logger = new LoggerHolder();
				logger.name = name;
				logger.additivity = additivity;
				logger.levelInt = levelInt;
				logger.appenders = new ArrayList<String>(attachedNames.length);

				for (String appenderName : attachedNames) {
					logger.appenders.add(appenderName);
				}

				loggerMap.put(logger.name, logger);

				/*
				 * To keep from wasting time building a list from the Collection
				 * of values in the Map and then sorting it ourselves, we just
				 * make a list of loggers along with the map. The content
				 * provider originally got the list of loggers from Logback, and
				 * Logback sorted the list before returning it.
				 */
				tempList.add(logger);

				if (logger.name.equals(Logger.ROOT_LOGGER_NAME)) {
					rootHolder = logger;
				}

			}
		}

		this.loggerTree = makeTree(tempList);

		this.mListView = super.getListView();

		this.mAdapter = new LoggerIconAdapter(loggerTree, this,
				R.layout.logger_row, R.id.logger_text);
		this.setListAdapter(mAdapter);

		this.selectionText = (TextView) findViewById(R.id.selection_text);
		this.levelSpinner = (WellBehavedSpinner) findViewById(R.id.level_spinner);

		final ArrayAdapter<CharSequence> spinAdapter = ArrayAdapter
				.createFromResource(this, R.array.level_options,
						android.R.layout.simple_spinner_item);
		spinAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		this.levelSpinner.setAdapter(spinAdapter);
		this.spinnerListener = new MyOnSpinnerDialogClickListener();
		this.levelSpinner.setOnSpinnerDialogClickListener(this.spinnerListener);
		this.levelSpinner.setSelection(CLEAR_IX);

		// Set the selection text to indicate nothing is selected
		this.updateSelText(null);

		if (savedInstanceState == null)
			return;

		// Set the list back to its previous position
		final int savedVisiblePosition = savedInstanceState
				.getInt("savedVisiblePosition");
		this.mListView.setSelection(savedVisiblePosition);

		this.selectedView = null;
		this.selectedLogger = null;

		final boolean wasLoggerSelected = savedInstanceState
				.getBoolean("wasLoggerSelected");
		if (wasLoggerSelected) {
			Toast.makeText(this, "Please reselect logger.", Toast.LENGTH_LONG)
					.show();
		}

		final boolean showAppText = savedInstanceState
				.getBoolean("showAppText");
		this.showAppenderText.set(showAppText);

	}

	@Override
	public void onStart() {
		super.onStart();
		startWatchingExternalStorage();
	}

	@Override
	public void onStop() {
		super.onStop();
		stopWatchingExternalStorage();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		final int savedVisiblePosition = this.mListView
				.getFirstVisiblePosition();
		outState.putInt("savedVisiblePosition", savedVisiblePosition);

		final boolean wasLoggerSelected = (selectedLogger != null);
		outState.putBoolean("wasLoggerSelected", wasLoggerSelected);

		final boolean showAppText = this.showAppenderText.get();
		outState.putBoolean("showAppText", showAppText);

	}

	private Tree<LoggerHolder> makeTree(Collection<LoggerHolder> collection) {

		// If we never found a root logger, we make our own root logger with
		// fields that will indicate in the UI that the root logger wasn't
		// found.
		// We make sure to initialize all of the fields so we don't accidentally
		// get a null pointer exception
		if (rootHolder == null) {
			rootHolder = new LoggerHolder();
			rootHolder.name = "ROOT_NOT_FOUND";
			rootHolder.levelInt = Level.OFF_INT;
			rootHolder.appenders = new ArrayList<String>();
		}

		final Tree<LoggerHolder> mTree = Tree.newInstance(rootHolder);

		for (final LoggerHolder logger : collection) {
			// The root was already added so we skip it
			if (logger == rootHolder) {
				continue;
			}
			safelyAddLeaf(mTree, logger);
		}

		return mTree;

	}

	/**
	 * Adds a leaf to the tree with only the assumption that the Root logger is
	 * at the top of the tree. The order in which leaves are added does not
	 * matter because the algorithm always checks if the parent leaves have been
	 * added to the tree before adding a child leaf. For example, if a Logger
	 * named "edu.foo.bar" were given, then we would first check if "edu.foo"
	 * and "edu" had been added to the tree, and if not, we would add them
	 * before adding "edu.foo.bar"
	 * 
	 * @param tree
	 *            -- the Tree to which leaves are added
	 * @param aLogger
	 *            -- the Logger to be added to the Tree
	 */
	private void safelyAddLeaf(Tree<LoggerHolder> tree, LoggerHolder aLogger) {

		if (tree.contains(aLogger))
			return;

		final String parentName = Loggers.getParentLoggerName(aLogger.name);

		if (parentName == Logger.ROOT_LOGGER_NAME) {
			// The root logger is the parent, so we add this Logger to the tree
			tree.addLeaf(aLogger);
			return;
		}

		LoggerHolder parent = loggerMap.get(parentName);

		if (parent == null) {
			// Ideally this should never happen, but if it does, we log an error
			// and just add the child logger to the tree, giving up.
			personalLogger.error("No parent logger named {} for logger {}",
					parentName, aLogger.name);
			tree.addLeaf(aLogger);
			return;
		}

		safelyAddLeaf(tree, parent);
		tree.addLeaf(parent, aLogger);
		return;
	}

	@Override
	protected void onListItemClick(ListView parent, View row, int position,
			long id) {

		final LoggerHolder nextSelectedLogger = (LoggerHolder) parent
				.getItemAtPosition(position);
		final Level effective = nextSelectedLogger.getEffectiveLevel();

		updateSelText(nextSelectedLogger.name);

		if (this.selectedLogger == null) {
			this.spinnerListener.updateSpinner(effective, this.levelSpinner);
		} else if (nextSelectedLogger.equals(this.selectedLogger)) {
			return;
		} else if ((!effective.equals(selectedLogger.getEffectiveLevel()))
				|| this.levelSpinner.getSelectedItemPosition() == CLEAR_IX) {
			this.spinnerListener.updateSpinner(effective, this.levelSpinner);
		}
		this.selectedLogger = nextSelectedLogger;
		this.selectedView = row;

		refreshList();
	}

	private void refreshList() {
		this.mListView.invalidateViews();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, READ_MENU, Menu.NONE, "Read logs");
		menu.add(Menu.NONE, TOGGLE_APPENDER_TEXT_MENU, Menu.NONE,
				"Toggle Appender text");
		menu.add(Menu.NONE, SAVE_CONFIGURATION_MENU, Menu.NONE,
				"Save current logger settings");
		menu.add(Menu.NONE, LOAD_CONFIGURATION_MENU, Menu.NONE,
				"Load logger settings");

		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		boolean returnValue = true;
		switch (item.getItemId()) {
		case READ_MENU:
			createReaderSelectorDialog();
			break;
		case TOGGLE_APPENDER_TEXT_MENU:
			this.showAppenderText.set(!this.showAppenderText.get());
			refreshList();
			break;
		case SAVE_CONFIGURATION_MENU:
			promptForSave();
			break;
		case LOAD_CONFIGURATION_MENU:
			if (mExternalStorageAvailable) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("text/xml");
				startActivityForResult(intent, PICKFILE_REQUEST_CODE);
			} else {
				Toast.makeText(this, "Cannot read from external storage",
						Toast.LENGTH_LONG).show();
			}
			break;
		default:
			returnValue = false;
		}

		return returnValue;
	}

	private void promptForSave() {

		final Dialog myDialog = new Dialog(this);

		myDialog.setTitle("Save logger settings to file");
		myDialog.setContentView(R.layout.logger_filesave_dialog);

		final EditText filenameEdit = (EditText) myDialog
				.findViewById(R.id.dialog_file_name_edit);
		final EditText directoryEdit = (EditText) myDialog
				.findViewById(R.id.dialog_directory_edit);
		final Button saveButton = (Button) myDialog
				.findViewById(R.id.dialog_file_save_button);
		final Button cancelButton = (Button) myDialog
				.findViewById(R.id.dialog_cancel_save_button);

		directoryEdit.setText(Environment.getExternalStorageDirectory()
				+ DEFAULT_SAVE_DIRECTORY);

		cancelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				myDialog.dismiss();
			}
		});

		saveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mExternalStorageWriteable) {

					final String filenameEditStr = filenameEdit.getText()
							.toString();

					if (filenameEditStr.equals("")) {
						Toast.makeText(LoggerEditor.this,
								"Enter a nonempty filename", Toast.LENGTH_LONG)
								.show();
						return;
					}

					final String filename = formatFilename(filenameEditStr);
					final String directory = directoryEdit.getText().toString();
					saveFile(filename, directory);
				}
				myDialog.dismiss();
			}

			private String formatFilename(String str) {
				final String trimmed = str.trim();
				if (!trimmed.endsWith(".xml")) {
					return str += ".xml";
				} else {
					return trimmed;
				}
			}

		});

		myDialog.show();

	}

	private void saveFile(String filename, String directory) {
		final File dirs = new File(directory);

		if (dirs.mkdirs()) {
			personalLogger.info("Directory {} was created for save",
					dirs.getAbsolutePath());
		} else {
			personalLogger.info("Directory {} was not created for save",
					dirs.getAbsolutePath());
		}

		// TODO: Make sure this is okay according to Android IO conventions
		PrintStream outStream = null;
		try {
			outStream = new PrintStream(directory + "/" + filename);
		} catch (FileNotFoundException e) {
			personalLogger.error("FileNotFoundException! Unable to write file");
			e.printStackTrace();
			return;
		} finally {
			if (outStream != null) {
				outStream.flush();
				outStream.close();
			}
		}

		for (LoggerHolder logger : loggerMap.values()) {
			writeXML(outStream, logger);
		}

	}

	private void writeXML(PrintStream outStream, LoggerHolder logger) {

		final String name = logger.name;
		final int levelInt = logger.levelInt;
		final String appenderStr = makeAppenderStr(logger.appenders);
		final String additivityStr = Boolean.toString(logger.additivity);

		final StringBuilder bldr = new StringBuilder();
		final String openQuoteStr = "=\"";
		final String closeQuoteStr = "\" ";

		bldr.append("<logger ").append(LoggerConfigureAction.NAME_ATR)
				.append(openQuoteStr).append(name).append(closeQuoteStr)
				.append(LoggerConfigureAction.LEVEL_ATR).append(openQuoteStr)
				.append(levelInt).append(closeQuoteStr)
				.append(LoggerConfigureAction.APPENDER_ATR)
				.append(openQuoteStr).append(appenderStr).append(closeQuoteStr)
				.append(LoggerConfigureAction.ADDITIVITY_ATR)
				.append(openQuoteStr).append(additivityStr)
				.append(closeQuoteStr).append("/>");
		final String outputStr = bldr.toString();

		personalLogger.trace("Writing line to file: {}", outputStr);
		outStream.println(outputStr);

	}

	private String makeAppenderStr(List<String> appenders) {

		if (appenders.isEmpty())
			return LoggerConfigureAction.NO_APPENDER_STR;

		StringBuilder bldr = new StringBuilder();
		for (String appenderName : appenders) {
			bldr.append(appenderName).append(" ");
		}
		return bldr.toString().trim();
	}

	private void createReaderSelectorDialog() {

		final Dialog dialog = new Dialog(this);
		dialog.setTitle("View logs");

		dialog.setContentView(R.layout.log_viewer_dialog);

		Button logcatButton = (Button) dialog
				.findViewById(R.id.log_viewer_dialog_logcat_button);
		logcatButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent().setClass(LoggerEditor.this,
						LogcatLogViewer.class);
				startActivity(intent);
				dialog.dismiss();
			}
		});

		LinearLayout ll = (LinearLayout) dialog
				.findViewById(R.id.log_viewer_dialog_file_appender_ll);

		// TODO: Make this code work by getting the appenders from the content
		// provider
		/*
		 * for (final Appender<ILoggingEvent> a : availableAppenders) { if (!(a
		 * instanceof FileAppender)) continue; Button button = new Button(this);
		 * button.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
		 * button.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
		 * button.setText(a.getName()); button.setOnClickListener(new
		 * View.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) {
		 * 
		 * // TODO: Remove this toast once file reading works
		 * Toast.makeText(LoggerEditor.this,
		 * "File reading is not yet available", Toast.LENGTH_LONG).show();
		 * return;
		 * 
		 * // Intent intent = new Intent().setClass(LoggerEditor.this, //
		 * FileLogViewer.class); // intent.putExtra(LogViewerBase.EXTRA_NAME, //
		 * ((FileAppender<ILoggingEvent>) a).getFile()); //
		 * startActivity(intent); // dialog.dismiss(); } }); ll.addView(button);
		 * }
		 */

		dialog.show();

	}

	/**
	 * Update the icon for the logger view.
	 * 
	 * @param lvl
	 * @param row
	 */
	private void updateIcon(Level lvl, View row) {
		final ImageView iv = (ImageView) (row.findViewById(R.id.logger_icon));

		if (selectedLogger.levelInt == LoggerHolder.NO_LEVEL) {
			setEffectiveIcon(lvl, iv);
		} else {
			setActualIcon(lvl, iv);
		}
		refreshList();
	}

	private void setEffectiveIcon(Level lvl, ImageView iv) {
		switch (lvl.levelInt) {
		case Level.TRACE_INT:
			iv.setImageResource(R.drawable.effective_trace_level_icon);
			break;
		case Level.DEBUG_INT:
			iv.setImageResource(R.drawable.effective_debug_level_icon);
			break;
		case Level.INFO_INT:
			iv.setImageResource(R.drawable.effective_info_level_icon);
			break;
		case Level.WARN_INT:
			iv.setImageResource(R.drawable.effective_warn_level_icon);
			break;
		case Level.ERROR_INT:
			iv.setImageResource(R.drawable.effective_error_level_icon);
			break;
		case Level.OFF_INT:
		default:
			iv.setImageResource(R.drawable.effective_off_level_icon);
		}
	}

	private void setActualIcon(Level lvl, ImageView iv) {
		switch (lvl.levelInt) {
		case Level.TRACE_INT:
			iv.setImageResource(R.drawable.actual_trace_level_icon);
			break;
		case Level.DEBUG_INT:
			iv.setImageResource(R.drawable.actual_debug_level_icon);
			break;
		case Level.INFO_INT:
			iv.setImageResource(R.drawable.actual_info_level_icon);
			break;
		case Level.WARN_INT:
			iv.setImageResource(R.drawable.actual_warn_level_icon);
			break;
		case Level.ERROR_INT:
			iv.setImageResource(R.drawable.actual_error_level_icon);
			break;
		case Level.OFF_INT:
		default:
			iv.setImageResource(R.drawable.actual_off_level_icon);
		}
	}

	private void updateSelText(String selection) {
		selectionText
				.setText((selection == null) ? "None selected" : selection);
	}

	static final int TRACE_IX = 0;
	static final int DEBUG_IX = 1;
	static final int INFO_IX = 2;
	static final int WARN_IX = 3;
	static final int ERROR_IX = 4;
	static final int OFF_IX = 5;
	static final int CLEAR_IX = 6;

	public static class ViewHolder extends TreeAdapter.ViewHolder {
		ImageView levelIV;
		ImageView appenderIV;

		public ViewHolder(TreeAdapter.ViewHolder holder) {
			this.tv = holder.tv;
		}

	}

	public class LoggerIconAdapter extends TreeAdapter<LoggerHolder> {
		final private LoggerEditor parent = LoggerEditor.this;

		public LoggerIconAdapter(Tree<LoggerHolder> objects, Context context,
				int resource, int textViewResourceId) {
			super(objects, context, resource, textViewResourceId);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup group) {

			final View row = super.getView(position, convertView, group);
			final Object tag = row.getTag();
			final LoggerEditor.ViewHolder holder;

			if (!(tag instanceof LoggerEditor.ViewHolder)) {
				holder = new LoggerEditor.ViewHolder(
						(TreeAdapter.ViewHolder) tag);
				holder.levelIV = (ImageView) (row
						.findViewById(R.id.logger_icon));
				holder.appenderIV = (ImageView) (row
						.findViewById(R.id.appender_icon));
				row.setTag(holder);
			} else {
				holder = (LoggerEditor.ViewHolder) tag;
			}

			final LoggerHolder logger = super.getItem(position);

			final StringBuilder txtBldr = new StringBuilder(logger.name);
			if (parent.showAppenderText.get()) {
				txtBldr.append("  ").append(getAllAppenderString(logger));
			}

			holder.tv.setText(txtBldr.toString());

			if (logger.levelInt == LoggerHolder.NO_LEVEL) {
				holder.tv.setTextAppearance(parent,
						R.style.unselected_logger_font);
				parent.setEffectiveIcon(logger.getEffectiveLevel(),
						holder.levelIV);
			} else {
				holder.tv.setTextAppearance(parent,
						R.style.selected_logger_font);
				parent.setActualIcon(logger.getEffectiveLevel(), holder.levelIV);
			}

			if (logger == rootHolder) {
				holder.appenderIV
						.setImageResource(R.drawable.appender_attached_icon);
			} else if (!logger.additivity) {
				holder.appenderIV
						.setImageResource(R.drawable.appender_attached_icon);
			} else {
				holder.appenderIV.setImageBitmap(null);
			}

			return row;
		}

		/**
		 * Returns a String assuming that all Loggers do not have additivity
		 * enabled for Appenders. This means that there are no longer two
		 * different categories of Appenders (attached and effective), so the
		 * String can express the Appenders affecting a Logger in a more terse
		 * way.
		 */
		@SuppressWarnings("unused")
		private String getTerseAppenderString(LoggerHolder aLogger) {

			StringBuilder nameBldr = new StringBuilder("[ Appenders: ");

			if (aLogger.appenders.isEmpty()) {
				nameBldr.append("none ");
			} else {
				for (String appenderName : aLogger.appenders) {
					nameBldr.append(appenderName).append(" ");
				}
			}

			nameBldr.append(']');
			return nameBldr.toString();

		}

		/**
		 * Makes a String indicating both the attached and inherited Appenders.
		 */
		private String getAllAppenderString(final LoggerHolder aLogger) {

			// TODO Implement this again; for now we are just directing it to
			// display only the appenders directly attached to the logger
			return getTerseAppenderString(aLogger);

			/*
			 * final StringBuilder nameBldr = new StringBuilder();
			 * nameBldr.append(" [ "); final List<Appender<ILoggingEvent>>
			 * effectiveList = Loggers .getEffectiveAppenders(aLogger);
			 * 
			 * if (effectiveList.isEmpty()) { nameBldr.append("none "); } else {
			 * for (Appender<ILoggingEvent> app : effectiveList) {
			 * nameBldr.append(app.getName()).append(" "); } }
			 * 
			 * return nameBldr.append(" ]").toString();
			 */

		}

	}

	// private void setViewColor(View row, int color) {
	// row.setBackgroundColor(color);
	// }

	/**
	 * Get the appenders for the selected logger (matches current view) The
	 * selected logger is passed to the AppenderSelector.
	 * 
	 * @param v
	 *            unused
	 */
	public void configureAppenders(View v) {

		// TODO: It shouldn't be necessary to start a new activity anymore.
		// We should try using a dialog. That way we won't have to implement
		// Parcelable or do any ugly serialization with the logger holder

		/*
		 * if (this.selectedLogger != null) { final Intent intent = new
		 * Intent().putExtra(
		 * "edu.vu.isis.ammo.core.ui.LoggerEditor.selectedLogger",
		 * selectedLogger).setClass(this, AppenderSelector.class);
		 * 
		 * startActivityForResult(intent, 0); } else { Toast.makeText(this,
		 * "Pick a logger before trying to configure its appenders.", 0).show();
		 * }
		 */

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,
			Intent outputIntent) {

		switch (requestCode) {

		// TODO: Appender request code stuff should probably be deleted.

		// case APPENDER_REQUEST_CODE:
		// /*
		// * Determine if appenders match the parent. Additivity is used as
		// * the "effective v. actual" indicator.
		// */
		// if (this.selectedLogger == Loggers.ROOT_LOGGER) {
		// refreshList();
		// return;
		// }
		//
		// if (Loggers.hasSameAppendersAsParent(this.selectedLogger)) {
		// this.selectedLogger.setAdditive(true);
		// Loggers.clearAppenders(this.selectedLogger);
		// } else {
		// this.selectedLogger.setAdditive(false);
		// }
		// refreshList();
		// break;

		case PICKFILE_REQUEST_CODE:
			if (outputIntent == null || outputIntent.getData() == null)
				return;
			loadFile(outputIntent.getData());
			break;
		default:
			break;
		}
	}

	private void loadFile(Uri fileUri) {

		final InputStream fileInputStream;

		try {
			fileInputStream = getContentResolver().openInputStream(fileUri);
		} catch (FileNotFoundException e) {
			Toast.makeText(this, "Could not find file: " + fileUri,
					Toast.LENGTH_LONG);
			e.printStackTrace();
			return;
		}

		final Map<Pattern, Action> ruleMap = new HashMap<Pattern, Action>();
		ruleMap.put(new Pattern("logger"), new LoggerConfigureAction());

		final SimpleConfigurator simpleConfigurator = new SimpleConfigurator(
				ruleMap);

		// link the configurator with its context
		// Note that logback has its own Context class, which conflicts
		// with the Android Context, hence the ugly type cast
		simpleConfigurator
				.setContext((ch.qos.logback.core.Context) LoggerFactory
						.getILoggerFactory());

		try {
			simpleConfigurator.doConfigure(fileInputStream);
		} catch (JoranException e) {
			final String errorMessage = "Error loading file: could not parse XML";
			personalLogger.error(errorMessage);
			Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

		refreshList();

	}

	/**
	 * the spinner makes use of this listener.
	 */
	public class MyOnSpinnerDialogClickListener implements
			OnSpinnerDialogClickListener {

		final LoggerEditor parent = LoggerEditor.this;

		/**
		 * Sets the current text on the Spinner to match the given Level
		 * 
		 * @param lvl
		 */
		public void updateSpinner(final Level lvl, final Spinner spinner) {

			switch (lvl.levelInt) {
			case Level.TRACE_INT:
				spinner.setSelection(TRACE_IX);
				break;
			case Level.DEBUG_INT:
				spinner.setSelection(DEBUG_IX);
				break;
			case Level.INFO_INT:
				spinner.setSelection(INFO_IX);
				break;
			case Level.WARN_INT:
				spinner.setSelection(WARN_IX);
				break;
			case Level.ERROR_INT:
				spinner.setSelection(ERROR_IX);
				break;
			case Level.OFF_INT:
			default:
				spinner.setSelection(OFF_IX);
			}
		}

		/**
		 * Updates the logger and the icon in its row based on the selected
		 * level.
		 */
		public void onSpinnerDialogClick(int which) {

			if (parent.selectedLogger == null) {
				Toast.makeText(parent, "Please select a logger.",
						Toast.LENGTH_SHORT).show();
				return;
			}

			if (parent.selectedView == null)
				return;

			final int nextLevelInt;

			switch (which) {
			case TRACE_IX:
				nextLevelInt = Level.TRACE_INT;
				break;
			case DEBUG_IX:
				nextLevelInt = Level.DEBUG_INT;
				break;
			case INFO_IX:
				nextLevelInt = Level.INFO_INT;
				break;
			case WARN_IX:
				nextLevelInt = Level.WARN_INT;
				break;
			case ERROR_IX:
				nextLevelInt = Level.ERROR_INT;
				break;
			case OFF_IX:
				nextLevelInt = Level.OFF_INT;
				break;
			case CLEAR_IX:
			default:
				if (selectedLogger.equals(Loggers.ROOT_LOGGER)) {
					Toast.makeText(parent,
							"Clearing the root logger is not allowed",
							Toast.LENGTH_LONG).show();
					return;
				}
				nextLevelInt = LoggerHolder.NO_LEVEL;
			}

			parent.selectedLogger.levelInt = nextLevelInt;

			// We want to use the effective level for the icon if the
			// Logger's level is null
			updateIcon(
					(nextLevelInt == LoggerHolder.NO_LEVEL) ? parent.selectedLogger.getEffectiveLevel()
							: Level.toLevel(nextLevelInt, Level.DEBUG),
					parent.selectedView);

		}

	}

	protected class LoggerHolder {

		public static final int NO_LEVEL = -152;

		int levelInt = NO_LEVEL;
		boolean additivity;
		String name;
		List<String> appenders;

		public Level getEffectiveLevel() {

			if (levelInt != NO_LEVEL)
				return Level.toLevel(levelInt);

			final String parentName = Loggers.getParentLoggerName(name);
			final LoggerHolder parent = loggerMap.get(parentName);

			if (parent == null) {
				// The parent should never be null, but just log an error and
				// return our own level if it does
				personalLogger
						.error("Could not find parent logger for {} when getting effective level",
								name);
				return Level.toLevel(levelInt);
			}

			return parent.getEffectiveLevel();

		}

	}

	/*
	 * The following code comes from
	 * http://developer.android.com/reference/android
	 * /os/Environment.html#getExternalStorageDirectory() It allows us to
	 * monitor the state of the external storage so we know if we are allowed to
	 * read or write
	 */
	private BroadcastReceiver mExternalStorageReceiver;
	private boolean mExternalStorageAvailable = false;
	private boolean mExternalStorageWriteable = false;

	private void updateExternalStorageState() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		personalLogger.info("Read access: {}  Write access: {}",
				mExternalStorageAvailable, mExternalStorageWriteable);
		// handleExternalStorageState(mExternalStorageAvailable,
		// mExternalStorageWriteable);
	}

	private void startWatchingExternalStorage() {
		personalLogger.info("Starting to monitor storage state");
		mExternalStorageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				personalLogger.info("Storage state changed: {}",
						intent.getData());
				updateExternalStorageState();
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		registerReceiver(mExternalStorageReceiver, filter);
		updateExternalStorageState();
	}

	private void stopWatchingExternalStorage() {
		personalLogger.info("No longer monitoring storage state");
		unregisterReceiver(mExternalStorageReceiver);
	}

}
