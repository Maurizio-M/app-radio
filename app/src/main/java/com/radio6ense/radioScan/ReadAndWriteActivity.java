package com.radio6ense.radioScan;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDLogicalSource;
import com.caen.RFIDLibrary.CAENRFIDReader;
import com.caen.RFIDLibrary.CAENRFIDTag;
import com.radio6ense.radioScan.utils.HexWatcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ReadAndWriteActivity extends Activity {

	public static int CLEAR_ON_CHANGE_EPC=1;
	
	private int WORD=2; //2 bytes
	private CAENRFIDReader mReader;
	private CAENRFIDLogicalSource mSource;
	private CAENRFIDTag mTag;
	private Spinner mBankSpinner;
	private CheckBox mASCIIInput;
	private TextView [] mAddress;
	private EditText [] mValue;
	private HexWatcher[] mValueWatcher;
	private EditText mPassword;
	private HexWatcher mHexWpw;
	private EditText mInitAddress;
	private HexWatcher mHexWia;
	private String mTagHex;
	private String mTagASCII;
	private TextView mSelectedTag;
	private boolean mChangeEPC;
	private Button mReadButton;
	private Button mWriteButton;
	
	private InputFilter [] filters1=new InputFilter[1];//2 char
	private InputFilter [] filters2=new InputFilter[1];//4 char
	private InputFilter [] filters3=new InputFilter[1];//8 char
	private ProgressDialog mLoadingDialog;

	
	
	private class ReaderTask extends AsyncTask<Object, Boolean, Boolean> {

		byte[] values=new byte[12]; //12 bytes
		boolean [] errors=new boolean[6]; //6 word
		boolean data_loss=false;
		
		protected Boolean doInBackground(Object... pars) {
			
			for(int i=0;i<6;i++){
				values[(i*2)]=0x00;
				values[(i*2)+1]=0x00;
				errors[i]=false;
			}
			
			//retrieve mem bank
			String [] banks=getResources().getStringArray(R.array.RWBank);
			String bank=(String)mBankSpinner.getSelectedItem();
			short mbank=0;
			if(bank.equals(banks[0])){
				mbank=0;
			}else if(bank.equals(banks[1])){
				mbank=1;
			}else if(bank.equals(banks[2])){
				mbank=2;
			}else{
				mbank=3;
			}
			byte [] id=RFIDTag.hexStringToByteArray(mTagHex);
            Log.d("Test", "ReadTask");
            //mTag=new CAENRFIDTag(id, (short) id.length, mSource, "Ant0");
            //EM4325TagData data = mSource.EM4325_GetSensorData(mTag, true, true);
            //Log.d("TASK DATA", RFIDTag.toHexString(data.GetSensorData()));
            short addr=Short.parseShort(mInitAddress.getText().toString(), 16);//2
            byte[] tmp=new byte[2];
            boolean data_loss=false;
            String pwds=mPassword.getText().toString();

            for(int i=0;i<6;i++){
                if(pwds.equals(""))
                    tmp=RFIDTag.ReadWithRetry(mTag, mbank, (short)((addr*WORD)+(WORD*i)), (short)(WORD));
                else
                    tmp=RFIDTag.ReadWithRetry(mTag, mbank, (short)((addr*WORD)+(WORD*i)), (short)(WORD), Integer.valueOf(pwds, 16));
                if(tmp!=null){
                    values[(i*2)]=tmp[0];
                    values[(i*2)+1]=tmp[1];
                }else{
                    errors[i]=true;
                    data_loss=true;
                }
            }
            return true;
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected void onPostExecute(Boolean result){
			short addr=Short.parseShort(mInitAddress.getText().toString(), 16);//2
			String tmps=null;
			String tmpa=null;
			for(int i=0; i<6; i++){
				if(errors[i]){
					mValue[i].setText("????");
					mValue[i].setTextColor(Color.GRAY);
				}else{
					tmps=(String)RFIDTag.toHexString(new byte[]{values[(i*2)],values[(i*2)+1]});
					tmpa=(String)RFIDTag.toASCII(new byte[]{values[(i*2)],values[(i*2)+1]});
					mValue[i].setText(mASCIIInput.isChecked()?tmpa:tmps);
					mValue[i].setTextColor(Color.BLACK);
				}
				mAddress[i].setText((String)String.format("%04X",addr+i)); //word oriented
			}
			if(data_loss){
				String bank=(String)mBankSpinner.getSelectedItem();
				Toast.makeText(getApplicationContext(), "Cannot read some data.Retry,change password/address,try to unlock "+bank+" bank,or restart app.", Toast.LENGTH_SHORT).show();
			}
			mLoadingDialog.dismiss();
		}
	}
	
	private class WriterTask extends AsyncTask<Object, Boolean, Boolean> {

		byte[] values=new byte[12]; //12 bytes
		boolean [] errors=new boolean[6]; //6 word
		boolean data_loss=false;
		
		protected Boolean doInBackground(Object... pars) {
			//retrieve mem bank
			String [] banks=getResources().getStringArray(R.array.RWBank);
			String bank=(String)mBankSpinner.getSelectedItem();
			short mbank=0;
			if(bank.equals(banks[0])){
				mbank=0;
			}else if(bank.equals(banks[1])){
				mbank=1;
			}else if(bank.equals(banks[2])){
				mbank=2;
			}else{
				mbank=3;
			}
			byte [] id=RFIDTag.hexStringToByteArray(mTagHex);
			byte[] tmp=null;
			byte[] writeValues=new byte[12];
			for(int i=0;i<6;i++){
				if((!mASCIIInput.isChecked() && mValueWatcher[i].isMalformed()) || mValue[i].getText().toString().equals("????"))
				{
					writeValues[(i*2)]=0x00;
					writeValues[(i*2) + 1]=0x00;
					continue;
				}
				tmp=mASCIIInput.isChecked()?RFIDTag.ASCIIStringToASCIIByteArray(mValue[i].getText().toString()):RFIDTag.hexStringToByteArray(mValue[i].getText().toString());
				writeValues[(i*2)]=tmp.length==1?0x0:tmp[0];
				writeValues[(i*2) + 1]=tmp.length==1?tmp[0]:tmp[1];
			}
			try {
				mTag=new CAENRFIDTag(id, (short) id.length, mSource, "Ant0");
				short addr=Short.parseShort(mInitAddress.getText().toString(), 16);//2
				String pwds=mPassword.getText().toString();
				if(pwds.equals(""))
					RFIDTag.WriteWithRetry(mTag, mbank,(short)((addr*WORD)) ,writeValues);
				else
					RFIDTag.WriteWithRetry(mTag, mbank,(short)((addr*WORD)) ,writeValues,Integer.valueOf(pwds, 16));
			} catch (CAENRFIDException e) {
				e.printStackTrace();
				data_loss=true;
			}
			return true;
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected void onPostExecute(Boolean result){
			mLoadingDialog.dismiss();
			if(data_loss){
				String bank=(String)mBankSpinner.getSelectedItem();
				Toast.makeText(getApplicationContext(), "Cannot write some data.Retry,change password/address,try to unlock "+bank+" bank,or do new inventory.", Toast.LENGTH_LONG).show();
			}
			if(mChangeEPC){
				setResult(ReadAndWriteActivity.CLEAR_ON_CHANGE_EPC);
				finish();
			}
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.read_and_write_activity);
		mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
		mChangeEPC=false;
		//Get Tag
		Bundle b=this.getIntent().getExtras();
		mTagHex=b.getString("TAG_HEX");
		mTagASCII=b.getString("TAG_ASCII");
		mReadButton=(Button)this.findViewById(R.id.read_and_write_read_button);
		mReadButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				//check if ther's malformed field
				if(mHexWpw.isMalformed()){
					Toast.makeText(getApplicationContext(), "Please correct password and retry", Toast.LENGTH_SHORT).show();
					return;
				}
				if(mHexWia.isMalformed()){
					Toast.makeText(getApplicationContext(), "Please check address and retry", Toast.LENGTH_SHORT).show();
					return;
				}
				mLoadingDialog=ProgressDialog.show(ReadAndWriteActivity.this, "", "Loading data...", true);
				new ReaderTask().execute(new Object());
			}
		});
		mWriteButton=(Button)this.findViewById(R.id.read_and_write_write_button);
		mWriteButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(mHexWpw.isMalformed()){
					Toast.makeText(getApplicationContext(), "Please correct password and retry", Toast.LENGTH_SHORT).show();
					return;
				}
				if(mHexWia.isMalformed()){
					Toast.makeText(getApplicationContext(), "Please check address and retry", Toast.LENGTH_SHORT).show();
					return;
				}
				int l=0;
				for(int i=0;i<6;i++){
					if(mASCIIInput.isChecked() && (mValue[i].length()!=2)){
							Toast.makeText(getApplicationContext(), "ERROR: each value must be 2 characters long", Toast.LENGTH_SHORT).show();
							return;
					}
				}
				mLoadingDialog=ProgressDialog.show(ReadAndWriteActivity.this, "", "Storing data...", true);
				if (((String) mBankSpinner.getSelectedItem()).equals("EPC")) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							ReadAndWriteActivity.this);
					builder.setMessage(
							"Changing EPC memory will bring back to inventory after operation.It's better to use 'Program Tag' for EPC changing. Continue anyway?")
							.setCancelable(false)
							.setPositiveButton("Yes",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											new WriterTask()
													.execute(new Object());
											mChangeEPC=true;
										}
									})
							.setNegativeButton("No",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog, int id) {
											dialog.cancel();
											mLoadingDialog.dismiss();
										}
									});
					AlertDialog alert = builder.create();
					alert.show();
				}else{
					new WriterTask()
						.execute(new Object());
					mChangeEPC=false;
				}
			}
			
		});
		filters1[0]=new InputFilter.LengthFilter(2);
		filters2[0]=new InputFilter.LengthFilter(4);
		filters3[0]=new InputFilter.LengthFilter(8);
		
		
		mBankSpinner=(Spinner)this.findViewById(R.id.read_and_write_bank_spinner);
		mASCIIInput=(CheckBox)this.findViewById(R.id.read_and_write_ascii_input);
		mSelectedTag=(TextView)this.findViewById(R.id.read_and_write_tag_selected);
		mSelectedTag.setText(Preferences.sAsciiOn?(String)mTagASCII:(String)mTagHex);
		mPassword=(EditText)this.findViewById(R.id.read_and_write_password_edittext);
		float text_dimension=mPassword.getTextSize(); //used for resizing
		mPassword.setFilters(filters3); //set max 8 hex char ('XXXX')
		mHexWpw=new HexWatcher(mPassword, mASCIIInput,false,false);
		mPassword.addTextChangedListener(mHexWpw);
		mPassword.setWidth((int) (text_dimension*8));
		mInitAddress=(EditText)this.findViewById(R.id.read_and_write_init_address);
		mInitAddress.setText("2");
		mInitAddress.setFilters(filters2);
		mHexWia=new HexWatcher(mInitAddress, mASCIIInput,false,false);//no explicit form needed,but hex string required
		mInitAddress.addTextChangedListener(mHexWia);
		mInitAddress.setWidth((int) (text_dimension*4));
		
		mValue=new EditText[6];
		mValueWatcher=new HexWatcher[6];
		mValue[0]=(EditText)this.findViewById(R.id.read_and_write_value0);
		mValue[1]=(EditText)this.findViewById(R.id.read_and_write_value1);
		mValue[2]=(EditText)this.findViewById(R.id.read_and_write_value2);
		mValue[3]=(EditText)this.findViewById(R.id.read_and_write_value3);
		mValue[4]=(EditText)this.findViewById(R.id.read_and_write_value4);
		mValue[5]=(EditText)this.findViewById(R.id.read_and_write_value5);
		
		for(int i=0;i<6;i++){
			mValue[i].setWidth((int) (text_dimension*4)); //resize edittext;
			mValue[i].setFilters(filters2); //set max 4 hex char ('XXXX')
			mValueWatcher[i]=new HexWatcher(mValue[i],mASCIIInput,false);
			mValue[i].addTextChangedListener(mValueWatcher[i]);
			mValue[i].setGravity(Gravity.CENTER);
		}
		mAddress=new TextView[6];
		mAddress[0]=(TextView)this.findViewById(R.id.read_and_write_addr1_label);
		mAddress[1]=(TextView)this.findViewById(R.id.read_and_write_addr2_label);
		mAddress[2]=(TextView)this.findViewById(R.id.read_and_write_addr3_label);
		mAddress[3]=(TextView)this.findViewById(R.id.read_and_write_addr4_label);
		mAddress[4]=(TextView)this.findViewById(R.id.read_and_write_addr5_label);
		mAddress[5]=(TextView)this.findViewById(R.id.read_and_write_addr6_label);
		
		for(int i=0;i<6;i++){
			mAddress[i].setWidth((int) (text_dimension*4)); //resize edittext;
			mAddress[i].setFilters(filters2); //set max 4 hex char ('XXXX')
			mAddress[i].setGravity(Gravity.CENTER);
			
		}
		
		mASCIIInput.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				String s=null;
				for(int i=0;i<6;i++){
					s=mValue[i].getText().toString();
					if(s.length()==1){
						s="000"+s;
					}
					if(s.length()==3)
						s="0"+s;
					if(s.length()==0 || s.equals(isChecked?"????":"??")){
						mValue[i].setFilters(isChecked?filters1:filters2);//if ascii 2 char max, if hex string max 4 char
						mValue[i].setText(isChecked?"??":"????");
						s=isChecked?"??":"????";
					}else{
						mValue[i].setFilters(isChecked?filters1:filters2);
						mValue[i].setText(isChecked?(String)RFIDTag.toASCII(RFIDTag.hexStringToByteArray(s)):(String)RFIDTag.toHexString((RFIDTag.ASCIIStringToASCIIByteArray(s))));
					}
				}
			}
			
		});
		
		byte [] id=RFIDTag.hexStringToByteArray(mTagHex);
		mLoadingDialog=ProgressDialog.show(ReadAndWriteActivity.this, "", "Loading.Please wait...",true);
		try {
			mSource=mReader.GetSource("Source_0");
			mTag=new CAENRFIDTag(id, (short) id.length, mSource, "Ant0");
			//Leggi le prime 6 word del EPC's ID
			short addr=Short.parseShort(mInitAddress.getText().toString(), 16);//2
			byte[] tmp=new byte[2];
			boolean data_loss=false;
			for(int i=0;i<6;i++){
				tmp=RFIDTag.ReadWithRetry(mTag, (short)1, (short)((addr*WORD)+(WORD*i)), (short)(WORD));
				if(tmp==null){
					data_loss=true;
					mValue[i].setText("????");
					mValue[i].setTextColor(Color.GRAY);
				}
				else{
					mValue[i].setText((String)RFIDTag.toHexString(tmp));
					mValue[i].setTextColor(Color.BLACK);
				}
				mAddress[i].setText(String.format("%04X",addr+i)); //word oriented
			}
			if(data_loss)
				Toast.makeText(this.getApplicationContext(), "Cannot read some data, change password or retry", Toast.LENGTH_SHORT).show();
		} catch (CAENRFIDException e) {
			Toast.makeText(this.getApplicationContext(), "Some error occurred during read, please retry press Read button", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		mASCIIInput.setChecked(Preferences.sAsciiOn);
		mBankSpinner.setSelection(1);//EPC
		mLoadingDialog.dismiss();
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		if(mChangeEPC){
			this.setResult(ReadAndWriteActivity.CLEAR_ON_CHANGE_EPC);
			this.finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

}
