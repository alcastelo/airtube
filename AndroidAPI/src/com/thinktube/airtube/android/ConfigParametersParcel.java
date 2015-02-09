package com.thinktube.airtube.android;

import com.thinktube.airtube.ConfigParameters;

import android.os.Parcel;
import android.os.Parcelable;

public class ConfigParametersParcel extends ConfigParameters implements Parcelable {
	ConfigParametersParcel(ConfigParameters c) {
		super(c);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getString());
	}

	public static final Parcelable.Creator<ConfigParametersParcel> CREATOR = new Parcelable.Creator<ConfigParametersParcel>() {
		public ConfigParametersParcel createFromParcel(Parcel in) {
			return new ConfigParametersParcel(in);
		}

		public ConfigParametersParcel[] newArray(int size) {
			return new ConfigParametersParcel[size];
		}
	};

	private ConfigParametersParcel(Parcel in) {
		super(in.readString());
	}

	static ConfigParametersParcel wrap(ConfigParameters conf) {
		if (conf instanceof ConfigParametersParcel) {
			return (ConfigParametersParcel)conf;
		} else if (conf != null) {
			return new ConfigParametersParcel(conf);
		} else {
			return null;
		}
	}
}
