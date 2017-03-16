package com.playground.notification.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.playground.notification.app.App;

import java.io.File;

/**
 * Classical helper pattern on Android DB ops.
 *
 * @author Xinyue Zhao
 */
public final class DatabaseHelper extends SQLiteOpenHelper {
	/**
	 * DB name.
	 */
	public static final String DATABASE_NAME;

	static {
		File dbDir  = App.Instance.getExternalFilesDir( Environment.DIRECTORY_DCIM );
		File dbFile = new File( dbDir, "playgrounds.db" );
		DATABASE_NAME = dbFile.getAbsolutePath();
	}

	private static final int DATABASE_VERSION = 1;

	/**
	 * Constructor of {@link DatabaseHelper}.
	 *
	 * @param context
	 * 		{@link Context}.
	 */
	public DatabaseHelper( Context context ) {
		super( context, DATABASE_NAME, null, DATABASE_VERSION );
	}

	@Override
	public void onCreate( SQLiteDatabase db ) {

	}

	@Override
	public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {

	}
}
