package com.radio6ense.radioScan;

public class InventoryMode {
	public static boolean isMaskActive=false; // enable target radiobutton
	public static boolean isTriggerActive=false; //enable button radiobutton
	public static boolean isRSSIActive=false;//enable RSSI in inventory
	public static boolean isSelected=true; //true if choosed by spinner or false if by input mask.
	public static byte [] mask=null; //mask choosen
	public static boolean isASCIImask;//mask format type: ascii or hex.
	
	public static boolean isASCIIFormat=false;//spinner's tags format: ASCII or Hex String
	public static boolean ASCIIinput=false; //ascii edittext checkbox state
	public static int maskMatch=2; //0 match, 1 not match, 2 both
	public static int startMatchPosition=0; // byte from which start to compare the mask. 
}
