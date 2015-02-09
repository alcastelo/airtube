package com.thinktube.airtube.android.gui;

import android.os.Bundle;

import com.thinktube.airtube.AirTubeID;
import com.thinktube.airtube.MonitorCallbackI.Location;
import com.thinktube.airtube.android.R;

public class ClientsFragment extends MyListFragment<ClientsFragment.ClientInfo> {
	class ClientInfo implements MyArrayAdapter.ListItem {
		AirTubeID id;
		Location type;

		ClientInfo(AirTubeID id, Location location) {
			this.id = id;
			this.type = location;
		}

		public String toString() {
			return id.getString();
		}

		@Override
		public String toStringSmall() {
			return (type == Location.LOCAL ? "Local " : "Remote ");
		}

		@Override
		public int getImageResource() {
			return type == Location.LOCAL ? R.drawable.ic_local : R.drawable.ic_remote;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
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
			ClientInfo other = (ClientInfo) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}

		@Override
		public void setFromBundle(Bundle b) {
			// not used
		}
	}
}
