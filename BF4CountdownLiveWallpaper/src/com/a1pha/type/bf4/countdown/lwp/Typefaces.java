package com.a1pha.type.bf4.countdown.lwp;

import java.util.Hashtable;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

/**
 * Local Cache for loaded typefaces (necessary for  abug in Android
 * which reloads already loaded typefaces)
 * @author IKAROS
 *
 */
public class Typefaces {
	private static final String TAG = "Typefaces";

	private static final Hashtable<String, Typeface> cache = new Hashtable<String, Typeface>();

	public static Typeface get(Context c, String assetPath) {
		synchronized (cache) {
			if (!cache.containsKey(assetPath)) {
				try {
					Typeface t = Typeface.createFromAsset(c.getAssets(),
							assetPath);
					cache.put(assetPath, t);
				} catch (Exception e) {
					Log.e(TAG, "Could not get typeface '" + assetPath
							+ "' because " + e.getMessage());
					return null;
				}
			}
			return cache.get(assetPath);
		}
	}
}
