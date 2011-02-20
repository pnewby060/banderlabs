package bander.muclient.activity;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import bander.muclient.Main;
import bander.muclient.R;
import bander.muclient.net.TextConnection;
import bander.muclient.view.SizingScrollView;
import bander.muclient.view.SizingScrollView.OnSizeListener;
import bander.provider.Host;
import bander.provider.Shortcut;

/** Secondary activity for MuClient, shows a connected session. */
public class HostSession extends Activity implements OnClickListener, OnSizeListener, OnKeyListener, Callback {
	private static final int MENU_EDIT			= Menu.FIRST + 0;
	private static final int MENU_CONNECT		= Menu.FIRST + 1;
	private static final int MENU_DISCONNECT	= Menu.FIRST + 2;
	private static final int MENU_PREFS			= Menu.FIRST + 3;
	
	private static final int DIALOG_SHORTCUTS	= 1;

	private final Handler mHandler = new Handler(this);

	private Uri mUri;

	private SizingScrollView mOutputScroll;
	private TextView mOutputText;
	private Button mShortcutButton;
	private EditText mInputEdit;

	private TextConnection mTextConnection = null;
	
	private boolean mLinkify = true;
	private String[] mShortcuts;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String action = intent.getAction();
		if (Intent.ACTION_VIEW.equals(action)) {
			mUri = intent.getData();
		}
		if (mUri == null) {
			finish();
			return;
		}
		
		setContentView(R.layout.host_session);
		
		mOutputScroll = (SizingScrollView) findViewById(R.id.scroll_output);
		mOutputScroll.setOnSizeListener(this);
		
		mOutputText = (TextView) findViewById(R.id.text_output);
		
		mShortcutButton = (Button) findViewById(R.id.button_shortcut);
		mShortcutButton.setOnClickListener(this);
		
		mInputEdit = (EditText) findViewById(R.id.edit_input);
		mInputEdit.setOnKeyListener(this);
		
		Object retained = this.getLastNonConfigurationInstance();
		if (retained != null) {
			mTextConnection = (TextConnection) retained;
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Main muClient = (Main) getApplication();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		int fontSize = preferences.getInt("fontSize", PrefFont.DEFAULT_SIZE);
		mOutputText.setTextSize(PrefFont.toTextSize(fontSize));
		int fontForeColor = preferences.getInt("fontForeColor", PrefFont.DEFAULT_COLOR_FORE);
		mOutputText.setTextColor(PrefFont.toColor(fontForeColor));
		int fontBackColor = preferences.getInt("fontBackColor", PrefFont.DEFAULT_COLOR_BACK);
		mOutputScroll.setBackgroundColor(PrefFont.toColor(fontBackColor));
		mOutputText.setBackgroundColor(PrefFont.toColor(fontBackColor));
		
		boolean fullScreen = preferences.getBoolean("fullScreen", false);
		getWindow().setFlags(
			(fullScreen) ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0, 
			WindowManager.LayoutParams.FLAG_FULLSCREEN
		);
		
		mLinkify = preferences.getBoolean("linkify", true);
		mOutputText.setAutoLinkMask(mLinkify ? Linkify.WEB_URLS : 0);
		mOutputText.setMovementMethod(mLinkify ? LinkMovementMethod.getInstance() : ScrollingMovementMethod.getInstance());
		mOutputText.setLinksClickable(mLinkify);
		
		boolean keepScreen = preferences.getBoolean("keepScreen", false);
		mOutputText.setKeepScreenOn(keepScreen);
		
		boolean showShortcuts = preferences.getBoolean("showShortcuts", false);
		mShortcutButton.setVisibility(showShortcuts ? View.VISIBLE : View.GONE);
		
		boolean keepWifi = preferences.getBoolean("keepWifi", false);
		WifiLock wifiLock = muClient.getWifiLock();
		if ((keepWifi == true) && (wifiLock.isHeld() == false)) {
			wifiLock.acquire();
		}
		if ((keepWifi == false) && (wifiLock.isHeld() == true)) {
			wifiLock.release();
		}
		
		ArrayList<String> shortcuts = new ArrayList<String>();
		Cursor cursor = managedQuery(Shortcut.CONTENT_URI, Shortcut.PROJECTION, null, null, null);
		while (cursor.moveToNext()) {
			shortcuts.add(cursor.getString(cursor.getColumnIndexOrThrow(Shortcut.SHORTCUT)));
		}
		cursor.close();
		mShortcuts = shortcuts.toArray(new String[0]);
		
		cursor = managedQuery(mUri, Host.PROJECTION, null, null, null);
		Host host = Host.fromCursor(cursor);
		cursor.close();
		
		this.setTitle(host.getWorldName());
		
		if (mTextConnection == null) {
			mTextConnection = muClient.getConnection(host.getUri());
			if (mTextConnection == null) {
				mTextConnection = new TextConnection(
					mHandler, 
					host.getHostName(), host.getPort(), host.getUseSsl(), host.getPostConnect()
				);
				mTextConnection.start();
				muClient.setConnection(host.getUri(), mTextConnection);
			} 
		}
		mTextConnection.setHandler(mHandler);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (mTextConnection.isAlive()) {
			mTextConnection.setHandler(null);	
		}
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return mTextConnection;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//menu.add(0, MENU_EDIT, 0, R.string.context_editworld)
		//	.setIcon(android.R.drawable.ic_menu_edit);
		menu.add(0, MENU_CONNECT, 0, R.string.menu_connect)
			.setIcon(android.R.drawable.ic_menu_upload);
		menu.add(0, MENU_DISCONNECT, 0, R.string.menu_disconnect)
			.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(0, MENU_PREFS, 0, R.string.menu_prefs)
			.setIcon(android.R.drawable.ic_menu_preferences);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.getItem(0).setEnabled(mTextConnection.canConnect());
		menu.getItem(1).setEnabled(mTextConnection.canDisconnect());
		
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case MENU_EDIT:
				startActivity(new Intent(Intent.ACTION_EDIT, mUri));
				return true;
			case MENU_CONNECT:
				Cursor cursor = managedQuery(mUri, Host.PROJECTION, null, null, null);
				Host host = Host.fromCursor(cursor);
				cursor.close();
				this.setTitle(host.getWorldName());
				mTextConnection = new TextConnection(mHandler,
					host.getHostName(), host.getPort(), host.getUseSsl(), host.getPostConnect()
				);
				Main muClient = (Main) getApplication();	
				muClient.setConnection(host.getUri(), mTextConnection);
				mTextConnection.start();
				return true;
			case MENU_DISCONNECT:
				mTextConnection.disconnect();
				return true;
			case MENU_PREFS:
				Intent prefsActivity = new Intent(this, Preferences.class);
				startActivity(prefsActivity);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_SHORTCUTS:
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
					this, R.layout.dropdown_item, mShortcuts
				);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getResources().getText(R.string.header_shortcut));
				builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						mInputEdit.append(mShortcuts[item]);
					}
				});
				return builder.create();
		}
		return null;
	}

	// View.OnClickListener
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.button_shortcut:
				showDialog(DIALOG_SHORTCUTS);
				break;
		}
	}
	
	// SizingScrollView.OnSizeListener
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		if ((oldw != 0) && (oldh != 0)) {
			mOutputScroll.scrollTo(0, mOutputText.getHeight());
		}
	}
	
	// View.OnKeyListener
	public boolean onKey(View view, int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_ENTER)) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				try {
					mTextConnection.write(mInputEdit.getText().toString());
				} catch (IOException e) {
					appendOutput(e.getMessage());
				}
				mInputEdit.setText("");
			}
			return true;
		}
		return false;
	}
	
	// Handler.Callback
	public boolean handleMessage(Message msg) {
		switch (msg.arg1) {
			case TextConnection.MESSAGE_CLEAR:
				mOutputText.setText("");
				break;
			case TextConnection.MESSAGE_SYSTEM:
				appendOutput((String) msg.obj);
				break;
			case TextConnection.MESSAGE_LINE:
				if (mLinkify) {
					Spannable spannable = new SpannableString((String) msg.obj);
					Linkify.addLinks(spannable, Linkify.WEB_URLS);
					appendOutput(spannable);
				} else {
					appendOutput((String) msg.obj);
				}
				break;
		}
		return true;
	}
		
	private void appendOutput(CharSequence text) {
		mOutputText.append(text);
		mOutputText.post(new Runnable() {
			public void run() {
				mOutputScroll.scrollTo(0, mOutputText.getHeight());
			}
		});
	}

}
