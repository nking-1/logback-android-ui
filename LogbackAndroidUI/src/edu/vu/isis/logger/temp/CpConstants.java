package edu.vu.isis.logger.temp;

import android.net.Uri;
import android.provider.BaseColumns;

public class CpConstants {

	public static final String AUTHORITY = "edu.vu.isis.logger.LogbackContentProvider";

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
		public static final String EFFECTIVE_APPENDER_NAMES = "effective_appender_names";
		public static final String ATTACHED_APPENDER_NAMES = "attached_appender_names";

		public static final String[] COLUMN_NAMES = { NAME, LEVEL_INT,
				EFFECTIVE_APPENDER_NAMES, ATTACHED_APPENDER_NAMES };

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

}
