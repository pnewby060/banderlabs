package bander.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

/** Data class representing a single host. */
public class Host implements BaseColumns, Parcelable {

	/** The content:// style URL for this data class. */
	public static final Uri CONTENT_URI = Uri.parse("content://" + MuClient.AUTHORITY + "/hosts");

	/** The MIME type of {@link #CONTENT_URI} providing a directory of hosts. */
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/bander.host";

	/** The MIME type of a {@link #CONTENT_URI} sub-directory of a single host. */
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/bander.host";

	/** The database table this entity is stored in. */
	public static final String DATABASE_TABLE = "hosts";

	/** The default sort order. */
	public static final String DEFAULT_SORT_ORDER = "name_world";

	/** The display name of the host. <p>Type: TEXT</p> */
	public static final String WORLD_NAME = "name_world";
	/** The address of the host. <p>Type: TEXT</p> */
	public static final String HOST_NAME = "name_host";
	/** The port number of the host. <p>Type: INT</p> */
	public static final String PORT = "port";
	/** Whether the connection should use SSL. <p>Type: INT</p> */
	public static final String USE_SSL = "use_ssl";
	/** The post-connect command for this host. <p>Type: TEXT</p> */
	public static final String POST_CONNECT = "post_connect";
	
	/** Projection to use for <code>Host.fromCursor()</code>. */
	public static final String[] PROJECTION = new String[] {
		Host._ID,
		Host.WORLD_NAME,
		Host.HOST_NAME,
		Host.PORT,
		Host.USE_SSL,
		Host.POST_CONNECT
	};

	private long mId;
	private String mWorldName;
	private String mHostName;
	private int mPort;
	private boolean mUseSsl;
	private String mPostConnect;

	public Uri getUri() 			{ return ContentUris.withAppendedId(CONTENT_URI, mId); }

	public String getWorldName()	{ return mWorldName; }
	public String getHostName()		{ return mHostName; }
	public int getPort() 			{ return mPort; }
	public boolean getUseSsl() 		{ return mUseSsl; }
	public String getPostConnect()	{ return mPostConnect; }

	/** Creates a new instance of the <code>Host</code> class. */
	public Host() {
		mId = -1;
		mWorldName = "";
		mHostName = "";
		mPort = 0;
		mUseSsl = false;
		mPostConnect = "";
	}

	/** Copy constructor */
	public Host(Host host) {
		mId = host.mId;
		mWorldName = host.mWorldName;
		mHostName = host.mHostName;
		mPort = host.mPort;
		mUseSsl = host.mUseSsl;
		mPostConnect = host.mPostConnect;
	}

	private Host(Parcel in) {
		mId = in.readLong();
		mWorldName = in.readString();
		mHostName = in.readString();
		mPort = in.readInt();
		mUseSsl = (in.readInt() == 1);
		mPostConnect = in.readString();
	}

	/** Returns a <code>ContentValues</code> object representing this host.
	 * @return <code>ContentValues</code> instance holding the values of the host.
	 */
	public ContentValues getContentValues() {
		final ContentValues values = new ContentValues();

		values.put(_ID, mId);
		values.put(WORLD_NAME, mWorldName);
		values.put(HOST_NAME, mHostName);
		values.put(PORT, mPort);
		values.put(USE_SSL, mUseSsl ? 1 : 0);
		values.put(POST_CONNECT, mPostConnect);

		return values;
	}

	/** Creates a new host instance from a cursor returned by <code>bander.muclient.Provider</code>.
	 * @param c Cursor to a row representing a host.
	 * @return new instance of a host object.
	 */
	public static Host fromCursor(Cursor c) {
		final Host host = new Host();

		if (c.moveToFirst()) {
			host.mId = c.getLong(c.getColumnIndexOrThrow(_ID));
			host.mWorldName = c.getString(c.getColumnIndexOrThrow(WORLD_NAME));
			host.mHostName = c.getString(c.getColumnIndexOrThrow(HOST_NAME));
			host.mPort = c.getInt(c.getColumnIndexOrThrow(PORT));
			host.mUseSsl = (c.getInt(c.getColumnIndexOrThrow(USE_SSL)) == 1);
			host.mPostConnect = c.getString(c.getColumnIndexOrThrow(POST_CONNECT));
		}
		return host;
	}

	// Parcelable
	public int describeContents() {
		return 0;
	}

	// Parcelable
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mId);
		dest.writeString(mWorldName);
		dest.writeString(mHostName);
		dest.writeInt(mPort);
		dest.writeInt(mUseSsl ? 1 : 0);
		dest.writeString(mPostConnect);
	}

	public static final Creator<Host> CREATOR = new Creator<Host>() {
		public Host createFromParcel(Parcel in) {
			return new Host(in);
		}

		public Host[] newArray(int size) {
			return new Host[size];
		}
	};
}
