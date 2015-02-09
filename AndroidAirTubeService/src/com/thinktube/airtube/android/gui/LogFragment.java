package com.thinktube.airtube.android.gui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.thinktube.airtube.android.R;

public class LogFragment extends Fragment {
	private TextView text;
	private ScrollView scroller;
	private String log = new String();
	private Time now = new Time();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.log_view, container, false);
		text = (TextView) rootView.findViewById(R.id.text);
		scroller = (ScrollView) rootView.findViewById(R.id.scroller);
		text.setText(log);
		scroller.smoothScrollTo(0, text.getBottom());
		return rootView;
	}

	public void log(String msg) {
		now.setToNow();
		final String strTime=now.format("%H:%M:%S");
		log = log + strTime + ' ' + msg + "\n";

		Activity act = getActivity();
		if (act == null || text == null)
			return;

		text.setText(log);
		scroller.smoothScrollTo(0, text.getBottom());
	}
	public void clear() {
		log = "";
	}
}
