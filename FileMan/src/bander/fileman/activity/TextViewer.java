package bander.fileman.activity;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import bander.fileman.R;
import bander.fileman.util.FileUtils;

/** Secondary activity of FileMan, shows the contents of a text file and allows editing. */
public class TextViewer extends Activity {
	private static final int		REVERT_ID			= Menu.FIRST;
	private static final int		PREFS_ID			= Menu.FIRST + 1;

	private static final String		ORIGINAL_TEXT 		= "originalText";

	private Uri						mUri;
	private EditText				mText;
	
	private File					mFile;
	private String					mOriginalText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		
		final String action = intent.getAction();
		if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) {
			mUri = intent.getData();
			setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));
		} else {
			finish();
			return;
		}

		setContentView(R.layout.textviewer);
        
		mText = (EditText) findViewById(R.id.text);
        
		mFile = new File(mUri.getPath());
		
		if (savedInstanceState != null) {
			mOriginalText = savedInstanceState.getString(ORIGINAL_TEXT);
		}		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		if ((mFile != null) && (mFile.canWrite())) {
			String text = mText.getText().toString();
			if (text != mOriginalText) {
				try {
					byte[] buffer = text.getBytes();
					BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(mFile));
					stream.write(buffer);
					stream.close();
				} catch (IOException e) {
					// fail silently
				}
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		float textSize = Float.valueOf(preferences.getString("textSize", "16"));
		mText.setTextSize(textSize);

		mText.setEnabled(mFile.canWrite());

		setTitle(mUri.getPath());
		if (mFile != null) {
			try {
				String string = FileUtils.readTextFile(mFile);
				string = string.replace("\r\n", "\n");
				string = string.replace("\r", "\n");
				
				mText.setTextKeepState(string);
				
				if (mOriginalText == null) {
					mOriginalText = string;
				}
			} catch (IOException e) {
				mFile = null;
				Toast.makeText(this, 
					String.format(getString(R.string.failed_open_explanation), mUri.getPath()), 
					Toast.LENGTH_SHORT
				).show();
				return;
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(ORIGINAL_TEXT, mOriginalText);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem revertItem = menu.add(0, REVERT_ID, 0, R.string.menu_revert);
		revertItem.setIcon(android.R.drawable.ic_menu_revert);
		
		MenuItem prefsItem = menu.add(0, PREFS_ID, 0, R.string.menu_prefs);
		prefsItem.setIcon(android.R.drawable.ic_menu_preferences);
		
		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case REVERT_ID:
				mText.setTextKeepState(mOriginalText);
				return true;
			case PREFS_ID:
				Intent prefsActivity = new Intent(this, Preferences.class);
				startActivity(prefsActivity);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
