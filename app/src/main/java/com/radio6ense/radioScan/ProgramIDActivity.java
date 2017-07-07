package com.radio6ense.radioScan;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDLogicalSource;
import com.caen.RFIDLibrary.CAENRFIDReader;
import com.caen.RFIDLibrary.CAENRFIDTag;
import com.radio6ense.radioScan.utils.HexWatcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ProgramIDActivity extends Activity {

	private static final int DIALOG_PROGRAMID_ADVISE = 0;

	private EditText et;
	private EditText pwd;
	private CheckBox cb;
	private CheckBox force;
	private CAENRFIDReader mReader;
	private String mTagHex;
	private String mTagASCII;
	private TextView mTagSelected; 
	private HexWatcher mPwdWatcher;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.program_id_activity);
		Bundle b=this.getIntent().getExtras();
		mTagHex=b.getString("TAG_HEX");
		mTagASCII=b.getString("TAG_ASCII");
		mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
		et=(EditText)this.findViewById(R.id.program_tag_id_edittext);
		force=(CheckBox)this.findViewById(R.id.program_tag_force_id);
		cb=(CheckBox)this.findViewById(R.id.program_tag_ascii_input_checkbox);
		mTagSelected=(TextView)this.findViewById(R.id.tag_selected);
		mTagSelected.setText(Preferences.sAsciiOn?mTagASCII:mTagHex);
		cb.setChecked(Preferences.sAsciiOn);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				String id=et.getText().toString();
				if(!isChecked){
					if ( !InventoryModeActivity.isHexDigit(id) || ((id.length()%2)!=0)){
						et.setTextColor(Color.RED);
					}else{
						et.setTextColor(Color.BLACK);
					}
				}else{
					et.setTextColor(Color.BLACK);
				}
				
			}
			
		});
		pwd=(EditText)this.findViewById(R.id.program_tag_pwd_edittext);
		mPwdWatcher=new HexWatcher(pwd, cb, false, false);
		pwd.addTextChangedListener(mPwdWatcher);
		et.setText(cb.isChecked()?mTagASCII:mTagHex);
		et.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				String id=s.toString();
				if(s.equals(""))
					return;
				if(!cb.isChecked()){
					if ( !InventoryModeActivity.isHexDigit(id) || ((id.length()%2)!=0)){
						et.setTextColor(Color.RED);
					}else{
						et.setTextColor(Color.BLACK);
					}
				}
				
			}
		});
	}
	@Override
	public void onBackPressed() {
		setResult(ReadAndWriteActivity.CLEAR_ON_CHANGE_EPC);
		finish();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	public void ProgramID(View v){
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Warning:In presence of more tag, behaviour is undefined, so be sure there's only one tag under the antenna/s. Proceede?");
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				if(mPwdWatcher.isMalformed()){
					Toast.makeText(getApplicationContext(), "Check password and retry!", Toast.LENGTH_SHORT).show();
					return;
				}
				CAENRFIDLogicalSource s0 = null;
				try {
					s0 = mReader.GetSource("Source_0");
				} catch (CAENRFIDException e1) {
				}
				String pw=pwd.getText().toString();
				String sid=et.getText().toString();
				if(sid.equals("")){
					Toast.makeText(getApplicationContext(), "ID cannot be empty!", Toast.LENGTH_SHORT).show();
					return;
				}
				byte[] new_id=null;
				if(cb.isChecked()){
					new_id=RFIDTag.ASCIIStringToASCIIByteArray(sid);
				}else{
					if((et.length()%2)==1){ //if odd
						Toast.makeText(getApplicationContext(), "Check the new tag id, and retry", Toast.LENGTH_SHORT).show();
						return;
					}
					new_id=InventoryModeActivity.isHexDigit(sid)?RFIDTag.hexStringToByteArray(sid):null;		   
				}
				if(new_id==null || new_id.length>=64){
					Toast.makeText(getApplicationContext(), "Insert a valid ID!", Toast.LENGTH_SHORT).show();
					return;
				}
				int ipwd=0;
				CAENRFIDTag[] tags;
				try {
					tags = s0.InventoryTag();
				} catch (CAENRFIDException e) {
					Toast.makeText(getApplicationContext(), "Communication Error", Toast.LENGTH_SHORT).show();
					return;
				}
				if( ( !(RFIDTag.toHexString(tags[0].GetId()).equals(mTagHex)) && force.isChecked() ) || (RFIDTag.toHexString(tags[0].GetId()).equals(mTagHex)) ){
					CAENRFIDTag new_tag;
					try {
						new_tag = new CAENRFIDTag(new_id,(short)new_id.length,s0,"Ant0");
						if(pwd.getText().toString().equals(""))
							s0.ProgramID_EPC_C1G2(new_tag, (short) 0);
						else
							s0.ProgramID_EPC_C1G2(new_tag, (short)0, Integer.valueOf(pwd.getText().toString(), 16));
					} catch (CAENRFIDException e) {
						if(e.getMessage().contains("writing")) ///Error writing in tag
							Toast.makeText(getApplicationContext(), "Some error occurred during programming", Toast.LENGTH_SHORT).show();
						return;
					}

				}else{
					if(!force.isChecked())
						Toast.makeText(getApplicationContext(), "Tag is not the selected one. Check Force Programming?", Toast.LENGTH_SHORT).show();
					return;
				}
				setResult(ReadAndWriteActivity.CLEAR_ON_CHANGE_EPC);
				finish();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	protected Dialog onCreateDialog(int id) {
	    Dialog dialog = null;
	    switch(id) {
	    case DIALOG_PROGRAMID_ADVISE:
	    	
	        break;
	    default:
	        dialog = null;
	    }
	    return dialog;
	}
}
