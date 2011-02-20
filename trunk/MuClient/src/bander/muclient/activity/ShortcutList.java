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
import bander.muclient.R;
import bander.provider.Shortcut;

public class ShortcutList extends ListActivity {
	public static final int MENU_INSERT 		= Menu.FIRST + 1;
	
	public static final int CONTEXT_EDIT 		= Menu.FIRST + 3;
	public static final int CONTEXT_DELETE 		= Menu.FIRST + 4;

	private static final int COLUMN_SHORTCUT 	= 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shortcut_list);

		Intent intent = getIntent();
		if (intent.getData() == null) {
			intent.setData(Shortcut.CONTENT_URI);
		}

		registerForContextMenu(getListView());
	}

	@Override
	public void onResume() {
		super.onResume();

		Cursor cursor = managedQuery(
			getIntent().getData(), Shortcut.PROJECTION, null, null, Shortcut.DEFAULT_SORT_ORDER
		);
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
			android.R.layout.simple_list_item_1,
			cursor, 
			new String[] { Shortcut.SHORTCUT }, 
			new int[] { android.R.id.text1 }
		);
		setListAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_INSERT, 0, R.string.menu_addshortcut)
			.setIcon(android.R.drawable.ic_menu_add);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_INSERT:
				startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
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

		menu.setHeaderTitle(cursor.getString(COLUMN_SHORTCUT));

		menu.add(0, CONTEXT_EDIT, 0, R.string.context_editshortcut);
		menu.add(0, CONTEXT_DELETE, 0, R.string.context_deleteshortcut);
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
			case CONTEXT_DELETE:
				deleteShortcut(this, uri);
				return true;
		}
		return false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Uri uri = ContentUris.withAppendedId(Shortcut.CONTENT_URI, id);
		startActivity(new Intent(Intent.ACTION_EDIT, uri));
	}
	
	/** Delete a shortcut, confirm when preferred.
	 * @param context Context to use.
	 * @param uri Uri of the shortcut to delete.
	 */
	private void deleteShortcut(final ContextWrapper context, final Uri uri) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		boolean deleteConfirmation = preferences.getBoolean("deleteConfirmation", true);
		if (deleteConfirmation) {
			AlertDialog alertDialog = new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.dialog_deleteshortcut)
				.setMessage(R.string.deleteshortcut_confirmation)
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

