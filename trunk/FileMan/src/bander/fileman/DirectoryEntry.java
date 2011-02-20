package bander.fileman;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;

import android.content.Context;
import android.text.format.DateFormat;

/** A directory entry. */
public class DirectoryEntry {
	private final	Context	mContext;
	private final	boolean	mIsDirectory;
	private final	String 	mName;
	private	final	long	mLength;
	private	final	long	mLastModified;
	private 		int	 	mIcon;
	private			String  mSize;
	private			String 	mTimestamp;

	/** Indicates if this directory entry represents a directory on the underlying file system. */
	public boolean isDirectory()	{ return mIsDirectory; }
	/** Returns the name of the file or directory represented by this directory entry. */
	public String getName()			{ return mName; }
	/** Returns the length of this file in bytes. The result for a directory is not defined. */
	public long length()			{ return mLength; }
	/** Returns the time when this file was last modified. */
	public long lastModified()		{ return mLastModified; }

	/** Return the resource ID for the icon representing this directory entry. */
	public int getIcon() {
		if (mIcon == 0) mIcon = MimeUtils.getIconResource(mName);
		return mIcon; 
	}
	/** Returns a string representation of the length of this directory entry. */
	public String size() {
		if (mSize == null) mSize = formatSize(mLength);
		return mSize; 
	}
	/** Returns a string representation of the time this directory entry was last modified. */
	public String timeStamp() {
		if (mTimestamp == null) mTimestamp = formatLastModified(mLastModified);
		return mTimestamp;
	}

	public DirectoryEntry(Context context, int icon, String name) {
		mContext = context;
		mIsDirectory = true;
		mName = name;
		mLength = 0;
		mLastModified = Long.MAX_VALUE;
		
		mIcon = icon;
		mSize = "";
		mTimestamp = "";
	}
	public DirectoryEntry(Context context, File file) {
		mContext = context;
		mIsDirectory = file.isDirectory();
		mName = file.getName();
		mLength = file.length();
		mLastModified = file.lastModified();
		
		if (mIsDirectory) {
			mIcon = R.drawable.folder;
			mSize = "";
		}
	}

	/** Format an integer representing a file size as a string.
	 * @param fileSize The file size to format.
	 * @return The formatted file size.
	 */
	private String formatSize(long fileSize) {
		final long GIGABYTE		= (1024*1024*1024);
		final long MEGABYTE		= (1024*1024);
		final long KILOBYTE		= (1024);
		
		double size = (double) fileSize;
		String suffix = "";
		if (fileSize > GIGABYTE) {
			size = size / GIGABYTE;
			suffix = "G";
		} else if (fileSize > MEGABYTE) {
			size = size / MEGABYTE;
			suffix = "M";
		} else if (fileSize > KILOBYTE) {
			size = size / KILOBYTE;
			suffix = "K";
		}
		NumberFormat formatter = DecimalFormat.getNumberInstance();
		if (size >= 100.0) {
			formatter.setMaximumFractionDigits(0);
		} else if (size >= 10.0) {
			formatter.setMaximumFractionDigits(1);
		} else {
			formatter.setMaximumFractionDigits(2);
		}
		return formatter.format(size) + suffix;
	}

	/** Format an integer representing a file date as a string.
	 * @param lastModified The file date to format.
	 * @return The formatted file date.
	 */
	private String formatLastModified(long lastModified) {
		try {
			if (lastModified > 0) {
				Date date = new Date(lastModified);
				java.text.DateFormat dateFormat = DateFormat.getDateFormat(mContext);
				java.text.DateFormat timeFormat = DateFormat.getTimeFormat(mContext);
				return 
					dateFormat.format(date) + " " + timeFormat.format(date);
			}
		} catch (Exception e) {
			// fail silently
		}
		return "";
	}
}
