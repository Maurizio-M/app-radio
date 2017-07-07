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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.InputFilter;

	
public class KillTagActivity extends Activity {
	private CAENRFIDReader mReader;
	private CAENRFIDLogicalSource mSource;
	Button mKillButton;
	EditText mKillPwd;
	ProgressDialog mKillDialog;
	private String mTagHex;
	private String mTagASCII;
	private TextView mTagSelected;
	private HexWatcher killPwdWatcher;
	private InputFilter[] pwd_filter;
	private int mPwd;
	private class KillTagTask extends AsyncTask<Object, Boolean, Boolean> {
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
				mSource.KillTag_EPC_C1G2(TheTag, mPwd);
			}catch(CAENRFIDException err){
				err.printStackTrace();
				return false;
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return false;
			}
			setResult(ReadAndWriteActivity.CLEAR_ON_CHANGE_EPC);
			finish();
			return true;
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected void onPostExecute(Boolean result){
			if(!result)
				Toast.makeText(getApplicationContext(), "Tag killing error!", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(getApplicationContext(), "Tag successfully killed!", Toast.LENGTH_SHORT).show();
				
			mKillDialog.dismiss();
		}
	}

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kill_tag_activity);
        Bundle b=this.getIntent().getExtras();
		mTagHex=b.getString("TAG_HEX");
		mTagASCII=b.getString("TAG_ASCII");
		mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
	    try {
			mSource=mReader.GetSource("Source_0");
		} catch (CAENRFIDException e1) {
			e1.printStackTrace();
		}
		mKillPwd=(EditText)this.findViewById(R.id.kill_tag_killpwd);
		mKillButton=(Button)this.findViewById(R.id.kill_tag_kill_button);
		mTagSelected=(TextView)this.findViewById(R.id.tag_selected);
        
        killPwdWatcher=new HexWatcher(mKillPwd, null, false, false);
        pwd_filter=new InputFilter[1];
        pwd_filter[0]=new InputFilter.LengthFilter(8);
        mKillPwd.setFilters(pwd_filter);
		mKillPwd.setWidth((int)(mKillPwd.getTextSize()*8));
		mKillPwd.addTextChangedListener(killPwdWatcher);
		mKillButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(killPwdWatcher.isMalformed()){
					Toast.makeText(getApplicationContext(), "Check password and retry", Toast.LENGTH_SHORT).show();
					return;
				}
				try
				{
					try
					{
						mPwd = Integer.valueOf(mKillPwd.getText().toString(), 16);
					}
					catch (Exception err)
					{
						if(!mKillPwd.getText().toString().equals("")){
							err.printStackTrace();
							return;
						}else
							mPwd=0;
					}
					AlertDialog.Builder builder = new AlertDialog.Builder(
							KillTagActivity.this);
					builder.setMessage(
							"Are you sure to kill this tag?")
							.setCancelable(false)
							.setPositiveButton("Yes",
									new DialogInterface.OnClickListener() {

								public void onClick(
										DialogInterface dialog, int id) {
									mKillDialog=ProgressDialog.show(KillTagActivity.this, "", 
					                        "Killing tag. Please wait...", true);
									new KillTagTask().execute(new Object());
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.kill_tag_activity, menu);
        return true;
    }

    
}
