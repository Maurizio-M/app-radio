package com.radio6ense.radioScan;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDLogicalSource;
import com.caen.RFIDLibrary.CAENRFIDLogicalSourceConstants;
import com.caen.RFIDLibrary.CAENRFIDReader;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class ProtocolConfigurationActivity extends Activity {
	
	private CAENRFIDReader mReader;
	CAENRFIDLogicalSource source;
	private Spinner mSessionSpinner;
	private Button mAddQ;
	private Button mSubQ;
	private TextView mQResult;
	private OnItemSelectedListener listener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.protocol_configuration_activity);
		
		mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
		View v=this.findViewById(R.id.session_spinner);
		mSessionSpinner=(Spinner)v;
		mAddQ=(Button)this.findViewById(R.id.add_Q_button);
		mSubQ=(Button)this.findViewById(R.id.sub_Q_button);
		mQResult=(TextView)this.findViewById(R.id.Q_result);
		
		
		try {
			source = mReader.GetSource("Source_0");
		} catch (CAENRFIDException e1) {
			e1.printStackTrace();
		}
		try {
			int q=source.GetQ_EPC_C1G2();
			mQResult.setText(Integer.toString(q));
			CAENRFIDLogicalSourceConstants ses=source.GetSession_EPC_C1G2();
			if(ses.equals(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S0))
				mSessionSpinner.setSelection(0);
			else if(ses.equals(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S1))
				mSessionSpinner.setSelection(1);
			else if(ses.equals(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S2))
				mSessionSpinner.setSelection(2);
			else
				mSessionSpinner.setSelection(3);
			
		} catch (CAENRFIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		listener=new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				try{
					switch(arg2){
					case 0:
						source.SetSession_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S0);
						break;
					case 1:
						source.SetSession_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S1);
						break;
					case 2:
						source.SetSession_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S2);
						break;
					case 3:
						source.SetSession_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S3);
						break;
					}
				}catch(CAENRFIDException err){
					err.printStackTrace();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
		};
		mSessionSpinner.setOnItemSelectedListener(listener);
	}
	
	public void addQ(View v){
		int q=Integer.parseInt((String) mQResult.getText());
		if(q>=15){
			try {
				source.SetQ_EPC_C1G2(15);
			} catch (CAENRFIDException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			try {
				source.SetQ_EPC_C1G2(q+1);
			} catch (CAENRFIDException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mQResult.setText(Integer.toString(q+1));
		}
	}
	
	public void subQ(View v){
		int q=Integer.parseInt((String) mQResult.getText());
		if(q<=0){
			try {
				source.SetQ_EPC_C1G2(0);
			} catch (CAENRFIDException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			try {
				source.SetQ_EPC_C1G2(q-1);
			} catch (CAENRFIDException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mQResult.setText(Integer.toString(q-1));
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
	}
	
}
