package bander.fileman.activity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import bander.fileman.DirectoryEntry;
import bander.fileman.R;
import bander.fileman.util.FileUtils;
import bander.fileman.util.MimeUtils;

/** Main activity for FileMan, shows the contents of a single file-system directory. */
public class FileList extends ListActivity {
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

	private static final String 		CURRENT_DIRECTORY	= "currentDirectory";

	private enum Sorting				{ NONE, NAME, SIZE, DATE }
	private Sorting						mCurrentSorting		= Sorting.NAME;
	private boolean						mDetailedView		= false;
	private boolean						mHideDot			= false;

	private List<DirectoryEntry>		mDirectoryEntries	= new ArrayList<DirectoryEntry>(); 
	private File						mCurrentDirectory	= new File("/"); 

	private int							mClipboardAction	= 0;
	private File						mClipboardFile		= null;
	
	private boolean						mIsFilePicker		= false;
	
	private ListDirectoryTask 			mListDirectoryTask	= null;
	private UpdateEntriesTask 			mUpdateEntriesTask	= null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		final Intent intent = getIntent();
		final String action = intent.getAction();
		if (Intent.ACTION_GET_CONTENT.equals(action)) {
			mIsFilePicker = true;
			setContentView(R.layout.filepicker);
			Button cancelButton = (Button) findViewById(R.id.cancel);
			cancelButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					finish();
				}
			});
		} else {
			setContentView(R.layout.main);
		}
			
		if (savedInstanceState != null) {
			String currentDirectory = savedInstanceState.getString(CURRENT_DIRECTORY);
			mCurrentDirectory = new File(currentDirectory);
		} else {
			// Start up in the external storage directory, if available.
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				mCurrentDirectory = Environment.getExternalStorageDirectory();
			}
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
		
		//DirectoryEntry.setFormat(getBaseContext());

		listDirectory(mCurrentDirectory);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		int index = (int) id;

		String selectedFileName = (String) mDirectoryEntries.get(index).getName();
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
									// OnClickListener
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
									// OnClickListener
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
		String selectedFileName = (String) mDirectoryEntries.get(info.position).getName();
		
		if (selectedFileName == getString(R.string.dir_parent)) return;
		
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
		final String selectedFileName = (String) mDirectoryEntries.get(info.position).getName();
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
						// OnClickListener
						public void onClick(DialogInterface dialog, int which) {
							if (!deleteFile(clickedFile)) {
								Toast.makeText(getBaseContext(), 
									String.format(getString(R.string.failed_delete_explanation), selectedFileName), 
									Toast.LENGTH_SHORT
								).show();
							} else {
								listDirectory(mCurrentDirectory);
							}
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
				if (!filename_from.equals(filename_to)) {
					File file_from = getSelectedFile(filename_from);
					File file_to = getSelectedFile(filename_to);
					if (!file_from.renameTo(file_to)) {
						Toast.makeText(this, 
							String.format(getString(R.string.failed_rename_explanation), filename_from, filename_to), 
							Toast.LENGTH_LONG
						).show();
					}
				}
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (mCurrentDirectory.getParent() != null) {
				listDirectory(mCurrentDirectory.getParentFile());
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/** View an item from the list. Directories are listed, files are opened.
	 * @param file Item to view.
	 */
	private void viewItem(final File file) {
		if (file.isDirectory()) {
			listDirectory(file);
		} else {
			if (mIsFilePicker) {
				Intent intent = getIntent();
				intent.setData(Uri.fromFile(file));
				setResult(RESULT_OK, intent);
				finish();
			} else {
				openFile(file);
			}
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

	/** Deletes the specified file or directory, and in case of directories
	 * all contained files and sub-directories.  
	 * @param file File to delete
	 * @return True if the delete succeeded, false otherwise.
	 */
	private boolean deleteFile(final File file) {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File child : files) {
				if (!deleteFile(child)) {
					return false;
				}
			}
		}
		return file.delete();
	}
	
	/** Utility object to sort DirectoryEntry objects. */
	private class FileComparer implements Comparator<DirectoryEntry> {
		private final Sorting mSorting;
		
		public FileComparer(Sorting sorting) {
			mSorting = sorting;
		}
		
		public int compare(DirectoryEntry o1, DirectoryEntry o2) {
			if ((o1.isDirectory() == true) && (o2.isDirectory() == false)) return -1;
			if ((o1.isDirectory() == false) && (o2.isDirectory() == true)) return +1;
			
			if (mSorting == Sorting.SIZE) {
				if (o1.length() < o2.length()) return -1;
				if (o1.length() > o2.length()) return +1;
			}
			if (mSorting == Sorting.DATE) {
				if (o1.lastModified() > o2.lastModified()) return -1;
				if (o1.lastModified() < o2.lastModified()) return +1;
			}
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	}

	/** Adapter to bind DirectoryEntry objects. */
	private class EntryAdapter extends ArrayAdapter<DirectoryEntry> {
		private final LayoutInflater mInflater;
		private final int mResource;

		public EntryAdapter(Context context, List<DirectoryEntry> objects, int resource) {
			super(context, 0, objects);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mResource = resource;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null) {
				view = mInflater.inflate(mResource, null);
			}
			
			DirectoryEntry entry = this.getItem(position);
	
			view.setTag(entry.getName());
			entry.setView(view);
			
			((ImageView) view.findViewById(R.id.file_icon)).setImageDrawable(entry.getDrawable());
			((TextView) view.findViewById(R.id.file_name)).setText(entry.getName());
			((TextView) view.findViewById(R.id.file_size)).setText(entry.size());
			TextView modifiedView = (TextView) view.findViewById(R.id.file_lastmodified);
			if (modifiedView != null) modifiedView.setText(entry.timeStamp());
			
			return view;
		}
	}

	/** AsyncTask to update the directory entries. */
	private class UpdateEntriesTask extends AsyncTask<Void, DirectoryEntry, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			List<DirectoryEntry> directoryEntries = mDirectoryEntries;
			for (DirectoryEntry entry : directoryEntries) {
				entry.update();
				publishProgress(entry);
				if (isCancelled()) return null;
			}
			return null;
		}
		@Override
		protected void onProgressUpdate(DirectoryEntry... progress) {
			DirectoryEntry entry = progress[0];
			View view = entry.getView();
			if (view != null) {
				if (entry.getName().equals(view.getTag())) {
					EntryAdapter adapter = (EntryAdapter) getListAdapter();
					if (adapter != null) adapter.notifyDataSetChanged();
				}
			}
		}
		@Override
		protected void onPostExecute(Void param) {
			mUpdateEntriesTask = null;
		}
	}

	/** AsyncTask to list contents of a directory. */
	private class ListDirectoryTask extends AsyncTask<File, Void, List<DirectoryEntry>> {

		@Override
		protected void onPreExecute() {
			if (mUpdateEntriesTask != null) {
				mUpdateEntriesTask.cancel(true);
			}
			
			setTitle(mCurrentDirectory.getAbsolutePath());
			setProgressBarIndeterminateVisibility(true);
		}

		@Override
		protected List<DirectoryEntry> doInBackground(File... params) {
			File[] files = params;
			
			mDirectoryEntries.clear();
				
			if (mCurrentDirectory.getParent() != null) {
				DirectoryEntry parentDir = new DirectoryEntry(
					getBaseContext(), R.drawable.parent, getString(R.string.dir_parent)
				);
				mDirectoryEntries.add(parentDir);
			}
			
			for (File file : files) {
				if (mHideDot && file.isHidden()) continue;
				
				DirectoryEntry entry = new DirectoryEntry(getBaseContext(), file);
				mDirectoryEntries.add(entry);
				
				if (isCancelled()) return null;
			}

			if (mCurrentSorting != Sorting.NONE) {
				Collections.sort(mDirectoryEntries, new FileComparer(mCurrentSorting));
			}	
			return mDirectoryEntries;
		}
		
		@Override
		protected void onPostExecute(List<DirectoryEntry> entries) {
			EntryAdapter adapter = new EntryAdapter(
				getBaseContext(), entries,
				(mDetailedView) ? R.layout.list_item_2 : R.layout.list_item_1
			);
			setListAdapter(adapter);
			
			setProgressBarIndeterminateVisibility(false);
			mListDirectoryTask = null;
			
			// Chain the update task.
			mUpdateEntriesTask = new UpdateEntriesTask();
			mUpdateEntriesTask.execute();
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
		
		if (mListDirectoryTask != null) {
			mListDirectoryTask.cancel(true);
		}
		mCurrentDirectory = directory;
		setListAdapter(null);
		mListDirectoryTask = new ListDirectoryTask();
		mListDirectoryTask.execute(files);
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
