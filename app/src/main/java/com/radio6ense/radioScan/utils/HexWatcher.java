package com.radio6ense.radioScan.utils;

import com.radio6ense.radioScan.InventoryModeActivity;

import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;

public class HexWatcher implements TextWatcher {

	private EditText mTWOwner;
	private CheckBox mASCIIDesclaimer;
	private boolean IsMalformed;
	private boolean fullExplicit;//true if 4 hex char fixed (ex. "0002"), false if not (ex. "2)
	
	public HexWatcher(EditText et,CheckBox asciiDesclaimer,boolean isMalformed){
		this.mTWOwner=et;
		this.mASCIIDesclaimer=asciiDesclaimer;
		this.setIsMalformed(isMalformed);
		this.fullExplicit=true;
	}
	public HexWatcher(EditText et,CheckBox asciiDesclaimer,boolean isMalformed,boolean explicitFormat){
		this.mTWOwner=et;
		this.mASCIIDesclaimer=asciiDesclaimer;
		this.setIsMalformed(isMalformed);
		this.fullExplicit=explicitFormat;
	}
	public HexWatcher(EditText et,boolean isMalformed,boolean explicitFormat){
		this.mTWOwner=et;
		this.mASCIIDesclaimer=null;
		this.setIsMalformed(isMalformed);
		this.fullExplicit=explicitFormat;
	}
	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub
		String id=this.mTWOwner.getText().toString();
		if(id.equals("????"))
			this.mTWOwner.setTextColor(Color.GRAY);
		if(this.mASCIIDesclaimer==null){
			if(this.fullExplicit){
				if ( !InventoryModeActivity.isHexDigit(id) || ((id.length()%2)!=0)){
					this.mTWOwner.setTextColor(Color.RED);
					this.setIsMalformed(true);
				}else{
					this.mTWOwner.setTextColor(Color.BLACK);
					this.setIsMalformed(false);
				}
			}else{
				try{
					Integer.valueOf(id,16);
					this.mTWOwner.setTextColor(Color.BLACK);
					this.setIsMalformed(false);
				}catch(NumberFormatException nfe){
					this.mTWOwner.setTextColor(Color.RED);
					this.setIsMalformed(true);
				}
			}
		}
		else if(!this.mASCIIDesclaimer.isChecked()){
			if(this.fullExplicit){
				if ( !InventoryModeActivity.isHexDigit(id) || ((id.length()%2)!=0)){
					this.mTWOwner.setTextColor(Color.RED);
					this.setIsMalformed(true);
				}else{
					this.mTWOwner.setTextColor(Color.BLACK);
					this.setIsMalformed(false);
				}
			}else{
				try{
					Integer.valueOf(id,16);
					this.mTWOwner.setTextColor(Color.BLACK);
					this.setIsMalformed(false);
				}catch(NumberFormatException nfe){
					this.mTWOwner.setTextColor(Color.RED);
					this.setIsMalformed(true);
				}
			}
		}else{
			this.mTWOwner.setTextColor(Color.BLACK);
			if(id.equalsIgnoreCase(""))
				this.setIsMalformed(true);
			else
				this.setIsMalformed(false);
		}
	}
	public boolean isMalformed() {
		return IsMalformed;
	}
	public void setIsMalformed(boolean isMalformed) {
		IsMalformed = isMalformed;
	}

}
