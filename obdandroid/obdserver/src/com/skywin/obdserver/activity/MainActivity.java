package com.skywin.obdserver.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.skywin.obd.model.ModelConstant;
import com.skywin.obd.model.OnlineInfo;
import com.skywin.obd.util.CommonUtils;
import com.skywin.obd.util.NetUtil;
import com.skywin.obdserver.R;
import com.skywin.obdserver.adapter.OnLineListAdapter;
import com.skywin.obdserver.callback.ConnectCallback;
import com.skywin.obdserver.thread.DemoThread;

public class MainActivity extends Activity implements OnItemSelectedListener {

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

	private LinearLayout lonlinelist;

	private ListView lvonline;

	private OnLineListAdapter onLineListAdapter;

	private LinearLayout ldemolog;

	private ListView lvcmdlog;

	private ArrayAdapter<String> cmdlogAdapter;

	private Spinner spcmd;

	private Button btnsendcmd;

	private Button btnautocmd;

	private String curCmd = "01";

	private boolean isautocmdsend = false;

	private LinearLayout lsetting;

	private EditText etip;

	private List<OnlineInfo> onlinelist = new ArrayList<OnlineInfo>();

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

		lonlinelist = (LinearLayout) findViewById(R.id.ll_onlinelist);
		lvonline = (ListView) lonlinelist.findViewById(R.id.lv_online);
		// 加一个demo
		OnlineInfo info = new OnlineInfo();
		info.deviceid = 1;
		info.devicename = "demo发送端";
		onlinelist.add(info);

		onLineListAdapter = new OnLineListAdapter(this, onlinelist,
				new ConnectCallback() {

					@Override
					public void connectCallback(OnlineInfo info) {
						deviceid = info.deviceid;
						demoThread.connectToDevice(deviceid);
						ldemolog.setVisibility(View.VISIBLE);
						rsplash.setVisibility(View.GONE);
						lonlinelist.setVisibility(View.GONE);
						lsetting.setVisibility(View.GONE);
						cmdlogAdapter.clear();
					}
				});
		
		lvonline.setAdapter(onLineListAdapter);
		

		lsetting = (LinearLayout) findViewById(R.id.ll_setting);
		etip = (EditText) findViewById(R.id.et_ip);

		ldemolog = (LinearLayout) findViewById(R.id.ll_demolog);

		lvcmdlog = (ListView) ldemolog.findViewById(R.id.lv_cmdlog);

		cmdlogAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		lvcmdlog.setAdapter(cmdlogAdapter);

		spcmd = (Spinner) ldemolog.findViewById(R.id.sp_cmd);
		spcmd.setAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, new String[] { "油量", "引擎",
						"压力", "温度", "重置" }));

		spcmd.setOnItemSelectedListener(this);

		btnsendcmd = (Button) ldemolog.findViewById(R.id.btn_sendcmd);
		btnautocmd = (Button) ldemolog.findViewById(R.id.btn_autocmd);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		serverip = findSpValue(CONFIG_SERVERIP);
		if (serverip.isEmpty()) {
			serverip = ModelConstant.DEF_SERVER_IP;
			saveSpValue(CONFIG_SERVERIP, serverip);
		}
		hasNet = NetUtil.checkNet(this);

		if (hasNet) {
			demoThread = new DemoThread(serverip, port, handler);
			new Thread(demoThread).start();
		}
		findViews();
		rsplash.setVisibility(View.VISIBLE);
		lonlinelist.setVisibility(View.GONE);
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
			lonlinelist.setVisibility(View.GONE);
			etip.setText(findSpValue(CONFIG_SERVERIP));
			return true;
		} else if (id == R.id.action_online) {
			lonlinelist.setVisibility(View.VISIBLE);
			ldemolog.setVisibility(View.GONE);
			lsetting.setVisibility(View.GONE);
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
			demoThread = new DemoThread(serverip, 7000, handler);
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
				demoThread = new DemoThread(serverip, 7000, handler);
				new Thread(demoThread).start();
			}
			rsplash.setVisibility(View.VISIBLE);
			lonlinelist.setVisibility(View.GONE);
			ldemolog.setVisibility(View.GONE);
			lsetting.setVisibility(View.GONE);
		} else {
			lonlinelist.setVisibility(View.VISIBLE);
			ldemolog.setVisibility(View.GONE);
			rsplash.setVisibility(View.GONE);
			lsetting.setVisibility(View.GONE);
		}
	}

	public void onSendCmd(View btn) {
		demoThread.requestCmd(curCmd);
	}

	public void onAutoCmd(View btn) {
		isautocmdsend = !isautocmdsend;
		spcmd.setEnabled(!isautocmdsend);
		if (isautocmdsend) {
			btnsendcmd.setVisibility(View.GONE);
			handler.sendEmptyMessage(10);
			btnautocmd.setText("停止自动发送");
		} else {
			btnsendcmd.setVisibility(View.VISIBLE);
			btnautocmd.setText("自动发送");
		}

	}
	
	public void onRefreshOnline(View btn) {
		hasNet = NetUtil.checkNet(this);
		if(!hasNet){
			showToast("需要首先打开网络连接");
			return ;
		}
		if(isautocmdsend){
			onAutoCmd(null);
		}
		if(demoThread==null){
			demoThread = new DemoThread(serverip, port, handler);
			new Thread(demoThread).start();
		}else{
			demoThread.requestOnine();
		}
		

	}
	

	private void prependCmdList(String str) {
		str = "响应数据: [" + str + "]--" + CommonUtils.format(new Date());
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
				}
				break;
			case 1: {
				rsplash.setVisibility(View.GONE);
				lonlinelist.setVisibility(View.VISIBLE);
				lsetting.setVisibility(View.GONE);
				ldemolog.setVisibility(View.GONE);
				OnlineInfo demo = onlinelist.get(0);
				onlinelist.clear();
				onlinelist.add(demo);
				if (msg.obj != null) {
					List<OnlineInfo> rs = (List<OnlineInfo>) msg.obj;
					onlinelist.addAll(rs);
				}
				onLineListAdapter.notifyDataSetChanged();
			}
				break;
			case 2: {
				prependCmdList(msg.obj.toString());
			}
				break;
			case 10: {
				demoThread.requestCmd(curCmd);
				curCmd = "0" + ((Integer.parseInt(curCmd) + 1)%(spcmd.getCount()+1));
				spcmd.setSelection((spcmd.getSelectedItemPosition() + 1)
						% spcmd.getCount());
				if (isautocmdsend) {
					this.sendEmptyMessageDelayed(10, 3000);
				}
			}

				break;
			default:
				break;
			}
		}

	}

	private void showToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		curCmd = "0" + (position + 1);

	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}
}
