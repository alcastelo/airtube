package com.thinktube.airtube.android.gui;

import android.os.Bundle;

import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.android.R;

public class SubscriptionsFragment extends MyListFragment<SubscriptionsFragment.SubsInfo> {
	class SubsInfo implements MyArrayAdapter.ListItem {
		AirTubeID sid;
		AirTubeID cid;

		SubsInfo(AirTubeID sid, AirTubeID cid) {
			this.sid = sid;
			this.cid = cid;
		}

		public String toString() {
			return sid.getString() + " - " + cid.getString();
		}

		@Override
		public String toStringSmall() {
			return "";
		}

		@Override
		public int getImageResource() {
			return R.drawable.ic_subscr;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((cid == null) ? 0 : cid.hashCode());
			result = prime * result + ((sid == null) ? 0 : sid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SubsInfo other = (SubsInfo) obj;
			if (cid == null) {
				if (other.cid != null)
					return false;
			} else if (!cid.equals(other.cid))
				return false;
			if (sid == null) {
				if (other.sid != null)
					return false;
			} else if (!sid.equals(other.sid))
				return false;
			return true;
		}

		@Override
		public void setFromBundle(Bundle b) {
			// not used
		}
	}
}
