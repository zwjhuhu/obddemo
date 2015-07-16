package com.github.pires.obd.reader.io;

import java.io.IOException;

import pt.lighthouselabs.obd.commands.ObdCommand;
import android.os.Binder;
import android.util.Log;

import com.github.pires.obd.reader.config.Constants;
import com.github.pires.obd.reader.io.ObdCommandJob.ObdCommandJobState;
import com.skywin.obd.remote.activity.ConfigActivity;
import com.skywin.obd.remote.activity.MainActivity;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a more
 * OBD Bluetooth interface.
 * <p/>
 * Secondarily, it will serve as a repository of ObdCommandJobs and at the same
 * time the application state-machine.
 */
public class ObdGatewayService extends AbstractGatewayService {

	private static final String TAG = ObdGatewayService.class.getName();

	// private final IBinder binder = new ObdGatewayServiceBinder();

	private int deviceid;
	private short sequenceid = 0;

	public void startService() throws IOException {
		Log.d(TAG, "Starting service..");

		isRunning = true;

		// Let's configure the connection.
//		Log.d(TAG, "Queueing jobs for connection configuration..");
//		queueJob(new ObdCommandJob(new ObdResetCommand()));
//		queueJob(new ObdCommandJob(new EchoOffObdCommand()));
//
//		/*
//		 * Will send second-time based on tests.
//		 * 
//		 * TODO this can be done w/o having to queue jobs by just issuing
//		 * command.run(), command.getResult() and validate the result.
//		 */
//		queueJob(new ObdCommandJob(new EchoOffObdCommand()));
//		queueJob(new ObdCommandJob(new LineFeedOffObdCommand()));
//		queueJob(new ObdCommandJob(new TimeoutObdCommand(62)));
//
//		// Get protocol from preferences
//		String protocol = prefs.getString(ConfigActivity.PROTOCOLS_LIST_KEY,
//				"AUTO");
//		queueJob(new ObdCommandJob(new SelectProtocolObdCommand(
//				ObdProtocols.valueOf(protocol))));
//
//		// Job for returning dummy data
//		queueJob(new ObdCommandJob(new AmbientAirTemperatureObdCommand()));

		queueCounter = 0L;
		Log.d(TAG, "Initialization jobs queued.");

	}

	/**
	 * This method will add a job to the queue while setting its ID to the
	 * internal queue counter.
	 * 
	 * @param job
	 *            the job to queue.
	 */
	@Override
	public void queueJob(ObdCommandJob job) {
		// This is a good place to enforce the imperial units option
		job.getCommand().useImperialUnits(
				prefs.getBoolean(ConfigActivity.IMPERIAL_UNITS_KEY, false));

		// Now we can pass it along
		super.queueJob(job);
	}

	/**
	 * Runs the queue until the service is stopped
	 */
	protected void executeQueue() throws InterruptedException {
		Log.d(TAG, "Executing queue..");
		while (!Thread.currentThread().isInterrupted()) {
			ObdCommandJob job = null;
			ObdCommand cmd = null;
			try {
				job = jobsQueue.take();
				deviceid = prefs.getInt(Constants.KEY_CHOOSE_DEVICEID, 1);
				Log.i(TAG,"deviceid:"+deviceid);
				// log job
				Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

				if (job.getState().equals(ObdCommandJobState.NEW)) {
					Log.d(TAG, "Job state is NEW. Run it..");
					job.setState(ObdCommandJobState.RUNNING);
					cmd = job.getCommand();
					cmd.setServerid((short)serverid);
					cmd.setDeviceid((short)deviceid);
					cmd.setSequenceid(sequenceid);
					cmd.run(socket.getInputStream(),
							socket.getOutputStream());
					sequenceid = (short) ((0xFFFF&sequenceid)+1);
				} else
					// log not new job
					Log.e(TAG,
							"Job state was not new, so it shouldn't be in queue. BUG ALERT!");
			} catch (InterruptedException i) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				job.setState(ObdCommandJobState.EXECUTION_ERROR);
				Log.e(TAG, "Failed to run command. "+cmd.getName()+"-> " + e.getMessage());
				job = null;
			}

			if (job != null) {
				final ObdCommandJob job2 = job;
				((MainActivity) ctx).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((MainActivity) ctx).stateUpdate(job2);
					}
				});
			}
		}
	}

	/**
	 * Stop OBD connection and queue processing.
	 */
	public void stopService() {
		Log.d(TAG, "Stopping service..");

//		notificationManager.cancel(NOTIFICATION_ID);
		jobsQueue.removeAll(jobsQueue); // TODO is this safe?
		isRunning = false;
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			socket = null;
		}

		// kill service
		stopSelf();
	}

	public boolean isRunning() {
		return isRunning;
	}

	public class ObdGatewayServiceBinder extends Binder {
		public ObdGatewayService getService() {
			return ObdGatewayService.this;
		}
	}

}
