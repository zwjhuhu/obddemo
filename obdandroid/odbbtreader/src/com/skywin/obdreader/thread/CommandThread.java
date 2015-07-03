package com.skywin.obdreader.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.skywin.obd.DemoConstant;

import eu.lighthouselabs.obd.reader.io.ObdCommandJob;
import eu.lighthouselabs.obd.reader.io.ObdCommandJob.ObdCommandJobState;

public class CommandThread implements Runnable {

	private static final String TAG = "CommandThread";

	private Handler handler;

	private volatile boolean isRunning = true;

	private BluetoothDemoService demoservice;

	private BlockingQueue<ObdCommandJob> queue = new LinkedBlockingQueue<ObdCommandJob>();

	public CommandThread(Handler handler, BluetoothDemoService demoservice) {
		this.handler = handler;
		this.demoservice = demoservice;
	}

	@Override
	public void run() {
		if (demoservice != null) {
			demoservice.start();
		}
		while (isRunning) {
			try {
				executeQueue();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void destroy() {
		isRunning = false;
		try {
			queue.clear();
			demoservice.stop();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				Thread.sleep(5000);
				Thread.currentThread().interrupt();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		demoservice = null;
		Log.i(TAG, "cmdThread exit!");
	}

	/**
	 * Runs the queue until the service is stopped
	 */
	private void executeQueue() {
		Log.d(TAG, "running queue!");
		ObdCommandJob job = null;
		try {
			job = queue.poll(5, TimeUnit.SECONDS);
			if (job == null) {
				return;
			}

			// log job
			Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

			if (job.getState().equals(ObdCommandJobState.NEW)) {
				Log.d(TAG, "Job state is NEW. Run it..");

				job.setState(ObdCommandJobState.RUNNING);
				job.getCommand().run(demoservice.getInputStream(),
						demoservice.getOutputStream());
			} else {
				// log not new job
				Log.e(TAG,
						"Job state was not new, so it shouldn't be in queue. BUG ALERT!");
			}
		} catch (Exception e) {
			job.setState(ObdCommandJobState.EXECUTION_ERROR);
			Log.e(TAG, "Failed to run command. -> " + e.getMessage());
			job = null;
		}

		if (job != null) {
			Log.d(TAG, "Job is finished.");
			job.setState(ObdCommandJobState.FINISHED);
			Message msg = handler.obtainMessage(DemoConstant.MESSAGE_UI, job);
			handler.sendMessage(msg);

		}

	}

	public void addJobToQueue(ObdCommandJob job) {
		queue.add(job);
	}

}