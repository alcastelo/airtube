package com.thinktube.airtube.android;

import com.thinktube.airtube.AirTubeID;

import android.os.Parcel;
import android.os.Parcelable;

public class AirTubeIDParcel extends AirTubeID implements Parcelable {
	public AirTubeIDParcel(AirTubeID aid) {
		super(aid);
	}

	public AirTubeIDParcel(short port, long deviceId) {
		super(port, deviceId);
	}

	public AirTubeIDParcel(String s) {
		super(s);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(port);
		dest.writeLong(deviceId);
	}

	public static final Parcelable.Creator<AirTubeIDParcel> CREATOR = new Parcelable.Creator<AirTubeIDParcel>() {
		public AirTubeIDParcel createFromParcel(Parcel in) {
			return new AirTubeIDParcel(in);
		}

		public AirTubeIDParcel[] newArray(int size) {
			return new AirTubeIDParcel[size];
		}
	};

	private AirTubeIDParcel(Parcel in) {
		super((short)in.readInt(), in.readLong());
	}

	static AirTubeIDParcel wrap(AirTubeID id) {
		if (id instanceof AirTubeIDParcel) {
			return (AirTubeIDParcel)id;
		} else if (id != null) {
			return new AirTubeIDParcel(id);
		} else {
			return null;
		}
	}
}
