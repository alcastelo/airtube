package com.thinktube.airtube;

import android.os.Parcel;
import android.os.Parcelable;

public class ServiceData implements Parcelable {
	public String test;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(test);

	}

	public static final Parcelable.Creator<ServiceData> CREATOR = new Parcelable.Creator<ServiceData>() {
		public ServiceData createFromParcel(Parcel in) {
			return new ServiceData(in);
		}

		public ServiceData[] newArray(int size) {
			return new ServiceData[size];
		}
	};

	private ServiceData(Parcel in) {
		test = in.readString();
	}

	public ServiceData() {
	}

	public ServiceData(String data) {
		this.test = data;
	}
}
