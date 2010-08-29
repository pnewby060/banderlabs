package bander.fileman;

import java.io.File;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

/** Secondary activity for FileMan, displays properties of a single file 
  * and allows editing of the file name.
  */
public class Properties extends Activity {
	public static final	String		FILENAME_FROM		= "nameFrom";
	public static final	String		FILENAME_TO			= "nameTo";
	
	private static final int		REVERT_ID 			= Menu.FIRST + 0;
	private static final int		SEND_ID 			= Menu.FIRST + 1;
	private static final int		PREFS_ID 			= Menu.FIRST + 2;
	
	private EditText	mFilenameEdit;
	
	private File		mFile;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.properties);

		Bundle extras = getIntent().getExtras();            
		String filename = (extras != null)
			? extras.getString(FILENAME_FROM) 
			: "";

		mFile = new File(filename);

		mFilenameEdit = (EditText) findViewById(R.id.edit_filename);
		Button okButton = (Button) findViewById(R.id.button_ok);
		Button cancelButton = (Button) findViewById(R.id.button_cancel);

		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra(FILENAME_FROM, mFile.getName());
				resultIntent.putExtra(FILENAME_TO, mFilenameEdit.getText().toString());
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		});
		cancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		fillView();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		menu.add(0, REVERT_ID, 0, R.string.menu_revert)
		.setIcon(android.R.drawable.ic_menu_revert);

		menu.add(0, SEND_ID, 0, R.string.menu_send)
			.setIcon(android.R.drawable.ic_menu_send);

		menu.add(0, PREFS_ID, 0, R.string.menu_prefs)
		.setIcon(android.R.drawable.ic_menu_preferences);

		return result;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case REVERT_ID:
				mFilenameEdit.setTextKeepState(mFile.getName());
				return true;
			case SEND_ID:
				Intent sendIntent = new Intent(Intent.ACTION_SEND);
				sendIntent.setDataAndType(Uri.fromFile(mFile), MimeUtils.getType(mFile.getName()));
				startActivity(Intent.createChooser(sendIntent, getString(R.string.context_send)));
				return true;
			case PREFS_ID:
				Intent prefsActivity = new Intent(this, Preferences.class);
				startActivity(prefsActivity);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void fillView() {
		setTitle(mFile.getParent());
		mFilenameEdit.setTextKeepState(mFile.getName());
		
		if (mFile.isDirectory()) {
			setTextView(R.id.props_type, getString(R.string.props_type_directory));
		} else {
			setTextView(R.id.props_type, MimeUtils.getType(mFile.getName()));
		}
		
		if (mFile.length() > 0) {
			setVisibility(R.id.props_filesize, View.VISIBLE);
			Format integerFormat = NumberFormat.getIntegerInstance();
			setTextView(R.id.props_filesize_value, 
				String.format(getString(R.string.props_filesize_value), integerFormat.format(mFile.length()))
			);
		} else {
			setVisibility(R.id.props_filesize, View.GONE);
		}
		
		if (mFile.lastModified() > 0) { 
			setVisibility(R.id.props_lastmodified, View.VISIBLE);
			Date lastModified = new Date(mFile.lastModified());
			java.text.DateFormat dateFormat = DateFormat.getLongDateFormat(getBaseContext());
			java.text.DateFormat timeFormat = DateFormat.getTimeFormat(getBaseContext());
			setTextView(R.id.props_lastmodified_value, 
				dateFormat.format(lastModified) + " " + timeFormat.format(lastModified)
			);
		} else {
			setVisibility(R.id.props_lastmodified, View.GONE);
		}
		
		setChecked(R.id.props_readable, mFile.canRead());
		setChecked(R.id.props_writable, mFile.canWrite());
		setChecked(R.id.props_hidden, mFile.isHidden());
	}
	
	private final void setTextView(int id, String text) {
		((TextView) findViewById(id)).setText(text);
	}
	private final void setVisibility(int id, int visibility) {
		findViewById(id).setVisibility(visibility);
	}
	private final void setChecked(int id, boolean checked) {
		((CheckBox) findViewById(id)).setChecked(checked);
	}
	
}
