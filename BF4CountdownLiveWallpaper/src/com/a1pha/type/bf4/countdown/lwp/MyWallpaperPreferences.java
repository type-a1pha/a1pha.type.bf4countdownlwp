package com.a1pha.type.bf4.countdown.lwp;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * 
 * @author IKAROS
 *
 */
public class MyWallpaperPreferences extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.my_wallpaper_preferences);
	}

}
