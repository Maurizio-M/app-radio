package com.radio6ense.radioScan;

import java.util.StringTokenizer;

import android.app.Activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;


public class TCPSelection extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    // TODO Auto-generated method stub
	    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	    this.setContentView(R.layout.tcp_selection);
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}
	
	public void Cancel(View v){
    	finish();
    }
	
	public void SelectReader(View v){
		String ip=(String)((EditText)this.findViewById(R.id.tcp_address)).getText().toString();
		ip=ip.trim();
		if(isValidIP(ip)){
			Intent newIntent = new Intent();
			newIntent.putExtra("IP_ADDRESS", ip);
			setResult(RESULT_OK, newIntent);
			this.finish();
		}
	}
	
	private boolean isValidIP(String ip) {
		// TODO Auto-generated method stub
		int tmp;
		if(ip.length()<7 || ip.length()>15)
			return false;
		StringTokenizer st=new StringTokenizer(ip,".");
		if(st!=null){
			if(st.countTokens()==4){
				while(st.hasMoreTokens()){
					tmp=Integer.parseInt(st.nextToken());
					if(tmp>255 || tmp<0)
						return false;
				}
			}else
				return false;
		}
		return true;
		
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
	
}
