package edu.vu.isis.logger.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;

public class LauiContentProvider extends ContentProvider {

	public static final String AUTHORITY = "edu.vu.isis.logger.LauiContentProvider";

	public static final class LoggerTable implements BaseColumns {

		// URI and MIME type constants
		public static final String MIME_DIR_PREFIX = "vnd.andriod.cursor.dir";
		public static final String MIME_ITEM_PREFIX = "vnd.android.cursor.item";
		public static final String MIME_ITEM = "vnd.vu.loggers";
		public static final String MIME_TYPE_SINGLE = MIME_ITEM_PREFIX + "/"
				+ MIME_ITEM;
		public static final String MIME_TYPE_MULTIPLE = MIME_DIR_PREFIX + "/"
				+ MIME_ITEM;
		public static final String PATH_SINGLE = "loggers/#";
		public static final String PATH_MULTIPLE = "loggers";
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/" + PATH_MULTIPLE);

		// Columns
		public static final String NAME = "name";
		public static final String LEVEL_INT = "level_int";
		public static final String ADDITIVITY = "additivity";
		public static final String ATTACHED_APPENDER_NAMES = "attached_appender_names";

		public static final String[] COLUMN_NAMES = { NAME, LEVEL_INT,
				ADDITIVITY, ATTACHED_APPENDER_NAMES };

	}

	public static final class AppenderTable implements BaseColumns {

		// URI and MIME type constants

		public static final String MIME_DIR_PREFIX = "vnd.andriod.cursor.dir";
		public static final String MIME_ITEM_PREFIX = "vnd.android.cursor.item";
		public static final String MIME_ITEM = "vnd.vu.appenders";
		public static final String MIME_TYPE_SINGLE = MIME_ITEM_PREFIX + "/"
				+ MIME_ITEM;
		public static final String MIME_TYPE_MULTIPLE = MIME_DIR_PREFIX + "/"
				+ MIME_ITEM;
		public static final String PATH_SINGLE = "appenders/#";
		public static final String PATH_MULTIPLE = "appenders";
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/" + PATH_MULTIPLE);

		// Columns
		public static final String NAME = "name";
		public static final String FILE_PATH_STRING = "file_path_string";

		public static final String[] COLUMN_NAMES = { NAME, FILE_PATH_STRING };

	}

	// URI Matcher return constants
	private static final int SINGLE_LOGGER = 1;
	private static final int MULTIPLE_LOGGERS = 2;
	private static final int SINGLE_APPENDER = 3;
	private static final int MULTIPLE_APPENDERS = 4;

	// Key contants for updating tables
	public static final String LEVEL_KEY = "level";
	public static final String APPENDER_KEY = "appender";

	private static UriMatcher URI_MATCHER;
	public static final Uri OP_NOT_SUPPORTED = Uri
			.parse("That operation is not supported");

	/** Indicates a null level on a logger */
	public static final int NO_LEVEL = -152;

	private static final LoggerContext LOGGER_CONTEXT = (LoggerContext) LoggerFactory
			.getILoggerFactory();

	private String logTag;

	// Initialize the Uri matcher
	static {
		LauiContentProvider.URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		LauiContentProvider.URI_MATCHER.addURI(LauiContentProvider.AUTHORITY,
				LauiContentProvider.LoggerTable.PATH_SINGLE,
				LauiContentProvider.SINGLE_LOGGER);
		LauiContentProvider.URI_MATCHER.addURI(LauiContentProvider.AUTHORITY,
				LauiContentProvider.LoggerTable.PATH_MULTIPLE,
				LauiContentProvider.MULTIPLE_LOGGERS);
		LauiContentProvider.URI_MATCHER.addURI(LauiContentProvider.AUTHORITY,
				LauiContentProvider.AppenderTable.PATH_SINGLE,
				LauiContentProvider.SINGLE_APPENDER);
		LauiContentProvider.URI_MATCHER.addURI(LauiContentProvider.AUTHORITY,
				LauiContentProvider.AppenderTable.PATH_MULTIPLE,
				LauiContentProvider.MULTIPLE_APPENDERS);

	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		// Try broadcasting an intent which an intent filter in the
		// LAUI app will catch

		// Since multiple apps might be using this library, we get the
		// application name so we can tag our logs without being ambiguous.
		final Resources resources = getContext().getResources();
		if (resources != null) {
			CharSequence appName = resources.getText(resources.getIdentifier(
					"app_name", "string", getContext().getPackageName()));
			logTag = (appName == null) ? "LauiCp" : appName + "_LauiCp";
		} else {
			logTag = "LauiCp";
		}

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		final MatrixCursor cursor;
		switch (LauiContentProvider.URI_MATCHER.match(uri)) {

		case SINGLE_LOGGER: {
			Log.i(logTag, "Got a query for a single logger");
			cursor = new MatrixCursor(
					LauiContentProvider.LoggerTable.COLUMN_NAMES, 1);
			final int whichLogger = Integer.parseInt(uri.getPathSegments().get(
					1));

			final List<Logger> loggerList = LOGGER_CONTEXT.getLoggerList();
			if (whichLogger > loggerList.size() - 1) {
				Log.e(logTag, "Got request for logger #" + whichLogger
						+ " but there are only " + loggerList.size()
						+ " loggers.");
				return null;
			}

			Logger selectedLogger = loggerList.get(whichLogger);
			cursor.addRow(makeLoggerRow(selectedLogger));
		}
			break;

		case MULTIPLE_LOGGERS: {
			Log.i(logTag, "Got a query for all loggers");

			final List<Logger> loggerList = LOGGER_CONTEXT.getLoggerList();
			cursor = new MatrixCursor(
					LauiContentProvider.LoggerTable.COLUMN_NAMES,
					loggerList.size());
			for (Logger logger : loggerList) {
				cursor.addRow(makeLoggerRow(logger));
			}
		}
			break;

		case SINGLE_APPENDER: {
			Log.i(logTag, "Got a query for a single appender");
			cursor = new MatrixCursor(
					LauiContentProvider.AppenderTable.COLUMN_NAMES, 1);
			final int whichAppender = Integer.parseInt(uri.getPathSegments()
					.get(1));
			final List<Appender<ILoggingEvent>> appenderList = getAppenderList();

			if (whichAppender > appenderList.size() - 1) {
				Log.e(logTag, "Got request for logger #" + whichAppender
						+ " but there are only " + appenderList.size()
						+ " loggers.");
				return null;
			}

			Appender<?> selectedAppender = appenderList.get(whichAppender);
			cursor.addRow(makeAppenderRow(selectedAppender));
		}
			break;

		case MULTIPLE_APPENDERS: {
			Log.i(logTag, "Got a query for all appenders");
			List<Appender<ILoggingEvent>> appenderList = getAppenderList();
			cursor = new MatrixCursor(
					LauiContentProvider.AppenderTable.COLUMN_NAMES,
					appenderList.size());
			for (Appender<?> appender : appenderList) {
				cursor.addRow(makeAppenderRow(appender));
			}
		}
			break;

		default:
			Log.i(logTag, "Could not match Uri for query");
			cursor = null;

		}
		return cursor;
	}

	private Object[] makeAppenderRow(Appender<?> appender) {
		String name = appender.getName();
		String filePathString = null;
		if (appender instanceof FileAppender) {
			filePathString = ((FileAppender<?>) appender).getFile();
		}
		Object[] fields = { name, filePathString };
		return fields;
	}

	private Object[] makeLoggerRow(Logger logger) {
		String name = logger.getName();
		Log.v(logTag, "Adding row for logger " + name);
		Level level = logger.getLevel();

		Integer levelInt = (level == null) ? NO_LEVEL : level.levelInt;

		// We use an integer instead of a boolean because it's standard
		// SQLite style to do so
		int additivityInt = logger.isAdditive() ? 1 : 0;

		List<Appender<ILoggingEvent>> attachedAppenders = Loggers
				.getAttachedAppenders(logger);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < attachedAppenders.size(); i++) {
			sb.append(attachedAppenders.get(i).getName()).append(',');
		}

		// Delete the last comma
		if(sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}

		Object[] fields = { name, levelInt, additivityInt, sb.toString() };
		return fields;
	}

	@Override
	public String getType(Uri uri) {
		switch (LauiContentProvider.URI_MATCHER.match(uri)) {
		case SINGLE_LOGGER:
			return LauiContentProvider.LoggerTable.MIME_TYPE_SINGLE;
		case MULTIPLE_LOGGERS:
			return LauiContentProvider.LoggerTable.MIME_TYPE_MULTIPLE;
		case SINGLE_APPENDER:
			return LauiContentProvider.AppenderTable.MIME_TYPE_SINGLE;
		case MULTIPLE_APPENDERS:
			return LauiContentProvider.AppenderTable.MIME_TYPE_MULTIPLE;
		default:
			return null;
		}
	}

	/**
	 * This operation is not supported by this ContentProvider
	 * 
	 * @return OP_NOT_SUPPORTED (operation not supported)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return OP_NOT_SUPPORTED;
	}

	/**
	 * This operation is not supported by this ContentProvider
	 * 
	 * @return -1 (operation not supported)
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return -1;
	}

	/**
	 * Updates a Logger given by the selection string(s). A Uri should be given
	 * to indicate whether one or multiple loggers are to be edited. If only one
	 * logger is to be edited, put its name in the selection argument. If
	 * multiple loggers are to be edited, put all of their names in the
	 * selectionArgs argument.
	 * <p>
	 * The ContentValues argument should have a key and a value indicating the
	 * new level and/or appender for the given logger(s). Use the LEVEL_KEY and
	 * APPENDER_KEY constants provided by this class as keys for putting
	 * information into a ContentValues object. The value of levels should be
	 * indicated by the integer constants in the Logback Level class. To set a
	 * logger's level to null, use the NO_LEVEL constant provided by this class.
	 * <p>
	 * Appender values should be indicated by a String holding their name. If
	 * there are multiple appenders to update, the String should be comma
	 * delimited (ex. Foo,Bar,Baz). Spaces may be put between the names if
	 * desired. <b>The logger(s) will be updated to have only the specified
	 * Appenders attached.</b> This means that if a Logger has the Appenders Foo
	 * and Bar attached to it and the only Appender specified in the
	 * ContentValues is Baz, then the Appenders Foo and Bar will be removed from
	 * the Logger and Baz will be attached to it. If the desired effect is to
	 * add Baz to the Logger and leave its other Appenders attached, the String
	 * should be some permutation of "Foo,Bar,Baz".
	 * <p>
	 * If the Appenders are specified such that the logger will have the same
	 * Appenders as its nearest non-additive ancestor, then its additivity will
	 * be set to true. Otherwise, its additivity will be set to false.
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int updateCount = 0;
		switch (URI_MATCHER.match(uri)) {
		case SINGLE_LOGGER:
			if (updateLoggerByName(selection, values))
				updateCount++;
			break;

		case MULTIPLE_LOGGERS:
			for (String sel : selectionArgs) {
				if (updateLoggerByName(sel, values))
					updateCount++;
			}
			break;

		default:
			return -1;
		}
		return updateCount;
	}

	private boolean updateLoggerByName(String selection, ContentValues values) {
		Logger logger = LOGGER_CONTEXT.exists(selection);
		Integer levelInt = values.getAsInteger(LEVEL_KEY);
		String appenderStr = values.getAsString(APPENDER_KEY);
		boolean isUpdate = false;

		if (logger == null)
			return isUpdate;

		if (levelInt != null)
			isUpdate |= updateLevel(logger, levelInt);
		if (appenderStr != null)
			isUpdate |= updateAppender(logger, appenderStr);

		return isUpdate;
	}

	private boolean updateAppender(Logger logger, String appenderStr) {
		Loggers.clearAppenders(logger);

		List<Appender<ILoggingEvent>> appenderList = getAppenderList();
		if (appenderList.isEmpty())
			return false;

		String[] appenderNames = appenderStr.split(",");
		Log.v(logTag, "Attaching appenders " + Arrays.toString(appenderNames)
				+ " to Logger " + logger.getName());

		for (String name : appenderNames) {
			name = name.trim();
			Appender<ILoggingEvent> found = findAppender(name, appenderList);
			if (found == null) {
				Log.w(logTag, "Appender " + name + " was not found, skipping");
				continue;
			}
			logger.addAppender(found);
		}

		if (Loggers.hasSameAppendersAsParent(logger)) {
			Log.d(logTag, "Setting additivity of Logger " + logger.getName()
					+ " to false.");
			logger.setAdditive(true);
			Loggers.clearAppenders(logger);
		} else {
			logger.setAdditive(false);
		}

		return true;
	}

	private static Appender<ILoggingEvent> findAppender(String name,
			List<Appender<ILoggingEvent>> appenderList) {
		for (Appender<ILoggingEvent> a : appenderList) {
			if (a.getName().equals(name))
				return a;
		}
		return null;
	}

	private static List<Appender<ILoggingEvent>> getAppenderList() {
		List<Appender<ILoggingEvent>> appenderList = new ArrayList<Appender<ILoggingEvent>>();
		try {
			// Joran provided a raw type so we don't really have a choice here
			@SuppressWarnings("rawtypes")
			Map appenderMap = AppenderStore.getAppenderMap();
			for (Object o : appenderMap.values()) {
				@SuppressWarnings("unchecked")
				Appender<ILoggingEvent> a = (Appender<ILoggingEvent>) o;
				appenderList.add(a);
			}
		} catch (IllegalStateException e) {
			Log.e("LauiCp", "Unable to get a reference to the AppenderStore");
		}
		return appenderList;
	}

	/**
	 * Updates the level of a logger based on the corresponding level integer
	 * 
	 * @param logger
	 *            the logger to update
	 * @param levelInt
	 *            the enumeration of the selected level
	 * @return true if the logger is updated, false if not
	 */
	private boolean updateLevel(Logger logger, Integer levelInt) {

		final String levelName;

		switch (levelInt.intValue()) {
		case NO_LEVEL:
			logger.setLevel(null);
			levelName = "null";
			break;
		case Level.TRACE_INT:
			logger.setLevel(Level.TRACE);
			levelName = Level.TRACE.toString();
			break;
		case Level.DEBUG_INT:
			logger.setLevel(Level.DEBUG);
			levelName = Level.DEBUG.toString();
			break;
		case Level.INFO_INT:
			logger.setLevel(Level.INFO);
			levelName = Level.INFO.toString();
			break;
		case Level.WARN_INT:
			logger.setLevel(Level.WARN);
			levelName = Level.WARN.toString();
			break;
		case Level.ERROR_INT:
			logger.setLevel(Level.ERROR);
			levelName = Level.ERROR.toString();
			break;
		case Level.OFF_INT:
			logger.setLevel(Level.OFF);
			levelName = Level.OFF.toString();
			break;
		default:
			Log.e(logTag, "Unable to match against level of int " + levelInt);
			return false;
		}

		Log.v(logTag, "Updated " + logger.getName() + " to level " + levelName);
		return true;

	}

}
