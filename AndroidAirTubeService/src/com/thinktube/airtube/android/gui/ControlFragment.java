package com.thinktube.airtube.android.gui;

import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.TextView;

import com.thinktube.airtube.android.R;

public class ControlFragment extends Fragment {
	interface ControlListener {
		public void start();
		public void stop();
		public void flush();
		public void test(); /* generic "test" button, function can be assigned internally in AT */
		public boolean isStarted();
		public boolean isMonitorConnected();
		public void registerMonitor();
		public void unregisterMonitor();
		public void setProxy(InetAddress ip);
		public void setOnDemandEnabled(boolean on);
	}

	String localIP;
	long deviceId;
	ControlListener control;
	TextView ip;
	TextView devId;
	private EditText proxyIp;
	private ToggleButton startToggle;
	private ToggleButton guiToggle;
	private ToggleButton proxyToggle;
	private ToggleButton odToggle;

	/* used to avoid setOnCheckedChangeListener updates while programatically updating toggle state with setChecked() */
	private boolean internalToggleUpdate;

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            control = (ControlListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ControlListener");
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.control_view, container, false);

		startToggle = (ToggleButton) rootView.findViewById(R.id.startToggle);
		startToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean on) {
				if (internalToggleUpdate) {
					return;
				}

				if (on) {
					control.start();
				} else {
					control.stop();
				}
			}
		});

		Button flush = (Button) rootView.findViewById(R.id.flushButton);
		flush.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				control.flush();
			}
		});
		
		Button test = (Button) rootView.findViewById(R.id.testButton);
		test.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				control.test();
			}
		});

		guiToggle = (ToggleButton) rootView.findViewById(R.id.guiToggle);
		guiToggle.setChecked(control.isMonitorConnected());

		guiToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean on) {
				if (internalToggleUpdate) {
					return;
				}

				if (on) {
					control.registerMonitor();
				} else {
					control.unregisterMonitor();
				}
			}
		});

		ip = (TextView) rootView.findViewById(R.id.ipText);
		if (localIP != null) {
			ip.setText(localIP);
		}

		devId = (TextView) rootView.findViewById(R.id.devIdText);
		setDeviceID(deviceId);
		proxyIp = (EditText) rootView.findViewById(R.id.proxyIP);
		proxyToggle = (ToggleButton) rootView.findViewById(R.id.proxyToggle);

		proxyToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean on) {
				if (on) {
					try {
						InetAddress proxyIP = InetAddress.getByName(proxyIp.getText().toString());
						control.setProxy(proxyIP);
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
				} else {
					control.setProxy(null);
				}
			}
		});

		odToggle = (ToggleButton) rootView.findViewById(R.id.odToggle);
		odToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean on) {
				if (!internalToggleUpdate) {
					control.setOnDemandEnabled(on);
				}
			}
		});
		return rootView;
	}

	public void setIP(final String iface, final String[] ips) {
		String s = new String();
		for (int i=0; i < ips.length; i++) {
			s += ips[i];
			if (i < ips.length-1)
				s += ", ";
		}
		s += " (" + iface + ")";
		localIP = s;

		if (ip != null) {
			ip.setText(s);
		}
	}

	public void setDeviceID(long id) {
		deviceId = id;
		if (devId != null) {
			devId.setText(Long.toHexString(id));
		}
	}

	public void updateState(final boolean started, final boolean gui, final boolean onDemand) {
		internalToggleUpdate = true;
		if (startToggle != null)
			startToggle.setChecked(started);
		if (guiToggle != null)
			guiToggle.setChecked(gui);
		if (odToggle != null)
			odToggle.setChecked(onDemand);
		internalToggleUpdate = false;
	}
}
