package com.thinktube.airtube.android.gui;

import com.thinktube.airtube.android.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class PeersFragment extends MyListFragment<PeersFragment.PeerInfo> {
	private AirTubeMonitorActivity act;

	public class PeerInfo implements MyArrayAdapter.ListItem {
		long id;
		String info;
		long lastTime;

		PeerInfo(long id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return Long.toHexString(id);
		}

		@Override
		public String toStringSmall() {
			return info + " (" + (System.currentTimeMillis() - lastTime) + "ms)";
		}

		public int getImageResource() {
			return R.drawable.ic_launcher;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PeerInfo other = (PeerInfo) obj;
			if (id != other.id)
				return false;
			return true;
		}

		@Override
		public void setFromBundle(Bundle b) {
			if (b.containsKey("info")) this.info = b.getString("info");
			if (b.containsKey("info")) this.lastTime = b.getLong("lastTime");
		}
	}

	final Runnable updateTimer = new Runnable() {
		@Override
		public void run() {
			adapter.notifyDataSetChanged();
			act.myHandler.postDelayed(updateTimer, 150);
		};
    };

	@Override
    public void onAttach(Activity activity) {
		super.onAttach(activity);
		// TODO: It's cleaner to define an interface like in ControlFragment,
		// but can't be bothered now
		act = (AirTubeMonitorActivity)activity;
		if (act != null) {
			act.myHandler.postDelayed(updateTimer, 500);
		}
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		PeerInfo pi = (PeerInfo)adapter.getItem(position);
		android.util.Log.d("XXX", "list item clicked: pos " + position + " " + pi);
		act.switchToTrace(position);
	}
}
