package com.radio6ense.radioScan;

import java.util.ArrayList;
import java.util.List;

import com.caen.RFIDLibrary.CAENRFIDPort;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class InventoryModeActivity extends Activity {

	private CheckBox mEnableRssi;
	private CheckBox mButtonMode;
	private CheckBox mEnableMask;
	private CheckBox mASCIIMask;
	private CheckBox mASCIISelectedMask;
	
	private RadioButton mSelectTagList;
	private RadioButton mInputTagMask;
	
	private Spinner mTagList;
	
	private EditText mTargetMask;
	
	private RadioButton mMatch;
	private RadioButton mNotMatch;
	private RadioButton mBothMatch;
	
	private TextView mStartByteMaskResult;
	private Button mStartByteMaskForward;
	private Button mStartByteMaskBack;
	
	private static List<String> sNoTags;
	
	private ArrayList<String> mTagsHex;
	private ArrayList<String> mTagsASCII;
	
	protected DemoReader mReader;
	
	public static boolean isHexDigit(String hexDigit)
	{
		char[] hexDigitArray = hexDigit.toCharArray();
		int hexDigitLength = hexDigitArray.length;

		boolean isNotHex;
		for (int i = 0; i < hexDigitLength; i++) {
			isNotHex = Character.digit(hexDigitArray[i], 16) == -1;
			if (isNotHex) {
				return false;
			}
		}

		return true;
	}
	
	
	
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
		this.setContentView(R.layout.inventory_mode);
		
		IntentFilter disc_filt = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		this.registerReceiver(mReceiverBTDisconnect,disc_filt );
		
		IntentFilter disc_filt2= new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		this.registerReceiver(mReceiverWFDisconnect,disc_filt2 );
		
		IntentFilter disc_filt3= new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		this.registerReceiver(mReceiverBTDisconnect,disc_filt3 );
		
		mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader);
		
		
		Bundle b=this.getIntent().getExtras();
		this.mTagsHex=b.getStringArrayList("TAGS_HEX");
		this.mTagsASCII=b.getStringArrayList("TAGS_ASCII");
		if(sNoTags==null){
			sNoTags=new ArrayList<String>(1);
			sNoTags.add("no tags to refer");
		}
		mEnableRssi=(CheckBox)this.findViewById(R.id.Inventory_mode_rssi_checkbox);
		mButtonMode=(CheckBox)this.findViewById(R.id.Inventory_mode_button_checkbox);
		mEnableMask=(CheckBox)this.findViewById(R.id.inventory_mode_mask_checkbox);
		mASCIIMask=(CheckBox)this.findViewById(R.id.target_ascii_mask_checkbox);
		mASCIISelectedMask=(CheckBox)this.findViewById(R.id.target_selected_ascii_checkbox);
		mSelectTagList=(RadioButton)this.findViewById(R.id.target_by_select_tag_radiobutton);
		mInputTagMask=(RadioButton)this.findViewById(R.id.target_by_mask_radiobutton);
		mTagList=(Spinner)this.findViewById(R.id.target_by_select_tag_spinner2);
		mTargetMask=(EditText)this.findViewById(R.id.target_mask_text);
		mMatch=(RadioButton)this.findViewById(R.id.target_match_radiobutton);
		mNotMatch=(RadioButton)this.findViewById(R.id.target_not_match_radiobutton);
		mBothMatch=(RadioButton)this.findViewById(R.id.target_both_match_radiobutton);
		mStartByteMaskResult=(TextView)this.findViewById(R.id.start_byte_mask_textview);
		mStartByteMaskForward=(Button)this.findViewById(R.id.forward_byte_button);
		mStartByteMaskBack=(Button)this.findViewById(R.id.back_byte_button);
		
		mEnableRssi.setChecked(InventoryMode.isRSSIActive);
		mButtonMode.setChecked(InventoryMode.isTriggerActive);
		mEnableMask.setChecked(InventoryMode.isMaskActive);
		mSelectTagList.setChecked(InventoryMode.isSelected);
		mInputTagMask.setChecked(!InventoryMode.isSelected);
		mASCIISelectedMask.setChecked(InventoryMode.isASCIImask);
		SpinnerAdapter sa=(SpinnerAdapter)new ArrayAdapter(this.getApplicationContext(),android.R.layout.simple_spinner_item, InventoryMode.isASCIImask?this.mTagsASCII:this.mTagsHex);
		mTagList.setAdapter(sa);
		//se [(la maschera c') e (corrisponde ad un item dello spinner)]
		if(InventoryMode.mask!=null && mTagList.getCount()!=0){
			//Riempire lo spinner (in ASCII se Preferences.sASCII  attivo)
			
			boolean found=false;
			for(int i=0;i<mTagList.getCount();i++){
				if(InventoryMode.mask.equals(mTagList.getItemAtPosition(i))){
					mTagList.setPrompt((CharSequence) mTagList.getItemAtPosition(i));
					found=true;
					break;
				}
			}
			if(!found){
				mTagList.setPrompt((String) mTagList.getItemAtPosition(0));
				//lo scrive nella maschera
				mTargetMask.setText(InventoryMode.isASCIImask?RFIDTag.toASCII(InventoryMode.mask):RFIDTag.toHexString(InventoryMode.mask));
			}
		}
		
		mASCIIMask.setChecked(InventoryMode.ASCIIinput);
		switch(InventoryMode.maskMatch){
		case 0:
			mMatch.setChecked(true);
			break;
		case 1:
			mNotMatch.setChecked(true);
			break;
		case 2:
			mBothMatch.setChecked(true);
			break;
		default:
			break;
		}
		mStartByteMaskResult.setText(Integer.toString(InventoryMode.startMatchPosition));
		//-----------------HANDLER INIT
		mEnableRssi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				InventoryMode.isRSSIActive=isChecked;
			}
			
		});
		mButtonMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				InventoryMode.isTriggerActive=isChecked;
			}
		});
		mEnableMask.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				InventoryMode.isMaskActive=isChecked;
			}
			
		});
		mASCIISelectedMask.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				InventoryMode.isASCIIFormat=isChecked;
				SpinnerAdapter sa=(SpinnerAdapter)new ArrayAdapter(getApplicationContext(), android.R.layout.simple_spinner_item, InventoryMode.isASCIIFormat?mTagsASCII:mTagsHex);
				mTagList.setAdapter(sa);
			}
		});
		mASCIIMask.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				InventoryMode.ASCIIinput=isChecked;
				String mask=mTargetMask.getText().toString();
				if(!isChecked){
					if(((mask.length() % 2)!=0) || (!isHexDigit(mask)))
							mTargetMask.setTextColor(Color.RED);
				}else{
					mTargetMask.setTextColor(Color.BLACK);
				}
			}
		});
		mMatch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked)
					InventoryMode.maskMatch=0;	
			}
		});
		mNotMatch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked)
					InventoryMode.maskMatch=1;	
			}
		});
		mBothMatch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(isChecked)
					InventoryMode.maskMatch=2;	
			}
		});
		mStartByteMaskForward.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String mask;
				if(mInputTagMask.isChecked()){
					mask=mTargetMask.getText().toString();
					if(((InventoryMode.ASCIIinput) && (InventoryMode.startMatchPosition+1>mask.length())) ||
							((!InventoryMode.ASCIIinput) && (InventoryMode.startMatchPosition+1>=mask.length()/2))){
						mStartByteMaskResult.setTextColor(Color.RED);
					}else{
						mStartByteMaskResult.setTextColor(Color.WHITE);
					}
				}else{
					mask=(String) mTagList.getSelectedItem();
					if(((InventoryMode.isASCIIFormat) && (InventoryMode.startMatchPosition+1>mask.length())) ||
							((!InventoryMode.isASCIIFormat) && (InventoryMode.startMatchPosition+1>=mask.length()/2))){
						mStartByteMaskResult.setTextColor(Color.RED);
					}else{
						mStartByteMaskResult.setTextColor(Color.WHITE);
					}
				}
					InventoryMode.startMatchPosition++;
					mStartByteMaskResult.setText(Integer.toString(InventoryMode.startMatchPosition));
			}
		});
		mStartByteMaskBack.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(InventoryMode.startMatchPosition==0)
					return;
				String mask;
				if(mInputTagMask.isChecked()){
					mask=mTargetMask.getText().toString();
					if(((InventoryMode.ASCIIinput) && (InventoryMode.startMatchPosition-1>=mask.length())) ||
							((!InventoryMode.ASCIIinput) && (InventoryMode.startMatchPosition-1>=mask.length()/2))){
						mStartByteMaskResult.setTextColor(Color.RED);
					}else{
						mStartByteMaskResult.setTextColor(Color.WHITE);
					}
				}else{
					mask=(String) mTagList.getSelectedItem();
					if(((InventoryMode.isASCIIFormat) && (InventoryMode.startMatchPosition-1>=mask.length())) ||
							((!InventoryMode.isASCIIFormat) && (InventoryMode.startMatchPosition-1>=mask.length()/2))){
						mStartByteMaskResult.setTextColor(Color.RED);
					}else{
						mStartByteMaskResult.setTextColor(Color.WHITE);
					}
				}
				InventoryMode.startMatchPosition--;
				mStartByteMaskResult.setText(Integer.toString(InventoryMode.startMatchPosition));
			}
		});
		mTargetMask.addTextChangedListener(new TextWatcher() {

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
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String mask=s.toString();
				
				if(!mASCIIMask.isChecked()){
					if ( !isHexDigit(mask) || ((mask.length()%2)!=0)){
						mTargetMask.setTextColor(Color.RED);
					}else{
						mTargetMask.setTextColor(Color.BLACK);
					}
				}
				
			}
		});
		mSelectTagList.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if(mTagList.getCount()!=0){
					if(isChecked){
						if(InventoryMode.startMatchPosition==0)
							return;
						String mask;
						if(mInputTagMask.isChecked()){
							mask=mTargetMask.getText().toString();
							if(((InventoryMode.ASCIIinput) && (InventoryMode.startMatchPosition>=mask.length())) ||
									((!InventoryMode.ASCIIinput) && (InventoryMode.startMatchPosition>=mask.length()/2))){
								mStartByteMaskResult.setTextColor(Color.RED);
							}else{
								mStartByteMaskResult.setTextColor(Color.WHITE);
							}
						}else{
							mask=(String) mTagList.getSelectedItem();
							if(((InventoryMode.isASCIIFormat) && (InventoryMode.startMatchPosition>=mask.length())) ||
									((!InventoryMode.isASCIIFormat) && (InventoryMode.startMatchPosition>=mask.length()/2))){
								mStartByteMaskResult.setTextColor(Color.RED);
							}else{
								mStartByteMaskResult.setTextColor(Color.WHITE);
							}
						}
					}
				}else{
					mInputTagMask.setChecked(true);
				}
			}
		});
		
		if(InventoryMode.mask!=null)
			if(InventoryMode.startMatchPosition>InventoryMode.mask.length){
				InventoryMode.startMatchPosition=0;
				Toast.makeText(getApplicationContext(), "invalid match position", Toast.LENGTH_SHORT).show();
			}
		if(mTagList.getCount()==0)
			mSelectTagList.setChecked(true);
	}
	@Override
	public void onBackPressed() {
		String sMask=mTargetMask.getText().toString();
		if(mInputTagMask.isChecked()){
			if(InventoryMode.ASCIIinput && !isHexDigit(sMask)){
				InventoryMode.mask=null;
				Toast.makeText(getApplicationContext(), "no valid mask selected", Toast.LENGTH_SHORT).show();
			}else{
				InventoryMode.mask=InventoryMode.ASCIIinput?RFIDTag.ASCIIStringToASCIIByteArray(sMask):RFIDTag.hexStringToByteArray(sMask);
				InventoryMode.isASCIImask=InventoryMode.ASCIIinput;
			}
		}else{
			if(mTagList.getCount()!=0){
				sMask=(String)mTagList.getSelectedItem();
				InventoryMode.mask=InventoryMode.isASCIIFormat?RFIDTag.ASCIIStringToASCIIByteArray(sMask):RFIDTag.hexStringToByteArray(sMask);
				InventoryMode.isASCIImask=InventoryMode.isASCIIFormat;
			}else{
				InventoryMode.mask=null;
				Toast.makeText(getApplicationContext(), "no valid mask selected", Toast.LENGTH_SHORT).show();
			}
		}
		if(InventoryMode.mask!=null)
			if(InventoryMode.startMatchPosition>InventoryMode.mask.length){
				InventoryMode.startMatchPosition=0;
				Toast.makeText(getApplicationContext(), "invalid match position", Toast.LENGTH_SHORT).show();
			}
		super.onBackPressed();
		
	}

	@Override
	protected void onDestroy() {
		this.unregisterReceiver(mReceiverBTDisconnect);
		this.unregisterReceiver(mReceiverWFDisconnect);
		super.onDestroy();
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

}
