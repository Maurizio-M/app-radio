package com.radio6ense.radioScan;

import com.caen.RFIDLibrary.CAENRFIDPort;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.TabHost;

public class ConfigurationActivity extends TabActivity{
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
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reader_configuration_activity);
		
		mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader);

		IntentFilter disc_filt = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		this.registerReceiver(mReceiverBTDisconnect,disc_filt );
		
		IntentFilter disc_filt2= new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		this.registerReceiver(mReceiverWFDisconnect,disc_filt2 );
		
		IntentFilter disc_filt3= new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		this.registerReceiver(mReceiverBTDisconnect,disc_filt3 );
		
		Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost();  // The activity TabHost
		TabHost.TabSpec spec;  // Resusable TabSpec for each tab
		Intent intent;  // Reusable Intent for each tab

		// Create an Intent to launch an Activity for the tab (to be reused)
		intent = new Intent().setClass(this, ReaderConfigurationActivity.class);

		// Initialize a TabSpec for each tab and add it to the TabHost
		spec = tabHost.newTabSpec("configuration");
		spec.setIndicator("Configuration",res.getDrawable(R.drawable.ic_tab_reader_conf)).setContent(intent);
		tabHost.addTab(spec);

		// Do the same for the other tabs
		intent = new Intent().setClass(this, AntennaConfigurationActivity.class);
		spec = tabHost.newTabSpec("antennae");
		spec.setIndicator("Antennae",res.getDrawable(R.drawable.ic_tab_antenna)).setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, ProtocolConfigurationActivity.class);
		spec = tabHost.newTabSpec("protocol");
		spec.setIndicator("Protocol",res.getDrawable(R.drawable.ic_tab_protocol)).setContent(intent);
		tabHost.addTab(spec);
		
		tabHost.setCurrentTab(0);
	}

	@Override
	protected void onDestroy() {
		this.unregisterReceiver(mReceiverBTDisconnect);
		this.unregisterReceiver(mReceiverWFDisconnect);
		super.onDestroy();
	}

}
