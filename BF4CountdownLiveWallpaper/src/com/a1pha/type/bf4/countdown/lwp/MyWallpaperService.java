package com.a1pha.type.bf4.countdown.lwp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

/**
 * WallPaper Service implementation
 * @author IKAROS
 *
 */
public class MyWallpaperService extends WallpaperService {

	private static final String TAG = "MyWallpaperService";
	private static int base = 0;

	@Override
    public Engine onCreateEngine() {
  	  	if(BuildConfig.DEBUG) Log.d(TAG,"engine created");
        return new MyWallpaperEngine(this);
    }
	
	/**
	 * Wallpaper engine
	 * @author IKAROS
	 *
	 */
	private class MyWallpaperEngine extends WallpaperService.Engine implements OnSharedPreferenceChangeListener{

		@SuppressWarnings("unused")
		private int num;
		
		private final Context context;
		private final SharedPreferences prefs;
		// locally used thread references
		private BitmapLoaderThread bitmapLoaderThread;
		private SurfaceUpdateThread surfaceUpdateThread;
		
		private float currentXOffset = 0.5f, currentYOffset = 0.5f;
				
	    public MyWallpaperEngine(Context context){
	    	super();
	    	this.context = context;
	    	this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    	this.num = base++;
	    }
	    
	    @Override
	    public void onVisibilityChanged(boolean visible) {
	  	  	if(BuildConfig.DEBUG) Log.d(TAG,"visibility changed");
	  	  	if (visible) {
	  	  		if(surfaceUpdateThread != null){
	  	  			surfaceUpdateThread.stopUpdating();
	  	  		}
	  	  		surfaceUpdateThread = 
	  	  				new SurfaceUpdateThread(getSurfaceHolder(), bitmapLoaderThread, context);
	  	  		surfaceUpdateThread.start();
	  	  		if(this.isPreview()){
	  	  			surfaceUpdateThread.updateOffset(0.5f,0.5f);
	  	  		} else {
	  	  			surfaceUpdateThread.updateOffset(currentXOffset, currentYOffset);
	  	  		}
	      	} else {
	      		if(surfaceUpdateThread != null){
	      			surfaceUpdateThread.stopUpdating();
	      			surfaceUpdateThread = null;
	      		}
	      	}
	      	super.onVisibilityChanged(visible);
	    }

	    @Override
	    public void onSurfaceCreated(SurfaceHolder holder) {
	  	  	if(BuildConfig.DEBUG) Log.d(TAG,"surface created");
	  	  	super.onSurfaceCreated(holder);
	  	  	
	    	prefs.registerOnSharedPreferenceChangeListener(this);
	  	  	
	  	  	//starting the thread to load the bitmap
    		loadBackgroundBitmap();
	    }

	    @Override
	    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	      	if(BuildConfig.DEBUG) Log.d(TAG,"surface changed");
	      	
  	  		if(surfaceUpdateThread != null){
  	  			surfaceUpdateThread.stopUpdating();
  	  		}

	    	super.onSurfaceChanged(holder, format, width, height);
	    	
  	  		surfaceUpdateThread = new SurfaceUpdateThread(getSurfaceHolder(), bitmapLoaderThread, context);
  	  		surfaceUpdateThread.start();
  	  		if(this.isPreview()){
  	  			surfaceUpdateThread.updateOffset(0.5f,0.5f);
  	  		} else {
  	  			surfaceUpdateThread.updateOffset(currentXOffset, currentYOffset);
  	  		}
	    }

	    @Override
	    public void onSurfaceDestroyed(SurfaceHolder holder) {
	  	  	if(BuildConfig.DEBUG) Log.d(TAG,"surface destroyed");

	  	  	prefs.unregisterOnSharedPreferenceChangeListener(this);
	  	  	
      		if(surfaceUpdateThread != null){
      			surfaceUpdateThread.stopUpdating();
      			surfaceUpdateThread = null;
      		}
      		
	  	  	super.onSurfaceDestroyed(holder);
	    }
	    
	    public void onOffsetsChanged (
	    		float xOffset, float yOffset, 
	    		float xOffsetStep, float yOffsetStep, 
	    		int xPixelOffset, int yPixelOffset){
	    	currentXOffset = xOffset;
	    	currentYOffset = yOffset;
	    	if(surfaceUpdateThread != null && !isPreview()){
	    		surfaceUpdateThread.updateOffset(currentXOffset,currentYOffset);
	    	}
	  	  	//if(BuildConfig.DEBUG) Log.d(TAG,"surface destroyed");
	    }
	    
	    public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key){
	    	if(key.equals("pref_background_image") || (key.equals("pref_scrolling_image") && !isPreview())){
	    		loadBackgroundBitmap();
	    	}
	    }
	    
	    private void loadBackgroundBitmap(){
	    	synchronized(this){
	    		// retrieving preferences
	    		String str = prefs.getString("pref_background_image", context.getResources().getString(R.string.background_image_default));
	    		// in preview you can't scroll the screen
	    		int resId = ResourceMapping.drawableIdByPreferenceString(str);
	    		if(bitmapLoaderThread != null){
	    			// don't reload if the same image in the same size is already loaded
	    			if(bitmapLoaderThread.getResId() == resId){
	    				return;
	    			}
	    			else{
	    				bitmapLoaderThread.stopLoading();
	    			}
	    		}
	  	  		bitmapLoaderThread = new BitmapLoaderThread(context.getResources(),resId);
	  	  		bitmapLoaderThread.start();  	  		
	  	  		if(BuildConfig.DEBUG)
	  	  			Toast.makeText(context, getResources().getString(R.string.background_image_loading_msg), Toast.LENGTH_LONG).show();
	    	}
	    }
	
	}
	
}
