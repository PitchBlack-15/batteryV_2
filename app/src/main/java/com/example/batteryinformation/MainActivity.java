package com.example.batteryinformation;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.ArrayList;
import java.util.UUID;

import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;


public class MainActivity extends Activity {

	private PowerManager.WakeLock wl;
	//widgets
	private TextView batteryInfo;
	Button btnPaired;
	ListView devicelist;
	private ProgressDialog progress;
	//Bluetooth
	private BluetoothAdapter myBluetooth = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;

	private Set<BluetoothDevice> pairedDevices;
	String address = null;

	//SPP UUID. Look for it
	static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String TAG = "bluetooth1";



	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		wl.acquire();

		//Calling widgets
		//batteryInfo=(TextView)findViewById(R.id.textViewBatteryInfo);
		btnPaired = (Button)findViewById(R.id.button_scan);
		devicelist = (ListView)findViewById(R.id.listView);

		//if the device has bluetooth
		myBluetooth = BluetoothAdapter.getDefaultAdapter();
		checkBTavailable();

		btnPaired.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pairedDevicesList();
			}
		});

	}

	private void checkBTavailable()
	{
		if(myBluetooth == null)
		{
			//Show a mensag. that the device has no bluetooth adapter
			Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();

			//finish apk
			finish();
		}
		else if(!myBluetooth.isEnabled())
		{
			//Ask to the user turn the bluetooth on
			Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(turnBTon,1);
		}
	}

	private void pairedDevicesList()
	{
		pairedDevices = myBluetooth.getBondedDevices();
		ArrayList list = new ArrayList();

		if (pairedDevices.size()>0)
		{
			for(BluetoothDevice bt : pairedDevices)
			{
				list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
			}
		}
		else
		{
			Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
		}

		final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
		devicelist.setAdapter(adapter);
		devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

	}


	private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener()
	{
		public void onItemClick (AdapterView<?> av, View v, int arg2, long arg3)
		{
			// Get the device MAC address, the last 17 chars in the View
			String info = ((TextView) v).getText().toString();
			address = info.substring(info.length() - 17);
			ConnectBT(address);
//			// Make an intent to start next activity.
//			Intent i = new Intent(MainActivity.this, ledControl.class);
//			//Change the activity.
//			i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
//			startActivity(i);
		}
	};


//	@Override
//	public boolean onCreateOptionsMenu(Menu menu)
//	{
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.menu_device_list, menu);
//		return true;
//	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}



	private void showText(String msg) {Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();}
	private void errorExit(String title, String message){
		Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
		//finish();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		wl.release();
		Disconnect();
	}


	private void runThreadSendBT()
	{
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					while (!isInterrupted()) {
						Thread.sleep(10000);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
								Intent batteryStatus = registerReceiver(null, ifilter);
								if (batteryStatus != null) {
									int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
									int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
									float batteryPct = level / (float) scale;
									showText("percent : "+ batteryPct);
									if (btSocket!=null)
									{
										showText("Send to BT : "+ batteryPct);
                						sendData(String.valueOf(batteryPct));
									}
								}
							}
						});
					}
				} catch (InterruptedException e) {
				}
			}
		};
		t.start();
	}

	private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
		if(Build.VERSION.SDK_INT >= 10){
			try {
				final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
				return (BluetoothSocket) m.invoke(device, myUUID);
			} catch (Exception e) {
				Log.e(TAG, "Could not create Insecure RFComm Connection", e);
			}
		}
		return  device.createRfcommSocketToServiceRecord(myUUID);
	}


	private void ConnectBT(String BTaddress)
	{
		// Set up a pointer to the remote node using it's address.
		BluetoothDevice device = myBluetooth.getRemoteDevice(BTaddress);

		// Two things are needed to make a connection:
		//   A MAC address, which we got above.
		//   A Service ID or UUID.  In this case we are using the
		//     UUID for SPP.
		try {
			btSocket = createBluetoothSocket(device);
		} catch (IOException e1) {
            showText("In onResume() and socket create failed: " + e1.getMessage() + ".");
		}

		// Discovery is resource intensive.  Make sure it isn't going on
		// when you attempt to connect and pass your message.
		myBluetooth.cancelDiscovery();

		// Establish the connection.  This will block until it connects.
        showText("...Connecting...");
		try {
			btSocket.connect();
            showText("...Connection ok...");
            // Create a data stream so we can talk to server.
            showText("...Creating Socket...");
            try {
                outStream = btSocket.getOutputStream();
                runThreadSendBT();
            } catch (IOException e) {
                showText("In onResume() and output stream creation failed:" + e.getMessage() + ".");
            }
		} catch (IOException e) {
			try {
                showText("...Can't connect to Device...");
				btSocket.close();

			} catch (IOException e2) {
                showText("In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
			}
		}
	}

	private void Disconnect()
	{
		if (btSocket!=null) //If the btSocket is busy
		{
			try
			{
				btSocket.close(); //close connection
			}
			catch (IOException e) {
				showText("Error Disconnection");}
		}
	}


	private void sendData(String message) {
		byte[] msgBuffer = message.getBytes();

		Log.d(TAG, "...Send data: " + message + "...");
		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
			if (address.equals("00:00:00:00:00:00"))
				msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
			msg = msg +  ".\n\nCheck that the SPP UUID: " + myUUID.toString() + " exists on server.\n\n";
			errorExit("Fatal Error", msg);
		}
	}
}
