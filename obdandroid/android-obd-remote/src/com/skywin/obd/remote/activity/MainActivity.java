package com.skywin.obd.remote.activity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import pt.lighthouselabs.obd.commands.ObdCommand;
import pt.lighthouselabs.obd.commands.SpeedObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineRPMObdCommand;
import pt.lighthouselabs.obd.commands.engine.EngineRuntimeObdCommand;
import pt.lighthouselabs.obd.enums.AvailableCommandNames;
import roboguice.RoboGuice;
import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.reader.config.Constants;
import com.github.pires.obd.reader.config.ObdConfig;
import com.github.pires.obd.reader.io.AbstractGatewayService;
import com.github.pires.obd.reader.io.ObdCommandJob;
import com.github.pires.obd.reader.io.ObdGatewayService;
import com.github.pires.obd.reader.io.ObdProgressListener;
import com.github.pires.obd.reader.trips.TripLog;
import com.github.pires.obd.reader.trips.TripRecord;
import com.google.inject.Inject;
import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.model.OnlineInfo;
import com.skywin.obd.protocol.ProtocolEntity;
import com.skywin.obd.remote.R;
import com.skywin.obd.util.NetUtil;
import com.skywin.obd.util.ProtocolUtils;

// Some code taken from https://github.com/barbeau/gpstest

@ContentView(R.layout.main)
public class MainActivity extends RoboActivity implements ObdProgressListener,
		LocationListener, GpsStatus.Listener {

	static {
		RoboGuice.setUseAnnotationDatabases(false);
	}

	private static boolean bluetoothDefaultIsEnable = false;

	public ConcurrentHashMap<String, String> commandResult = new ConcurrentHashMap<String, String>();

	boolean mGpsIsStarted = false;
	private LocationManager mLocService;
	private LocationProvider mLocProvider;
	private Location mLastLocation;

	private static final String TAG = MainActivity.class.getName();
	// private static final int NO_BLUETOOTH_ID = 0;
	// private static final int BLUETOOTH_DISABLED = 1;
	private static final int START_LIVE_DATA = 2;
	private static final int STOP_LIVE_DATA = 3;
	private static final int SETTINGS = 4;
	private static final int GET_DTC = 5;
	private static final int GET_ONLINE = 6;
	private static final int TABLE_ROW_MARGIN = 7;
	private static final int NO_ORIENTATION_SENSOR = 8;
	// private static final int NO_GPS_SUPPORT = 9;
	private static final int TRIPS_LIST = 10;
	private static final int SAVE_TRIP_NOT_AVAILABLE = 11;

	private static final int NO_NETWORK = 12;

	private int serverid;

	// / the trip log
	private TripLog triplog;
	private TripRecord currentTrip;

	private Context context;

	private Socket socket;

	private boolean deviceready;

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
			updateTextView(compass, dir);
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// do nothing
		}
	};

	private final Runnable mQueueCommands = new Runnable() {
		public void run() {
			if (service != null && service.isRunning() && service.queueEmpty()) {
				queueCommands();

				double lat = 0;
				double lon = 0;
				final int posLen = 7;
				if (mGpsIsStarted && mLastLocation != null) {
					lat = mLastLocation.getLatitude();
					lon = mLastLocation.getLongitude();

					StringBuffer sb = new StringBuffer();
					sb.append("Lat: ");
					sb.append(String.valueOf(mLastLocation.getLatitude())
							.substring(0, posLen));
					sb.append(" Lon: ");
					sb.append(String.valueOf(mLastLocation.getLongitude())
							.substring(0, posLen));
					gpsStatusTextView.setText(sb.toString());
				}
				// if (prefs.getBoolean(ConfigActivity.UPLOAD_DATA_KEY, false))
				// {
				// final String vin = prefs.getString(
				// ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
				// Map<String, String> temp = new HashMap<String, String>();
				// temp.putAll(commandResult);
				// // ObdReading reading = new ObdReading(lat, lon,
				// // System.currentTimeMillis(), vin, temp);
				// // new UploadAsyncTask().execute(reading);
				// }
				commandResult.clear();
			}
			// run again in period defined in preferences
			new Handler().postDelayed(mQueueCommands,
					ConfigActivity.getObdUpdatePeriod(prefs));
		}
	};
	@InjectView(R.id.compass_text)
	private TextView compass;

	@InjectView(R.id.BT_STATUS)
	private TextView btStatusTextView;

	@InjectView(R.id.OBD_STATUS)
	private TextView obdStatusTextView;

	@InjectView(R.id.GPS_POS)
	private TextView gpsStatusTextView;

	@InjectView(R.id.vehicle_view)
	private LinearLayout vv;

	@InjectView(R.id.data_table)
	private TableLayout tl;
	@Inject
	private SensorManager sensorManager;
	@Inject
	private PowerManager powerManager;
	@Inject
	private SharedPreferences prefs;
	private boolean isServiceBound;

	private AbstractGatewayService service;
	private ServiceConnection serviceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.d(TAG, className.toString() + " service is bound");
			isServiceBound = true;
			service = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder)
					.getService();
			service.setContext(MainActivity.this);
			service.setSocket(socket);
			service.setServerId(serverid);
			Log.d(TAG, "Starting live data");
			try {
				service.startService();
				// btStatusTextView.setText(getString(R.string.status_bluetooth_connected));
			} catch (IOException ioe) {
				Log.e(TAG, "Failure Starting live data");
				// btStatusTextView.setText(getString(R.string.status_bluetooth_error_connecting));
				doUnbindService();
			}
		}

		@Override
		protected Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		// This method is *only* called when the connection to the service is
		// lost unexpectedly
		// and *not* when the client unbinds
		// (http://developer.android.com/guide/components/bound-services.html)
		// So the isServiceBound attribute should also be set to false when we
		// unbind from the service.
		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.d(TAG, className.toString() + " service is unbound");
			isServiceBound = false;
		}
	};

	private Sensor orientSensor = null;
	private PowerManager.WakeLock wakeLock = null;

	public void updateTextView(final TextView view, final String txt) {
		new Handler().post(new Runnable() {
			public void run() {
				view.setText(txt);
			}
		});
	}

	public static String LookUpCommand(String txt) {
		for (AvailableCommandNames item : AvailableCommandNames.values()) {
			if (item.getValue().equals(txt))
				return item.name();
		}
		return txt;
	}

	public void stateUpdate(final ObdCommandJob job) {
		final String cmdName = job.getCommand().getName();
		String cmdResult = "";
		final String cmdID = LookUpCommand(cmdName);

		if (job.getState().equals(
				ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)) {
			cmdResult = job.getCommand().getResult();
			if (cmdResult != null)
				obdStatusTextView.setText(cmdResult.toLowerCase());
		} else {
			cmdResult = job.getCommand().getFormattedResult();
			obdStatusTextView.setText(getString(R.string.status_obd_data));
		}

		if (vv.findViewWithTag(cmdID) != null) {
			TextView existingTV = (TextView) vv.findViewWithTag(cmdID);
			existingTV.setText(cmdResult);
		} else
			addTableRow(cmdID, cmdName, cmdResult);
		commandResult.put(cmdID, cmdResult);
		updateTripStatistic(job, cmdID);
	}

	private void updateTripStatistic(final ObdCommandJob job, final String cmdID) {

		if (currentTrip != null) {
			if (cmdID.equals(AvailableCommandNames.SPEED.toString())) {
				SpeedObdCommand command = (SpeedObdCommand) job.getCommand();
				currentTrip.setSpeedMax(command.getMetricSpeed());
			} else if (cmdID
					.equals(AvailableCommandNames.ENGINE_RPM.toString())) {
				EngineRPMObdCommand command = (EngineRPMObdCommand) job
						.getCommand();
				currentTrip.setEngineRpmMax(command.getRPM());
			} else if (cmdID.endsWith(AvailableCommandNames.ENGINE_RUNTIME
					.toString())) {
				EngineRuntimeObdCommand command = (EngineRuntimeObdCommand) job
						.getCommand();
				currentTrip.setEngineRuntime(command.getFormattedResult());
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {

			serverid = Integer.parseInt(prefs.getString(
					ConfigActivity.SERVER_ID_KEY, "1"));
		} catch (RuntimeException e) {
			serverid = 1;
		}

		// get Orientation sensor
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_ORIENTATION);
		if (sensors.size() > 0)
			orientSensor = sensors.get(0);
		else
			showDialog(NO_ORIENTATION_SENSOR);

		if (NetUtil.checkNet(this)) {
			new SocketAsyncTask().execute();
		} else {
			showDialog(NO_NETWORK);
		}

		context = this.getApplicationContext();
		// create a log instance for use by this application
		triplog = TripLog.getInstance(context);
	}

	private class SocketAsyncTask extends AsyncTask<Void, Void, Void> {

		private boolean connected = false;
		private ProgressDialog mypDialog;

		private String ip;

		@Override
		protected void onPreExecute() {

			ip = prefs.getString(ConfigActivity.UPLOAD_SERVER_KEY,
					"120.24.213.12");

			mypDialog = new ProgressDialog(MainActivity.this);
			// 实例化
			mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			// 设置进度条风格，风格为圆形，旋转的
			mypDialog.setTitle("提示");
			// 设置ProgressDialog 标题
			mypDialog.setMessage("正在连接到: " + ip);
			// 设置ProgressDialog 的一个Button
			mypDialog.setCancelable(false);
			// 设置ProgressDialog 是否可以按退回按键取消
			mypDialog.show();
			// 让ProgressDialog显示

		}

		@Override
		protected Void doInBackground(Void... entity) {

			try {
				socket = new Socket();
				// connect to server
				socket.connect(new InetSocketAddress(ip, 7000), 30000);

				System.out.println("connect to " + ip);
				MainActivity.this.readOnlineDevices();
				connected = true;
			} catch (Exception e) {
				e.printStackTrace();
				if (socket != null) {
					try {
						socket.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					socket = null;
				}
			}

			return null;

		}

		@Override
		protected void onPostExecute(Void result) {
			mypDialog.dismiss();
			if (connected) {
				Toast.makeText(context, "连接到" + ip, 0).show();
			} else {
				Toast.makeText(context, "client连接到" + ip + "失败", 0).show();
			}

		}

	}

	public void readOnlineDevices() throws IOException {
		ProtocolEntity msg = new ProtocolEntity();
		msg.setSide(ModelConstant.SIDE_SERVER);
		msg.setCode(ModelConstant.CODE_BEAT);
		msg.setContent(null);
		msg.setContentlen((byte) 0);
		msg.setDestdeviceid((short) 0);
		msg.setSequenceid((short) 0);
		msg.setSrcdeviceid((short) serverid);
		InputStream in = socket.getInputStream();
		OutputStream out = socket.getOutputStream();
		byte[] buf = ProtocolUtils.transToByte(msg);
		out.write(buf);
		buf = new byte[1024];
		int count = in.read(buf);
		if (count > 0) {
			msg = ProtocolUtils.createfromByte(buf);
			List<OnlineInfo> infos = new ArrayList<OnlineInfo>();
			if (msg.getCode() == ModelConstant.CODE_BEAT) {
				if (msg.getContentlen() > 0 && msg.getContent() != null
						&& msg.getContentlen() == msg.getContent().length) {
					byte[] blist = msg.getContent();
					int len = blist.length;
					if (len % 2 > 0) {
						len--;
					}
					for (int i = 0; i < len;) {
						int hi = blist[i];
						i++;
						int low = blist[i];
						i++;

						OnlineInfo info = new OnlineInfo();
						info.deviceid = (hi & 0xFF) * 256 + (low & 0xFF);
						info.devicename = "远程终端-" + info.deviceid;
						infos.add(info);
					}
				}
				OnlineInfo info = new OnlineInfo();
				info.deviceid = 1;
				info.devicename = "demo-1";
				infos.add(0, info);
				// update online list;
				Editor ed = prefs.edit();
				StringBuilder sb = new StringBuilder();
				for (OnlineInfo inf : infos) {
					sb.append(',').append(inf.deviceid).append("_")
							.append(inf.devicename);
				}
				sb.deleteCharAt(0);
				ed.putString(Constants.KEY_ONLINE_LIST, sb.toString());
				ed.commit();
				deviceready = true;
			} else {
				throw new IllegalStateException("find online devices error!");
			}

		} else {
			throw new IllegalStateException("find online devices error!");
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "Entered onStart...");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			socket = null;
		}

		if (mLocService != null) {
			mLocService.removeGpsStatusListener(this);
			mLocService.removeUpdates(this);
		}

		releaseWakeLockIfHeld();
		if (isServiceBound) {
			doUnbindService();
		}

		endTrip();

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
		if (wakeLock.isHeld())
			wakeLock.release();
	}

	protected void onResume() {
		super.onResume();
		Log.d(TAG, "Resuming..");
		sensorManager.registerListener(orientListener, orientSensor,
				SensorManager.SENSOR_DELAY_UI);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"ObdReader");

	}

	private void updateConfig() {
		startActivity(new Intent(this, ConfigActivity.class));
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, START_LIVE_DATA, 0,
				getString(R.string.menu_start_live_data));
		menu.add(0, STOP_LIVE_DATA, 0, getString(R.string.menu_stop_live_data));
		menu.add(0, GET_ONLINE, 0, getString(R.string.menu_get_online_list));
		// menu.add(0, TRIPS_LIST, 0, getString(R.string.menu_trip_list));
		menu.add(0, SETTINGS, 0, getString(R.string.menu_settings));
		return true;
	}

	// private void staticCommand() {
	// Intent commandIntent = new Intent(this, ObdReaderCommandActivity.class);
	// startActivity(commandIntent);
	// }

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case START_LIVE_DATA:
			startLiveData();
			return true;
		case STOP_LIVE_DATA:
			stopLiveData();
			return true;
		case GET_ONLINE:
			startActivity(new Intent(this, OnLineDeviceActivity.class));
			return true;
		case SETTINGS:
			updateConfig();
			return true;
		case TRIPS_LIST:
			startActivity(new Intent(this, TripListActivity.class));
			return true;
			// case COMMAND_ACTIVITY:
			// staticCommand();
			// return true;
		}
		return false;
	}

	// private void getTroubleCodes() {
	// startActivity(new Intent(this, TroubleCodesActivity.class));
	// }

	private void startLiveData() {
		Log.d(TAG, "Starting live data..");
		if (!NetUtil.checkNet(this)) {
			showDialog(NO_NETWORK);
			return;
		}
		tl.removeAllViews(); // start fresh
		doBindService();

		currentTrip = triplog.startTrip();
		if (currentTrip == null)
			showDialog(SAVE_TRIP_NOT_AVAILABLE);

		// start command execution
		new Handler().post(mQueueCommands);

		gpsStatusTextView.setText(getString(R.string.status_gps_not_used));

		// screen won't turn off until wakeLock.release()
		wakeLock.acquire();
	}

	private void stopLiveData() {
		Log.d(TAG, "Stopping live data..");

		doUnbindService();
		endTrip();

		releaseWakeLockIfHeld();
	}

	protected void endTrip() {
		if (currentTrip != null) {
			currentTrip.setEndDate(new Date());
			triplog.updateRecord(currentTrip);
		}
	}

	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder build = new AlertDialog.Builder(this);
		switch (id) {
		case NO_ORIENTATION_SENSOR:
			build.setMessage(getString(R.string.text_no_orientation_sensor));
			return build.create();
		case NO_NETWORK:
			build.setMessage(getString(R.string.text_no_network_available));
			return build.create();
		case SAVE_TRIP_NOT_AVAILABLE:
			build.setMessage(getString(R.string.text_save_trip_not_available));
			return build.create();
		}
		return null;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem startItem = menu.findItem(START_LIVE_DATA);
		MenuItem stopItem = menu.findItem(STOP_LIVE_DATA);
		MenuItem settingsItem = menu.findItem(SETTINGS);
		MenuItem getOnlineItem = menu.findItem(GET_ONLINE);

		if (service != null && service.isRunning()) {
			getOnlineItem.setEnabled(false);
			startItem.setEnabled(false);
			stopItem.setEnabled(true);
			settingsItem.setEnabled(false);
		} else {
			getOnlineItem.setEnabled(deviceready);
			stopItem.setEnabled(false);
			startItem.setEnabled(NetUtil.checkNet(this) && deviceready);
			settingsItem.setEnabled(true);
		}

		return true;
	}

	private void addTableRow(String id, String key, String val) {

		TableRow tr = new TableRow(this);
		MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(TABLE_ROW_MARGIN, TABLE_ROW_MARGIN, TABLE_ROW_MARGIN,
				TABLE_ROW_MARGIN);
		tr.setLayoutParams(params);

		TextView name = new TextView(this);
		name.setGravity(Gravity.RIGHT);
		name.setText(key + ": ");
		TextView value = new TextView(this);
		value.setGravity(Gravity.LEFT);
		value.setText(val);
		value.setTag(id);
		tr.addView(name);
		tr.addView(value);
		tl.addView(tr, params);
	}

	/**
   *
   */
	private void queueCommands() {
		if (isServiceBound) {
			for (ObdCommand Command : ObdConfig.getCommands()) {
				if (prefs.getBoolean(Command.getName(), true))
					service.queueJob(new ObdCommandJob(Command));
			}
		}
	}

	private void doBindService() {
		if (!isServiceBound) {
			Log.d(TAG, "Binding OBD service..");
			if (NetUtil.checkNet(this) && socket != null) {
				// btStatusTextView.setText(getString(R.string.status_bluetooth_connecting));
				Intent serviceIntent = new Intent(this, ObdGatewayService.class);
				bindService(serviceIntent, serviceConn,
						Context.BIND_AUTO_CREATE);
			} else {
				// btStatusTextView.setText(getString(R.string.status_bluetooth_disabled));
				// Intent serviceIntent = new Intent(this,
				// MockObdGatewayService.class);
				// bindService(serviceIntent, serviceConn,
				// Context.BIND_AUTO_CREATE);
			}
		}
	}

	private void doUnbindService() {
		if (isServiceBound) {
			if (service.isRunning()) {
				service.stopService();
				// if (preRequisites)
				// btStatusTextView.setText(getString(R.string.status_bluetooth_ok));
			}
			Log.d(TAG, "Unbinding OBD service..");
			unbindService(serviceConn);
			isServiceBound = false;
			obdStatusTextView
					.setText(getString(R.string.status_obd_disconnected));
		}
	}

	/**
	 * Uploading asynchronous task
	 */
	private class UploadAsyncTask extends AsyncTask<ProtocolEntity, Void, Void> {

		@Override
		protected Void doInBackground(ProtocolEntity... entity) {
			/*
			 * Log.d(TAG, "Uploading " + readings.length + " readings.."); //
			 * instantiate reading service client final String endpoint =
			 * prefs.getString(ConfigActivity.UPLOAD_URL_KEY, ""); RestAdapter
			 * restAdapter = new RestAdapter.Builder() .setEndpoint(endpoint)
			 * .build(); ObdService service =
			 * restAdapter.create(ObdService.class); // upload readings for
			 * (ObdReading reading : readings) { try { Response response =
			 * service.uploadReading(reading); assert response.getStatus() ==
			 * 200; } catch (RetrofitError re) {Log.e(TAG, re.toString());}
			 * 
			 * } Log.d(TAG, "Done"); return null;
			 */
			return null;
		}

	}

	public void onLocationChanged(Location location) {
		mLastLocation = location;
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onProviderDisabled(String provider) {
	}

	public void onGpsStatusChanged(int event) {

		switch (event) {
		case GpsStatus.GPS_EVENT_STARTED:
			gpsStatusTextView.setText(getString(R.string.status_gps_started));
			break;
		case GpsStatus.GPS_EVENT_STOPPED:
			gpsStatusTextView.setText(getString(R.string.status_gps_stopped));
			break;
		case GpsStatus.GPS_EVENT_FIRST_FIX:
			gpsStatusTextView.setText(getString(R.string.status_gps_fix));
			break;
		case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
			break;
		}
	}
}
