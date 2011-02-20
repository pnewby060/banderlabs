package bander.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class Shortcut implements BaseColumns, Parcelable {
	/** The content:// style URL for this data class. */
	public static final Uri CONTENT_URI = Uri.parse("content://" + MuClient.AUTHORITY + "/shortcuts");

	/** The MIME type of {@link #CONTENT_URI} providing a directory of shortcuts. */
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/bander.shortcut";

	/** The MIME type of a {@link #CONTENT_URI} sub-directory of a single shortcut. */
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/bander.shortcut";

	/** The database table this entity is stored in. */
	public static final String DATABASE_TABLE = "shortcuts";

	/** The default sort order. */
	public static final String DEFAULT_SORT_ORDER = "shortcut";

	/** The display name of the host. <p>Type: TEXT</p> */
	public static final String SHORTCUT = "shortcut";
	
	/** Projection to use for <code>Host.fromCursor()</code>. */
	public static final String[] PROJECTION = new String[] {
		Shortcut._ID,
		Shortcut.SHORTCUT
	};

	private long mId;
	private String mShortcut;

	public Uri getUri() 			{ return ContentUris.withAppendedId(CONTENT_URI, mId); }

	public String getShortcut()		{ return mShortcut; }

	/** Creates a new instance of the <code>Shortcut</code> class. */
	public Shortcut() {
		mId = -1;
		mShortcut = "";
	}

	/** Copy constructor */
	public Shortcut(Shortcut shortcut) {
		mId = shortcut.mId;
		mShortcut = shortcut.mShortcut;
	}

	private Shortcut(Parcel in) {
		mId = in.readLong();
		mShortcut = in.readString();
	}

	/** Returns a <code>ContentValues</code> object representing this host.
	 * @return <code>ContentValues</code> instance holding the values of the host.
	 */
	public ContentValues getContentValues() {
		final ContentValues values = new ContentValues();

		values.put(_ID, mId);
		values.put(SHORTCUT, mShortcut);

		return values;
	}

	/** Creates a new shortcut instance from a cursor returned by <code>bander.muclient.Provider</code>.
	 * @param c Cursor to a row representing a shortcut.
	 * @return new instance of a shortcut object.
	 */
	public static Shortcut fromCursor(Cursor c) {
		final Shortcut shortcut = new Shortcut();

		if (c.moveToFirst()) {
			shortcut.mId = c.getLong(c.getColumnIndexOrThrow(_ID));
			shortcut.mShortcut = c.getString(c.getColumnIndexOrThrow(SHORTCUT));
		}
		return shortcut;
	}

	// Parcelable
	public int describeContents() {
		return 0;
	}

	// Parcelable
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mId);
		dest.writeString(mShortcut);
	}

	public static final Creator<Shortcut> CREATOR = new Creator<Shortcut>() {
		public Shortcut createFromParcel(Parcel in) {
			return new Shortcut(in);
		}

		public Shortcut[] newArray(int size) {
			return new Shortcut[size];
		}
	};

}
