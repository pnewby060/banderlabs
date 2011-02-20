package bander.muclient.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import bander.muclient.R;

public class PrefFont extends Activity implements OnSeekBarChangeListener, OnItemSelectedListener {

	public static int DEFAULT_SIZE			= 50;
	public static int DEFAULT_COLOR_FORE	= 8;
	public static int DEFAULT_COLOR_BACK	= 0;

	private static int COLORS[] = {
		Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.CYAN, Color.GRAY, Color.WHITE
	};

	public static float toTextSize(int value) { return 4.0f + ((value/100.0f) * 20.0f); }
	public static int toColor(int value) { return COLORS[value]; }
	
	private SeekBar mFontSize;
	private Spinner mFontForeColor;
	private Spinner mFontBackColor;
	private TextView mExampleText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final String action = getIntent().getAction();
		if (!Intent.ACTION_EDIT.equals(action)) {
			finish();
			return;
		}

		setContentView(R.layout.pref_font);

		mFontSize = (SeekBar) findViewById(R.id.edit_fontsize);
		mFontSize.setOnSeekBarChangeListener(this);
		
		String[] colors = getResources().getStringArray(R.array.colors);
				
		mFontForeColor = (Spinner) findViewById(R.id.edit_fontforecolor);
		mFontForeColor.setOnItemSelectedListener(this);
		ArrayAdapter<String> fore = new ArrayAdapter<String>(
			this, android.R.layout.simple_spinner_item, colors
		);
		fore.setDropDownViewResource(R.layout.dropdown_item);
		mFontForeColor.setAdapter(fore);
		
		mFontBackColor = (Spinner) findViewById(R.id.edit_fontbackcolor);
		mFontBackColor.setOnItemSelectedListener(this);
		ArrayAdapter<String> back = new ArrayAdapter<String>(
			this, android.R.layout.simple_spinner_item, colors
		);
		back.setDropDownViewResource(R.layout.dropdown_item);
		mFontBackColor.setAdapter(back);

		mExampleText = (TextView) findViewById(R.id.text_example);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		int fontSize = preferences.getInt("fontSize", DEFAULT_SIZE);
		mFontSize.setProgress(fontSize);
		
		int fontForeColor = preferences.getInt("fontForeColor", DEFAULT_COLOR_FORE);
		mFontForeColor.setSelection(fontForeColor);
		
		int fontBackColor = preferences.getInt("fontBackColor", DEFAULT_COLOR_BACK);
		mFontBackColor.setSelection(fontBackColor);
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = preferences.edit();
		editor.putInt("fontSize", mFontSize.getProgress());
		editor.putInt("fontForeColor", mFontForeColor.getSelectedItemPosition());
		editor.putInt("fontBackColor", mFontBackColor.getSelectedItemPosition());
		editor.commit();
	}

	// SeekBar.OnSeekBarChangeListener
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		mExampleText.setTextSize(toTextSize(progress));
	}
	public void onStartTrackingTouch(SeekBar seekBar) {
	}
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	// AdapterView.OnItemSelectedListener
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == mFontForeColor) {
			mExampleText.setTextColor(toColor(position));
		}
		if (parent == mFontBackColor) {
			mExampleText.setBackgroundColor(toColor(position));
		}
	}
	public void onNothingSelected(AdapterView<?> arg0) {
	}
}
