package com.skywin.obdclient.activity;

import java.util.Date;
import java.util.Random;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.util.CommonUtils;
import com.skywin.obd.util.NetUtil;
import com.skywin.obdclient.R;
import com.skywin.obdclient.thread.DemoThread;

public class MainActivity extends Activity {

	private DemoThread demoThread;

	private DemoHandler handler = new DemoHandler();

	private static final String CONFIG_SERVERIP = "serverip";

	private static final int port = 7000;

	private long lastBackTime = System.currentTimeMillis();

	private RelativeLayout rsplash;

	private LinearLayout lprocessing;

	private LinearLayout lnetnone;

	private TextView tvnet;

	private Button btnreconnet;

	private LinearLayout ldemolog;

	private ListView lvcmdlog;

	private ArrayAdapter<String> cmdlogAdapter;

	private LinearLayout lsetting;

	private EditText etip;

	private int deviceid = 0;

	private String serverip;

	private boolean hasNet = false;

	private void findViews() {

		rsplash = (RelativeLayout) findViewById(R.id.rl_splash);

		lprocessing = (LinearLayout) rsplash.findViewById(R.id.ll_processing);

		lnetnone = (LinearLayout) rsplash.findViewById(R.id.ll_netnone);

		tvnet = (TextView) lnetnone.findViewById(R.id.tv_net);

		btnreconnet = (Button) lnetnone.findViewById(R.id.btn_reconnect);

		if (!hasNet) {
			lprocessing.setVisibility(View.GONE);
			lnetnone.setVisibility(View.VISIBLE);
			tvnet.setText("需要网络连接,请打开网络连接后点击\"重试\"按钮");
			btnreconnet.setText("重试");
		} else {
			lprocessing.setVisibility(View.VISIBLE);
			lnetnone.setVisibility(View.GONE);
		}

		lsetting = (LinearLayout) findViewById(R.id.ll_setting);
		etip = (EditText) findViewById(R.id.et_ip);

		ldemolog = (LinearLayout) findViewById(R.id.ll_demolog);

		lvcmdlog = (ListView) ldemolog.findViewById(R.id.lv_cmdlog);

		cmdlogAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		lvcmdlog.setAdapter(cmdlogAdapter);

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Random ran = new Random();
		deviceid = ran.nextInt(100);
		while (deviceid <= 1) {
			deviceid = ran.nextInt(100);
		}
		serverip = findSpValue(CONFIG_SERVERIP);
		if (serverip.isEmpty()) {
			serverip = ModelConstant.DEF_SERVER_IP;
			saveSpValue(CONFIG_SERVERIP, serverip);
		}
		hasNet = NetUtil.checkNet(this);

		if (hasNet) {
			demoThread = new DemoThread(serverip, port, deviceid, handler);
			new Thread(demoThread).start();
		}
		findViews();
		rsplash.setVisibility(View.VISIBLE);
		lsetting.setVisibility(View.GONE);
		ldemolog.setVisibility(View.GONE);

	}

	private String findSpValue(String key) {
		SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
		return sp.getString(key, "");
	}

	private void saveSpValue(String key, String value) {
		SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
		sp.edit().putString(key, value).commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			lsetting.setVisibility(View.VISIBLE);
			ldemolog.setVisibility(View.GONE);
			etip.setText(findSpValue(CONFIG_SERVERIP));
			return true;
		} else if (id == R.id.action_exit) {
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK
				&& KeyEvent.ACTION_DOWN == event.getAction()) {
			if (System.currentTimeMillis() - lastBackTime <= 15000) {
				finish();
			} else {
				lastBackTime = System.currentTimeMillis();
				Toast.makeText(this, "再按一次退出", 0).show();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	public void onReconnect(View btn) {
		hasNet = NetUtil.checkNet(this);
		if (!hasNet) {
			showToast("请打开网络连接之后再\"重试\"");
			return;
		} else {
			if (demoThread != null) {
				demoThread.destroy();
			}
			demoThread = new DemoThread(serverip, 7000, deviceid, handler);
			new Thread(demoThread).start();
			lnetnone.setVisibility(View.GONE);
			lprocessing.setVisibility(View.VISIBLE);
		}

	}

	public void onSaveSetting(View btn) {
		btnreconnet.setVisibility(View.VISIBLE);
		if (!hasNet) {
			showToast("请首先打开网络连接");
			return;
		}
		String ip = etip.getText().toString();
		if (TextUtils.isEmpty(ip)) {
			showToast("需要输入服务器地址");
			return;
		}
		saveSpValue(CONFIG_SERVERIP, ip);
		if (!ip.equals(serverip)) {
			serverip = ip;
			if (hasNet) {
				if (demoThread != null) {
					demoThread.destroy();
				}
				demoThread = new DemoThread(serverip, 7000, deviceid, handler);
				new Thread(demoThread).start();
			}
			rsplash.setVisibility(View.VISIBLE);
			ldemolog.setVisibility(View.GONE);
			lsetting.setVisibility(View.GONE);
		} else {
			ldemolog.setVisibility(View.GONE);
			rsplash.setVisibility(View.GONE);
			lsetting.setVisibility(View.GONE);
		}
	}

	private void prependCmdList(String str, int kind) {
		String prefix = "";
		switch (kind) {
		case 1:
			prefix = "接收命令";
			break;
		case 2:
			prefix = "返回数据";
			break;
		default:
			break;
		}
		str = prefix + ": [" + str + "]--" + CommonUtils.format(new Date());
		cmdlogAdapter.setNotifyOnChange(false);
		if (cmdlogAdapter.getCount() == 20) {
			int count = cmdlogAdapter.getCount();
			cmdlogAdapter.remove(cmdlogAdapter.getItem(count - 1));
		}
		cmdlogAdapter.setNotifyOnChange(true);
		cmdlogAdapter.insert(str, 0);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (demoThread != null) {
			demoThread.destroy();
		}
	}

	private class DemoHandler extends Handler {
		@SuppressWarnings("unchecked")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				showToast(msg.obj.toString());
				if (msg.arg1 != 0) {
					lprocessing.setVisibility(View.GONE);
					tvnet.setText("无法连接到" + serverip
							+ "请检查服务器ip设置\n提示: 如果使用的是内网地址,请确保处于局域网环境中!");
					btnreconnet.setVisibility(View.GONE);
					lnetnone.setVisibility(View.VISIBLE);
				}else{
					rsplash.setVisibility(View.GONE);
					ldemolog.setVisibility(View.VISIBLE);
				}
				break;
			case 1:
			case 2:
				prependCmdList(msg.obj.toString(), msg.what);
				break;
			default:
				break;
			}
		}

	}

	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

}
