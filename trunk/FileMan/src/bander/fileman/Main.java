package bander.fileman;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/** Main activity for FileMan, shows the contents of a single file-system directory. */
public class Main extends ListActivity {
	private static final int			ACTIVITY_PROPERTIES	= 0;	
	
	private static final int			REFRESH_ID 			= Menu.FIRST + 0;
	private static final int			ROOT_ID 			= Menu.FIRST + 1;
	private static final int			PASTE_ID 			= Menu.FIRST + 2;
	private static final int			PREFS_ID 			= Menu.FIRST + 3;
    
	private static final int			VIEW_ID 			= Menu.FIRST + 4;
	private static final int			SEND_ID 			= Menu.FIRST + 5;
	private static final int			MOVE_ID				= Menu.FIRST + 6;
	private static final int			COPY_ID				= Menu.FIRST + 7;
	private static final int			DELETE_ID			= Menu.FIRST + 8;
	private static final int			PROPERTIES_ID		= Menu.FIRST + 9;
	
	private static final String 		ICON				= "icon";
	private static final String 		NAME				= "name";
	private static final String 		SIZE				= "size";
	private static final String 		LENGTH				= "length";
	private static final String			ISDIRECTORY			= "isDirectory";
	private static final String 		LASTMODIFIED		= "lastModified";
	
	private static final String 		CURRENT_DIRECTORY	= "currentDirectory";
    
	private enum Sorting				{ NONE, NAME, SIZE }
	private Sorting						mCurrentSorting		= Sorting.NAME;
	private Boolean						mDetailedView		= false;
	private Boolean						mHideDot			= false;
	
	private List<Map<String,Object>>	mDirectoryEntries	= new ArrayList<Map<String,Object>>(); 
	private File						mCurrentDirectory	= new File("/"); 
    
	private int							mClipboardAction	= 0;
	private File						mClipboardFile		= null;
    	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		if (savedInstanceState != null) {
			String currentDirectory = savedInstanceState.getString(CURRENT_DIRECTORY);
			mCurrentDirectory = new File(currentDirectory);
		}

		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(CURRENT_DIRECTORY, mCurrentDirectory.getAbsolutePath());
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);		
	    
		mDetailedView = preferences.getBoolean("detailedView", false);
		int sortOrder = Integer.valueOf(preferences.getString("sortOrder", "1"));
		mCurrentSorting = Sorting.values()[sortOrder];
		mHideDot = preferences.getBoolean("hideDot", false);
		
		listDirectory(mCurrentDirectory);
    }
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		int index = (int) id;

		String selectedFileName = (String) mDirectoryEntries.get(index).get(NAME);
		if (selectedFileName.equals(getString(R.string.dir_parent))) {
			if (mCurrentDirectory.getParent() != null) {
				listDirectory(mCurrentDirectory.getParentFile());
			}
		} else {
			File clickedFile = getSelectedFile(selectedFileName);
			if (clickedFile != null) {
				viewItem(clickedFile);
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		//MenuItem refreshItem = menu.add(0, REFRESH_ID, 0, R.string.menu_refresh);
		//refreshItem.setIcon(R.drawable.ic_menu_refresh);

		MenuItem rootItem = menu.add(0, ROOT_ID, 0, R.string.menu_root);
		rootItem.setIcon(android.R.drawable.ic_menu_revert);

		MenuItem pasteItem = menu.add(0, PASTE_ID, 0, R.string.menu_paste);
		pasteItem.setIcon(R.drawable.ic_menu_paste);
		
		MenuItem sortItem = menu.add(0, PREFS_ID, 0, R.string.menu_prefs);
		sortItem.setIcon(android.R.drawable.ic_menu_preferences);

		return result;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);

		menu.getItem(1).setEnabled(mClipboardFile != null);

		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case REFRESH_ID:
				listDirectory(mCurrentDirectory);
				return true;
			case ROOT_ID:
				listDirectory(new File("/"));
				return true;
			case PASTE_ID:
				if (mClipboardFile != null) {
					switch (mClipboardAction) {
						case MOVE_ID:
							showAlertDialog(getString(R.string.dialog_move), 
								String.format(getString(R.string.move_confirmation), mClipboardFile.getName(), mCurrentDirectory),
								new DialogInterface.OnClickListener() {					
									@Override
									public void onClick(DialogInterface dialog, int which) {
										mClipboardFile.renameTo(getSelectedFile(mClipboardFile.getName()));
										mClipboardAction = 0;
										mClipboardFile = null;
										listDirectory(mCurrentDirectory);
									}
								}
							);
							break;
						case COPY_ID:
							showAlertDialog(getString(R.string.dialog_copy), 
								String.format(getString(R.string.copy_confirmation), mClipboardFile.getName(), mCurrentDirectory),
								new DialogInterface.OnClickListener() {					
									@Override
									public void onClick(DialogInterface dialog, int which) {
										try {
											FileUtils.copyFile(mClipboardFile, getSelectedFile(mClipboardFile.getName()));
										} catch (IOException e){
											Toast.makeText(getBaseContext(), 
												String.format(getString(R.string.failed_copy_explanation), mClipboardFile.getName()), 
												Toast.LENGTH_SHORT
											).show();
										} finally {
											mClipboardAction = 0;
											mClipboardFile = null;
											listDirectory(mCurrentDirectory);
										}
									}
								}
							);
							break;
					}
				}
				return true;
			case PREFS_ID:
				Intent prefsActivity = new Intent(this, Preferences.class);
				startActivity(prefsActivity);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		String selectedFileName = (String) mDirectoryEntries.get(info.position).get(NAME);
		menu.setHeaderTitle(selectedFileName);

		//menu.add(0, VIEW_ID, 0, R.string.context_view);
		menu.add(0, SEND_ID, 0, R.string.context_send);
		menu.add(0, MOVE_ID, 0, R.string.context_move);
		menu.add(0, COPY_ID, 0, R.string.context_copy);
		menu.add(0, DELETE_ID, 0, R.string.context_delete);
		menu.add(0, PROPERTIES_ID, 0, R.string.context_properties);	
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		String selectedFileName = (String) mDirectoryEntries.get(info.position).get(NAME);
		final File clickedFile = getSelectedFile(selectedFileName);
		switch (item.getItemId()) {
			case VIEW_ID:
				viewItem(clickedFile);
				return true;
			case SEND_ID:
				Intent sendIntent = new Intent(Intent.ACTION_SEND);
				sendIntent.setDataAndType(Uri.fromFile(clickedFile), MimeUtils.getType(selectedFileName));
				startActivity(Intent.createChooser(sendIntent, getString(R.string.context_send)));
				return true;
			case MOVE_ID:
			case COPY_ID:
				mClipboardAction = item.getItemId();
				mClipboardFile = clickedFile;
				Toast.makeText(this, R.string.file_explanation, Toast.LENGTH_LONG).show();
				return true;
			case DELETE_ID:
				showAlertDialog(getString(R.string.dialog_delete), 
					String.format(getString(R.string.delete_confirmation), clickedFile.getName()), 
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							clickedFile.delete();
							listDirectory(mCurrentDirectory);
						}
					}
				);
				return true;
			case PROPERTIES_ID:
				Intent propertiesIntent = new Intent(this, Properties.class);
				propertiesIntent.putExtra(Properties.FILENAME_FROM, clickedFile.getAbsolutePath());
				startActivityForResult(propertiesIntent, ACTIVITY_PROPERTIES);
				return true;
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (requestCode == ACTIVITY_PROPERTIES) {
			if (resultCode == RESULT_OK) {
				String filename_from = intent.getStringExtra(Properties.FILENAME_FROM);
				String filename_to = intent.getStringExtra(Properties.FILENAME_TO);
				File file_from = getSelectedFile(filename_from);
				File file_to = getSelectedFile(filename_to);
				file_from.renameTo(file_to);
			}
		}
		listDirectory(mCurrentDirectory);
	}
	
	/** View an item from the list. Directories are listed, files are opened.
	 * @param file Item to view.
	 */
	private void viewItem(final File file) {
		if (file.isDirectory()) {
			listDirectory(file);
		} else {
			openFile(file);
		}
	}
	
	/** Opens a specified file for viewing. Files are opened with the default
	 * viewer for their specific MIME type. If no MIME type could be detected, a
	 * wildcard MIME type is used and all registered applications are shown to
	 * the end user.
	 * @param file File to open.
	 */
	private void openFile(final File file) {
		String filename = file.getName();
		try {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(file), MimeUtils.getType(filename));
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(getBaseContext(), 
				String.format(getString(R.string.failed_open_explanation), filename), 
				Toast.LENGTH_SHORT
			).show();
		}
	}
	
	/** Utility object to sort file list item objects. */
	private class FileComparer implements Comparator<Map<String, Object>> {
		private Sorting mSorting;
		
		public FileComparer(Sorting sorting) {
			mSorting = sorting;
		}
		
		public int compare(Map<String, Object> o1, Map<String, Object> o2) {
			boolean d1 = (Boolean) o1.get(ISDIRECTORY);
			boolean d2 = (Boolean) o2.get(ISDIRECTORY);
			if ((d1 == true) && (d2 == false)) return -1;
			if ((d1 == false) && (d2 == true)) return +1;
			
			if (mSorting == Sorting.SIZE) {
				long s1 = (Long) o1.get(LENGTH);
				long s2 = (Long) o2.get(LENGTH);
				if (s1 < s2) return -1;
				if (s1 > s2) return +1;
			}
			String l1 = (String) o1.get(NAME);
			String l2 = (String) o2.get(NAME);
			return l1.compareToIgnoreCase(l2);			
		}		
	}
	
	/** Shows the specified directory in the list. The directory specified also
	 * becomes the new current directory.
	 * @param directory Directory to show.
	 */
	private void listDirectory(final File directory) {
		File[] files = null;
		try {
			files = directory.listFiles();
		} catch (SecurityException e) {
			files = null;
		}	
		if (files == null) {
			Toast.makeText(this, 
				String.format(getString(R.string.failed_open_explanation), directory.getName()), 
				Toast.LENGTH_SHORT
			).show();
			return;
		}
		
		mCurrentDirectory = directory;
		setTitle(mCurrentDirectory.getAbsolutePath());
		
		mDirectoryEntries.clear();
		
		if (mCurrentDirectory.getParent() != null) {
			Map<String, Object> parentDir = new HashMap<String, Object>();
			parentDir.put(ISDIRECTORY, (Boolean) true);
			parentDir.put(ICON, R.drawable.parent);
			parentDir.put(NAME, getString(R.string.dir_parent));
			parentDir.put(LENGTH, (Long) 0l);
			mDirectoryEntries.add(parentDir);
		}
		
		for (File file : files) {
			if (mHideDot && file.isHidden()) continue;
			
			Map<String, Object> entry = new HashMap<String, Object>();
			entry.put(NAME, file.getName());
			if (file.isDirectory()) {
				entry.put(ISDIRECTORY, (Boolean) true);
				entry.put(ICON, R.drawable.folder);
				entry.put(LENGTH, (Long) 0l);
			} else {
				entry.put(ISDIRECTORY, (Boolean) false);
				entry.put(ICON, MimeUtils.getIconResource(file.getName()));
				entry.put(SIZE, formatSize(file.length()));
				entry.put(LENGTH, (Long) file.length());
			}
			try {
				if (file.lastModified() > 0) {
					Date lastModified = new Date(file.lastModified());
					java.text.DateFormat dateFormat = DateFormat.getDateFormat(getBaseContext());
					java.text.DateFormat timeFormat = DateFormat.getTimeFormat(getBaseContext());
					entry.put(LASTMODIFIED, 
						dateFormat.format(lastModified) + " " + timeFormat.format(lastModified)
					);
				}
			} catch (Exception e) {
				// fail silently
			}
			mDirectoryEntries.add(entry);
		}

		if (mCurrentSorting != Sorting.NONE) {
			Collections.sort(mDirectoryEntries, new FileComparer(mCurrentSorting));
		}
		
		SimpleAdapter adapter = new SimpleAdapter(
			this, mDirectoryEntries,
			(mDetailedView) ? R.layout.list_item_2 : R.layout.list_item_1,
			new String[] { ICON, NAME, SIZE, LASTMODIFIED }, 
			new int[] { R.id.file_icon, R.id.file_name, R.id.file_size, R.id.file_perms }
		);
		setListAdapter(adapter);
	}
	
	/** Returns a File object corresponding to a file in the current directory.
	 * @param selectedFileName Name of the file selected.
	 * @return The selected File.
	 */
	private File getSelectedFile(String selectedFileName) {
		String currentPath = mCurrentDirectory.getAbsolutePath();
		return new File(
			((currentPath.length() == 1) ? "/" : mCurrentDirectory .getAbsolutePath() + "/")
			+ selectedFileName
		);
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
	
	/** Displays an AlertDialog.
	 * @param title The title of the AlertDialog.
	 * @param message Message shown in the AlertDialog.
	 * @param positiveListener Listener that is run when the AlertDialog's PositiveButton is clicked.
	 */
	private void showAlertDialog(String title, String message, DialogInterface.OnClickListener positiveListener) {
		new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle(title).setMessage(message)
			.setPositiveButton(getString(R.string.dialog_yes), positiveListener)
			.setNegativeButton(getString(R.string.dialog_no), null)
			.show();
	}
}
