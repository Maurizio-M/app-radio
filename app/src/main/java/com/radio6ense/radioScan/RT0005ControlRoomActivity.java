package com.radio6ense.radioScan;

import java.nio.ByteBuffer;
import java.util.Date;

import com.caen.RFIDLibrary.*;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class RT0005ControlRoomActivity extends Activity {
	private String mTagHex;
	private String mTagASCII;
	private CAENRFIDReader mReader;
	private CAENRFIDLogicalSource mSource;
	private TextView mTagSelected;
	private ImageView mHistoImage;
	private ImageView mElapsedImage;
	private ImageView mMemoryImage;
	private TextView mShelfLifeValue;
	private TextView mKineticValue;
	private ImageView mBatteryImage;
	private TextView mShippingValue;
	private TextView mStopValue;
	private Button mRefreshButton;
	private TextView mMainTemp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rt0005_control_room_activity);
		Bundle b=this.getIntent().getExtras();
		mTagHex=b.getString("TAG_HEX");
		mTagASCII=b.getString("TAG_ASCII");
		mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
	    try {
			mSource=mReader.GetSource("Source_0");
		} catch (CAENRFIDException e1) {
			e1.printStackTrace();
		}
	    //take components
	    mTagSelected=(TextView)this.findViewById(R.id.tag_selected);
	    mMainTemp=(TextView)this.findViewById(R.id.rt0005_main_temperature);
	    mHistoImage=(ImageView)this.findViewById(R.id.rt0005_histo_image);
	    mElapsedImage=(ImageView)this.findViewById(R.id.rt0005_estimed_image);
	    
	    mMemoryImage=(ImageView)this.findViewById(R.id.rt0005_memory_image);
	    /*
	     * Only for complete layout.
	    mShelfLifeValue=(TextView)this.findViewById(R.id.rt0005_shelf_value);    
	    mKineticValue=(TextView)this.findViewById(R.id.rt0005_kinetic_label);
	    */
	    mBatteryImage=(ImageView)this.findViewById(R.id.rt0005_battery_image);
	    mShippingValue=(TextView)this.findViewById(R.id.rt0005_shipping_value);
	    mStopValue=(TextView)this.findViewById(R.id.rt0005_stop_value);
	    mRefreshButton=(Button)this.findViewById(R.id.rt0005_refresh_button);
	    
	    try {
			refreshSettings();
		} catch (CAENRFIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		mTagSelected.setText(Preferences.sAsciiOn?mTagASCII:mTagHex);
		
	}
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
	@Override
	public void finishActivity(int requestCode) {
		// TODO Auto-generated method stub
		super.finishActivity(requestCode);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	public void Refresh(View v){
		try {
			refreshSettings();
		} catch (CAENRFIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void refreshSettings() throws CAENRFIDException{
		CAENRFIDTag tag=null;
		byte[] id=RFIDTag.hexStringToByteArray(mTagHex);
		tag=new CAENRFIDTag(id,(short)id.length,mSource);
		//main temperature
		byte[] mtb=mSource.ReadTagData_EPC_C1G2(tag, (short)3, (short)204, (short)2); //(0x66*2) byte
		ByteBuffer bb=ByteBuffer.wrap(mtb);
		short mt=bb.getShort();//8.5 fixed
		short sign=(short)(mt>>>12);	// sign bit position =(8+5) -1 =12
		float mtf=sign==0?(((float)mt)/32):(((float)(mt-8192))/32);
		
		mMainTemp.setText(String.format("%.1f", mtf)+" C");
		
		byte[] alarms= mSource.ReadTagData_EPC_C1G2(tag, (short)3, (short)162, (short)2); //alarm bits
		
		if((alarms[1] & 0x10) == 0x10) //alarm on
			mHistoImage.setImageResource(R.drawable.led_rosso);
		else
			mHistoImage.setImageResource(R.drawable.led_verde);
		
		if((alarms[1] & 0x8) == 0x8)
			mElapsedImage.setImageResource(R.drawable.led_rosso);
		else
			mElapsedImage.setImageResource(R.drawable.led_verde);
		
		if((alarms[1] & 0x4) == 0x4)
			mMemoryImage.setImageResource(R.drawable.led_rosso);
		else
			mMemoryImage.setImageResource(R.drawable.led_verde);
		
		byte battState=(byte)(alarms[1] & 0x3);
		
		switch(battState){
		case 0:
			this.mBatteryImage.setImageResource(R.drawable.batteria_scarica);
			break;
		case 1:
			this.mBatteryImage.setImageResource(R.drawable.batteria_bassa);
			break;
		case 2:
			this.mBatteryImage.setImageResource(R.drawable.batteria_carica);
			break;
		case 3:
			this.mBatteryImage.setImageResource(R.drawable.batteria_carica);
			break;
		}
		
		byte[] s_rem=mSource.ReadTagData_EPC_C1G2(tag, (short)3, (short)166, (short)4);
		/*
		 * Only for complete layout
		float shelftime=(float)( (s_rem[3]<<24)|(s_rem[2]<<16) | (s_rem[1]<<8) | (s_rem[0]));
		this.mShelfLifeValue.setText(shelftime+" h");
		if((alarms[1]& 0x40)==0x40)
			this.mShelfLifeValue.setTextColor(Color.RED);
		else
			this.mShelfLifeValue.setTextColor(Color.GREEN);
		
		byte[] kin=mSource.ReadTagData_EPC_C1G2(tag, (short)3, (short)164, (short)2);
		mt=(short)(kin[1]<<8|kin[0]); //8.5 fixed
		sign=(short)(mt>>>12);	// sign bit position =(8+5) -1 =12
		intpart=(short)(mt>>>5);	// integer part
		decpart=(short)(mt & 0x001F);
		this.mKineticValue.setText((sign>0?"-":"")+intpart+","+decpart+"C");
		if( (alarms[1] & 0x50)==0x50 )
			this.mKineticValue.setTextColor(Color.RED);
		else
			this.mKineticValue.setTextColor(Color.GREEN);
		*/
		byte []ship=mSource.ReadTagData_EPC_C1G2(tag, (short)3, (short)212, (short)4);
		byte []stop=mSource.ReadTagData_EPC_C1G2(tag, (short)3, (short)216, (short)4);
		
		ByteBuffer bbshipunixdate=ByteBuffer.wrap(new byte[]{ship[2],ship[3],ship[0],ship[1]});
		ByteBuffer bbstopunixdate=ByteBuffer.wrap(new byte[]{stop[2],stop[3],stop[0],stop[1]});
		
		Date shiptime=new Date((long)bbshipunixdate.getInt()*1000);
		Date stoptime=new Date((long)bbstopunixdate.getInt()*1000);
		
		this.mShippingValue.setText(shiptime.toGMTString());
		this.mStopValue.setText(stoptime.toGMTString());
		return;
		
	}
	
	

}
