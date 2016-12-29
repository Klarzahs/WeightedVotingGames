package util;

import java.text.DecimalFormat;

public class Log {
	private final static boolean DEBUG = true;
	
	public static void d(String s){
		if(DEBUG)
			System.out.println(s);
	}
	
	public static void d(double d){
		String s;
		if(d == 0){
			s = "0.00";
		}else if(d == 1){
			s = "1.00";
		}else {
			DecimalFormat df = new DecimalFormat("#.##");
			s = df.format(d);			
		}
		if(s.length() == 3)
			s = s + "0";
		System.out.print(s);
	}
	
	
	public static void d(double[][] connections){
		for(int i = 0; i < connections.length; i++){
			for(int j = 0; j < connections[i].length; j++){
				Log.d(connections[i][j]);
				System.out.print(", ");
			}
			System.out.println("");
		}
	}
	
	public static void e(String s){
		System.err.println(s);
	}

	public static void d(int[] weights) {
		for(int w : weights){
			System.out.print(w+", ");
		}
		System.out.println();
	}
}
