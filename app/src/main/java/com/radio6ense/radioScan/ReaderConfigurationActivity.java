package com.radio6ense.radioScan;

import java.math.BigDecimal;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDReader;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class ReaderConfigurationActivity extends Activity {

	private CAENRFIDReader mReader;
	private SeekBar psb;
	private TextView pres;
	private Spinner pvms;
	private String[] pwm;
	private int maxPower; 
	public static int getMaxPower(String model){
		if(model.equalsIgnoreCase("R4300P"))
			return 1500;
		else
			return 500;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.power_configuration_activity);
		
		int power=0;
		int max_sb=100;
		maxPower=getMaxPower(RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReaderName());
		
		psb=(SeekBar)this.findViewById(R.id.power_seekbar);
		pres=(TextView)this.findViewById(R.id.power_result);
		pvms=(Spinner)this.findViewById(R.id.power_view_mode_spinner);
		
		mReader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
		
		try {
			power=mReader.GetPower();
		} catch (CAENRFIDException e) {
			e.printStackTrace();
		}
		psb.setMax(max_sb);
		
		int power_percentage=(int) (((float)power/maxPower)*max_sb);
		if(power_percentage>max_sb)
			power_percentage=100;
		psb.setProgress(power_percentage);
		
		pres.setText(Integer.toString(power));
		
		pvms.setSelection(0);
		
		pwm= this.getResources().getStringArray(R.array.mWViewMode);
		psb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			boolean started=false;
			int aprogress=0;
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				aprogress=progress;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				started = true;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				started = false;
				//depending on powerviewmode, write the new calculated power
				pres.setText(CalculatePower(seekBar, aprogress, pvms, started));
			}
			
		});
	
		pvms.setOnItemSelectedListener( new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				
				int calculated_power=((psb.getProgress()*maxPower)/psb.getMax());
				
				switch(arg2){
				case 0:
					//mW
					pres.setText(Integer.toString(calculated_power));
					break;
				case 1:
					//dB
					BigDecimal bg=null;
					if(calculated_power>0)
						 bg = new BigDecimal(10*Math.log10(calculated_power));
					else
						bg=new BigDecimal(0);
					bg = bg.setScale(1, BigDecimal.ROUND_HALF_UP); 
					pres.setText(bg.toString());
					break;
				}
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
			
		});
	}
	
	protected String CalculatePower(SeekBar seekBar, int progress,
			Spinner pvms2, boolean started) {
		
		int maxPower=getMaxPower(RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReaderName());
		int calculated_power=((progress*maxPower)/seekBar.getMax());
		try {
			mReader.SetPower(calculated_power);
		} catch (CAENRFIDException e) {
			e.printStackTrace();
			return "Err";
		}
		if(pvms2.getSelectedItem().equals(pwm[0])){
			//mW
			return Integer.toString(calculated_power);
		}else{
			//dbm
			BigDecimal bg=null;
			if(calculated_power>0)
				 bg = new BigDecimal(10*Math.log10(calculated_power));
			else
				bg=new BigDecimal(0);
			bg = bg.setScale(1, BigDecimal.ROUND_HALF_UP); 
			return bg.toString();
		}
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
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
	
}
