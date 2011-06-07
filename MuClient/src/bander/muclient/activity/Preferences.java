package bander.muclient.activity;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import bander.muclient.R;

/** Preferences activity for MuckClient. */
public class Preferences extends PreferenceActivity {
	private static final String KEY_NOSUGGESTIONS = "noSuggestions";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		
		// TYPE_TEXT_FLAG_NO_SUGGESTIONS introduced in Android 2.0, API 5
		CheckBoxPreference noSuggestionsPreference = (CheckBoxPreference) findPreference(KEY_NOSUGGESTIONS);
		boolean enabled = (android.os.Build.VERSION.SDK_INT > 4);
		noSuggestionsPreference.setEnabled(enabled);
		if (enabled == false) noSuggestionsPreference.setSummaryOff(R.string.pref_noSuggestionsDisabled);
	}

}
