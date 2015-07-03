package com.skywin.obdserver.adapter;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skywin.obd.model.OnlineInfo;
import com.skywin.obdserver.R;
import com.skywin.obdserver.callback.ConnectCallback;

public class OnLineListAdapter extends BaseAdapter implements OnClickListener {

	private List<OnlineInfo> onlinelist;

	private Context context;

	private ConnectCallback connectCallback;

	public OnLineListAdapter(Context context, List<OnlineInfo> onlinelist,
			ConnectCallback connectCallback) {
		this.context = context;
		this.onlinelist = onlinelist;
		this.connectCallback = connectCallback;
	}

	@Override
	public int getCount() {
		return onlinelist.size();
	}

	@Override
	public Object getItem(int position) {
		return onlinelist.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LinearLayout item = null;
		OnlineInfo info = onlinelist.get(position);
		if (convertView != null) {
			item = (LinearLayout) convertView;
			ViewHolder holder = (ViewHolder) item.getTag();
			holder.deviceid = info.deviceid;
			holder.devicename = info.devicename;
			TextView tvname = holder.tvname;
			tvname.setText(holder.devicename);
		} else {
			item = (LinearLayout) View.inflate(context,
					R.layout.onlinelistitem, null);
			ViewHolder holder = new ViewHolder();
			TextView tvname = (TextView) item.findViewById(R.id.tv_name);
			Button btnconnect = (Button) item.findViewById(R.id.btn_connect);

			holder.tvname = tvname;
			holder.btnconnect = btnconnect;
			item.setTag(holder);
			tvname.setText(info.devicename);
			holder.deviceid = info.deviceid;
			holder.devicename = info.devicename;
			btnconnect.setOnClickListener(this);

		}

		return item;
	}

	private static class ViewHolder {
		private TextView tvname;
		private Button btnconnect;
		private int deviceid;
		private String devicename;
	}

	@Override
	public void onClick(View v) {
		View parent = (View) v.getParent();
		ViewHolder holder = (ViewHolder) parent.getTag();
		OnlineInfo info = new OnlineInfo();
		info.deviceid = holder.deviceid;
		info.devicename = holder.devicename;
		connectCallback.connectCallback(info);
	}
}
