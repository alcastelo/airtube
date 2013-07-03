package com.thinktube.airtube;

import android.os.Parcel;
import android.os.Parcelable;

public class ServiceDescription implements Parcelable {
	public String name;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);

	}

	public static final Parcelable.Creator<ServiceDescription> CREATOR = new Parcelable.Creator<ServiceDescription>() {
		public ServiceDescription createFromParcel(Parcel in) {
			return new ServiceDescription(in);
		}

		public ServiceDescription[] newArray(int size) {
			return new ServiceDescription[size];
		}
	};

	private ServiceDescription(Parcel in) {
		name = in.readString();
	}

	public ServiceDescription(String name) {
		this.name = name;
	}
}
