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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.vu.isis.logger.R;
import edu.vu.isis.logger.lib.LauiContentProvider;
import edu.vu.isis.logger.lib.Loggers;
import edu.vu.isis.logger.lib.Tree;
import edu.vu.isis.logger.provider.LauiContentUri;
import edu.vu.isis.logger.util.TreeAdapter;

/**
 * This class provides a user interface to edit the Level and Appenders of all
 * Logger objects provided by the ContentProvider. The ContentProvider is
 * queried for all of the Loggers and Appenders once when the Activity is
 * started. The user can also force another query from a menu selection. Loggers
 * and Appenders are stored inside of LoggerHolder and AppenderHolder objects.
 * These allow us to keep track of the levels of the Loggers and Appenders
 * without querying the ContentProvider excessively. The UI only reflects what
 * we have stored in our AppenderHolder and LoggerHolder objects, so it is
 * important to note that if something happens that causes the level of a Logger
 * to change in the other application, then we will not know and our UI will
 * show invalid data.
 * 
 * This class also has the capability of saving and loading Logger
 * configurations. This is achieved by reading and writing XML files.
 * 
 * @author Nick King
 * 
 */

public class LoggerEditor extends ListActivity {

	private static final int READ_MENU = Menu.NONE + 0;
	private static final int TOGGLE_APPENDER_TEXT_MENU = Menu.NONE + 1;
	private static final int RELOAD_MENU = Menu.NONE + 2;
	private static final int LOAD_CONFIGURATION_MENU = Menu.NONE + 3;
	private static final int SAVE_CONFIGURATION_MENU = Menu.NONE + 4;

	private static final int PICKFILE_REQUEST_CODE = 1;

	private static final String DEFAULT_SAVE_DIRECTORY = "/logger/save";

	public static final String EXTRA_NAME = "cpauthority";

	private LoggerHolder selectedLogger;
	private View selectedView;
	private Tree<LoggerHolder> loggerTree;

	// We use this logger to log for this Activity
	private final Logger personalLogger = Loggers
			.getLoggerByName("ui.logger.editor");

	private TextView selectionText;
	private HintSpinner levelSpinner;
	private MyOnSpinnerDialogClickListener spinnerListener;
	private ListView mListView;
	private LoggerIconAdapter mAdapter;

	// Storing our LoggerHolders in a Map makes it easier to find our
	// previously created LoggerHolders by name instead of having to search
	// a list every time. This is mainly used when we make the tree of
	// LoggerHolders.
	private Map<String, LoggerHolder> loggerMap = new HashMap<String, LoggerHolder>();

	// We use this Map so that we can reference the same AppenderHolders
	// each time we attach one to a LoggerHolder
	private Map<String, AppenderHolder> appenderMap = new HashMap<String, AppenderHolder>();

	private List<AppenderHolder> appenderList = new ArrayList<AppenderHolder>();
	private LoggerHolder rootLogger;
	private AtomicBoolean showAppenderText = new AtomicBoolean(true);
	private LauiContentUri mLauiContentUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.logger_editor);

		this.mListView = super.getListView();
		getDataAndSetUpList();

		this.selectionText = (TextView) findViewById(R.id.selection_text);
		this.levelSpinner = (HintSpinner) findViewById(R.id.level_spinner);

		final ArrayAdapter<CharSequence> spinAdapter = ArrayAdapter
				.createFromResource(this, R.array.level_options,
						android.R.layout.simple_spinner_item);
		spinAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		this.levelSpinner.setAlwaysShowHint(true);
		this.levelSpinner.setAdapter(spinAdapter);
		this.spinnerListener = new MyOnSpinnerDialogClickListener();
		this.levelSpinner.setOnItemSelectedListener(this.spinnerListener);

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

	private void getDataAndSetUpList() {
		String authority = getIntent().getStringExtra(EXTRA_NAME);
		if (authority != null) {
			mLauiContentUri = new LauiContentUri(authority);
		} else {
			TextView emptyView = new TextView(this);
			emptyView.setText("Unable to find ContentProvider with authority: "
					+ authority);
			this.mListView.setEmptyView(emptyView);
			return;
		}

		// Query the Content Provider for the Appender list
		Cursor cursor = getContentResolver().query(
				mLauiContentUri.getAppenderTableContentUri(), null, null, null,
				null);

		if (cursor != null && cursor.getCount() >= 1) {
			final int nameIndex = cursor
					.getColumnIndex(LauiContentProvider.AppenderTable.NAME);
			final int filepathIndex = cursor
					.getColumnIndex(LauiContentProvider.AppenderTable.FILE_PATH_STRING);
			while (cursor.moveToNext()) {
				String name = cursor.getString(nameIndex);
				AppenderHolder appender = new AppenderHolder(name);
				appender.filepath = cursor.getString(filepathIndex);

				appenderList.add(appender);
				appenderMap.put(appender.name, appender);
			}
		}

		// Query the Content Provider for the logger list
		cursor = getContentResolver().query(
				mLauiContentUri.getLoggerTableContentUri(), null, null, null,
				null);
		List<LoggerHolder> tempList = new ArrayList<LoggerHolder>();

		if (cursor != null && cursor.getCount() >= 1) {
			final int nameIndex = cursor
					.getColumnIndexOrThrow(LauiContentProvider.LoggerTable.NAME);
			final int levelIndex = cursor
					.getColumnIndexOrThrow(LauiContentProvider.LoggerTable.LEVEL_INT);
			final int additivityIndex = cursor
					.getColumnIndexOrThrow(LauiContentProvider.LoggerTable.ADDITIVITY);
			final int appenderIndex = cursor
					.getColumnIndexOrThrow(LauiContentProvider.LoggerTable.ATTACHED_APPENDER_NAMES);
			while (cursor.moveToNext()) {
				final String loggerName = cursor.getString(nameIndex);
				final int levelInt = cursor.getInt(levelIndex);
				final boolean additivity = cursor.getInt(additivityIndex) == 1;

				String rawAppenderNames = cursor.getString(appenderIndex);
				final LoggerHolder logger;
				if (rawAppenderNames == null || rawAppenderNames.equals("")) {
					logger = new LoggerHolder(loggerName);
				} else {
					final String[] attachedNames = (rawAppenderNames == null) ? new String[0]
							: rawAppenderNames.split(",");

					List<AppenderHolder> appenderList = new ArrayList<AppenderHolder>(
							attachedNames.length);

					personalLogger.trace("Raw appender name String: {}",
							rawAppenderNames);
					personalLogger.trace("Split String array: {}",
							Arrays.toString(attachedNames));
					for (String appenderName : attachedNames) {
						personalLogger.trace(
								"Adding AppenderHolder {} to LoggerHolder {}",
								appenderName, loggerName);
						AppenderHolder holder = appenderMap.get(appenderName);
						if (holder == null) {
							holder = new AppenderHolder(appenderName);
							appenderMap.put(holder.name, holder);
						}
						appenderList.add(holder);
					}
					logger = new LoggerHolder(loggerName, appenderList);
				}

				logger.additivity = additivity;
				logger.levelInt = levelInt;

				loggerMap.put(logger.name, logger);

				/*
				 * We want our ListView to show the Loggers in alphabetical
				 * order. To keep from wasting time building a list from the
				 * Collection of values in the Map and then sorting it
				 * ourselves, we just make a temporary list of loggers along
				 * with the map. The content provider originally got the list of
				 * loggers from Logback, and Logback sorted the list before
				 * returning it.
				 */
				tempList.add(logger);

				if (logger.name.equals(Logger.ROOT_LOGGER_NAME)) {
					rootLogger = logger;
				}

			}
		}

		this.loggerTree = makeTree(tempList);

		this.mAdapter = new LoggerIconAdapter(loggerTree, this,
				R.layout.logger_row, R.id.logger_text);
		setListAdapter(mAdapter);
	}

	private Tree<LoggerHolder> makeTree(Collection<LoggerHolder> collection) {

		// If we never found a root logger, we make our own root logger with
		// fields that will indicate in the UI that the root logger wasn't
		// found.
		// We make sure to initialize all of the fields so we don't accidentally
		// get a null pointer exception
		if (rootLogger == null) {
			rootLogger = new LoggerHolder("ROOT_NOT_FOUND");
			rootLogger.levelInt = Level.OFF_INT;
			rootLogger.appenders = new HashSet<AppenderHolder>();
		}

		final Tree<LoggerHolder> mTree = Tree.newInstance(rootLogger);

		for (final LoggerHolder logger : collection) {
			// The root was already added so we skip it
			if (logger == rootLogger) {
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

		levelSpinner.clearSelection();
		updateSelText(nextSelectedLogger.name);

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
		menu.add(Menu.NONE, RELOAD_MENU, Menu.NONE, "Reload Logger list");
		menu.add(Menu.NONE, SAVE_CONFIGURATION_MENU, Menu.NONE,
				"Save logger settings");
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
		case RELOAD_MENU:
			loggerMap.clear();
			appenderList.clear();
			rootLogger = null;
			selectedLogger = null;
			selectedView = null;
			getDataAndSetUpList();
			break;
		case SAVE_CONFIGURATION_MENU:
			promptForSave();
			break;
		case LOAD_CONFIGURATION_MENU:
			if (mExternalStorageAvailable) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("text/xml");
				try {
					startActivityForResult(intent, PICKFILE_REQUEST_CODE);
				} catch (ActivityNotFoundException e) {
					// They didn't have an app that allows them to pick a file,
					// so we display a dialog that just prompts them
					// for a file
					promptForLoad();
				}
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

	private void promptForLoad() {
		try {
			Class<?> types[] = new Class<?>[] { String.class, String.class };
			Method m = LoggerEditor.class.getDeclaredMethod("loadFile", types);
			makePrompt("Load logger settings from file", "Load", m);
		} catch (NoSuchMethodException e) {
			personalLogger.error("Error finding loadFile method", e);
		}
	}

	private void promptForSave() {
		try {
			Class<?> types[] = new Class<?>[] { String.class, String.class };
			Method m = LoggerEditor.class.getDeclaredMethod("saveFile", types);
			makePrompt("Save logger settings to file", "Save", m);
		} catch (NoSuchMethodException e) {
			personalLogger.error("Error finding saveFile method", e);
		}
	}

	private void makePrompt(String title, String confirmButtonStr,
			final Method confirmButtonMethod) {
		final Dialog dialog = new Dialog(this);
		dialog.setTitle(title);
		dialog.setContentView(R.layout.logger_file_dialog);

		final EditText filenameEdit = (EditText) dialog
				.findViewById(R.id.dialog_file_name_edit);
		final EditText directoryEdit = (EditText) dialog
				.findViewById(R.id.dialog_directory_edit);
		final Button confirmButton = (Button) dialog
				.findViewById(R.id.dialog_confirm_button);
		final Button cancelButton = (Button) dialog
				.findViewById(R.id.dialog_cancel_button);

		confirmButton.setText(confirmButtonStr);
		directoryEdit.setText(Environment.getExternalStorageDirectory()
				+ DEFAULT_SAVE_DIRECTORY);

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		confirmButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mExternalStorageWriteable) {
					String filename = filenameEdit.getText().toString();
					if (filename.equals("")) {
						Toast.makeText(LoggerEditor.this,
								"Enter a nonempty filename", Toast.LENGTH_LONG)
								.show();
						return;
					}
					String directory = directoryEdit.getText().toString();
					if (directory.equals("")) {
						Toast.makeText(LoggerEditor.this,
								"Empter a nonempty directory",
								Toast.LENGTH_LONG).show();
						return;
					}

					filename = format(filename, ".xml");
					directory = format(directory, "/");
					try {
						confirmButtonMethod.setAccessible(true);
						confirmButtonMethod.invoke(LoggerEditor.this, filename,
								directory);
					} catch (Exception e) {
						personalLogger.error("Error when invoking method {}",
								confirmButtonMethod.getName(), e);
					}
				}
				dialog.dismiss();
			}

			private String format(String str, String properEnding) {
				str = str.trim();
				if (!str.endsWith(properEnding)) {
					str += properEnding;
				}
				return str;
			}

		});

		dialog.show();
	}

	// This is called reflectively by the dialog
	@SuppressWarnings("unused")
	private void saveFile(String filename, String directory) {
		final File dirFile = new File(directory);
		final File file = new File(directory + filename);

		if (dirFile.mkdirs()) {
			personalLogger.info("Directory {} was created for save",
					dirFile.getAbsolutePath());
		} else {
			personalLogger.info("Directory {} was not created for save",
					dirFile.getAbsolutePath());
		}
		OutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			writeXML(fos);
		} catch (FileNotFoundException e) {
			personalLogger.error(
					"FileNotFoundException! Unable to write file {}.", file, e);
		} finally {
			if (fos != null) {
				try {
					fos.flush();
					fos.close();
				} catch (IOException e) {
					personalLogger.error(
							"Exception when closing output stream for file {}",
							file, e);
				}
			}
		}
	}

	// This is called reflectively by the dialog
	@SuppressWarnings("unused")
	private void loadFile(String directory, String filename) {
		Uri loadUri = Uri.parse("file://" + directory + "/" + filename);
		loadFile(loadUri);
	}

	private void loadFile(Uri fileUri) {
		InputStream fis = null;
		try {
			fis = getContentResolver().openInputStream(fileUri);
			ContentHandler handler = new LoggerXmlHandler();
			Xml.parse(fis, Xml.Encoding.UTF_8, handler);
		} catch (FileNotFoundException e) {
			personalLogger.error("File not found: {}", fileUri);
			Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			personalLogger.error("IOException when reading XML file: {}",
					fileUri, e);
		} catch (SAXException e) {
			personalLogger.error("SAX fatal error when reading XML file: {}",
					fileUri, e);
		} finally {
			if (fis == null) {
				return;
			}
			try {
				fis.close();
			} catch (IOException e) {
				personalLogger.error("Error closing input stream for file: {}",
						fileUri, e);
			}
		}
		mListView.invalidateViews();
	}

	private static final String LEVEL_ATT = "level";
	private static final String NAME_ATT = "name";
	private static final String APPENDER_ATT = "appender";
	private static final String ADDITIVITY_ATT = "additivity";

	private class LoggerXmlHandler extends DefaultHandler {

		@Override
		public void startElement(String namespaceURI, String localName,
				String qName, Attributes atts) throws SAXException {
			if (!localName.equals("logger")) {
				personalLogger.error("Got a bad tag: {}. Skipping", localName);
				return;
			}

			String levelStr = atts.getValue(LEVEL_ATT);
			String nameStr = atts.getValue(NAME_ATT);
			String additivityStr = atts.getValue(ADDITIVITY_ATT);
			String appenderStr = atts.getValue(APPENDER_ATT);

			if (levelStr == null || nameStr == null || additivityStr == null
					|| appenderStr == null) {
				personalLogger
						.error("One of the XML attributes was null. Skipping");
			}

			LoggerHolder logger = loggerMap.get(nameStr);
			if (logger == null) {
				personalLogger.warn("Logger {} was not found.  Skipping",
						nameStr);
			}

			Set<AppenderHolder> appenderSet = new HashSet<AppenderHolder>();
			for (String appenderName : appenderStr.split(" ")) {
				AppenderHolder appender = appenderMap.get(appenderName.trim());
				appenderSet.add(appender);
			}

			logger.levelInt = levelStr.equals("null") ? LauiContentProvider.NO_LEVEL
					: Level.valueOf(levelStr).toInteger();
			logger.additivity = Boolean.valueOf(additivityStr);
			logger.appenders = appenderSet;
		}

	}

	private void writeXML(OutputStream os) {
		XmlSerializer serializer = Xml.newSerializer();
		try {
			serializer.setOutput(os, "UTF-8");
			serializer.startDocument("UTF-8", true);
			for (LoggerHolder logger : loggerMap.values()) {
				serializer.startTag("", "logger");
				serializer.attribute("", NAME_ATT, logger.name);
				serializer
						.attribute(
								"",
								LEVEL_ATT,
								(logger.levelInt != LauiContentProvider.NO_LEVEL) ? Level
										.toLevel(logger.levelInt).toString()
										: "null");
				serializer.attribute("", APPENDER_ATT,
						makeAppenderStr(logger.appenders));
				serializer.attribute("", ADDITIVITY_ATT,
						Boolean.toString(logger.additivity));
				serializer.endTag("", "logger");
			}
			serializer.endDocument();
		} catch (IOException e) {
			final String errorMessage = "Error saving XML file";
			personalLogger.error(errorMessage, e);
			Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
		}
	}

	private String makeAppenderStr(Collection<AppenderHolder> appenders) {
		if (appenders.isEmpty())
			return "none";

		StringBuilder bldr = new StringBuilder();
		for (AppenderHolder holder : appenders) {
			bldr.append(holder.name).append(" ");
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

		for (final AppenderHolder a : appenderList) {
			if (!(a.hasFilepath()))
				continue;
			Button button = new Button(this);
			button.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
			button.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			button.setText(a.name);
			button.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {

					Intent intent = new Intent().setClass(LoggerEditor.this,
							FileLogViewer.class);
					intent.putExtra(LogViewerBase.EXTRA_NAME, a.filepath);
					startActivity(intent);
					dialog.dismiss();
				}
			});
			ll.addView(button);
		}

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

		if (selectedLogger.levelInt == LauiContentProvider.NO_LEVEL) {
			setEffectiveIcon(lvl, iv);
		} else {
			setActualIcon(lvl, iv);
		}
		refreshList();
	}

	private void setEffectiveIcon(Level lvl, ImageView iv) {
		switch (lvl.levelInt) {
		case Level.TRACE_INT:
			iv.setImageResource(R.drawable.ic_effective_logger_trace);
			break;
		case Level.DEBUG_INT:
			iv.setImageResource(R.drawable.ic_effective_logger_debug);
			break;
		case Level.INFO_INT:
			iv.setImageResource(R.drawable.ic_effective_logger_info);
			break;
		case Level.WARN_INT:
			iv.setImageResource(R.drawable.ic_effective_logger_warn);
			break;
		case Level.ERROR_INT:
			iv.setImageResource(R.drawable.ic_effective_logger_error);
			break;
		case Level.OFF_INT:
		default:
			iv.setImageResource(R.drawable.ic_effective_logger_off);
		}
	}

	private void setActualIcon(Level lvl, ImageView iv) {
		switch (lvl.levelInt) {
		case Level.TRACE_INT:
			iv.setImageResource(R.drawable.ic_actual_logger_trace);
			break;
		case Level.DEBUG_INT:
			iv.setImageResource(R.drawable.ic_actual_logger_debug);
			break;
		case Level.INFO_INT:
			iv.setImageResource(R.drawable.ic_actual_logger_info);
			break;
		case Level.WARN_INT:
			iv.setImageResource(R.drawable.ic_actual_logger_warn);
			break;
		case Level.ERROR_INT:
			iv.setImageResource(R.drawable.ic_actual_logger_error);
			break;
		case Level.OFF_INT:
		default:
			iv.setImageResource(R.drawable.ic_actual_logger_off);
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

			if (logger.levelInt == LauiContentProvider.NO_LEVEL) {
				holder.tv.setTextAppearance(parent,
						R.style.unselected_logger_font);
				parent.setEffectiveIcon(getEffectiveLevel(logger),
						holder.levelIV);
			} else {
				holder.tv.setTextAppearance(parent,
						R.style.selected_logger_font);
				parent.setActualIcon(getEffectiveLevel(logger), holder.levelIV);
			}

			if (logger == rootLogger) {
				holder.appenderIV
						.setImageResource(R.drawable.ic_appender_attached);
			} else if (!logger.isAdditive()) {
				holder.appenderIV
						.setImageResource(R.drawable.ic_appender_attached);
			} else {
				holder.appenderIV.setImageBitmap(null);
			}

			// We have to save the padding that was set on the row
			// and reapply it after we set the background drawable
			// since setBackgroundDrawable clears padding
			final int rightPadding = row.getPaddingRight();
			final int leftPadding = row.getPaddingLeft();
			final int topPadding = row.getPaddingTop();
			final int bottomPadding = row.getPaddingBottom();

			if (selectedLogger == logger) {
				row.setBackgroundDrawable(getResources().getDrawable(
						R.color.selected_logger_bg));
			} else {
				row.setBackgroundDrawable(getResources().getDrawable(
						android.R.drawable.list_selector_background));
			}

			row.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);

			return row;
		}

		/**
		 * Makes a String indicating both the attached and inherited Appenders.
		 */
		private String getAllAppenderString(final LoggerHolder aLogger) {

			final StringBuilder nameBldr = new StringBuilder();
			nameBldr.append(" [ ");
			final List<AppenderHolder> effectiveList = getEffectiveAppenderList(aLogger);

			for (AppenderHolder holder : effectiveList) {
				nameBldr.append(holder.name).append(" ");
			}

			return nameBldr.append("]").toString();

		}

	}

	// private void setViewColor(View row, int color) {
	// row.setBackgroundColor(color);
	// }

	public void configureAppenders(View v) {

		if (selectedLogger == null) {
			Toast.makeText(this, "Please select a Logger first",
					Toast.LENGTH_SHORT).show();
			return;
		}

		final Dialog dialog = new Dialog(this);
		dialog.setTitle("Appender configuration");
		dialog.setContentView(R.layout.appender_selector_dialog);

		TextView tv = (TextView) dialog
				.findViewById(R.id.appender_selector_message);
		String message = "Configuring appenders for logger "
				+ selectedLogger.name + "\nClose dialog to save changes.";
		tv.setText(message);

		final LoggerHolder parentLogger = loggerMap.get(Loggers
				.getParentLoggerName(selectedLogger.name));
		final Set<AppenderHolder> parentEffAppenders = getEffectiveAppenderSet(parentLogger);
		final Set<AppenderHolder> selectedEffAppenders = getEffectiveAppenderSet(selectedLogger);

		final LinearLayout layout = (LinearLayout) dialog
				.findViewById(R.id.appender_layout);

		for (AppenderHolder a : appenderList) {
			CheckBox cb = new CheckBox(this);
			cb.setText(a.name);
			cb.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
			cb.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			cb.setTag(a);

			cb.setChecked(selectedEffAppenders.contains(a));

			layout.addView(cb);
		}

		dialog.setOnDismissListener(new OnDismissListener() {

			LoggerEditor parent = LoggerEditor.this;

			// We actually apply the changes to the appenders on the logger
			// when the dialog is dismissed
			@Override
			public void onDismiss(DialogInterface dialog) {
				StringBuilder sb = new StringBuilder();

				for (AppenderHolder a : appenderList) {
					CheckBox cb = (CheckBox) layout.findViewWithTag(a);
					if (cb.isChecked()) {
						sb.append(a.name).append(",");
						selectedLogger.appenders.add(a);
					} else {
						selectedLogger.appenders.remove(a);
					}
				}

				// Delete the trailing comma
				if (sb.length() > 0) {
					sb.deleteCharAt(sb.length() - 1);
				}

				personalLogger.trace("Selected logger attached appenders: {}",
						selectedLogger.appenders.toString());
				personalLogger.trace("Parent logger effective appenders: {}",
						parentEffAppenders.toString());

				if (selectedLogger.appenders.equals(parentEffAppenders)) {
					selectedLogger.additivity = true;
					selectedLogger.appenders.clear();
				} else {
					selectedLogger.additivity = false;
				}

				Uri updateUri = ContentUris.withAppendedId(
						mLauiContentUri.getLoggerTableContentUri(), 0);
				ContentValues values = new ContentValues();
				values.put(LauiContentProvider.APPENDER_KEY, sb.toString());
				parent.getContentResolver().update(updateUri, values,
						selectedLogger.name, null);

				parent.mListView.invalidateViews();
			}

		});

		dialog.show();

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode,
			Intent outputIntent) {

		switch (requestCode) {

		case PICKFILE_REQUEST_CODE:
			if (outputIntent == null || outputIntent.getData() == null)
				return;
			loadFile(outputIntent.getData());
			break;
		default:
			break;
		}
	}

	/**
	 * the spinner makes use of this listener.
	 */
	public class MyOnSpinnerDialogClickListener implements
			OnItemSelectedListener {

		final LoggerEditor parent = LoggerEditor.this;

		/**
		 * Updates the logger and the icon in its row based on the selected
		 * level.
		 */
		@Override
		public void onItemSelected(AdapterView<?> av, View view, int position,
				long id) {
			if (parent.selectedLogger == null) {
				Toast.makeText(parent, "Please select a logger.",
						Toast.LENGTH_SHORT).show();
				return;
			}

			if (parent.selectedView == null)
				return;

			final int nextLevelInt;

			switch (position) {
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
				if (selectedLogger == rootLogger) {
					Toast.makeText(parent,
							"Clearing the root logger is not allowed",
							Toast.LENGTH_LONG).show();
					return;
				}
				nextLevelInt = LauiContentProvider.NO_LEVEL;
			}

			parent.selectedLogger.levelInt = nextLevelInt;

			Uri updateUri = ContentUris.withAppendedId(
					mLauiContentUri.getLoggerTableContentUri(), 0);
			ContentValues levelValues = new ContentValues();
			levelValues.put(LauiContentProvider.LEVEL_KEY, nextLevelInt);

			final int numUpdates = getContentResolver().update(updateUri,
					levelValues, parent.selectedLogger.name, null);
			personalLogger.debug("Updated {} rows", numUpdates);

			if (numUpdates == 0) {
				Toast.makeText(
						parent,
						"The logger " + selectedLogger.name
								+ " was unable to be updated.",
						Toast.LENGTH_LONG).show();
				return;
			}

			// We want to use the effective level for the icon if the
			// Logger's level is null
			updateIcon(
					(nextLevelInt == LauiContentProvider.NO_LEVEL) ? getEffectiveLevel(parent.selectedLogger)
							: Level.toLevel(nextLevelInt, Level.DEBUG),
					parent.selectedView);

		}

		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}

	}

	/**
	 * Static class to hold the important fields of a Logger. Used to cache the
	 * results from queries to the ContentProvider so that we don't have to
	 * query it often. Be sure to update the fields of any instances of this
	 * class if you send an update to the ContentProvider.
	 * 
	 * @author Nick King
	 * 
	 */
	static class LoggerHolder {

		int levelInt = LauiContentProvider.NO_LEVEL;
		boolean additivity = true;
		String name;
		Set<AppenderHolder> appenders = new HashSet<AppenderHolder>();

		@SuppressWarnings("unused")
		private LoggerHolder() {
			throw new AssertionError("This constructor should never be called");
		}

		LoggerHolder(String name) {
			if (name == null)
				throw new NullPointerException("LoggerHolder given null name");
			this.name = name;
		}

		LoggerHolder(String name, List<AppenderHolder> appenders) {
			this(name);
			if (appenders == null)
				throw new NullPointerException(
						"LoggerHolder given null AppenderHolder list");
			// We don't want our list to point to the same list as the one we
			// were given
			this.appenders = new HashSet<AppenderHolder>(appenders);
		}

		boolean isAdditive() {
			return additivity;
		}

		@Override
		public String toString() {
			return "LoggerHolder \""
					+ name
					+ "\""
					+ " Level: "
					+ ((levelInt == LauiContentProvider.NO_LEVEL) ? "none"
							: Level.toLevel(levelInt, Level.ALL));
		}

	}

	/**
	 * @return an alphabetically sorted list of Appenders affecting the
	 *         LoggerHolder
	 */
	private List<AppenderHolder> getEffectiveAppenderList(LoggerHolder logger) {
		Set<AppenderHolder> set = getEffectiveAppenders(
				new HashSet<AppenderHolder>(), logger);
		List<AppenderHolder> list = new ArrayList<AppenderHolder>(set);
		Collections.sort(list, AppenderHolder.COMPARATOR);
		return list;
	}

	// Just a wrapper for the recursive function
	private Set<AppenderHolder> getEffectiveAppenderSet(LoggerHolder logger) {
		return getEffectiveAppenders(new HashSet<AppenderHolder>(), logger);
	}

	private Set<AppenderHolder> getEffectiveAppenders(
			Set<AppenderHolder> soFar, LoggerHolder logger) {
		soFar.addAll(logger.appenders);

		if (logger.additivity == false || logger == rootLogger) {
			return soFar;
		}

		String parentName = Loggers.getParentLoggerName(logger.name);
		LoggerHolder parent = loggerMap.get(parentName);

		if (parent != null) {
			return getEffectiveAppenders(soFar, parent);
		}

		// The parent should never be null, but if it is, we just
		// return what we have to prevent a null pointer exception
		return soFar;

	}

	private Level getEffectiveLevel(LoggerHolder logger) {
		if (logger.levelInt != LauiContentProvider.NO_LEVEL) {
			return Level.toLevel(logger.levelInt);
		}

		if (logger == rootLogger) {
			return Level.toLevel(logger.levelInt);
		}

		final String parentName = Loggers.getParentLoggerName(logger.name);
		final LoggerHolder parent = loggerMap.get(parentName);

		if (parent == null) {
			// The parent should never be null, but just log an error and
			// return our own level if it does
			personalLogger
					.error("Could not find parent logger for {} when getting effective level",
							logger.name);
			return Level.toLevel(logger.levelInt);
		}

		return getEffectiveLevel(parent);
	}

	/**
	 * Static class to hold the important fields of an Appender. Used to cache
	 * the results from queries to the ContentProvider so that we don't have to
	 * query it often. Be sure to update the fields of any instances of this
	 * class if you send an update to the ContentProvider.
	 * 
	 * @author Nick King
	 * 
	 */
	static class AppenderHolder {

		String name;
		String filepath;

		@SuppressWarnings("unused")
		private AppenderHolder() {
			throw new AssertionError("This constructor should never be called.");
		}

		AppenderHolder(String name) {
			if (name == null)
				throw new NullPointerException("AppenderHolder given null name");
			this.name = name;
		}

		static final Comparator<AppenderHolder> COMPARATOR = new Comparator<AppenderHolder>() {

			@Override
			public int compare(AppenderHolder holder1, AppenderHolder holder2) {
				// We just want to put them in alphabetical order by name
				return holder1.name.compareTo(holder2.name);
			}

		};

		boolean hasFilepath() {
			return filepath != null;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof AppenderHolder))
				return false;

			AppenderHolder holder = (AppenderHolder) o;
			return this.name.equals(holder.name);
		}

		@Override
		public String toString() {
			return "AppenderHolder \"" + name + "\"";
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
