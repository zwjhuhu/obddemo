/*
 * TODO put header
 */
package com.skywin.obdreader.activity;

import java.util.Date;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.MyCommand;
import com.skywin.obd.DemoConstant;
import com.skywin.obd.DeviceListActivity;
import com.skywin.obdreader.R;
import com.skywin.obdreader.thread.BluetoothDemoService;
import com.skywin.obdreader.thread.CommandThread;

import eu.lighthouselabs.obd.commands.SpeedObdCommand;
import eu.lighthouselabs.obd.commands.control.CommandEquivRatioObdCommand;
import eu.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import eu.lighthouselabs.obd.commands.engine.MassAirFlowObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelEconomyWithMAFObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelLevelObdCommand;
import eu.lighthouselabs.obd.commands.fuel.FuelTrimObdCommand;
import eu.lighthouselabs.obd.commands.temperature.AmbientAirTemperatureObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;
import eu.lighthouselabs.obd.enums.FuelTrim;
import eu.lighthouselabs.obd.enums.FuelType;
import eu.lighthouselabs.obd.reader.IPostListener;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob;

/**
 * The main activity.
 */
public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";

	/*
	 * TODO put description
	 */
	static final int NO_BLUETOOTH_ID = 0;
	static final int BLUETOOTH_DISABLED = 1;
	static final int NO_GPS_ID = 2;
	static final int SETTINGS = 5;
	static final int COMMAND_ACTIVITY = 6;
	static final int TABLE_ROW_MARGIN = 7;
	static final int NO_ORIENTATION_SENSOR = 8;

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	private Handler mHandler = new DemoHandler();

	private BluetoothAdapter mBluetoothAdapter;

	// Member object for the chat services
	private BluetoothDemoService mChatService = null;

	private IPostListener mListener = null;
	// private Intent mServiceIntent = null;
	private CommandThread mCmdThread = null;

	// private SensorManager sensorManager = null;
	// private Sensor orientSensor = null;
	private SharedPreferences prefs = null;

	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;

	private boolean preRequisites = true;

	private boolean autosend = false;

	private int speed = 1;
	private double maf = 1;
	private float ltft = 0;
	private double equivRatio = 1;

	private EditText commandText;
	private TextView resultText;
	private Button sendButton;

	private final SensorEventListener orientListener = new SensorEventListener() {
		public void onSensorChanged(SensorEvent event) {
			float x = event.values[0];
			String dir = "";
			if (x >= 337.5 || x < 22.5) {
				dir = "N";
			} else if (x >= 22.5 && x < 67.5) {
				dir = "NE";
			} else if (x >= 67.5 && x < 112.5) {
				dir = "E";
			} else if (x >= 112.5 && x < 157.5) {
				dir = "SE";
			} else if (x >= 157.5 && x < 202.5) {
				dir = "S";
			} else if (x >= 202.5 && x < 247.5) {
				dir = "SW";
			} else if (x >= 247.5 && x < 292.5) {
				dir = "W";
			} else if (x >= 292.5 && x < 337.5) {
				dir = "NW";
			}
			TextView compass = (TextView) findViewById(R.id.compass_text);
			updateTextView(compass, dir);
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
		}
	};

	public void updateTextView(final TextView view, final String txt) {
		new Handler().post(new Runnable() {
			public void run() {
				view.setText(txt);
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		commandText = (EditText) this.findViewById(R.id.commandText);
		resultText = (TextView) this.findViewById(R.id.resultText);
		sendButton = (Button) this.findViewById(R.id.sendButton);

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final MyCommand command = new MyCommand(commandText.getText()
						.toString());
				ObdCommandJob job = new ObdCommandJob(command);
				job.setId(new Date().getTime());
				mCmdThread.addJobToQueue(job);

				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						resultText.setText(command.getName() + ">>"
								+ command.getResult());
					}
				}, 2000);
			}
		});

		mListener = new IPostListener() {
			public void stateUpdate(ObdCommandJob job) {
				String cmdName = job.getCommand().getName();
				String cmdResult = job.getCommand().getFormattedResult();

				Log.d(TAG, FuelTrim.LONG_TERM_BANK_1.getBank() + " equals "
						+ cmdName + "?");

				if (AvailableCommandNames.ENGINE_RPM.getValue().equals(cmdName)) {
					TextView tvRpm = (TextView) findViewById(R.id.rpm_text);
					tvRpm.setText(cmdResult);
				} else if (AvailableCommandNames.SPEED.getValue().equals(
						cmdName)) {
					TextView tvSpeed = (TextView) findViewById(R.id.spd_text);
					tvSpeed.setText(cmdResult);
					speed = ((SpeedObdCommand) job.getCommand())
							.getMetricSpeed();
				} else if (AvailableCommandNames.MAF.getValue().equals(cmdName)) {
					maf = ((MassAirFlowObdCommand) job.getCommand()).getMAF();
					addTableRow(cmdName, cmdResult);
				} else if (FuelTrim.LONG_TERM_BANK_1.getBank().equals(cmdName)) {
					ltft = ((FuelTrimObdCommand) job.getCommand()).getValue();
				} else if (AvailableCommandNames.EQUIV_RATIO.getValue().equals(
						cmdName)) {
					equivRatio = ((CommandEquivRatioObdCommand) job
							.getCommand()).getRatio();
					addTableRow(cmdName, cmdResult);
				} else {
					addTableRow(cmdName, cmdResult);
				}
			}
		};

		/*
		 * Validate Bluetooth service.
		 */
		// Bluetooth device exists?
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			preRequisites = false;
			Toast.makeText(this, "蓝牙不可用", 0).show();
			finish();
			return;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (!mBluetoothAdapter.isEnabled()) {
			preRequisites = false;
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}else{
			if(mChatService==null){
				envSetup();
			}
		}
	}

	private void envSetup() {
		mChatService = new BluetoothDemoService(this, mHandler);
		mCmdThread = new CommandThread(mHandler, mChatService);
		new Thread(mCmdThread).start();
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
			}
			break;
		case REQUEST_ENABLE_BT:
			//Toast.makeText(this, "sdsds", Toast.LENGTH_SHORT).show();
			if (resultCode == Activity.RESULT_OK) {
				if(mChatService==null){
					envSetup();
				}
			} else {
				Toast.makeText(this, "蓝牙未开启,应用将退出", Toast.LENGTH_SHORT).show();
				finish();
			}
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		releaseWakeLockIfHeld();
		mListener = null;
		if(mHandler!=null){
			stopLiveData();
			mHandler = null;
		}
		if(mCmdThread!=null){
			mCmdThread.destroy();
			mCmdThread = null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "Pausing..");
		releaseWakeLockIfHeld();
	}

	/**
	 * If lock is held, release. Lock will be held when the service is running.
	 */
	private void releaseWakeLockIfHeld() {
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
	}

	protected void onResume() {
		super.onResume();

		Log.d(TAG, "Resuming..");

		// sensorManager.registerListener(orientListener, orientSensor,
		// SensorManager.SENSOR_DELAY_UI);
		// prefs = PreferenceManager.getDefaultSharedPreferences(this);
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"ObdReader");
		wakeLock.acquire();
		if (mChatService != null) {
			if (mChatService.getState() == BluetoothDemoService.STATE_NONE) {
				mChatService.start();
			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option_menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case R.id.secure_connect_scan:
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;
		case R.id.insecure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent,
					REQUEST_CONNECT_DEVICE_INSECURE);
			return true;
		case R.id.start_live_data:
			startLiveData();
			return true;
		case R.id.stop_live_data:
			stopLiveData();
			return true;
		}
		return false;
	}

	private void startLiveData() {
		Log.d(TAG, "Starting live data..");

		if (mCmdThread != null
				&& mChatService.getState() == BluetoothDemoService.STATE_CONNECTED) {
			autosend = true;
			mHandler.post(mQueueCommands);
		}
	}

	private void stopLiveData() {
		Log.d(TAG, "Stopping live data..");

		mHandler.removeCallbacks(mQueueCommands);
	}

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	private class DemoHandler extends Handler {

		private String mDevicename;

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DemoConstant.MESSAGE_STATE_CHANGE:

				switch (msg.arg1) {
				case BluetoothDemoService.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to,
							mDevicename));
					break;
				case BluetoothDemoService.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BluetoothDemoService.STATE_LISTEN:
				case BluetoothDemoService.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;
			case DemoConstant.MESSAGE_WRITE:
				// byte[] writeBuf = (byte[]) msg.obj;
				// String writeMessage = new String(writeBuf);
				break;
			case DemoConstant.MESSAGE_READ:
				// byte[] readBuf = (byte[]) msg.obj;
				// String readMessage = new String(readBuf, 0, msg.arg1);

				break;
			case DemoConstant.MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mDevicename = msg.getData().getString(DemoConstant.DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mDevicename, Toast.LENGTH_SHORT)
						.show();
				break;
			case DemoConstant.MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(DemoConstant.TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			case DemoConstant.MESSAGE_UI:
				mListener.stateUpdate((ObdCommandJob) msg.obj);
				break;
			}
		}
	}

	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem startItem = menu.findItem(R.id.start_live_data);
		MenuItem stopItem = menu.findItem(R.id.stop_live_data);

		if (mChatService != null
				&& mChatService.getState() == BluetoothDemoService.STATE_CONNECTED) {
			if (autosend) {
				startItem.setEnabled(false);
				stopItem.setEnabled(true);
			} else {
				stopItem.setEnabled(false);
				startItem.setEnabled(true);
			}
		} else {
			startItem.setEnabled(false);
			stopItem.setEnabled(false);
		}

		return true;
	}

	private void addTableRow(String key, String val) {
		TableLayout tl = (TableLayout) findViewById(R.id.data_table);
		TableRow tr = new TableRow(this);
		MarginLayoutParams params = new MarginLayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(TABLE_ROW_MARGIN, TABLE_ROW_MARGIN, TABLE_ROW_MARGIN,
				TABLE_ROW_MARGIN);
		tr.setLayoutParams(params);
		tr.setBackgroundColor(Color.BLACK);
		TextView name = new TextView(this);
		name.setGravity(Gravity.RIGHT);
		name.setText(key + ": ");
		TextView value = new TextView(this);
		value.setGravity(Gravity.LEFT);
		value.setText(val);
		tr.addView(name);
		tr.addView(value);
		tl.addView(tr, new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));

		if (tl.getChildCount() > 10)
			tl.removeViewAt(0);
	}

	/**
	 * 
	 */
	private Runnable mQueueCommands = new Runnable() {
		public void run() {
			/*
			 * If values are not default, then we have values to calculate MPG
			 */
			Log.d(TAG, "SPD:" + speed + ", MAF:" + maf + ", LTFT:" + ltft);
			if (speed > 1 && maf > 1 && ltft != 0) {
				FuelEconomyWithMAFObdCommand fuelEconCmd = new FuelEconomyWithMAFObdCommand(
						FuelType.DIESEL, speed, maf, ltft, false /* TODO */);
				TextView tvMpg = (TextView) findViewById(R.id.fuel_econ_text);
				String liters100km = String.format("%.2f",
						fuelEconCmd.getLitersPer100Km());
				tvMpg.setText("" + liters100km);
				Log.d(TAG, "FUELECON:" + liters100km);
			}

			if (mCmdThread != null)
				queueCommands();

			// run again in 2s
			mHandler.postDelayed(mQueueCommands, 2000);
		}
	};

	/**
	 * 
	 */
	private void queueCommands() {
		final ObdCommandJob airTemp = new ObdCommandJob(new AmbientAirTemperatureObdCommand());
		final ObdCommandJob speed = new ObdCommandJob(new SpeedObdCommand());
		// final ObdCommandJob fuelEcon = new ObdCommandJob(
		// new FuelEconomyObdCommand());
		final ObdCommandJob rpm = new ObdCommandJob(new EngineRPMObdCommand());
		final ObdCommandJob maf = new ObdCommandJob(new MassAirFlowObdCommand());
		final ObdCommandJob fuelLevel = new ObdCommandJob(
				new FuelLevelObdCommand());
		final ObdCommandJob ltft1 = new ObdCommandJob(new FuelTrimObdCommand(
				FuelTrim.LONG_TERM_BANK_1));
		/*
		 * final ObdCommandJob ltft2 = new ObdCommandJob(new FuelTrimObdCommand(
		 * FuelTrim.LONG_TERM_BANK_2)); final ObdCommandJob stft1 = new
		 * ObdCommandJob(new FuelTrimObdCommand( FuelTrim.SHORT_TERM_BANK_1));
		 * final ObdCommandJob stft2 = new ObdCommandJob(new FuelTrimObdCommand(
		 * FuelTrim.SHORT_TERM_BANK_2)); final ObdCommandJob equiv = new
		 * ObdCommandJob(new CommandEquivRatioObdCommand());
		 */
		mCmdThread.addJobToQueue(airTemp);
		mCmdThread.addJobToQueue(speed);
		// mServiceConnection.addJobToQueue(fuelEcon);
		mCmdThread.addJobToQueue(rpm);
		mCmdThread.addJobToQueue(maf);
		mCmdThread.addJobToQueue(fuelLevel);
		// mServiceConnection.addJobToQueue(equiv);
		mCmdThread.addJobToQueue(ltft1);
		// mServiceConnection.addJobToQueue(ltft2);
		// mServiceConnection.addJobToQueue(stft1);
		// mServiceConnection.addJobToQueue(stft2);
	}
}