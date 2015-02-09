package com.thinktube.airtube.android.gui;

import android.os.Bundle;

import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.MonitorCallbackI.Location;
import com.thinktube.airtube.ServiceDescription;
import com.thinktube.airtube.ServiceInfo;
import com.thinktube.airtube.android.R;

public class ServicesFragment extends MyListFragment<ServicesFragment.ServInfo> {
	public class ServInfo extends ServiceInfo implements MyArrayAdapter.ListItem {
		public ServInfo(AirTubeID id, ServiceDescription desc, Location type) {
			super(id, desc, type);
		}

		@Override
		public String toString() {
			return desc.name;
		}

		@Override
		public String toStringSmall() {
			return (type == Location.LOCAL ? "Local " : "Remote ") + id.getString() + ": (" + desc.type + "/" + desc.tos + ")";
		}

		@Override
		public int getImageResource() {
			return type == Location.LOCAL ? R.drawable.ic_local : R.drawable.ic_remote;
		}

		/* here we want to equal only on ID */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ServiceInfo other = (ServiceInfo) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public void setFromBundle(Bundle b) {
			// not used
		}
	}
}
