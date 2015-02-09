package com.thinktube.airtube.android.gui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.thinktube.airtube.android.R;

public class TracerouteFragment extends Fragment {
	interface TraceListener {
		public void startTrace(long dst, int type, int interval);
		public void stopTrace();
		public List<PeersFragment.PeerInfo> getPeersList();
	}

	ToggleButton startToggle;
	Spinner spinDst;
	EditText intTxt;
	ListView result;
	TextView resultTxt;
	TraceListener control;
	int traceType; /* Ping (0) or Traceroute (1) */
	int intv;
	long dst = 0;

	ArrayList<String> pingResult = new ArrayList<String>();
	ArrayAdapter<String> pingAdpt;

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            control = (TraceListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement TraceListener");
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.trace_view, container, false);

		intTxt = (EditText) rootView.findViewById(R.id.textInt);
		result = (ListView) rootView.findViewById(R.id.result);
		resultTxt = (TextView) rootView.findViewById(R.id.textResult);

		RadioGroup r1 = (RadioGroup) rootView.findViewById(R.id.radio_type);
		r1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			public void onCheckedChanged (RadioGroup group, int checkedId) {
				if (checkedId == R.id.radio_trace) {
					traceType = 1;
				} else {
					traceType = 0;
				}
			}
		});

		spinDst = (Spinner) rootView.findViewById(R.id.spinnerDst);
		final ArrayAdapter<PeersFragment.PeerInfo> adapDst = new ArrayAdapter<PeersFragment.PeerInfo>(getActivity(),
				android.R.layout.simple_spinner_item, control.getPeersList());
		adapDst.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinDst.setAdapter(adapDst);
		spinDst.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				PeersFragment.PeerInfo pi = (PeersFragment.PeerInfo)adapDst.getItem(pos);
				android.util.Log.d("TraceFrag", "selected " + Long.toHexString(pi.id));
				dst = pi.id;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		startToggle = (ToggleButton) rootView.findViewById(R.id.startToggle);
		startToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean on) {
				if (on) {
					String intStr = intTxt.getText().toString();
					if (intStr == null || intStr.equals("") || dst == 0) return;
					intv = Integer.parseInt(intStr);
					control.startTrace(dst, traceType, intv);
				} else {
					control.stopTrace();
				}
			}
		});

		pingAdpt = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, pingResult);

		return rootView;
	}

	public void setResult(String[] trace) {
		resultTxt.setText("Result from " + new java.util.Date());

		if (traceType == 0) {
			/*
			 * Ping: Add result to list
			 */
			pingAdpt.add(trace[0]);
			pingAdpt.notifyDataSetChanged();
			result.setAdapter(pingAdpt);
		} else {
			/*
			 * Traceroute: Show result list
			 */
			ArrayAdapter<String> adpt = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, trace);
			result.setAdapter(adpt);
		}

		if (intv == 0) {
			startToggle.setChecked(false);
		}
	}
}
