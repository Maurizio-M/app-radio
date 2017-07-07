package com.radio6ense.radioScan;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RFIDTagAdapter extends ArrayAdapter<RFIDTag> {

	private ArrayList<RFIDTag> items;
	private Context mContext;
	
	public RFIDTagAdapter(Context context, int resource, ArrayList<RFIDTag> objects) {
		super(context, resource, objects);
		this.mContext=context;
		this.items = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		//super.getView(position, convertView, parent);
		LinearLayout view =(LinearLayout) convertView;
		if (view == null) {
            LayoutInflater vi = (LayoutInflater)this.mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = (LinearLayout) vi.inflate(R.layout.inventory_item, null);
        }
		RFIDTag item = items.get(position);
		if (item != null) {
			if (view != null) {
				((TextView)view.getChildAt(0)).setText(Preferences.sAsciiOn?item.getAscii():item.getId());
				((TextView)view.getChildAt(0)).setTextColor(item.getColor());
				((TextView)view.getChildAt(1)).setText(Integer.toString(item.getCounter()));
                ((TextView)view.getChildAt(2)).setText(item.getTemperature());
			}
		}
		return view;
	}

	@Override
	public int getPosition(RFIDTag item) {
		int pos=0;
		for(RFIDTag e: this.items){
			if(e.equals(item)){
				return pos;
			}
			pos++;
		}
		return -1;
	}
}
