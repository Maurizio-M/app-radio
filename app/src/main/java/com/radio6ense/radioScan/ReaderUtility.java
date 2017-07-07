package com.radio6ense.radioScan;

import java.util.Arrays;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDTag;

public class ReaderUtility {

	private static final int MAX_RETRY = 3;

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
	public static byte[] ReadWithRetry(CAENRFIDTag tag, short MemBank, short Address, short Length, int Password)
    {
        int retry_count = 0;
        boolean retry = true;
        byte[] result = null;
        while (retry)
            try
            {
                result = tag.GetSource().ReadTagData_EPC_C1G2(tag, MemBank, Address, Length, Password);
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
        boolean retry = true;
        byte[] temparr = new byte[2]; //a word
        byte[] ID = null;
        if ((Value.length % 2) == 1)
        {
        	Value=Arrays.copyOf(Value, Value.length+1);
            //Value[Value.length - 1] = 0x0;
        }
        for (int i = 0; i < (Value.length / 2); i++)
        {
            retry = true;
            retry_count = 0;
            temparr=Arrays.copyOfRange(Value, i * 2, (i*2)+2);//  Array.Copy(Value, i * 2, temparr, 0, 2);
            while (retry)
            {
                try
                {
                    tag.GetSource().WriteTagData_EPC_C1G2(tag, MemBank, (short)(Address + (i * 2)), (short)2, temparr);
                    if ( ((i*2)+1) < tag.GetLength() && MemBank == 1 && Address > 3 && Value.length > 2 && i < ((Value.length / 2) - 1))//if ID's EPC is written,change EPC during write
                    {
                        ID = tag.GetId();
                        ID[(i*2)]=temparr[0];
                        ID[(i*2)+1]=temparr[1];
                        tag = new CAENRFIDTag(ID, (short)ID.length, tag.GetSource());
                    }
                    retry = false;
                }
                catch (CAENRFIDException err)
                {
                    if (retry_count > 2 && MemBank == 1 && Address > 3 && Value.length > 2 && i <= ((Value.length / 2) - 1)) //Write ID's EPC has failed and tag has written dirty data.
                    //So it's necessary to retrieve the new ID.
                    {
                        int tryRead=0;
                        CAENRFIDTag[] tags=null;
                        if ((i * 2) > 2)
                        {
                            while (tags == null && tryRead <= MAX_RETRY)
                            {
								tags = tag.GetSource().InventoryTag(ID, (short)((i * 2) - 4),(short) 0);
								tryRead++;
                            }
                        }
                        else
                        {
                            while (tags == null && tryRead <= MAX_RETRY)
                            {
                                temparr=Arrays.copyOfRange(tag.GetId(), ((i * 2) + 2), (((tag.GetId().length) - ((i * 2) + 2)))); //(tag.GetId(), ((i * 2) + 2), temparr, 0, (((tag.GetId().length) - ((i * 2) + 2))));
                                try {
									tags = tag.GetSource().InventoryTag(temparr, (short)(((tag.GetId().length) - ((i * 2) + 2))), (short)((i * 2) + 2));
									tryRead++;
								} catch (CAENRFIDException e) {
									tryRead++;
								}
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
	public static void WriteWithRetry(CAENRFIDTag tag, short MemBank, short Address, byte[] Value, int Password) throws CAENRFIDException
    {
        //Value length is pair( "Value" must be a group of at least one word)
        int retry_count = 0;
        boolean retry = true;
        byte[] temparr = new byte[2]; //a aword
        byte[] ID = null;
        if ((Value.length % 2) == 1)
        {
        	Value=Arrays.copyOf(Value, Value.length+1);
            //Value[Value.Length - 1] = 0x0;
        }
        for (int i = 0; i < (Value.length / 2); i++)
        {
            retry = true;
            retry_count = 0;
            temparr=Arrays.copyOfRange(Value, i * 2, (i*2)+2);//  Array.Copy(Value, i * 2, temparr, 0, 2);
            while (retry)
            {
                try
                {
                    tag.GetSource().WriteTagData_EPC_C1G2(tag, MemBank, (short)(Address + (i * 2)), (short)2, temparr, Password);
                    if (((i * 2) + 1) < tag.GetLength() && MemBank == 1 && Address > 3 && Value.length > 2 && i < ((Value.length / 2) - 1))//if ID's EPC is written,change EPC during write
                    {
                        ID = tag.GetId();
                        ID[(i*2)]=temparr[0];
                        ID[(i*2)+1]=temparr[1];
                        tag = new CAENRFIDTag(ID, (short)ID.length, tag.GetSource());
                    }
                    retry = false;
                }
                catch (CAENRFIDException err)
                {
                    if (retry_count > 2 && MemBank == 1 && Address > 3 && Value.length > 2 && i <= ((Value.length / 2) - 1)) //Write ID's EPC has failed and tag has written dirty data.
                    //So it's necessary to retrieve the new ID.
                    {
                        int tryRead = 0;
                        CAENRFIDTag[] tags = null;
                        if ((i * 2) > 2)
                        {
                            while (tags == null && tryRead <= MAX_RETRY)
                            {
                                tags = tag.GetSource().InventoryTag(ID, (short)((i * 2) - 4),(short) 0);
                                tryRead++;
                            }
                        }
                        else
                        {
                            while (tags == null && tryRead <= MAX_RETRY)
                            {
                            	temparr=Arrays.copyOfRange(tag.GetId(), ((i * 2) + 2), (((tag.GetId().length) - ((i * 2) + 2))));
                                //Array.Copy(tag.GetId(), ((i * 2) + 2), temparr, 0, (((tag.GetId().length) - ((i * 2) + 2))));
                                tags = tag.GetSource().InventoryTag(temparr, (short)(((tag.GetId().length) - ((i * 2) + 2))), (short)((i * 2) + 2));
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
	
}
