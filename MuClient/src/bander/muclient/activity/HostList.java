package bander.muclient.activity;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import bander.muclient.R;
import bander.provider.Host;

/** Main activity for MuckClient, shows a list of hosts. */
public class HostList extends ListActivity {
	public static final int MENU_INSERT 		= Menu.FIRST + 1;
	public static final int MENU_PREFS 			= Menu.FIRST + 2;

	public static final int CONTEXT_EDIT 		= Menu.FIRST + 3;
	public static final int CONTEXT_COPY		= Menu.FIRST + 4;
	public static final int CONTEXT_DELETE 		= Menu.FIRST + 5;


	private static final String[] PROJECTION	 = new String[] { 
		Host._ID, Host.WORLD_NAME, Host.HOST_NAME  
	};

	//private static final int COLUMN_ID	 		= 0;
	private static final int COLUMN_WORLDNAME 	= 1;
	
	private static boolean GIVEN_HINT			= false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.host_list);

		Intent intent = getIntent();
		if (intent.getData() == null) {
			intent.setData(Host.CONTENT_URI);
		}

		registerForContextMenu(getListView());
	}

	@Override
	public void onResume() {
		super.onResume();

		Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null, Host.DEFAULT_SORT_ORDER);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
			android.R.layout.simple_list_item_2,
			cursor, 
			new String[] { Host.WORLD_NAME, Host.HOST_NAME }, 
			new int[] { android.R.id.text1, android.R.id.text2 }
		);
		setListAdapter(adapter);
		
		if ((GIVEN_HINT == false) && (adapter.getCount() == 1)) {
			Toast.makeText(this, R.string.hint_longpress, Toast.LENGTH_LONG).show();
			GIVEN_HINT = true;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_INSERT, 0, R.string.menu_addworld)
			.setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_PREFS, 0, R.string.menu_prefs)
			.setIcon(android.R.drawable.ic_menu_preferences);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_INSERT:
				startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
				return true;
			case MENU_PREFS:
				Intent prefsActivity = new Intent(this, Preferences.class);
				startActivity(prefsActivity);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			return;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			return;
		}

		menu.setHeaderTitle(cursor.getString(COLUMN_WORLDNAME));

		menu.add(0, CONTEXT_EDIT, 0, R.string.context_editworld);
		//menu.add(0, CONTEXT_COPY, 0, R.string.context_copyworld);
		menu.add(0, CONTEXT_DELETE, 0, R.string.context_deleteworld);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			return false;
		}
		
		Uri uri = ContentUris.withAppendedId(getIntent().getData(), info.id);
		switch (item.getItemId()) {
			case CONTEXT_EDIT:
				startActivity(new Intent(Intent.ACTION_EDIT, uri));
				return true;
			case CONTEXT_COPY:
				//
				return true;
			case CONTEXT_DELETE:
				deleteHost(this, uri);
				return true;
		}
		return false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Uri uri = ContentUris.withAppendedId(Host.CONTENT_URI, id);
		startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}
	
	/** Delete a host, confirm when preferred.
	 * @param context Context to use.
	 * @param uri Uri of the host to delete.
	 */
	private void deleteHost(final ContextWrapper context, final Uri uri) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean deleteConfirmation = preferences.getBoolean("deleteConfirmation", true);
		if (deleteConfirmation) {
			AlertDialog alertDialog = new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.dialog_deleteworld)
				.setMessage(R.string.deleteworld_confirmation)
				.setPositiveButton(R.string.dialog_confirm,
					new DialogInterface.OnClickListener() {
						// OnClickListener
						public void onClick(DialogInterface dialog, int which) {
							context.getContentResolver().delete(uri, null, null);
						}
					}
				)
				.setNegativeButton(R.string.dialog_cancel, null)
				.create();
			alertDialog.show();
		} else {
			context.getContentResolver().delete(uri, null, null);
		}
	}

}

