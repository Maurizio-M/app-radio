package com.radio6ense.radioScan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import com.caen.RFIDLibrary.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class InventoryActivity extends Activity implements
		CAENRFIDEventListener {
	
	
	public static final short INIT_RSSI_VALUE=-1;
	public static final short MAX_TAGS_PER_LIST=700;
	public static final long SAMPLE_TIME=1000; //ms
	public static final int BELL_TIME=200; //ms
	
	public static Hashtable<String,Integer> mTagListPosition;
	public static int mSelectedTag;
	
	protected ArrayList<RFIDTag> mSampleTag;
	protected long mStartSampleTime;
	protected Timer mCurrentTimer;
	protected TimerTask mCurrentTimerTask;
	protected Object mCurrentFoundMutex;
	
	
	protected ArrayList<CAENRFIDNotify> mSwapTags;
	protected DemoReader mReader;
	protected RFIDTagAdapter mRFIDTagAdapter;
	protected Button mButtonInventory;
	protected ListView mInventoryList;
	protected TextView mTotalFound;
	protected TextView mCurrentFound;
	
	protected boolean isRunning = false;
	protected int reader_position = 0;
	
	
	protected short maxRssi=0;
	protected short minRssi=0;
	
	protected static int mBaseColor=Preferences.sInventoryColor;
	private int mCurrentFoundNum;
	private long mBelltime;
	
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

	
	private ToneGenerator tg;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inventory_selection);
		reader_position= RadioscanControllerActivity.Selected_Reader;
		mReader = RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader);
		mRFIDTagAdapter = new RFIDTagAdapter(getApplicationContext(), R.id.inventory_list, new ArrayList<RFIDTag>());
		mButtonInventory=(Button)this.findViewById(R.id.inventory_button);
		mInventoryList=(ListView)this.findViewById(R.id.inventory_list);
		mInventoryList.setAdapter(mRFIDTagAdapter);
		
		IntentFilter disc_filt = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		this.registerReceiver(mReceiverBTDisconnect,disc_filt );

		IntentFilter disc_filt2= new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		this.registerReceiver(mReceiverWFDisconnect,disc_filt2 );
		
		IntentFilter disc_filt3= new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		this.registerReceiver(mReceiverBTDisconnect,disc_filt3 );
		
		
		OnItemClickListener listListener=new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				if(isRunning){
					Toast.makeText(getApplicationContext(), "Must stop inventory", Toast.LENGTH_SHORT).show();
					return;
				}
				mSelectedTag=position;
				final CharSequence[] items = {"Program Tag", "Read and Write", "Lock Tag", "Kill tag"};
				final CharSequence[] items_2 = {"Program Tag", "Read and Write", "Lock Tag", "Kill tag", "RT0005 Log"};
				//Check if tag is a RT0005
				CAENRFIDReader reader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
				CAENRFIDNotify nTag= mRFIDTagAdapter.getItem(mSelectedTag).getTag();
				CAENRFIDTag tag=null;
				byte[] tid=null;
				try {
					tag=new CAENRFIDTag(nTag.getTagID(),(short)nTag.getTagID().length,reader.GetSources()[0]);
					tid=reader.GetSources()[0].ReadTagData_EPC_C1G2(tag, (short)2, (short)0, (short)4);
				} catch (CAENRFIDException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					tid=null;
				}
				boolean isRT0005=false;
				if(tid!=null){
					if(tid.length==4){
						if(tid[0]==-30 &&
								tid[1]==1 &&
								tid[2]==32 &&
								(tid[3]==4 || tid[3]==5))
							isRT0005=true;
					}
				}
				//end RT0005 
				
				AlertDialog.Builder builder = new AlertDialog.Builder(parent.getContext());
				builder.setTitle("Choose action");
				builder.setItems(isRT0005?items_2:items, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	Bundle b=new Bundle();
						String s1=mRFIDTagAdapter.getItem(mSelectedTag).getId();
						String s2=mRFIDTagAdapter.getItem(mSelectedTag).getAscii();
						b.putString("TAG_HEX", s1);
						b.putString("TAG_ASCII", s2);
				        switch(item){
				        case 0:
				        	//start inventory mode
							Intent programID = new Intent(getApplicationContext(),ProgramIDActivity.class);
							programID.putExtras(b);
							startActivityForResult(programID, 0);
				        	break;
				        case 1:
				        	//start inventory mode
							Intent randw = new Intent(getApplicationContext(),ReadAndWriteActivity.class);
							randw.putExtras(b);
							startActivityForResult(randw, 0);
				        	break;
				        case 2:
				        	Intent locktag = new Intent(getApplicationContext(),LockTagActivity.class);
				        	locktag.putExtras(b);
							startActivityForResult(locktag, 0);
				        	break;
				        case 3:
				        	Intent killtag = new Intent(getApplicationContext(),KillTagActivity.class);
				        	killtag.putExtras(b);
							startActivityForResult(killtag, 0);
				        	break;
				        case 4:
				        	Intent control_room = new Intent(getApplicationContext(),RT0005ControlRoomActivity.class);
				        	control_room.putExtras(b);
							startActivityForResult(control_room, 0);
				        	break;
				        }
				    }
				});
				AlertDialog alert = builder.create();
				alert.show();
			}
			
		};
		mInventoryList.setOnItemClickListener(listListener);
		mTotalFound=(TextView)this.findViewById(R.id.total_found_num);
		mCurrentFound=(TextView)this.findViewById(R.id.current_found_num);
		
		maxRssi=INIT_RSSI_VALUE;
		minRssi=INIT_RSSI_VALUE;
		mTagListPosition=new Hashtable<String,Integer>(MAX_TAGS_PER_LIST);
		mSampleTag=new ArrayList<RFIDTag>();
		mStartSampleTime=0;
		mCurrentFoundNum=0;
		mCurrentFoundMutex=new Object();
		mBelltime=System.currentTimeMillis();
		tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode==ReadAndWriteActivity.CLEAR_ON_CHANGE_EPC){
			ClearInventory(null);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return new Dialog(this.getApplicationContext());
	}

	@Override
	protected void onDestroy() {
		this.unregisterReceiver(mReceiverBTDisconnect);
		this.unregisterReceiver(mReceiverWFDisconnect);
		super.onDestroy();
		if (isRunning) {
			mButtonInventory.setText(R.string.inventory_button_start);
			mButtonInventory.setEnabled(false);
			stopInventory();
			mCurrentTimer.cancel();
			mCurrentTimerTask.cancel();
			mCurrentTimer.purge();
			isRunning=false;
		}
		RadioscanControllerActivity.returnFromActivity=true;
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void CAENRFIDTagNotify(CAENRFIDEvent evt) {
		mSwapTags= (ArrayList<CAENRFIDNotify>)evt.getData();
		this.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
                try {
                    stopInventory();
                    updateViews((ArrayList<CAENRFIDNotify>) mSwapTags.clone());
                    startInventory();
                } catch (CAENRFIDException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		});
		
		synchronized(mCurrentFoundMutex){
			mCurrentFoundNum+=mSwapTags.size();
		}
	}

    public static byte[] byteArray2BitArray(byte[] bytes) {
        byte[] bits = new byte[bytes.length * 8];
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0)
                bits[i] = 0x01;
            else
                bits[i] = 0x00;
        }
        return bits;
    }

    public static String bitArrayToString(byte[] bits)
    {
        String str = new String();
        for (int i = 0; i < bits.length; i++)
        {
            if(bits[i] == 0x01)
                str += '1';
            else
                str += '0';
        }
        return str;
    }

    public static String reverseBit(String bits)
    {
        String str = new String();
        for (int i = 0; i < bits.length(); i++)
        {
            if(bits.charAt(i) == '1')
                str += '0';
            else
                str += '1';
        }
        return str;
    }

	public void updateViews(ArrayList<CAENRFIDNotify> data) throws CAENRFIDException, IOException {
        Log.d("Test", "Update");
		ArrayList<CAENRFIDNotify> tags = data;
		mRFIDTagAdapter.getCount();
		
		boolean haveToNotify=true;
		int tag_color=0;
		RFIDTag tmp_tag=null;
		tags.size();
		
		String tmp_id=null;
		RFIDTag adpTag=null;
		Integer pos=null;
		short tmp_rssi=0;
		TextView tv=null; //tag id
		TextView tvc=null;// counter
		LinearLayout ll=null;
		
		for (CAENRFIDNotify tag : tags) {
			tmp_id=RFIDTag.toHexString(tag.getTagID());
			tmp_tag=null;
			tmp_rssi=tag.getRSSI();

			//controllo gli Rssi e aggiorno max e min
			if(maxRssi == INIT_RSSI_VALUE){
				//update for first time the max and min rssi
				maxRssi=tmp_rssi;
				minRssi=tmp_rssi;
			}
			if(tmp_rssi>maxRssi)
				maxRssi=tmp_rssi;
			else if(tmp_rssi<minRssi)
				minRssi=tmp_rssi;

			// SE - il tag  gi nella lista ALLORA cambiali solo il colore
			// aggiornato.
			adpTag=null;
			pos=mTagListPosition.get(tmp_id);
			String tmp_tv;
			if(pos!=null){
				ll=(LinearLayout) this.mInventoryList.getChildAt(pos);
				if(ll!=null){
					tv=(TextView)ll.getChildAt(0);
					tvc=(TextView)ll.getChildAt(1);
					adpTag=mRFIDTagAdapter.getItem(pos);
					tmp_tv=(String) tv.getText();

                    CAENRFIDReader reader= RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader();
                    CAENRFIDNotify nTag= mRFIDTagAdapter.getItem(pos).getTag();


                    try
                    {
                        CAENRFIDLogicalSource MySource = RadioscanControllerActivity.Readers.get(RadioscanControllerActivity.Selected_Reader).getReader().GetSource("Source_0");
                        CAENRFIDTag mytag = new CAENRFIDTag(nTag.getTagID(), (short) nTag.getTagID().length, MySource, "Ant0");
 // nTag.getTagType()
                        Log.d("Source", MySource.GetName());
                        EM4325TagData mydata = MySource.EM4325_GetSensorData(mytag, false, true);

                        byte[] tempArray = mydata.GetSensorData();
                        byte[] bitArray = InventoryActivity.byteArray2BitArray(tempArray);
                        byte[] tbis = Arrays.copyOfRange(bitArray, 7, 16);
                        byte sign = tbis[0];
                        byte[] intBytes = Arrays.copyOfRange(tbis, 1, 7);
                        //char signChar = (sign==0x01)?'1':'0';
                        if(sign==0x00) {
                            char signChar = '+';
                            short integer = (short) Integer.parseInt("0000000000" + bitArrayToString(intBytes), 2);

                            byte[] decBytes = Arrays.copyOfRange(tbis, 7, 9);
                            short decimal = (short) Integer.parseInt(bitArrayToString(decBytes), 2);
                            double temperature = integer + (decimal * 0.25);
                            Log.d("TEMPERATURE", temperature + "°C");
                            adpTag.setTemperature("+" + temperature + "°C");
                        }
                        else
                        {
                            char signChar = '-';
                            short integer = (short) Integer.parseInt("0000000000" + reverseBit(bitArrayToString(intBytes)), 2);

                            byte[] decBytes = Arrays.copyOfRange(tbis, 7, 9);
                            short decimal = (short) Integer.parseInt(bitArrayToString(decBytes), 2);
                            double temperature = integer + (decimal * 0.25) + 1;
                            Log.d("TEMPERATURE", temperature + "°C");
                            adpTag.setTemperature("-" + temperature + "°C");
                        }
//                        int complement = (short)Integer.parseInt("0000000000"+reverseBit("101000"), 2)+1;
//                        Log.d("TEMPERATURE2", "-"+complement);
                    }
                    catch(Exception e)
                    {
                        Log.d("Err", e.toString());
                    }

					if( (tmp_tv.equals(adpTag.getAscii())) || ( tmp_tv.equals(adpTag.getId()) ) ){ //if it's visible

						if(adpTag.getmRssi() != tmp_rssi){
							adpTag.setmRssi(tmp_rssi);
							adpTag.setColor(RFIDTag.getColor(Preferences.sInventoryColor, tmp_rssi, maxRssi, minRssi));
							tv.setTextColor(adpTag.getColor());
							adpTag.setCounter(adpTag.getCounter()+1);
							tvc.setText(Integer.toString(adpTag.getCounter()));
							this.mRFIDTagAdapter.notifyDataSetChanged();
						}else{																	   
							adpTag.setCounter(adpTag.getCounter()+1);
							tv.setTextColor(RFIDTag.getColor(Preferences.sInventoryColor, tmp_rssi, maxRssi, minRssi));
							tvc.setText(Integer.toString(adpTag.getCounter()));
							this.mRFIDTagAdapter.notifyDataSetChanged();
						}
					}else{ 																			//otherwise
						adpTag.setCounter(adpTag.getCounter()+1);
						adpTag.setmRssi(tmp_rssi);
					}
				}else{
					RFIDTag tt=this.mRFIDTagAdapter.getItem(pos);
					
					tt.setCounter(tt.getCounter()+1);
					tt.setColor(RFIDTag.getColor(Preferences.sInventoryColor, tmp_rssi, maxRssi, minRssi));
					this.mRFIDTagAdapter.notifyDataSetChanged();
				}
			}else{
				//aggiungi il nuovo elemento corrispondente al tag nella lista
				tag_color=RFIDTag.getColor(Preferences.sInventoryColor, tmp_rssi, maxRssi, minRssi);
				tmp_tag=new RFIDTag(tag,tag_color,tmp_id,tmp_rssi);
				tmp_tag.setCounter(tmp_tag.getCounter()+1);
				mRFIDTagAdapter.add(tmp_tag);
				mTagListPosition.put(tmp_tag.getId(), mRFIDTagAdapter.getCount()-1);
				if(haveToNotify) haveToNotify=false;
				mTotalFound.setText(Integer.toString(mRFIDTagAdapter.getCount()));
			}
		}
		if(Preferences.sBeepOn){
			try {
				if((System.currentTimeMillis()-mBelltime)>BELL_TIME){
					playSound();
					mBelltime=System.currentTimeMillis();
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

    public String toBinary( byte[] bytes )
    {
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for( int i = 0; i < Byte.SIZE * bytes.length; i++ )
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
        return sb.toString();
    }
	

	public void playSound() throws IllegalArgumentException, SecurityException, IllegalStateException,
	IOException {
		if(tg.startTone(ToneGenerator.TONE_PROP_BEEP, 50));
	}

	public void doInventoryAction(View v) {
		if (isRunning) {
			mButtonInventory.setText(R.string.inventory_button_start);
			mButtonInventory.setEnabled(false);
			stopInventory();
			mCurrentTimer.cancel();
			mCurrentTimerTask.cancel();
			mCurrentTimer.purge();
			isRunning=false;
		}else{
			this.maxRssi = InventoryActivity.INIT_RSSI_VALUE;
			this.minRssi = InventoryActivity.INIT_RSSI_VALUE;
			mButtonInventory.setText(R.string.inventory_button_stop);
			mButtonInventory.setEnabled(false);
			mCurrentTimer=new Timer();
			mCurrentTimerTask=new TimerTask(){

				@Override
				public void run() {
					
						runOnUiThread(new Runnable(){
							@Override
							public void run() {
								synchronized(mCurrentFoundMutex){
									mCurrentFound.setText(Integer.toString(mCurrentFoundNum));
									mCurrentFoundNum=0;
								}
							}
						});

				}
				
			};
			startInventory();
			mCurrentTimer.schedule(mCurrentTimerTask, 0, 1000);
			isRunning=true;
		}
	}

	public void ClearInventory(View v){
		if(isRunning)
			Toast.makeText(getApplicationContext(), "Must stop inventory", Toast.LENGTH_SHORT).show();
		else{
			mRFIDTagAdapter.clear();
			mTagListPosition=new Hashtable<String,Integer>(MAX_TAGS_PER_LIST);
			mSampleTag=new ArrayList<RFIDTag>();
			mStartSampleTime=0;
			mCurrentFoundNum=0;
			this.mTotalFound.setText("0");
			this.mCurrentFound.setText("0");
			minRssi=0;
			maxRssi=0;
		}
	}
	public void startInventory() {
		CAENRFIDReader reader=this.mReader.getReader();
		CAENRFIDLogicalSource TheSource = null;
		try {
			TheSource = reader.GetSource("Source_0");
		} catch (CAENRFIDException e1) {
			e1.printStackTrace();
		}
		try {
			TheSource.SetReadCycle(0);
			reader.addCAENRFIDEventListener(this);
			if(InventoryMode.isMaskActive){
				if(InventoryMode.mask!=null){
					CAENRFIDLogicalSourceConstants sel = CAENRFIDLogicalSourceConstants.EPC_C1G2_All_SELECTED;
					switch(InventoryMode.maskMatch){
					case 0:
						sel=CAENRFIDLogicalSourceConstants.EPC_C1G2_SELECTED_YES;
						break;
					case 1:
						sel=CAENRFIDLogicalSourceConstants.EPC_C1G2_SELECTED_NO;
						break;
					case 2:
						sel=CAENRFIDLogicalSourceConstants.EPC_C1G2_All_SELECTED;
						break;
					}
					TheSource.SetSelected_EPC_C1G2(sel);
					TheSource.EventInventoryTag(InventoryMode.mask,(short)((InventoryMode.mask.length*8)-(InventoryMode.startMatchPosition*8)) , (short)( InventoryMode.startMatchPosition*8), InventoryMode.isTriggerActive?(short)0x27:(short)0x07);
				}
			}else{
				TheSource.SetSelected_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_All_SELECTED);
				TheSource.EventInventoryTag(new byte[0], (short) 0x0, (short) 0x0, InventoryMode.isTriggerActive?InventoryMode.isRSSIActive?(short)0x27:(short)0x26:InventoryMode.isRSSIActive?(short)0x07:(short)0x06);
			}
			mButtonInventory.setEnabled(true);
		} catch (CAENRFIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void stopInventory(){
		CAENRFIDReader reader=this.mReader.getReader();
		reader.removeCAENRFIDEventListener(this);
		reader.InventoryAbort();
		mButtonInventory.setEnabled(true);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if(isRunning){
			Toast.makeText(getApplicationContext(), "Must stop inventory", Toast.LENGTH_SHORT).show();
			return false;
		}
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_reader_menu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.reader_configuration_item:
			//start reader configuration
			Intent config = new Intent(getApplicationContext(), ConfigurationActivity.class);
			startActivity(config);
			return true;
		case R.id.inventory_mode_item:
			//start inventory mode
			Bundle b=new Bundle();
			ArrayList<String> tags_hex=new ArrayList<String>();
			ArrayList<String> tags_ascii=new ArrayList<String>();
			for(int i=0;i<mRFIDTagAdapter.getCount();i++){
				if(!(tags_hex.contains(mRFIDTagAdapter.getItem(i).getId()))){
					tags_hex.add(mRFIDTagAdapter.getItem(i).getId());
					tags_ascii.add(mRFIDTagAdapter.getItem(i).getAscii());
				}		
			}
			b.putStringArrayList("TAGS_HEX", tags_hex);
			b.putStringArrayList("TAGS_ASCII", tags_ascii);
			Intent inv_mode = new Intent(getApplicationContext(), InventoryModeActivity.class);
			inv_mode.putExtras(b);
			startActivity(inv_mode);
			return true;
		case R.id.preferences_item:
			//start preferences
			Intent pref = new Intent(getApplicationContext(), PreferencesActivity.class);
			startActivityForResult(pref, 0);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if(isRunning){
			Toast.makeText(getApplicationContext(), "Must stop inventory", Toast.LENGTH_SHORT).show();
			return false;
		}
		return super.onMenuOpened(featureId, menu);
	}
	
	
}