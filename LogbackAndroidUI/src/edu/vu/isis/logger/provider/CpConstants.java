package edu.vu.isis.logger.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public interface CpConstants {

	public static final String AUTHORITY_SUFFIX = "LauiContentProvider";

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

		// Columns
		public static final String NAME = "name";
		public static final String FILE_PATH_STRING = "file_path_string";

		public static final String[] COLUMN_NAMES = { NAME, FILE_PATH_STRING };

	}

	// Key contants for updating tables
	public static final String LEVEL_KEY = "level";
	public static final String APPENDER_KEY = "appender";

	public static final Uri OP_NOT_SUPPORTED = Uri
			.parse("That operation is not supported");
	
	public static final int NO_LEVEL = -152;

}
