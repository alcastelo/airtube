package com.thinktube.airtube.android;

import com.thinktube.airtube.ServiceData;

import android.os.Parcel;
import android.os.Parcelable;

public class ServiceDataParcel extends ServiceData implements Parcelable {
	public ServiceDataParcel(byte[] data) {
		super(data);
	}

	public ServiceDataParcel(byte[] data, int len) {
		super(data, len);
	}

	ServiceDataParcel(ServiceData sd) {
		super(sd);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(len);
		dest.writeByteArray(data, 0, len);

	}

	public static final Parcelable.Creator<ServiceDataParcel> CREATOR = new Parcelable.Creator<ServiceDataParcel>() {
		public ServiceDataParcel createFromParcel(Parcel in) {
			return new ServiceDataParcel(in);
		}

		public ServiceDataParcel[] newArray(int size) {
			return new ServiceDataParcel[size];
		}
	};

	private ServiceDataParcel(Parcel in) {
		len = in.readInt();
		data = new byte[len];
		in.readByteArray(data);
	}

	static ServiceDataParcel wrap(ServiceData data) {
		if (data instanceof ServiceDataParcel) {
			return (ServiceDataParcel)data;
		} else if (data != null) {
			return new ServiceDataParcel(data);
		} else {
			return null;
		}
	}
}
