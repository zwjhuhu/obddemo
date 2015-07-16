package com.skywin.obd.remote.activity;

import java.util.ArrayList;
import java.util.List;

import roboguice.RoboGuice;
import roboguice.activity.RoboActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.github.pires.obd.reader.config.Constants;
import com.google.inject.Inject;
import com.skywin.obd.model.OnlineInfo;
import com.skywin.obd.remote.R;
import com.skywin.obd.remote.adapter.ConnectCallback;
import com.skywin.obd.remote.adapter.OnLineListAdapter;

@ContentView(R.layout.onlinelist)
public class OnLineDeviceActivity extends RoboActivity implements
		ConnectCallback {

	@Inject
	private SharedPreferences prefs;

	@InjectView(R.id.lv_online)
	ListView lvonline;

	private OnLineListAdapter onLineListAdapter;

	static {
		RoboGuice.setUseAnnotationDatabases(false);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		List<OnlineInfo> onlinelist = new ArrayList<OnlineInfo>();
		String str = prefs.getString(Constants.KEY_ONLINE_LIST, "");
		String[] pairs = str.split(",");
		for (String pair : pairs) {
			OnlineInfo info = new OnlineInfo();
			String[] vals = pair.split("_");
			info.deviceid = Integer.parseInt(vals[0]);
			info.devicename = vals[1];
			onlinelist.add(info);
		}
		onLineListAdapter = new OnLineListAdapter(this, onlinelist, this);

		lvonline.setAdapter(onLineListAdapter);

	}

	@Override
	public void connectCallback(OnlineInfo info) {
		prefs.edit().putInt(Constants.KEY_CHOOSE_DEVICEID, info.deviceid)
				.commit();
		finish();

	}

}
