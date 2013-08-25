package com.a1pha.type.bf4.countdown.lwp;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;

public class BitmapLoaderThread extends Thread {
	
	private static final String TAG = "BitmapLoaderThread";
	
	private static int baseNum = 0;
	private int num;
	
	private final Resources resources;
	private final int resId;
	
	Bitmap result;
	
	private volatile boolean isLoaded = false;
	private volatile boolean stop = false;
	
	public BitmapLoaderThread(Resources resources, int resId){
		this.resources = resources;
		this.resId = resId;
		num = baseNum++;
	}
	
	public int getResId(){
		return resId;
	}
	
	public boolean isLoaded(){
		return isLoaded;
	}
	
	public Bitmap getBitmap(){
		return result;
	}
	
	public void stopLoading(){
		stop = true;
	}
	
	public void run(){
		
		DisplayMetrics dm = resources.getDisplayMetrics();
		
		if(BuildConfig.DEBUG) Log.d(TAG,"loading bitmap (id:"+resId+") w/ required: "+ (dm.widthPixels) +"x"+ dm.heightPixels+" ("+num+")");

		if(stop) return;
		
		//retrieving bitmap from resources
		result = BitmapProcessor.decodeSampledBitmapFromResource(resources, resId, dm.widthPixels, dm.heightPixels);
		if(result == null){
			if(BuildConfig.DEBUG) Log.d(TAG,"ERROR loading bitmap. Possibly resource is missing!");			
			return;
		}
		//extract from the center of the bitmap a sub-bitmap of the required ratio
		/*float bitmapRatio = (float) result.getWidth() / (float) result.getHeight();
		float displayRatio = (float) (EXTENDED_FACTOR*dm.widthPixels) / (float) dm.heightPixels;
		
		if(BuildConfig.DEBUG) Log.d(TAG,"ratios "+ bitmapRatio +"x"+ displayRatio);
		
		if(stop) return;
		
		if( bitmapRatio > displayRatio ){ //height-bound -> horizontally centered
			
			int newWidth = (int) ( displayRatio * result.getHeight());
			int wBorder = (result.getWidth()- newWidth)/2;
			result = Bitmap.createBitmap(result, wBorder, 0, newWidth, result.getHeight());
			
		} else { //width-bound -> vertically centered
			
			int newHeight = (int) ( result.getWidth() / displayRatio );
			int hBorder = (result.getHeight()- newHeight)/2;
			result = Bitmap.createBitmap(result, 0, hBorder, result.getWidth(), newHeight);
			
		}
		
		if(stop) return;
		
		//scaling bitmap to display size
		result = Bitmap.createScaledBitmap(result, EXTENDED_FACTOR*dm.widthPixels, dm.heightPixels, true);
		*/
		isLoaded = true;
		
		if(BuildConfig.DEBUG) Log.d(TAG,"bitmap loaded (id:"+resId+") : "+result.getWidth()+"x"+result.getHeight()+" ("+num+")");
	}
	
}
