package bander.muclient.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import bander.muclient.R;
import bander.provider.Shortcut;

public class ShortcutEdit extends Activity implements OnClickListener {
	private static final int	MENU_REVERT			= Menu.FIRST + 0;
	private static final int	MENU_DELETE			= Menu.FIRST + 1;
		
	private static final int	STATE_EDIT 			= 0;
	private static final int	STATE_INSERT 		= 1;
	
	private static final String ORIGINAL_SHORTCUT	= "originalShortcut";
	
	private int			mState;
	private Uri			mUri;
	private Shortcut	mOriginalShortcut;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null) {
			final Object shortcut = savedInstanceState.get(ORIGINAL_SHORTCUT);
			if (shortcut != null) mOriginalShortcut = (Shortcut) shortcut;
		}
				
		final Intent intent = getIntent();
		final String action = intent.getAction();
		if (Intent.ACTION_EDIT.equals(action)) {
			mState = STATE_EDIT;
			mUri = intent.getData();
		} else if (Intent.ACTION_INSERT.equals(action)) {
			mState = STATE_INSERT;
			if (mOriginalShortcut == null) {
				mUri = getContentResolver().insert(intent.getData(), null);
			} else {
				mUri = mOriginalShortcut.getUri();
			}

			setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
		}
		if (mUri == null) {
			finish();
			return;
		}

		setContentView(R.layout.shortcut_edit);
		
		((Button) findViewById(R.id.button_confirm)).setOnClickListener(this);
		((Button) findViewById(R.id.button_cancel)).setOnClickListener(this);
	}
		
	@Override
	protected void onResume() {
		super.onResume();
		
		Cursor cursor = managedQuery(mUri, Shortcut.PROJECTION, null, null, null);
		Shortcut shortcut = Shortcut.fromCursor(cursor);
		cursor.close();
		
		if (shortcut != null) {
			if (mOriginalShortcut == null) mOriginalShortcut = shortcut;
			fillViews(shortcut);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(ORIGINAL_SHORTCUT, mOriginalShortcut);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
        
		if (mUri != null) {
			ContentValues values = mOriginalShortcut.getContentValues();

			values.put(Shortcut.SHORTCUT, getTextView(R.id.edit_shortcut));
	
			getContentResolver().update(mUri, values, null, null);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		if (mState == STATE_EDIT) {
			menu.add(0, MENU_REVERT, 0, R.string.option_revert)
				.setIcon(android.R.drawable.ic_menu_revert);
			menu.add(0, MENU_DELETE, 0, R.string.option_delete)
				.setIcon(android.R.drawable.ic_menu_delete);
		}
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_DELETE:
				deleteShortcut();
				finish();
				return true;
			case MENU_REVERT:
				fillViews(mOriginalShortcut);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onUserInteraction() {
		updateButtons();
	}

	// View.OnClickListener
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.button_confirm:
				finish();
				break;
			case R.id.button_cancel:
				cancelEdit();
				break;
		}
	}
	
	private void updateButtons() {
		((Button) findViewById(R.id.button_confirm)).setEnabled(
			(((EditText) findViewById(R.id.edit_shortcut)).getText().length() > 0)
		);
	}
	
	/** Fills the Activity's views with the information of a shortcut.
	 * @param shortcut Shortcut to draw information from. 
	 */
	private void fillViews(Shortcut shortcut) {
		setTextView(R.id.edit_shortcut, shortcut.getShortcut());
		
		updateButtons();
	}
	
	/** Cancels the current edit, finishes the activity. */
	private void cancelEdit() {
		if (mUri != null) {
			if (mState == STATE_EDIT) {
				ContentValues values = mOriginalShortcut.getContentValues();
				getContentResolver().update(mUri, values, null, null);
				mUri = null;
			} else if (mState == STATE_INSERT) {
				// Empty shortcut was inserted on startup, clean up.
				deleteShortcut();
			}
		}
		setResult(RESULT_CANCELED);
		finish();
	}
	
	/** Deletes the current item. */
	private void deleteShortcut() {
		if (mUri != null) {
			getContentResolver().delete(mUri, null, null);
			mUri = null;
		}
	}
	
	private final void setTextView(int id, String text) {
		((TextView) findViewById(id)).setText(text);
	}
	private final String getTextView(int id) {
		return ((TextView) findViewById(id)).getText().toString();
	}
}