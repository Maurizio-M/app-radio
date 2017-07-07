package com.radio6ense.radioScan;

import android.os.Parcel;
import android.os.Parcelable;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDPort;
import com.caen.RFIDLibrary.CAENRFIDRFRegulations;
import com.caen.RFIDLibrary.CAENRFIDReader;

public class DemoReader implements Parcelable{
	private CAENRFIDReader reader;
	private String name;
	private String serial;
	private String firmwareRelease;
	private String regulation;
	private CAENRFIDPort connectionType;
	
	public static final int EVENT_CONNECTED=10;
	public static final int EVENT_DISCONNECT=20;
	
	public DemoReader(CAENRFIDReader caenReader, String readerName, String serialNum, String fwRel, CAENRFIDPort connType){
		this.setReader(caenReader);
		this.setReaderName(readerName);
		this.setSerialNumber(serialNum);
		this.setFirmwareRelease(fwRel);
		try {
			this.setRegulation(caenReader.GetRFRegulation());
		} catch (CAENRFIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setConnectionType(connType);
	}

	public String getFirmwareRelease() {
		return firmwareRelease;
	}

	public void setFirmwareRelease(String firmwareRelease) {
		this.firmwareRelease = firmwareRelease;
	}

	public String getSerialNumber() {
		return serial;
	}

	public void setSerialNumber(String serial) {
		this.serial = serial;
	}

	public String getReaderName() {
		return name;
	}

	public void setReaderName(String name) {
		this.name = name;
	}

	public CAENRFIDReader getReader() {
		return reader;
	}

	public void setReader(CAENRFIDReader reader) {
		this.reader = reader;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		Object[] array=new Object[4];
		array[0]=this.reader;
		array[1]=this.name;
		array[2]=this.serial;
		array[3]=this.firmwareRelease;
		dest.writeArray(array);
	}
	
	public static final Parcelable.Creator<DemoReader> CREATOR = new Parcelable.Creator<DemoReader>() {
		public DemoReader createFromParcel(Parcel in) {
			return new DemoReader(in);
		}
		public DemoReader[] newArray(int size) {
			return new DemoReader[size];
		}
	};

	private DemoReader(Parcel in) {
		this.reader=(CAENRFIDReader) in.readValue(DemoReader.class.getClassLoader());
		this.name=in.readString();
		this.serial=in.readString();
		this.firmwareRelease=in.readString();
	}

	public String getRegulation() {
		// TODO Auto-generated method stub
		return this.regulation;
	}
	public void setRegulation(CAENRFIDRFRegulations reg){
		if(CAENRFIDRFRegulations.AUSTRALIA.equals(reg))
			this.regulation="AUSTRALIA";
		else if(CAENRFIDRFRegulations.BRAZIL.equals(reg))
			this.regulation="BRAZIL";
		else if(CAENRFIDRFRegulations.CHINA.equals(reg))
			this.regulation="CHINA";
		else if(CAENRFIDRFRegulations.ETSI_300220.equals(reg))
			this.regulation="ETSI 300220";
		else if(CAENRFIDRFRegulations.ETSI_302208.equals(reg))
			this.regulation="ETSI 302208";
		else if(CAENRFIDRFRegulations.FCC_US.equals(reg))
			this.regulation="FCC_US";
		else if(CAENRFIDRFRegulations.JAPAN.equals(reg))
			this.regulation="JAPAN";
		else if(CAENRFIDRFRegulations.KOREA.equals(reg))
			this.regulation="KOREA";
		else if(CAENRFIDRFRegulations.MALAYSIA.equals(reg))
			this.regulation="MALAYSIA";
		else if(CAENRFIDRFRegulations.SINGAPORE.equals(reg))
			this.regulation="SINGAPORE";
		else if(CAENRFIDRFRegulations.TAIWAN.equals(reg))
			this.regulation="TAIWAN";
		else
			this.regulation="UNKNOWN";
			
	}

	public CAENRFIDPort getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(CAENRFIDPort connectionType) {
		this.connectionType = connectionType;
	}

}
