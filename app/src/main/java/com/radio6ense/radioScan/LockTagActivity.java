package com.radio6ense.radioScan;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDLogicalSource;
import com.caen.RFIDLibrary.CAENRFIDReader;
import com.caen.RFIDLibrary.CAENRFIDTag;
import com.radio6ense.radioScan.utils.HexWatcher;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputFilter;

public class LockTagActivity extends Activity {

	private CAENRFIDReader mReader;
	private CAENRFIDLogicalSource mSource;
	private TextView mSelectedTag;
	private Spinner mBankSpinner;
	private Spinner mActionSpinner;
	private Button mApplyLockRuleButton;
	private Button mSetPasswordButton;
	private EditText mLockPwd;
	private EditText mAccessPwd;
	private EditText mKillPwd;
	private String mTagHex;
	private String mTagASCII;
	private Dialog mLockDialog;
	private InputFilter [] pwd_filter=new InputFilter[1];
	private HexWatcher pwdWatcher;
	private HexWatcher accessPwdWatcher;
	private HexWatcher killPwdWatcher;
	private int mPwd;
	private int mPayload;
	private class LockTagTask extends AsyncTask<Object, Boolean, Boolean> {


		
		protected Boolean doInBackground(Object... pars) {
			byte [] tag_id=RFIDTag.hexStringToByteArray(mTagHex);
			CAENRFIDTag TheTag = null;
			try {
				TheTag = new CAENRFIDTag(tag_id, (short) tag_id.length, mSource, "Ant0");
			} catch (CAENRFIDException e) {
				e.printStackTrace();
				return false;
			}
			try{
				if (mPwd == 0)
				{
					mSource.LockTag_EPC_C1G2(TheTag, mPayload);
				}
				else
				{
					mSource.LockTag_EPC_C1G2(TheTag, mPayload, (int)mPwd);
				}
			}catch(CAENRFIDException err){
				err.printStackTrace();
				return false;
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
			return true;
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected void onPostExecute(Boolean result){
			if(result)
				Toast.makeText(getApplicationContext(), "Tag successfully locked!", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(getApplicationContext(), "Tag locking error!", Toast.LENGTH_SHORT).show();
			mLockDialog.dismiss();
		}
	}
private class SetPwdTask extends AsyncTask<Object, Boolean, Boolean> {


		
		protected Boolean doInBackground(Object... pars) {
			byte [] tag_id=RFIDTag.hexStringToByteArray(mTagHex);
			CAENRFIDTag TheTag = null;
			try {
				TheTag = new CAENRFIDTag(tag_id, (short) tag_id.length, mSource, "Ant0");
			} catch (CAENRFIDException e) {
				e.printStackTrace();
				return false;
			}
			try{
				RFIDTag.WriteWithRetry(TheTag, (short)0, (short)0, RFIDTag.intToByteArray(Integer.valueOf(mKillPwd.getText().toString(),16)));
				RFIDTag.WriteWithRetry(TheTag, (short)0, (short)4, RFIDTag.intToByteArray(Integer.valueOf(mAccessPwd.getText().toString(),16)));
			}catch(CAENRFIDException err){
				err.printStackTrace();
				return false;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected void onPostExecute(Boolean result){
			if(result)
				Toast.makeText(getApplicationContext(), "Setting passwords succesfully!.", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(getApplicationContext(), "Error during set password.Please retry or check connection.", Toast.LENGTH_SHORT).show();
			mLockDialog.dismiss();
		}
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock_tag_activity);
        
        mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
        try {
			mSource=mReader.GetSource("Source_0");
		} catch (CAENRFIDException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//Get Tag
		Bundle b=this.getIntent().getExtras();
		mTagHex=b.getString("TAG_HEX");
		mTagASCII=b.getString("TAG_ASCII");
		
		mSelectedTag=(TextView)this.findViewById(R.id.tag_selected);
		mSelectedTag.setText(Preferences.sAsciiOn?mTagASCII:mTagHex);
		mBankSpinner=(Spinner)this.findViewById(R.id.lock_tag_bank_spinner);
		mActionSpinner=(Spinner)this.findViewById(R.id.lock_tag_action_spinner);
		mApplyLockRuleButton=(Button)this.findViewById(R.id.lock_tag_apply_button);
		mSetPasswordButton=(Button)this.findViewById(R.id.lock_tag_setpwd_button);
		mLockPwd=(EditText)this.findViewById(R.id.lock_tag_pwd);
		mAccessPwd=(EditText)this.findViewById(R.id.lock_tag_accesspwd);
		mKillPwd=(EditText)this.findViewById(R.id.lock_tag_killpwd);
		
		pwd_filter[0]=new InputFilter.LengthFilter(8);
		pwdWatcher=new HexWatcher(mLockPwd, null, false, false);
		accessPwdWatcher=new HexWatcher(mAccessPwd, null, false, false);
		killPwdWatcher=new HexWatcher(mKillPwd, null, false, false);
		
		
		mLockPwd.setFilters(pwd_filter);
		mLockPwd.setWidth((int)(mLockPwd.getTextSize()*8));
		mLockPwd.addTextChangedListener(pwdWatcher);
		
		mAccessPwd.setFilters(pwd_filter);
		mAccessPwd.setWidth((int)(mLockPwd.getTextSize()*8));
		mAccessPwd.addTextChangedListener(accessPwdWatcher);
		
		mKillPwd.setFilters(pwd_filter);
		mKillPwd.setWidth((int)(mLockPwd.getTextSize()*8));
		mKillPwd.addTextChangedListener(killPwdWatcher);
		
		mBankSpinner.setSelection(0);
		mActionSpinner.setSelection(0);
		
		mApplyLockRuleButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(pwdWatcher.isMalformed()){
					Toast.makeText(getApplicationContext(), "Check password and retry", Toast.LENGTH_SHORT).show();
					return;
				}
				try
				{
					int pwd = 0;
					int Action = 0;
					int Payload = 0;
					String action=(String)mActionSpinner.getSelectedItem();
					String payload=(String)mBankSpinner.getSelectedItem();
					if(action.equals("ACCESSIBLE"))
						Action = 0;
					else if(action.equals("PERMANENT ACCESSIBLE"))
						Action = 1;
					else if(action.equals("ACCESSIBLE ON SECURE"))
						Action = 2;
					else
						Action = 3;
					
					if(payload.equals("RESERVED(KILL PWD)"))
						mPayload = 0x000C0000 | (Action << 8);
					else if(payload.equals("RESERVED(ACCESS PWD)"))
						mPayload = 0x00030000 | (Action << 6);
					else if(payload.equals("EPC"))
						mPayload = 0x0000C000 | (Action << 4);
					else if(payload.equals("TID"))
						mPayload = 0x00003000 | (Action << 2);
					else
						mPayload = 0x00000C00 | Action;
					System.out.println("Payload:"+String.format("%04X", mPayload));
					try
					{
						mPwd = Integer.valueOf(mLockPwd.getText().toString(), 16);
					}
					catch (Exception err)
					{
						if(!mLockPwd.getText().toString().equals("")){
							err.printStackTrace();
							return;
						}else
							mPwd=0;
					}
					AlertDialog.Builder builder = new AlertDialog.Builder(
							LockTagActivity.this);
					builder.setMessage(
							"Are you sure to lock this tag?")
							.setCancelable(false)
							.setPositiveButton("Yes",
									new DialogInterface.OnClickListener() {
								public void onClick(
										DialogInterface dialog, int id) {
									mLockDialog=ProgressDialog.show(LockTagActivity.this, "", 
					                        "Locking tag. Please wait...", true);
									new LockTagTask().execute(new Object());
								}
							})
							.setNegativeButton("No",
									new DialogInterface.OnClickListener() {
								public void onClick(
										DialogInterface dialog, int id) {
									dialog.cancel();
								}
							});
					AlertDialog alert = builder.create();
					alert.show();
				}catch(Exception err){
					err.printStackTrace();
					Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
				}
			}
		});
		mSetPasswordButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(accessPwdWatcher.isMalformed() || killPwdWatcher.isMalformed() ){
					Toast.makeText(getApplicationContext(), "Check kill or access password and retry", Toast.LENGTH_SHORT).show();
					return;
				}
				if(mAccessPwd.getText().toString().equals("") || mKillPwd.getText().toString().equals("")){
					Toast.makeText(getApplicationContext(), "Check kill or access password and retry", Toast.LENGTH_SHORT).show();
					return;
				}
				mLockDialog=ProgressDialog.show(LockTagActivity.this, "", 
                        "Setting passwords. Please wait...", true);
				new SetPwdTask().execute(new Object());
			}
			
		});
    }

    @Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_lock_tag, menu);
        return true;
    }

    
}
