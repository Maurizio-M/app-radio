package com.radio6ense.radioScan;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDLogicalSource;
import com.caen.RFIDLibrary.CAENRFIDReadPointStatus;
import com.caen.RFIDLibrary.CAENRFIDReader;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

public class AntennaConfigurationActivity extends Activity {

	private CAENRFIDReader mReader;
	private CheckBox [] cb_array;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.antenna_configuration_activity);
		CAENRFIDReadPointStatus antenna_status;
		
		mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
		
		
		
		cb_array=new CheckBox[4];
		cb_array[0]=(CheckBox)this.findViewById(R.id.antenna_one_checkbox);
		cb_array[1]=(CheckBox)this.findViewById(R.id.antenna_two_checkbox);
		cb_array[2]=(CheckBox)this.findViewById(R.id.antenna_three_checkbox);
		cb_array[3]=(CheckBox)this.findViewById(R.id.antenna_four_checkbox);
		CAENRFIDLogicalSource S0 = null;
		try {
			S0 = mReader.GetSource("Source_0");
		} catch (CAENRFIDException e1) {
			e1.printStackTrace();
		}
		//check for all antenna presences.
		boolean [] ants_found=new boolean[4];
		boolean ant_found=false;
		for(int ant=0;ant<4;ant++){
			for(int src=0;src<4;src++){
				try {
					if(mReader.GetSource("Source_"+src).isReadPointPresent("Ant"+ant)){
						//check
						if(src==0)
							cb_array[ant].setChecked(true);
						ant_found=true;
						break;
					}
				} catch (CAENRFIDException e) {
					e.printStackTrace();
					break;
				}
			}
			ants_found[ant]=ant_found;
			ant_found=false;
		}
		
		for(int i=0;i<4;i++){
			try {
				//disable non-existing antenna
				if(!ants_found[i]){
					cb_array[i].setEnabled(false);
					continue;
				}
				//check attacched antenna
				cb_array[i].setChecked(S0.isReadPointPresent("Ant"+i));
				//refresh antenna status
				antenna_status=mReader.GetReadPointStatus("Ant"+i);
				if(antenna_status.equals(CAENRFIDReadPointStatus.STATUS_GOOD)){
					cb_array[i].setTextColor(Color.GREEN);
				}else if(antenna_status.equals(CAENRFIDReadPointStatus.STATUS_POOR)){
					cb_array[i].setTextColor(Color.YELLOW);
				}else{
					cb_array[i].setTextColor(Color.RED);
				}
			} catch (CAENRFIDException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void AntCheck(View v){
		CheckBox cb=(CheckBox)v;
		boolean checked=cb.isChecked();
		if(!onePresent()){
			Toast.makeText(getApplicationContext(), "No more antenna cannot remove", Toast.LENGTH_SHORT).show();
			cb.setChecked(!checked);
			return;
		}
		try{
		switch(v.getId()){
		case R.id.antenna_one_checkbox:
			if(!checked)
				mReader.GetSource("Source_0").RemoveReadPoint("Ant0");
			else
				mReader.GetSource("Source_0").AddReadPoint("Ant0");
			break;
		case R.id.antenna_two_checkbox:
			if(!checked)
				mReader.GetSource("Source_0").RemoveReadPoint("Ant1");
			else
				mReader.GetSource("Source_0").AddReadPoint("Ant1");
			break;
		case R.id.antenna_three_checkbox:
			if(!checked)
				mReader.GetSource("Source_0").RemoveReadPoint("Ant2");
			else
				mReader.GetSource("Source_0").AddReadPoint("Ant2");
			break;
		case R.id.antenna_four_checkbox:
			if(!checked)
				mReader.GetSource("Source_0").RemoveReadPoint("Ant3");
			else
				mReader.GetSource("Source_0").AddReadPoint("Ant3");
			break;
		}
		}catch(CAENRFIDException e){
			e.printStackTrace();
		}
	}
	
	private boolean onePresent(){
		int canRemove=0;
		for(int i=0;i<4;i++){
			if(cb_array[i].isChecked()){
				canRemove++;
				if(canRemove>0)//almost one is still selected
					return true;
			}
		}
		return false;
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

}
