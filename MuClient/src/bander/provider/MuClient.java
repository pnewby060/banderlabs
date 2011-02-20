package bander.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/** <code>ContentProvider</code> implementation for the <code>bander.MuClient</code> authority. */
public class MuClient extends ContentProvider {
	/** The authority for MuClient. */
	public static final String AUTHORITY = "bander.MuClient";
	
	private static final String		DATABASE_NAME			= "muclient.db";
	private static final int		DATABASE_VERSION		= 2;

	private static final int		HOSTS					= 1;
	private static final int		HOST_ID					= 2;
	private static final int		SHORTCUTS				= 3;
	private static final int		SHORTCUT_ID				= 4;

	private static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "hosts", HOSTS);
		sUriMatcher.addURI(AUTHORITY, "hosts/#", HOST_ID);
		sUriMatcher.addURI(AUTHORITY, "shortcuts", SHORTCUTS);
		sUriMatcher.addURI(AUTHORITY, "shortcuts/#", SHORTCUT_ID);
	}
	
	/** Helper class to open, create, and upgrade the database file. */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + Host.DATABASE_TABLE + " ("
				+ Host._ID + " INTEGER PRIMARY KEY,"
				+ Host.WORLD_NAME + " TEXT, "
				+ Host.HOST_NAME + " TEXT, "
				+ Host.PORT + " INT, "
				+ Host.USE_SSL + " INT, "
				+ Host.POST_CONNECT + " TEXT"
				+ ");"
			);
			db.execSQL("CREATE TABLE " + Shortcut.DATABASE_TABLE + " ("
				+ Shortcut._ID + " INTEGER PRIMARY KEY,"
				+ Shortcut.SHORTCUT + " TEXT"
				+ ");"
			);
			db.execSQL("INSERT INTO " + Shortcut.DATABASE_TABLE  + " (" + Shortcut.SHORTCUT + ")" + " VALUES ('QUIT');");
			db.execSQL("INSERT INTO " + Shortcut.DATABASE_TABLE  + " (" + Shortcut.SHORTCUT + ")" + " VALUES (':');");
			db.execSQL("INSERT INTO " + Shortcut.DATABASE_TABLE  + " (" + Shortcut.SHORTCUT + ")" + " VALUES ('=');");
			db.execSQL("INSERT INTO " + Shortcut.DATABASE_TABLE  + " (" + Shortcut.SHORTCUT + ")" + " VALUES (':)');");
			db.execSQL("INSERT INTO " + Shortcut.DATABASE_TABLE  + " (" + Shortcut.SHORTCUT + ")" + " VALUES (':(');");
			db.execSQL("INSERT INTO " + Shortcut.DATABASE_TABLE  + " (" + Shortcut.SHORTCUT + ")" + " VALUES (';)');");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onCreate(db);
		}
	}

	private DatabaseHelper mDatabaseHelper;
		
	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		switch (sUriMatcher.match(uri)) {

			case HOSTS:
				qb.setTables(Host.DATABASE_TABLE);
				break;
			case HOST_ID:
				qb.setTables(Host.DATABASE_TABLE);
				qb.appendWhere(Host._ID + "=" + uri.getPathSegments().get(1));
				break;
			case SHORTCUTS:
				qb.setTables(Shortcut.DATABASE_TABLE);
				break;
			case SHORTCUT_ID:
				qb.setTables(Shortcut.DATABASE_TABLE);
				qb.appendWhere(Shortcut._ID + "=" + uri.getPathSegments().get(1));
				break;
			default:
				throw new IllegalArgumentException("Unsupported URI " + uri);
		}

		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case HOSTS:
				return Host.CONTENT_TYPE;
			case HOST_ID:
				return Host.CONTENT_ITEM_TYPE;
			case SHORTCUTS:
				return Shortcut.CONTENT_TYPE;
			case SHORTCUT_ID:
				return Shortcut.CONTENT_ITEM_TYPE;
			default:
				throw new IllegalArgumentException("Unsupported URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		String table;
		String nullColumnHack = null;

		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();		
		switch (sUriMatcher.match(uri)) {
			case HOSTS:
				table = Host.DATABASE_TABLE;
				nullColumnHack = Host.WORLD_NAME;
				break;
			case SHORTCUTS:
				table = Shortcut.DATABASE_TABLE;
				nullColumnHack = Shortcut.SHORTCUT;
				break;
			default:
				throw new IllegalArgumentException("Unsupported URI " + uri);
		}
						
		long rowId = db.insert(table, nullColumnHack, initialValues);
		if (rowId > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
			Uri insertUri = ContentUris.withAppendedId(uri, rowId);
			return insertUri;
		}
		
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count;
		
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		switch (sUriMatcher.match(uri)) {
			case HOST_ID:
				String hostId = uri.getPathSegments().get(1);
				count = db.delete(Host.DATABASE_TABLE, Host._ID + "=" + hostId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;
			case SHORTCUT_ID:
				String shortcutId = uri.getPathSegments().get(1);
				count = db.delete(Shortcut.DATABASE_TABLE, Shortcut._ID + "=" + shortcutId
					+ (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
				break;
			default:
				throw new IllegalArgumentException("Unsupported URI " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		String table;
		switch (sUriMatcher.match(uri)) {
			case HOST_ID:
				table = Host.DATABASE_TABLE;
				break;
			case SHORTCUT_ID:
				table = Shortcut.DATABASE_TABLE;
				break;
			default:
				throw new IllegalArgumentException("Unsupported URI " + uri);
		}
		
		String segment = uri.getPathSegments().get(1);
		
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int count = db.update(table, values, 
			"_id=" + segment + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), 
			selectionArgs
		);
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
}
