package com.radio6ense.radioScan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDPort;
import com.caen.RFIDLibrary.CAENRFIDReader;
import com.caen.RFIDLibrary.CAENRFIDReaderInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;

public class RadioscanControllerActivity extends Activity {

    protected static final int ADD_READER_TCP = 0;
    protected static final int ADD_READER_BT = 1;
    protected static final int DO_INVENTORY = 2;
    protected static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static boolean returnFromActivity = false;
    public static Vector<DemoReader> Readers;
    public static int Selected_Reader;
    protected static boolean STARTED = true;
    protected static boolean DESTROYED = false;
    private final BroadcastReceiver mReceiverBTDisconnect = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (DESTROYED)
                return;
            String action = intent.getAction();
            if ((action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) && (!BluetoothAdapter
                    .getDefaultAdapter().isEnabled()))
                    || (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))) {
                int pos = 0;
                Vector<Integer> toRemove = new Vector<Integer>();
                for (DemoReader demoReader : Readers) {
                    try {
                        if (demoReader.getConnectionType().equals(
                                CAENRFIDPort.CAENRFID_BT)) {
                            data.remove(pos);
                            adapter.notifyDataSetChanged();
                            demoReader.getReader().Disconnect();
                            toRemove.add(new Integer(pos));
                        }
                    } catch (CAENRFIDException e) {
                        e.printStackTrace();
                    }
                    pos++;
                }
                for (int i = 0; i < toRemove.size(); i++) {
                    Readers.remove(toRemove.get(i).intValue());
                }
                if (!toRemove.isEmpty()) {
                    Toast.makeText(getApplicationContext(),
                            "Bluetooth device disconnected!",
                            Toast.LENGTH_SHORT).show();
                }
                toRemove = null;
            }
        }
    };
    private final BroadcastReceiver mReceiverWFDisconnect = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (DESTROYED)
                return;
            if (intent.getExtras() != null) {
                if (intent.getExtras().getBoolean(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY,
                        Boolean.FALSE)) {
                    /* SUPPRESS INITAL DISCONNECT(NETWORK CHANGE) SIGNAL */
                    if (STARTED) {
                        STARTED = false;
                        return;
                    }

                    int pos = 0;
                    Vector<Integer> toRemove = new Vector<Integer>();
                    for (DemoReader demoReader : Readers) {
                        try {
                            if (demoReader.getConnectionType().equals(
                                    CAENRFIDPort.CAENRFID_TCP)) {
                                data.remove(pos);
                                adapter.notifyDataSetChanged();
                                demoReader.getReader().Disconnect();
                                toRemove.add(new Integer(pos));
                            }
                        } catch (CAENRFIDException e) {
                            e.printStackTrace();
                        }
                        pos++;
                    }
                    for (int i = 0; i < toRemove.size(); i++) {
                        Readers.remove(toRemove.get(i).intValue());
                    }
                    toRemove = null;
                    Toast.makeText(getApplicationContext(),
                            "TCP device disconnected!", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    };
    protected static boolean CONNECTION_SUCCESFULL = false;
    private final BroadcastReceiver mReceiverUUIDCached = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (DESTROYED)
                return;
            String action = intent.getAction();
            if ((action.equals(BluetoothDevice.BOND_BONDED) && (!BluetoothAdapter
                    .getDefaultAdapter().isEnabled()))
                    || (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED))) {
                CONNECTION_SUCCESFULL = true;
            }
        }
    };
    private static boolean exitFromApp = false;
    private ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String, Object>>();
    private SimpleAdapter adapter;
    private ProgressDialog tcpBtWaitProgressDialog;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (!RadioscanControllerActivity.returnFromActivity) {
            Readers = new Vector<DemoReader>();
        } else
            RadioscanControllerActivity.returnFromActivity = false;

        IntentFilter disc_filt = new IntentFilter(
                BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiverBTDisconnect, disc_filt);

        IntentFilter disc_filt2 = new IntentFilter(
                ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(mReceiverWFDisconnect, disc_filt2);

        IntentFilter disc_filt3 = new IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mReceiverBTDisconnect, disc_filt3);

        IntentFilter disc_filt4 = new IntentFilter(
                BluetoothDevice.ACTION_ACL_CONNECTED);
        this.registerReceiver(mReceiverUUIDCached, disc_filt4);
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onPostResume() {

        super.onPostResume();
    }

    @Override
    protected void onRestart() {

        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        STARTED = true;
        DESTROYED = false;
        ListView lv = (ListView) this.findViewById(R.id.reader_list);
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Selected_Reader = position;
                Intent do_inventory = new Intent(getApplicationContext(),
                        InventoryActivity.class);
                startActivityForResult(do_inventory, DO_INVENTORY);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        DESTROYED = true;
        WifiManager wifiManager = (WifiManager) this
                .getSystemService(Context.WIFI_SERVICE);
        if (RadioscanControllerActivity.exitFromApp) {
            for (DemoReader demoReader : Readers) {
                try {

                    if ((demoReader.getConnectionType().equals(
                            CAENRFIDPort.CAENRFID_BT) && BluetoothAdapter
                            .getDefaultAdapter().isEnabled())
                            || (demoReader.getConnectionType().equals(
                            CAENRFIDPort.CAENRFID_TCP) && wifiManager
                            .isWifiEnabled())) {
                        demoReader.getReader().Disconnect();
                    }
                    demoReader = null;
                } catch (CAENRFIDException e) {
                    e.printStackTrace();
                }
            }
            Readers = null;
        }
        this.unregisterReceiver(mReceiverBTDisconnect);
        this.unregisterReceiver(mReceiverWFDisconnect);
        this.unregisterReceiver(mReceiverUUIDCached);
        exitFromApp = false;
        returnFromActivity = false;
    }

    public Activity getActivity() {
        return this;
    }

    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
            case 1:
                final CharSequence[] items = {"TCP", "Bluetooth"};

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Connection Type");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        if (item == 0) {
                            Toast.makeText(getApplicationContext(), items[item],
                                    Toast.LENGTH_SHORT).show();

                            Intent addReader = new Intent(getApplicationContext(),
                                    TCPSelection.class);
                            getActivity().startActivityForResult(addReader,
                                    ADD_READER_TCP);
                        } else {
                            Toast.makeText(getApplicationContext(), items[item],
                                    Toast.LENGTH_SHORT).show();

                            Intent addReader = new Intent(getApplicationContext(),
                                    BTSelection.class);
                            getActivity().startActivityForResult(addReader,
                                    ADD_READER_BT);
                        }
                    }
                });
                dialog = builder.create();
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    public void updateReadersList() {
        if (Readers != null) {
            CAENRFIDPort isTCP = null;
            ((ListView) findViewById(R.id.reader_list)).setAdapter(null);
            data.clear();

            for (int i = 0; i < Readers.size(); i++) {
                DemoReader r = Readers.get(i);

                HashMap<String, Object> readerMap = new HashMap<String, Object>();
                isTCP = r.getConnectionType();
                readerMap
                        .put("image",
                                isTCP.equals(CAENRFIDPort.CAENRFID_TCP) ? R.drawable.ic_tcp_reader
                                        : R.drawable.ic_bt_reader);
                readerMap.put("name", r.getReaderName());
                readerMap.put("info", "Serial: " + r.getSerialNumber()
                        + "\nFirmware: " + r.getFirmwareRelease()
                        + "\nRegulation: " + r.getRegulation());
                data.add(readerMap);
            }
        }
        String[] from = {"image", "name", "info"};
        int[] to = {R.id.reader_image, R.id.reader_name, R.id.reader_info};

        adapter = new SimpleAdapter(getApplicationContext(), data,
                R.layout.list_reader, from, to);

        // utilizzo dell'adapter

        ((ListView) findViewById(R.id.reader_list)).setAdapter(adapter);
    }

    public void addNewReaderActivity(View v) {
        showDialog(1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        String ip = null;
        BluetoothDevice dev = null;
        switch (requestCode) {
            case RadioscanControllerActivity.ADD_READER_TCP:
                if (resultCode == RESULT_OK) {
                    ip = (String) data.getStringExtra("IP_ADDRESS");
                    CAENRFIDReader r = new CAENRFIDReader();
                    tcpBtWaitProgressDialog = ProgressDialog.show(this,
                            "Connection ", "Connecting to " + ip, true, true);
                    new TCPConnector().execute(r, ip);
                }
                break;
            case RadioscanControllerActivity.ADD_READER_BT:
                if (resultCode == RESULT_OK) {
                    dev = (BluetoothDevice) ((Intent) data)
                            .getParcelableExtra("BT_DEVICE");
                    tcpBtWaitProgressDialog = ProgressDialog.show(this,
                            "Connection ", "Connecting to " + dev.getName(), true,
                            true);
                    new BTConnector().execute(dev);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        exitFromApp = true;
        finish();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }

    public void goToWeb(View v) {
        String url = "http://www.radio6ense.com";
        Intent i = new Intent(Intent.ACTION_VIEW);
        Uri u = Uri.parse(url);
        i.setData(u);

        try {
            // Start the activity
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            // Raise on activity not found
            Toast.makeText(this.getApplicationContext(), "Browser not found.",
                    Toast.LENGTH_SHORT).show();
        }

    }

    private class TCPConnector extends AsyncTask<Object, Boolean, Boolean> {

        @SuppressWarnings("unused")
        String ip = null;
        boolean error1 = false;
        boolean error2 = false;

        protected Boolean doInBackground(Object... pars) {
            CAENRFIDReaderInfo info = null;
            String fwrel = null;
            ip = (String) pars[1];
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            try {
                ((CAENRFIDReader) pars[0]).Connect(CAENRFIDPort.CAENRFID_TCP,
                        (String) pars[1]);
            } catch (CAENRFIDException e) {
                error1 = true;
            }
            if (!error1) {
                try {
                    error2 = false;
                    info = ((CAENRFIDReader) pars[0]).GetReaderInfo();
                    fwrel = ((CAENRFIDReader) pars[0]).GetFirmwareRelease();
                } catch (CAENRFIDException e) {
                    error2 = true;
                }
                if (!error2) {
                    DemoReader dr = new DemoReader(((CAENRFIDReader) pars[0]),
                            info.GetModel(), info.GetSerialNumber(), fwrel,
                            CAENRFIDPort.CAENRFID_TCP);
                    Readers.add(dr);
                }
            }
            return true;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (error1) {
                Toast.makeText(getApplicationContext(),
                        "Error during connection...", Toast.LENGTH_SHORT)
                        .show();
            } else if (error2) {
                Toast.makeText(getApplicationContext(),
                        "Error retriving info from reader...",
                        Toast.LENGTH_SHORT).show();
            } else {
                updateReadersList();
            }
            tcpBtWaitProgressDialog.dismiss();
        }
    }

    private class BTConnector extends AsyncTask<Object, Boolean, Boolean> {

        private BluetoothSocket sock;
        private CAENRFIDReaderInfo info;
        private String fwrel;

        protected Boolean doInBackground(Object... pars) {
            boolean secure = true;
            boolean no_connection = true;
            CAENRFIDReader r = null;
            while (no_connection) {
                try {
                    if (secure)
                        sock = ((BluetoothDevice) pars[0])
                                .createRfcommSocketToServiceRecord(MY_UUID);
                    else
                        sock = ((BluetoothDevice) pars[0])
                                .createInsecureRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e1) {
                    return false;
                }
                r = new CAENRFIDReader();
                try {
                    r.Connect(sock);
                    while (!CONNECTION_SUCCESFULL)
                        Thread.yield();
                    CONNECTION_SUCCESFULL = false;
                    no_connection = false;
                    int state = ((BluetoothDevice) pars[0]).getBondState();
                    while (state != BluetoothDevice.BOND_BONDED) {
                        state = ((BluetoothDevice) pars[0]).getBondState();
                    }
                } catch (CAENRFIDException e) {
                    if (!secure)
                        return false;
                    else {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        secure = false;
                    }
                }
            }
            try {
                // r.UnblockReader();
                info = r.GetReaderInfo();
                fwrel = r.GetFirmwareRelease();

            } catch (CAENRFIDException e) {
                e.printStackTrace();
                return false;
            }
            DemoReader dr = new DemoReader(r, info.GetModel(),
                    info.GetSerialNumber(), fwrel, CAENRFIDPort.CAENRFID_BT);
            Readers.add(dr);
            return true;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Toast.makeText(getApplicationContext(),
                        "Error during connection...", Toast.LENGTH_SHORT)
                        .show();
            }
            updateReadersList();
            tcpBtWaitProgressDialog.dismiss();
        }
    }
}
