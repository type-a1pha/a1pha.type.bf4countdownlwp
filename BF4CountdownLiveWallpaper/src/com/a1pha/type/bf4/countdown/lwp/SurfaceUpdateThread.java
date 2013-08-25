package com.a1pha.type.bf4.countdown.lwp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;

import com.nineoldandroids.animation.ArgbEvaluator;

/**
 * 
 * Thread which handles the continuos update of the wallpaper canvas.
 * Once this thread is started, it is guaranteed that the canvas will be available that is,
 * this thread is started only after the canvas is created (e.g. visible).
 * 
 * Once started, this thread suppose that nothing changes in user preferences.
 * To handle change in settings this thread needs to be restarted.
 * 
 * @author IKAROS
 *
 */
public class SurfaceUpdateThread extends Thread {

	private static final String TAG = "SurfaceUpdateThread";
	
	//variables to keep track of the started threads
	private static int base_num = 0;
	private int num;
	//indicate wheter the thread should keep running or stop itself
	private volatile boolean keepRunning = true;
	//convenience variables
	private final SurfaceHolder surfaceHolder;	
	private final SharedPreferences prefs;
	private final Resources resources;
	//thread who loads the bitmap
	private final BitmapLoaderThread bitmapLoaderThread;
	//background image
	private Bitmap background;
	private Rect subBitmapRect;
	//convenience variables for text layout
	private double hPaddingPercentage = 0.05;
	private double vPaddingPercentage = 0.15;
	private int thresholdError = 10; //maximum acceptable error when computing text size
	//private int thresholdLimit = 2; //minimum difference between text sizes when implementing variable text size
	//user preferences
	private long prefReleaseDateMillis;
	private long prefUpdateIntervalMillis; //update interval of the displayed time
	private Typeface prefTypeface;
	private int prefColor;
	private boolean prefVariableColor = false;
	private String prefFontSize;
	private boolean prefVariableSize = false;
	private String prefHPosition;
	private String prefVPosition;
	private boolean prefBold = false;
	private boolean prefGlow = false;
	private boolean prefSkew = false;
	private boolean prefStroke = false;
	private boolean prefUnderline = false;
	
	private boolean prefScrollingEnabled = false;
	private final float CENTERED_OFFSET = 0.5f; //means centered sub-bitmap
	@SuppressWarnings("unused")
	private final float MIN_OFFSET = 0.0f; // means all to the left
	@SuppressWarnings("unused")
	private final float MAX_OFFSET = 1.0f; //means all to the right
	@SuppressWarnings("unused")
	private float prevXOffset = CENTERED_OFFSET; //, prevYOffset = CENTERED_OFFSET;
	private float xOffset = CENTERED_OFFSET; //, yOffset = CENTERED_OFFSET;
	
	private float refreshRate = 30;
	
	private ArgbEvaluator argbEvaluator = new ArgbEvaluator();
	
	//utility variables for variable fontt size effect (i.e. prefFontSize.equals("variable"))
	int maxSize, minSize;
	long baseTime;
	
	public SurfaceUpdateThread(
			SurfaceHolder surfaceHolder, BitmapLoaderThread bitmapLoaderThread, Context context){
		//initializing base variables
		this.num = base_num++;
		this.surfaceHolder = surfaceHolder;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.resources = context.getResources();
		this.bitmapLoaderThread = bitmapLoaderThread;
		//initialize refresh rate
		//BUGGED -> refreshRate = ((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRefreshRate();
		loadSharedPreferences(context);
	}
	
	public void loadSharedPreferences(Context context){
		//retrieving release date preference
		try{
			
			prefReleaseDateMillis = Long.parseLong(
				prefs.getString("pref_release_date", resources.getString(R.string.release_date_default)));
			
		} catch (NumberFormatException e) {
			if(BuildConfig.DEBUG) Log.d(TAG,"UNEXPECTED NumberFormatException: release_date_millis");
			prefReleaseDateMillis = 0;
		}
		//retrieving update rate preference
		try{
			
			prefUpdateIntervalMillis = 1000* Long.parseLong(
				prefs.getString("pref_update_rate", resources.getString(R.string.update_rate_default)));
			
		} catch (NumberFormatException e) {
			if(BuildConfig.DEBUG) Log.d(TAG,"UNEXPECTED NumberFormatException");
			prefUpdateIntervalMillis = 1000;
		}
		//retrieving typeface
		prefTypeface = Typeface.MONOSPACE; //default value for typeface		
		String typefaceFile = (
			prefs.getString("pref_font_style", resources.getString(R.string.font_style_default)));
		if( ! typefaceFile.equals("Default") ){
			prefTypeface = Typefaces.get(context, "fonts/"+typefaceFile);
		}
		//retrieving text color
		prefColor = prefs.getInt("pref_color", resources.getInteger(R.integer.GREEN));
		//retrieving font size
		prefFontSize = (
				prefs.getString("pref_font_size", resources.getString(R.string.font_size_default)));
		//retrieving horizontal position
		prefHPosition = (
				prefs.getString("pref_text_horizontal_position", resources.getString(R.string.horizontal_position_default)));
		//retrieving vertical position
		prefVPosition = (
				prefs.getString("pref_text_vertical_position", resources.getString(R.string.vertical_position_default)));
		//retrieving text style and boolean values
		prefVariableColor = prefs.getBoolean("pref_variable_color", false);
		
		prefVariableSize = prefs.getBoolean("pref_variable_text", false);
		
		prefScrollingEnabled = prefs.getBoolean("pref_scrolling_image", false);
		
		prefBold = prefs.getBoolean("pref_bold", false);
		prefGlow = prefs.getBoolean("pref_glow", false);
		prefSkew = prefs.getBoolean("pref_skew", false);
		prefStroke = prefs.getBoolean("pref_stroke", false);
		prefUnderline = prefs.getBoolean("pref_underline", false);
	}
	
	/**
	 * xOffset and yOffset is in [0,1] where 0.5 means
	 * the center of the central tab in the home screen
	 * @param xOffset
	 */
	public void updateOffset(float xOffset, float yOffset){
		//this.prevXOffset = this.xOffset;
		//this.prevYOffset = this.yOffset;
		this.xOffset = xOffset;
		//this.yOffset = yOffset;
		//if(BuildConfig.DEBUG) Log.d(TAG,"xOffset: "+xOffset);
	}
	
	/**
	 * Called to force the thread to stop
	 */
	public void stopUpdating(){
		keepRunning = false;
		this.interrupt();
	}
	
	public boolean isUpdating(){
		return keepRunning;
	}
	
	/**
	 * Main updating cycle. It handles all the drawings on the canvas
	 */
	public void run(){
		if(BuildConfig.DEBUG) Log.d(TAG,"thread started "+num);
		
		Canvas canvas = surfaceHolder.lockCanvas();
		// retrieving canvas information before starting the update process
		if(canvas == null){
			if(BuildConfig.DEBUG) Log.d(TAG,"Canvas is NULL ... exiting: "+num);
			return;
		}
		// initializing the painting parameters
		Paint textPainter = createTextPainter(canvas);
		// computing text position
		Point textPosition = computeTextPosition(canvas,textPainter);
		// variables for effects
		if(prefVariableSize || prefVariableColor){
			//computing additional values for variable effects
			textPainter.setStrokeWidth(2);
			maxSize = computeTextSize(canvas, textPainter, "large");
			minSize = computeTextSize(canvas, textPainter, "small");
			baseTime = System.currentTimeMillis();
			//if(BuildConfig.DEBUG) Log.d(TAG,"SIZES: "+minSize+" - "+maxSize);
		}
		
		surfaceHolder.unlockCanvasAndPost(canvas);
		
		//rendering loop
		while(isUpdating()){
			
			if(background == null && bitmapLoaderThread.isLoaded())
				background = bitmapLoaderThread.getBitmap();
			String counterStr = stringToDisplay();
			// updating canvas content
			updateCanvas(counterStr, textPainter, textPosition);			
			try{
				if(prefVariableSize || prefVariableColor || prefScrollingEnabled){
					sleep( (int) (Constants.SECOND_MILLIS / (refreshRate)));
					//updating font size (variable)
					if(prefVariableSize){
						// local period of 2 seconds
						double newSize = minSize + (maxSize - minSize) *
							Math.abs( Math.sin( (double)(System.currentTimeMillis() - baseTime)/(Constants.SECOND_MILLIS) * Math.PI ) );
						textPainter.setTextSize((float) newSize);
					}
					if(prefVariableColor){
						// local period of 3 seconds
						int color = getVariableColor((double)(System.currentTimeMillis() - baseTime)/(3*Constants.SECOND_MILLIS));
						textPainter.setColor(color);
					    if(prefGlow){ textPainter.setShadowLayer(20.0f, 0.0f, 0.0f, color); }
					}
				} else {
					sleep(prefUpdateIntervalMillis);
				}
			} catch(InterruptedException e) {
				if(BuildConfig.DEBUG) Log.d(TAG,"thread interrupted "+num);
			}
			
		}
		
		if(BuildConfig.DEBUG) Log.d(TAG,"thread exiting "+num);
	}
	
	private int getVariableColor(double elapsedSeconds){
		int[] colors = ResourceMapping.getVariableColorsArray();
		int first = colors[((int)elapsedSeconds) % colors.length];
		int second = colors[((int)elapsedSeconds + 1) % colors.length];
		double factor = elapsedSeconds - (int)elapsedSeconds;
		return ((Integer)argbEvaluator.evaluate((float) factor, Integer.valueOf(first), Integer.valueOf(second))).intValue();
	}
	
	/**
	 * 
	 * @return The string of text to display
	 */
	private String stringToDisplay(){
		long millis = prefReleaseDateMillis - System.currentTimeMillis();		
		if(millis < 0){
			return "00:00:00:00";
		}
		
		millis = millis + (prefUpdateIntervalMillis - millis % prefUpdateIntervalMillis); //normalize to standard values
		
		long days = (long) millis/Constants.DAY_MILLIS;
		millis = millis - days*Constants.DAY_MILLIS;
		long hours = (long) millis/Constants.HOUR_MILLIS;
		millis = millis - hours*Constants.HOUR_MILLIS;
		long minutes = (long) millis/Constants.MINUTE_MILLIS;
		millis = millis - minutes*Constants.MINUTE_MILLIS;
		long seconds = (long) millis/Constants.SECOND_MILLIS;
		millis = millis - seconds*Constants.SECOND_MILLIS;
		String d = String.valueOf(days);
		String h = String.valueOf(hours);
		String m = String.valueOf(minutes);
		String s = String.valueOf(seconds);
		if(days == 0) d = "00";
		if(h.length() == 1) h = "0"+h;
		if(m.length() == 1) m = "0"+m;
		if(s.length() == 1) s = "0"+s;
		return d +":"+ h +":"+ m +":"+ s;
	}
	
	/*
	 * reqFontSize must be a value between small, medium or large
	 */
	private int computeTextSize(Canvas canvas, Paint textPainter, String reqFontSize){
		int textSize;
		//computing required text width
		boolean portrait = canvas.getWidth() < canvas.getHeight();
		//medium by default (fontSize.equals("medium"))
		int reqWidth;
		if(portrait){ reqWidth =  canvas.getWidth() * 3/4; }
		else { reqWidth =  canvas.getWidth() * 3/8; }
		if(reqFontSize.equals("small")){
			if(portrait){ reqWidth =  canvas.getWidth() * 1/2; }
			else { reqWidth =  canvas.getWidth() * 1/4; }		
		}
		if(reqFontSize.equals("large")){
			if(portrait){ reqWidth =  canvas.getWidth() - (int)(canvas.getWidth() * 2 * hPaddingPercentage); }
			else { reqWidth =  canvas.getWidth() * 1/2; }			
		}
		
		int tempTextSize = (int) textPainter.getTextSize();
		//computing textSize (to do at the end of this method since textSize is based on the other parameters)
	    textSize = 25;
	    if(reqFontSize.equals("small")) textSize = 15;
	    if(reqFontSize.equals("large")) textSize = 50;
	    textPainter.setTextSize(textSize);
	    //there are 11 or 12 characters in the string, so i take 11.5 as the mean number of characters
	    double textWidth = textPainter.measureText("0") * 11.5; //width of the characters -> monospaced
	    int iterations = 0;
	    while( iterations < 100 && Math.abs(textWidth - reqWidth) > thresholdError ){
	    	if( textWidth - reqWidth > 0 ){ //need to decrease text size
	    		textSize = (int) (textSize - 1);
	    	} else { //need to increase text size
	    		textSize = (int) (textSize + 1);
	    	}
	    	textPainter.setTextSize(textSize);
	    	textWidth = textPainter.measureText("0") * 11.5; //update text width
	    	iterations++;
	    }
		textPainter.setTextSize(tempTextSize);
		return textSize;
	}
	
	private Point computeTextPosition(Canvas canvas, Paint textPainter){//compute text horizontal position
		Point pos = new Point(10,10);
		
	    double textHeight = (textPainter.descent() + textPainter.ascent());
	    //default is centered
		textPainter.setTextAlign(Align.CENTER);
		pos.x = (canvas.getWidth() / 2);
	    pos.y = (int) ((canvas.getHeight() / 2) - (textHeight / 2)) ; 
	    //((textPaint.descent() + textPaint.ascent()) / 2) is the distance from the baseline to the center.
	    
	    //non-default horizontal positioning
	    if(prefHPosition.equals("left")){
	    	textPainter.setTextAlign(Align.LEFT);
	    	pos.x = (int) (canvas.getWidth() * hPaddingPercentage);
	    } 
	    if(prefHPosition.equals("right")){
	    	textPainter.setTextAlign(Align.RIGHT);
	    	pos.x = (int) (canvas.getWidth() - (canvas.getWidth() * hPaddingPercentage));
	    }
	    //non-default vertical positioning
	    if(prefVPosition.equals("upper")){
	    	pos.y = (int) (canvas.getHeight() * vPaddingPercentage - textHeight);
	    } 
	    if(prefVPosition.equals("lower")){
	    	pos.y = (int) (canvas.getHeight() - (canvas.getHeight() * vPaddingPercentage));
	    }

		//if(BuildConfig.DEBUG) Log.d(TAG,"position: ("+pos.x+","+pos.y+")");
	    return pos;
	}
	
	private Paint createTextPainter(Canvas canvas){		
		Paint textPainter = new Paint();		
		
		textPainter.setColor(prefColor);
	    textPainter.setAntiAlias(true);
	    if(prefTypeface != null)
	    	textPainter.setTypeface(prefTypeface);
	    
	    //setting text style
	    if(prefBold){ textPainter.setFakeBoldText(true); }
	    if(prefGlow){ textPainter.setShadowLayer(10.0f, 0.0f, 0.0f, prefColor); }
	    if(prefSkew){ textPainter.setTextSkewX(-0.50f); }
	    if(prefStroke){ 
	    	textPainter.setStyle(Paint.Style.STROKE); 
	    	textPainter.setStrokeJoin(Paint.Join.MITER);
	    	textPainter.setStrokeCap(Paint.Cap.BUTT);
	    	textPainter.setStrokeWidth(2);
	    	if(prefFontSize.equals("small")) textPainter.setStrokeWidth(1);
	    	if(prefFontSize.equals("large")) textPainter.setStrokeWidth(2);
	    }
	    if(prefUnderline){
	    	textPainter.setStyle(Paint.Style.FILL); 
	    	textPainter.setUnderlineText(prefUnderline);
	    }
	    //text size
	    textPainter.setTextSize(computeTextSize(canvas, textPainter, prefFontSize));

	    return textPainter;
	}
	
	private Rect computeBitmapRectToDraw(Rect dst, float localXOffset){
		Rect src = new Rect();
		double dstHeight = dst.bottom - dst.top;
		double dstWidth = dst.right - dst.left;
		double dstRatio = (double)dstHeight/(double)dstWidth;
		double bitmapRatio = (double)background.getHeight()/(double)background.getWidth();
	    // if(BuildConfig.DEBUG) Log.d(TAG,"RATIOS: "+dstRatio+"  "+bitmapRatio);
		if(dstRatio > bitmapRatio){ // full height
			
			src.top = 0; src.bottom = background.getHeight();
			
			double reqWidth = (background.getHeight()/dstHeight) * dstWidth;
			//moving left depending on x-axis offset
			if(localXOffset != this.CENTERED_OFFSET){
				double availableScrollingDistance = Math.min( Math.max(background.getWidth() - reqWidth,0) , reqWidth);
				double actualScrollingDistance = this.xOffset*availableScrollingDistance;
				src.left  = (int)( (double)background.getWidth()/2 - reqWidth/2 - availableScrollingDistance/2 + actualScrollingDistance);
			} else {
				src.left  = (int)( (double)background.getWidth()/2 - reqWidth/2 );
			}
			src.right = (int)( src.left + reqWidth );
			
		} else { // full width (no scrolling available because there is no space available on the bitmap)
			
			src.left = 0; src.right = src.left + background.getWidth();
			
			double reqHeight = (background.getWidth()/dstWidth) * dstHeight; 
			src.top  = (int)( (double)background.getHeight()/2 - reqHeight/2 );
			src.bottom = (int)( src.top + reqHeight );
			
		}
		return src;
	}
	
	private void updateCanvas(String text, Paint textPainter, Point textPosition){
		Canvas canvas = surfaceHolder.lockCanvas();
		
		if(canvas == null) return;
		
	    //drawing background color
		canvas.drawARGB(255, 0, 0, 0); //fill in with black before drawing		
		//drawing bitmap	
		if(background != null){ //still not laoded
			/*if(prefScrollingEnabled) {
				int availableScrollingDistance = background.getWidth() - canvas.getWidth();
				int actualScrollingDistance = (int) (this.xOffset*availableScrollingDistance);
				Rect src = new Rect();
				src.left = actualScrollingDistance;
				if(src.left < 0) src.left = 0;
				src.right = src.left + canvas.getWidth();
				if(src.right > background.getWidth()) src.right = background.getWidth();
				src.top = (background.getHeight() - canvas.getHeight()) /2;
				if(src.top < 0) src.top = 0;
				src.bottom = src.top + canvas.getHeight();
				if(src.bottom > background.getHeight()) src.bottom = background.getHeight();
				Rect dst = new Rect();
				dst.left = 0; dst.right = canvas.getWidth();
				dst.top = 0; dst.bottom = canvas.getHeight();
				canvas.drawBitmap(background, src, dst, null);
			} else { 
				canvas.drawBitmap(background, 0, 0, null); 
			}*/
			Rect dst = new Rect();
			dst.left = 0; dst.right = canvas.getWidth();
			dst.top = 0; dst.bottom = canvas.getHeight();
			if(subBitmapRect == null || this.prefScrollingEnabled){ //update the sub-bitmap rectangle to draw
				if(this.prefScrollingEnabled){
					subBitmapRect = computeBitmapRectToDraw(dst,this.xOffset);
				} else {
					subBitmapRect = computeBitmapRectToDraw(dst,this.CENTERED_OFFSET);
				}
				// if(BuildConfig.DEBUG) Log.d(TAG, 
				//	"HERE: src ("+(subBitmapRect.right - subBitmapRect.left)+"x"+(subBitmapRect.bottom - subBitmapRect.top)+") " +
				//	"dst ("+(dst.right - dst.left)+"x"+(dst.bottom - dst.top)+")");
			}
			canvas.drawBitmap(background, subBitmapRect, dst, null);
		}
	    //drawing text
	    canvas.drawText(text, textPosition.x, textPosition.y, textPainter);
		
		surfaceHolder.unlockCanvasAndPost(canvas);
	}
	
}
