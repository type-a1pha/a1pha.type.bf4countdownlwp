package com.a1pha.type.bf4.countdown.lwp;

/**
 * Util class for mapping names to resources ids
 * @author IKAROS
 *
 */
public class ResourceMapping {
	
	public static int[] colors = new int[] {
		Constants.GREEN,
		Constants.BLUE,
		Constants.INDIGO,
		Constants.VIOLET,
		Constants.RED,
		Constants.ORANGE,
		Constants.YELLOW
	};
	
	public static int drawableIdByPreferenceString(String str){
		if(str.equals("image_0.jpg")) return R.drawable.image_0;
		if(str.equals("image_1.jpg")) return R.drawable.image_1;
		if(str.equals("image_2.jpg")) return R.drawable.image_2;
		if(str.equals("image_3.jpg")) return R.drawable.image_3;
		if(str.equals("image_4.jpg")) return R.drawable.image_4;
		if(str.equals("image_5.jpg")) return R.drawable.image_5;
		if(str.equals("image_6.jpg")) return R.drawable.image_6;
		if(str.equals("image_7.jpg")) return R.drawable.image_7;
		if(str.equals("image_8.jpg")) return R.drawable.image_8;
		if(str.equals("image_9.jpg")) return R.drawable.image_9;
		if(str.equals("image_10.jpg")) return R.drawable.image_10;
		return R.drawable.image_0;
	}
	
	public static int[] getVariableColorsArray(){
		return colors;
	}
	
}
