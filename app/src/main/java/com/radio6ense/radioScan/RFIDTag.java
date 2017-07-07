package com.radio6ense.radioScan;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.graphics.Color;
import android.util.Log;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDNotify;
import com.caen.RFIDLibrary.CAENRFIDTag;
import com.caen.RFIDLibrary.EM4325TagData;

public class RFIDTag {
	private CAENRFIDNotify mTag;
	private int mColor;
	private String mId;
	private short mRssi;
	private int counter;
	private String ascii;

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    private String temperature;
	
	protected static final int MAX_PASS=256;
	protected static final int MAX_TRANSPARENCY=110; //0x00 invisible - 0xff full color
	protected static final int MIN_TRANSPARENCY=200;		  
	private static int MAX_RETRY=3;
	
	/**
	 * table to convert a nibble to a hex char.
	 */
	
	static final char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'A', 'B', 'C', 'D', 'E', 'F' };
	/**
	 * Fast convert a byte array to a hex string with possible leading zero.
	 * 
	 * @param b
	 *            array of bytes to convert to string
	 * @return hex representation, two chars per byte.
	 */
	public static String toHexString(byte[] b) {
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			// look up high nibble char
			sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
			// look up low nibble char
			sb.append(hexChar[b[i] & 0x0f]);
		}
		return sb.toString();
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	public static String toASCII(byte [] b){
		try {
			
			return new String(b,"ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	public static byte[] ASCIIStringToASCIIByteArray(String s){
		try {
			return s.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public static final byte[] intToByteArray(int value) {
	    return new byte[] {
	            (byte)(value >>> 24),
	            (byte)(value >>> 16),
	            (byte)(value >>> 8),
	            (byte)value};
	}

    public static byte[] GetSensorData(CAENRFIDTag tag) throws CAENRFIDException {
        EM4325TagData tagData = tag.GetSource().EM4325_GetSensorData(tag, false, true);
        Log.d("LogData", "here");
        return tagData.GetSensorData();
    }
	
	public static byte[] ReadWithRetry(CAENRFIDTag tag, short MemBank, short Address, short Length)
    {
        int retry_count = 0;
        boolean retry = true;
        byte[] result = null;
        while (retry)
            try
            {
                result = tag.GetSource().ReadTagData_EPC_C1G2(tag, MemBank, Address, Length);
                retry = false;
            }
            catch (CAENRFIDException err)
            {
                retry_count++;
                if (retry_count == MAX_RETRY)
                {
                    retry = false;
                }
            }
        return result;
    }

	public static byte[] ReadWithRetry(CAENRFIDTag tag, short MemBank, short Address, short Length,int password)
    {
        int retry_count = 0;
        boolean retry = true;
        byte[] result = null;
        while (retry)
            try
            {
                result = tag.GetSource().ReadTagData_EPC_C1G2(tag, MemBank, Address, Length, password);
                retry = false;
            }
            catch (CAENRFIDException err)
            {
                retry_count++;
                if (retry_count == MAX_RETRY)
                {
                    retry = false;
                }
            }
        return result;
    }
	
	public static void WriteWithRetry(CAENRFIDTag tag, short MemBank, short Address, byte[] Value) throws CAENRFIDException
    {
        //Value length is pair( "Value" must be a group of at least one word)
        int retry_count = 0;
        int WORD=2;
        int IDDefStartPos=4; //EPC C1G2 defines that EPC's ID default starts from the fourth byte of EPC memory bank. 
        boolean retry = true;
        byte[] temparr = new byte[2]; //a word
        byte[] ID = null;
        if (Value.length % WORD == 1)
        {
            //Array.Resize<byte>(ref Value, Value.Length + 1);
            Arrays.copyOf(Value,Value.length+1);
            Value[Value.length - 1] = 0x0;
        }
        for (int i = 0; i < (Value.length / WORD); i++)
        {
            retry = true;
            retry_count = 0;
            //Array.Copy(Value, i * 2, temparr, 0, 2);
            temparr=Arrays.copyOfRange(Value, (i*WORD), (i*WORD)+WORD);
            while (retry)
            {
                try
                {
                    tag.GetSource().WriteTagData_EPC_C1G2(tag, MemBank, (short)(Address + (i * WORD)), (short)WORD, temparr);
                    if ( ((i*WORD)+1) < tag.GetLength() && MemBank == 1 && Address > 3 && Address > (IDDefStartPos-1) && ((Address+(i*WORD))<(IDDefStartPos+tag.GetLength())) && Value.length > WORD && i < ((Value.length / WORD) - 1))//if ID's EPC is written,change EPC during write
                    {
                        ID = tag.GetId();
                        ID[i*WORD]=temparr[0];
                        ID[(i*WORD)+1]=temparr[1];
                        tag = new CAENRFIDTag(ID, (short)ID.length, tag.GetSource());
                    }
                    retry = false;
                }
                catch (CAENRFIDException err)
                {
                    if (retry_count > WORD && MemBank == 1 && Address > 3 && Address > (IDDefStartPos-1) && ((Address+(i*WORD))<(IDDefStartPos+tag.GetLength())) && Value.length > WORD && i <= ((Value.length / WORD) - 1)) //Write ID's EPC has failed and tag has written dirty data.
                    //So it's necessary to retrieve the new ID.
                    {
                        int tryRead=0;
                        CAENRFIDTag[] tags=null;
                        if ((i * WORD) > WORD)
                        {
                            while (tags == null && tryRead <= MAX_RETRY)
                            {
                            	if(ID==null)
                            		ID=tag.GetId();
                                tags = tag.GetSource().InventoryTag(ID, (short)((i * WORD) - 4),(short)0);
                                tryRead++;
                            }
                        }
                        else
                        {
                            while (tags == null && tryRead <= MAX_RETRY)
                            {
                                //Array.Copy(tag.GetId(), ((i * 2) + 2), temparr, 0, (((tag.GetId().length) - ((i * 2) + 2))));
                                temparr=Arrays.copyOfRange(tag.GetId(), ((i*WORD)+WORD), (((tag.GetId().length)-((i * WORD) + WORD))));
                                tags = tag.GetSource().InventoryTag(temparr, (short)(((tag.GetId().length) - ((i * WORD) + WORD))), (short)((i * WORD) + WORD));
                                tryRead++;
                            }
                        }
                        tag = tags[0];//if is the only tag in
                    }
                    retry_count++;
                    if (retry_count == MAX_RETRY)
                        throw err;
                }
            }
        }

    }
	
	public static void WriteWithRetry(CAENRFIDTag tag, short MemBank, short Address, byte[] Value,int password) throws CAENRFIDException
    {
        //Value length is pair( "Value" must be a group of at least one word)
        int retry_count = 0;
        int WORD=2;
        boolean retry = true;
        byte[] temparr = new byte[2]; //a word
        byte[] ID = null;
        int IDDefStartPos=4; //EPC C1G2 defines that EPC's ID default starts from the fourth byte of EPC memory bank. 
        if (Value.length % WORD == 1)
        {
            //Array.Resize<byte>(ref Value, Value.Length + 1);
            Arrays.copyOf(Value,Value.length+1);
            Value[Value.length - 1] = 0x0;
        }
        for (int i = 0; i < (Value.length / WORD); i++)
        {
            retry = true;
            retry_count = 0;
            //Array.Copy(Value, i * 2, temparr, 0, 2);
            temparr=Arrays.copyOfRange(Value, (i*WORD), (i*WORD)+WORD);
            while (retry)
            {
                try
                {
                    tag.GetSource().WriteTagData_EPC_C1G2(tag, MemBank, (short)(Address + (i * WORD)), (short)WORD, temparr, password);
                    if ( ((i*WORD)+1) < tag.GetLength() && MemBank == 1 && Address > (IDDefStartPos-1) && ((Address+(i*WORD))<(IDDefStartPos+tag.GetLength())) && Value.length > WORD && i < ((Value.length / WORD) - 1))//if ID's EPC is written,change EPC during write
                    {
                        ID = tag.GetId();
                        ID[i*2]=temparr[0];
                        ID[(i*WORD)+1]=temparr[1];
                        tag = new CAENRFIDTag(ID, (short)ID.length, tag.GetSource());
                    }
                    retry = false;
                }
                catch (CAENRFIDException err)
                {
                    if (retry_count > 2 && MemBank == 1 && Address > 3 && Address > (IDDefStartPos-1) && ((Address+(i*WORD))<(IDDefStartPos+tag.GetLength())) && Value.length > WORD && i <= ((Value.length / WORD) - 1)) //Write ID's EPC has failed and tag has written dirty data.
                    //So it's necessary to retrieve the new ID.
                    {
                        int tryRead=0;
                        CAENRFIDTag[] tags=null;
                        if ((i * WORD) > WORD)
                        {
                            while (tags == null && tryRead <= MAX_RETRY)
                            {
                            	if(ID==null)
                            		ID=tag.GetId();
                                tags = tag.GetSource().InventoryTag(ID, (short)((i * WORD) - 4),(short)0);
                                tryRead++;
                            }
                        }
                        else
                        {
                            while (tags == null && tryRead <= MAX_RETRY)
                            {
                                //Array.Copy(tag.GetId(), ((i * 2) + 2), temparr, 0, (((tag.GetId().length) - ((i * 2) + 2))));
                                temparr=Arrays.copyOfRange(tag.GetId(), ((i*WORD)+WORD), (((tag.GetId().length)-((i * WORD) + WORD))));
                                tags = tag.GetSource().InventoryTag(temparr, (short)(((tag.GetId().length) - ((i * WORD) + WORD))), (short)((i * WORD) + WORD));
                                tryRead++;
                            }
                        }
                        tag = tags[0];//if is the only tag in
                    }
                    retry_count++;
                    if (retry_count == MAX_RETRY)
                        throw err;
                }
            }
        }

    }
	
	/**
	 * This method calculate the transparency level of a mColor, based on
	 * RSSI detected for a mTag, take as limit the max and min RSSI provided.
	 * @param baseColor The base mColor where apply the transparency.
	 * @param rssi The RSSI detected for the mTag. 
	 * @param maxRssi The max RSSI limit (nearest to 0dB).
	 * @param minRssi The min RSSI limit (as far away from 0dB).
	 * @return The Color for this mTag.
	 */
	public static int getColor(int baseColor,short rssi, short maxRssi, short minRssi){
		int color=baseColor;
		int tmp =0x00000000;
		int added_transparency=0;
		
		if(rssi > maxRssi){
			return baseColor;
		} else if(rssi < minRssi){
			return Color.TRANSPARENT;
		}
		float pass=(float) ((float)(maxRssi-minRssi)/MAX_PASS);
		int i=0;
		for(i=0;( (rssi-(pass*i)) >= (minRssi) ) && (i<MAX_PASS-1);i++){
			added_transparency++;
		}
		if(added_transparency<MAX_TRANSPARENCY && added_transparency>=0)
			added_transparency=MAX_TRANSPARENCY;
		if(added_transparency>MIN_TRANSPARENCY)
			added_transparency= MAX_PASS-1;
		tmp+=added_transparency;
		tmp=tmp<<24;
		color=color & 0x00ffffff; //clean transparency
		color=color | tmp;
		return color;
	}
	

	public RFIDTag(CAENRFIDNotify tag,int color,String Id,short rssi){
		this.mTag=tag;
		this.mColor=color;
		this.mId=Id;
		this.setmRssi(rssi);
		this.setCounter(0);
		this.setAscii(toASCII(mTag.getTagID()));
	}
	public CAENRFIDNotify getTag() {
		return mTag;
	}
	public void setTag(CAENRFIDNotify tag) {
		this.mTag = tag;
	}
	@Override
	public boolean equals(Object o) {
		RFIDTag tag=(RFIDTag)o;
		byte [] A,B;
		A=tag.getTag().getTagID();
		B=this.mTag.getTagID();
		if(tag.getTag().getTagID().length != this.mTag.getTagID().length)
			return false;
		for(int i=0;i<A.length;i++) {
			if(A[i] != B[i]) return false;
		}
		return true;
	}
	@Override
	public String toString() {
		return this.mId;
	}
	public int getColor() {
		return this.mColor;
	}
	public void setColor(int color) {
		this.mColor = color;
	}
	public String getId() {
		return this.mId;
	}
	public void setId(String mId) {
		this.mId = mId;
	}
	public short getmRssi() {
		return mRssi;
	}
	public void setmRssi(short mRssi) {
		this.mRssi = mRssi;
	}
	public int getCounter() {
		return counter;
	}
	public void setCounter(int counter) {
		this.counter = counter;
	}
	public String getAscii() {
		return ascii;
	}
	public void setAscii(String ascii) {
		this.ascii = ascii;
	}
	
}
