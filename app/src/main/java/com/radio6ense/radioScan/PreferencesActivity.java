package com.radio6ense.radioScan;

import com.caen.RFIDLibrary.CAENRFIDPort;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

public class PreferencesActivity extends Activity implements ColorPickerDialog.OnColorChangedListener{
	private CheckBox beep;
	private CheckBox ascii;
	protected DemoReader mReader;
	
	private final BroadcastReceiver mReceiverBTDisconnect=new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if ((action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) && (!BluetoothAdapter
					.getDefaultAdapter().isEnabled()))
					|| (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))) {
				finish();
			}
		}
	};

	private final BroadcastReceiver mReceiverWFDisconnect=new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	     if(intent.getExtras()!=null) {
	    	 if(intent.getExtras().getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY,Boolean.FALSE)) {
	    		 if(mReader.getConnectionType().equals(CAENRFIDPort.CAENRFID_TCP))
	    			 finish();
		     }
	     }	     
	   }
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.preferences_activity);
		mReader = RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader);
		
		beep=(CheckBox) this.findViewById(R.id.beep_checkbox);
		ascii=(CheckBox) this.findViewById(R.id.ascii_checkBox);
		beep.setChecked(Preferences.sBeepOn);
		ascii.setChecked(Preferences.sAsciiOn);
		IntentFilter disc_filt = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		this.registerReceiver(mReceiverBTDisconnect,disc_filt );
		IntentFilter disc_filt2= new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		this.registerReceiver(mReceiverWFDisconnect,disc_filt2 );
		IntentFilter disc_filt3= new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		this.registerReceiver(mReceiverBTDisconnect,disc_filt3 );
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(mReceiverBTDisconnect);
		this.unregisterReceiver(mReceiverWFDisconnect);
	}

	@Override
	public void colorChanged(int color) {
		Preferences.sInventoryColor=color;
	}
	
	public void onChooseClick(View v){
		new ColorPickerDialog(this, this, Color.GREEN).show();
	}
	
	public void activateBeep(View v){
		Preferences.sBeepOn=beep.isChecked();
	}
	public void activateASCII(View v){
		Preferences.sAsciiOn=ascii.isChecked();
	}

	@Override
	public void onBackPressed() {
		this.setResult(ReadAndWriteActivity.CLEAR_ON_CHANGE_EPC);
		finish();
	}
}
