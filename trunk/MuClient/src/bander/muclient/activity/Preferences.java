package bander.muclient.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import bander.muclient.R;

/** Preferences activity for MuckClient. */
public class Preferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
	}

}
