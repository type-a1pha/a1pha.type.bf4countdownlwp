package com.a1pha.type.bf4.countdown.lwp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

/**
 * Utility class to store media files in persistent memory
 * (may need MEMORY_ACCESS permissions) 
 * @author IKAROS
 *
 */
@SuppressLint("SimpleDateFormat")
public class MediaSaver {

	private static final String TAG = "MediaSaver";
	
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;

	public static void notifySystem(Context context){
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
	}
	
	public static File savePicture(byte[] data, String folder_name){
		File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE, folder_name);
        if (pictureFile == null){
        	if(BuildConfig.DEBUG) Log.d(TAG, "Error creating media file, check storage permissions!");
            return null;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
        	if(BuildConfig.DEBUG) Log.d(TAG, "File not found: " + e.getMessage());
            return null;
        } catch (IOException e) {
        	if(BuildConfig.DEBUG) Log.d(TAG, "Error accessing file: " + e.getMessage());
            return null;
        }
        return pictureFile;
	}
	
	public static File saveBitmap(Bitmap bitmap, String folder_name){
		File filename = getOutputMediaFile(MEDIA_TYPE_IMAGE, folder_name);
        if (filename == null){
        	if(BuildConfig.DEBUG) Log.d(TAG, "Error creating media file, check storage permissions!");
            return null;
        }
        
		try {
		       FileOutputStream out = new FileOutputStream(filename);
		       bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
		       out.close();
		       if(BuildConfig.DEBUG) Log.d(TAG, "bitmap saved: "+ bitmap.getWidth() +"x"+ bitmap.getHeight());
		} catch (Exception e) {
		       e.printStackTrace();
		}
		return filename;
	}
	
	/** Create a file Uri for saving an image or video */
	@SuppressWarnings("unused")
	private static Uri getOutputMediaFileUri(int type, String folder_name){
	      return Uri.fromFile(getOutputMediaFile(type,  folder_name));
	}

	/** Create a File for saving an image or video 
	 *  null if unable to create the file */
	@SuppressLint("SimpleDateFormat")
	private static File getOutputMediaFile(int type, String folder_name){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), folder_name);
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	        	if(BuildConfig.DEBUG) Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }
	    if(BuildConfig.DEBUG) Log.d(TAG,mediaStorageDir.getPath() + File.separator +
    	        "IMG_"+ timeStamp + ".jpg");
	    return mediaFile;
	}
}
