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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import bander.muclient.R;
import bander.provider.Host;

/** Secondary activity for MuClient, shows the details of a single host. */
public class HostEdit extends Activity implements OnClickListener {
	private static final int	MENU_REVERT			= Menu.FIRST + 0;
	private static final int	MENU_DELETE			= Menu.FIRST + 1;
		
	private static final int	STATE_EDIT 			= 0;
	private static final int	STATE_INSERT 		= 1;
	
	private static final String ORIGINAL_HOST 		= "originalHost";
	
	private int			mState;
	private Uri			mUri;
	private Host		mOriginalHost;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null) {
			final Object host = savedInstanceState.get(ORIGINAL_HOST);
			if (host != null) mOriginalHost = (Host) host;
		}
				
		final Intent intent = getIntent();
		final String action = intent.getAction();
		if (Intent.ACTION_EDIT.equals(action)) {
			mState = STATE_EDIT;
			mUri = intent.getData();
		} else if (Intent.ACTION_INSERT.equals(action)) {
			mState = STATE_INSERT;
			if (mOriginalHost == null) {
				mUri = getContentResolver().insert(intent.getData(), null);
			} else {
				mUri = mOriginalHost.getUri();
			}

			setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
		}
		if (mUri == null) {
			finish();
			return;
		}

		setContentView(R.layout.host_edit);
		
		((Button) findViewById(R.id.button_confirm)).setOnClickListener(this);
		((Button) findViewById(R.id.button_cancel)).setOnClickListener(this);
	}
		
	@Override
	protected void onResume() {
		super.onResume();
		
		Cursor cursor = managedQuery(mUri, Host.PROJECTION, null, null, null);
		Host host = Host.fromCursor(cursor);
		cursor.close();
		
		if (host != null) {
			if (mOriginalHost == null) mOriginalHost = host;
			fillViews(host);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(ORIGINAL_HOST, mOriginalHost);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
        
		if (mUri != null) {
			ContentValues values = mOriginalHost.getContentValues();

			values.put(Host.WORLD_NAME, getTextView(R.id.edit_worldname));
			values.put(Host.HOST_NAME, getTextView(R.id.edit_hostname));
			values.put(Host.PORT, getTextView(R.id.edit_port));
			values.put(Host.USE_SSL, getCheckBox(R.id.edit_usessl) ? 1 : 0);
			values.put(Host.POST_CONNECT, getTextView(R.id.edit_login));

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
				deleteHost();
				finish();
				return true;
			case MENU_REVERT:
				fillViews(mOriginalHost);
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
			(((EditText) findViewById(R.id.edit_worldname)).getText().length() > 0) &&
			(((EditText) findViewById(R.id.edit_hostname)).getText().length() > 0) &&
			(((EditText) findViewById(R.id.edit_port)).getText().length() > 0)
		);
	}
	
	/** Fills the Activity's views with the information of a host.
	 * @param host Host to draw information from. 
	 */
	private void fillViews(Host host) {
		setTextView(R.id.edit_worldname, host.getWorldName());
		setTextView(R.id.edit_hostname, host.getHostName());
		String port = (host.getPort() == 0) ? "" : "" + host.getPort(); 
		setTextView(R.id.edit_port, port);
		setTextView(R.id.edit_login, host.getPostConnect());
		setCheckBox(R.id.edit_usessl, host.getUseSsl());
		
		updateButtons();
	}
	
	/** Cancels the current edit, finishes the activity. */
	private void cancelEdit() {
		if (mUri != null) {
			if (mState == STATE_EDIT) {
				ContentValues values = mOriginalHost.getContentValues();
				getContentResolver().update(mUri, values, null, null);
				mUri = null;
			} else if (mState == STATE_INSERT) {
				// Empty host was inserted on startup, clean up.
				deleteHost();
			}
		}
		setResult(RESULT_CANCELED);
		finish();
	}
	
	/** Deletes the current item. */
	private void deleteHost() {
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
	private final void setCheckBox(int id, boolean checked) {
		((CheckBox) findViewById(id)).setChecked(checked);
	}
	private final boolean getCheckBox(int id) {
		return ((CheckBox) findViewById(id)).isChecked();
	}
}
