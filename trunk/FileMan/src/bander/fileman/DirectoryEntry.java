package bander.fileman;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.View;

/** A directory entry. */
public class DirectoryEntry {
	//private static java.text.DateFormat sDateFormat;
	//private static java.text.DateFormat sTimeFormat;
	
	private final	Context		mContext;
	private final	boolean		mIsDirectory;
	private final	String 		mName;
	private final	String 		mAbsoluteName;
	private	final	long		mLength;
	private	final	long		mLastModified;
	private 		int	 		mIcon;
	private			String  	mSize;
	private			String 		mTimestamp;
	
	private			Drawable	mDrawable;

	///** Fetches the system date format settings for later use. */
	//public static void setFormat(Context context) {
	//	sDateFormat = DateFormat.getDateFormat(context);
	//	sTimeFormat = DateFormat.getTimeFormat(context);
	//}
	
	private			WeakReference<View> mViewReference;
	public void setView(View view) {
		mViewReference = new WeakReference<View>(view);
	}
	public View getView() {
		return (mViewReference != null) ? mViewReference.get() : null;
	}
	
	/** Indicates if this directory entry represents a directory on the underlying file system. */
	public boolean isDirectory()	{ return mIsDirectory; }
	/** Returns the name of the file or directory represented by this directory entry. */
	public String getName()			{ return mName; }
	/** Returns the length of this file in bytes. The result for a directory is not defined. */
	public long length()			{ return mLength; }
	/** Returns the time when this file was last modified. */
	public long lastModified()		{ return mLastModified; }

	/** Return the resource ID for the icon representing this directory entry. */
	public int getIcon() 			{ return mIcon; }
	/** Return a Drawable for the icon representing this directory entry. */
	public Drawable getDrawable()	{ return mDrawable; }
	/** Returns a string representation of the length of this directory entry. */
	public String size() 			{ return mSize; }
	/** Returns a string representation of the time this directory entry was last modified. */
	public String timeStamp() 		{ return mTimestamp; }

	public DirectoryEntry(Context context, int icon, String name) {
		mContext = context;
		mIsDirectory = true;
		mName = name;
		mAbsoluteName = name;
		mLength = 0;
		mLastModified = Long.MAX_VALUE;
		
		mIcon = icon;
		mDrawable = context.getResources().getDrawable(icon);
		mSize = "";
		mTimestamp = "";
	}
	public DirectoryEntry(Context context, File file) {
		mContext = context;
		mIsDirectory = file.isDirectory();
		mName = file.getName();
		mAbsoluteName = file.getAbsolutePath();
		mLength = file.length();
		mLastModified = file.lastModified();
		
		if (mIsDirectory) {
			mIcon = R.drawable.folder;
			mDrawable = context.getResources().getDrawable(mIcon);
			mSize = "";
		} else {
			// Temporary icon
			mDrawable = context.getResources().getDrawable(R.drawable.document);
		}
	}
	
	/** Updates the entry. */
	public void update() {
		if (mIcon == 0) {
			mIcon = MimeUtils.getIconResource(mName);
			if (mIcon == R.drawable.apk) {
				// If it's an Android package, attempt to load the application icon out of it.
				PackageManager packageManager = mContext.getPackageManager();
				PackageInfo packageInfo = packageManager.getPackageArchiveInfo(mAbsoluteName, 0);
				
				// On Android 2.2 and up these aren't set but they are needed for getApplicationIcon().
				packageInfo.applicationInfo.sourceDir = mAbsoluteName;
				packageInfo.applicationInfo.publicSourceDir = mAbsoluteName;
				
				mDrawable = packageManager.getApplicationIcon(packageInfo.applicationInfo);
			} else {
				mDrawable = mContext.getResources().getDrawable(mIcon);
			}
		}
		if (mSize == null) mSize = formatSize(mLength);
		if (mTimestamp == null) mTimestamp = formatLastModified(mLastModified);
	}

	private final long GIGABYTE		= (1024*1024*1024);
	private final long MEGABYTE		= (1024*1024);
	private final long KILOBYTE		= (1024);
	
	/** Format an integer representing a file size as a string.
	 * @param fileSize The file size to format.
	 * @return The formatted file size.
	 */
	private String formatSize(long fileSize) {
		String suffix = "";
		long integer = 0;
		long fraction = 0;
		
		// 8-bit binary fixed point fractions.
		// It's painfully low-level bit-twiddling, but one heck of a 
		// lot faster than a NumberFormat instance.
		
		// http://www.eetimes.com/discussion/other/4024639/Fixed-point-math-in-C
		// http://www.diycalculator.com/sp-round.shtml#A17
		
		if (fileSize > GIGABYTE) {
			integer = fileSize >> 30;
			fraction = fileSize >> 22;
			suffix = "G";
		} else if (fileSize > MEGABYTE) {
			integer = fileSize >> 20;
			fraction = fileSize >> 12;
			suffix = "M";
		} else if (fileSize > KILOBYTE) {
			integer = fileSize >> 10;
			fraction = fileSize >> 2; 
			suffix = "K";
		} else {
			return "" + fileSize;
		}
		if (integer >= 100) {
			fraction = ((((fraction & 0x0FF) * 10) + 128) >> 8);
			if (fraction > 4) integer++;
			return integer + suffix;
		} else if (integer >= 10) {
			fraction = ((((fraction & 0x0FF) * 10) + 128) >> 8);
			if (fraction == 10) { integer++; fraction = 0; }
			return integer + "." + fraction + suffix;
		}
		fraction = ((((fraction & 0x0FF) * 100) + 128) >> 8);
		if (fraction == 100) { integer++; fraction = 0; }
		return integer + ((fraction < 10) ? ".0" : ".") + fraction + suffix;
	}

	private Date mDate = new Date();
	
	/** Format an integer representing a file date as a string.
	 * @param lastModified The file date to format.
	 * @return The formatted file date.
	 */
	private String formatLastModified(long lastModified) {
		if (lastModified > 0) {
			mDate.setTime(lastModified);
			
			// The proper way, but too slow.
			//return sDateFormat.format(mDate) + " " + sTimeFormat.format(mDate);
			
			int month = (1+mDate.getMonth());
			int date = mDate.getDate();
			int hour = mDate.getHours();
			int minute = mDate.getMinutes();
			return 
				(1900 + mDate.getYear()) + 
				(month < 10 ? "/0" : "/") + (1+mDate.getMonth()) + 
				(date < 10 ? "/0" : "/") +  + mDate.getDate() +
				(hour < 10 ? " 0" : " ") + mDate.getHours() + 
				(minute < 10 ? ":0" : ":") +  + mDate.getMinutes();
		}
		return "";
	}

}
