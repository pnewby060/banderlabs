package bander.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

/** Data class representing a single note. */
public class Note implements BaseColumns, Parcelable {
	/** The authority for this data class. */
	public static final String AUTHORITY = "bander.Notepad";
	
	/** The content:// style URL for this data class. */
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/notes");
    
	/** The MIME type of {@link #CONTENT_URI} providing a directory of notes. */
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.bander.note";
	
	/** The MIME type of a {@link #CONTENT_URI} sub-directory of a single note. */
	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.bander.note";
	
	/** The default sort order. */
	public static final String DEFAULT_SORT_ORDER = "_id DESC";
	
	/** The title of the note. <P>Type: TEXT</P> */
	public static final String TITLE = "title";
	
	/** The body of the note. <P>Type: TEXT</P> */
	public static final String BODY = "body";
    
	/** The creation date of the note. <P>Type: INTEGER</P> */
	public static final String CREATED = "created";
	
    private int			mId;
    private String		mTitle;
    private String		mBody;
	
	public int 		getId()			{ return mId; }
	public String	getTitle()		{ return mTitle; }
	public String	getBody()		{ return mBody; }
    
	/** Creates a new instance of the <code>Note</code> class. */
	public Note() {
		mId = -1;
	}
    
	/** Copy constructor */
	public Note(Note note) {
		mId = note.mId;
		mTitle = note.mTitle;
		mBody = note.mBody;
	}
	
	private Note(Parcel in) {
		mId = in.readInt();
		mTitle = in.readString();
		mBody = in.readString();
	}

	/** Returns a <code>ContentValues</code> object representing this note.
	 * @return <code>ContentValues</code> instance holding the values of the note.
	 */
	public ContentValues getContentValues() {
		final ContentValues values = new ContentValues();
		
		values.put(_ID, mId);
		values.put(TITLE, mTitle);
		values.put(BODY, mBody);
		
		return values;
	}

	/** Creates a new note instance from a cursor returned by <code>bander.provider.NoteProvider</code>.
	 * @param c Cursor to a row representing a note.
	 * @return new instance of a note object.
	 */
	public static Note fromCursor(Cursor c) {
		final Note note = new Note();
		
		if (c.getCount() > 0) {
			if (c.moveToFirst()) {
				note.mId = c.getInt(c.getColumnIndexOrThrow(_ID));
				note.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));
				note.mBody = c.getString(c.getColumnIndexOrThrow(BODY));
			}
		}
		
		return note;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mId);
		dest.writeString(mTitle);
		dest.writeString(mBody);		
	}
	
	public static final Creator<Note> CREATOR = new Creator<Note>() {
		public Note createFromParcel(Parcel in) {
			return new Note(in);
		}

		public Note[] newArray(int size) {
			return new Note[size];
		}
	};
	
}
