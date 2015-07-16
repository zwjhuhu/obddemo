package com.github.pires.obd.reader.io;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import roboguice.service.RoboService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.inject.Inject;
import com.skywin.obd.reader.activity.ConfigActivity;
import com.skywin.obdclient.thread.SocketThread;

public abstract class AbstractGatewayService extends RoboService {
	private static final String TAG = AbstractGatewayService.class.getName();
//	public static final int NOTIFICATION_ID = 1;
//	@Inject
//	protected NotificationManager notificationManager;

	protected Context ctx;

	@Inject
	protected SharedPreferences prefs;
	protected boolean isRunning = false;
	private final IBinder binder = new AbstractGatewayServiceBinder();
	protected boolean isQueueRunning = false;
	protected Long queueCounter = 0L;
	protected BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<>();

	protected final ConcurrentHashMap<String, String> resultMap = new ConcurrentHashMap<String, String>();
	// Run the executeQueue in a different thread to lighten the UI thread
	Thread t = new Thread(new Runnable() {
		@Override
		public void run() {
			try {
				executeQueue();
			} catch (InterruptedException e) {
				t.interrupt();
			}
		}
	});

	private Thread socketThread;

	protected SocketThread socketRun;

	//private short sequenceid;

	protected void initSocketThread() {
		int deviceid = 10;
		try {

			deviceid = Integer.parseInt(prefs.getString(
					ConfigActivity.VEHICLE_ID_KEY, "10"));
		} catch (RuntimeException e) {
			deviceid = 10;
		}

		String ip = prefs.getString(ConfigActivity.UPLOAD_SERVER_KEY,
				"120.24.213.12");
		socketRun = new SocketThread(ip, 7000, deviceid, this);
		socketThread = new Thread(socketRun);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "Socket Thread start..");
		initSocketThread();
		socketThread.start();
		Log.d(TAG, "Creating service..");
		t.start();
		Log.d(TAG, "Service created.");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "Destroying service...");
		//notificationManager.cancel(NOTIFICATION_ID);
		t.interrupt();

		if (socketRun != null) {
			socketRun.destroy();
			socketThread.interrupt();
		}
		Log.d(TAG, "Service destroyed.");
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean queueEmpty() {
		return jobsQueue.isEmpty();
	}

	public class AbstractGatewayServiceBinder extends Binder {
		public AbstractGatewayService getService() {
			return AbstractGatewayService.this;
		}
	}

	/**
	 * This method will add a job to the queue while setting its ID to the
	 * internal queue counter.
	 * 
	 * @param job
	 *            the job to queue.
	 */
	public void queueJob(ObdCommandJob job) {
		queueCounter++;
		Log.d(TAG, "Adding job[" + queueCounter + "] to queue..");

		job.setId(queueCounter);
		try {
			jobsQueue.put(job);
			Log.d(TAG, "Job queued successfully.");
		} catch (InterruptedException e) {
			job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
			Log.e(TAG, "Failed to queue job.");
		}
	}

	/**
	 * Show a notification while this service is running.
	 */
//	protected void showNotification(String contentTitle, String contentText,
//			int icon, boolean ongoing, boolean notify, boolean vibrate) {
//		final PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
//				new Intent(ctx, MainActivity.class), 0);
//		final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
//				ctx);
//		notificationBuilder.setContentTitle(contentTitle)
//				.setContentText(contentText).setSmallIcon(icon)
//				.setContentIntent(contentIntent)
//				.setWhen(System.currentTimeMillis());
//		// can cancel?
//		if (ongoing) {
//			notificationBuilder.setOngoing(true);
//		} else {
//			notificationBuilder.setAutoCancel(true);
//		}
//		if (vibrate) {
//			notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
//		}
//		if (notify) {
//			notificationManager.notify(NOTIFICATION_ID,
//					notificationBuilder.getNotification());
//		}
//	}

	public void setContext(Context c) {
		ctx = c;
	}

	abstract protected void executeQueue() throws InterruptedException;

	abstract public void startService() throws IOException;

	abstract public void stopService();

	public void setPrefs(SharedPreferences prefs) {
		this.prefs = prefs;
	}

	public String readMsg(String key) {
		if (key == null) {
			return null;
		}
		if(key.equals("03")){
			return null;
		}
		return resultMap.get(key);

	}
}
