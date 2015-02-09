package com.thinktube.airtube.android;

import com.thinktube.airtube.ConfigParameters;
import com.thinktube.airtube.ServiceDescription;
import com.thinktube.airtube.TrafficClass;
import com.thinktube.airtube.TransmissionType;

import android.os.Parcel;
import android.os.Parcelable;

public class ServiceDescriptionParcel extends ServiceDescription implements Parcelable {
	ServiceDescriptionParcel(ServiceDescription sd) {
		super(sd);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeInt(type != null ? type.ordinal() : -1);
		dest.writeInt(tos != null ? tos.ordinal() : -1);
		//instead of dest.writeParcelable(config, flags):
		dest.writeString(config.getString());
	}

	public static final Parcelable.Creator<ServiceDescriptionParcel> CREATOR = new Parcelable.Creator<ServiceDescriptionParcel>() {
		public ServiceDescriptionParcel createFromParcel(Parcel in) {
			return new ServiceDescriptionParcel(in);
		}

		public ServiceDescriptionParcel[] newArray(int size) {
			return new ServiceDescriptionParcel[size];
		}
	};

	private ServiceDescriptionParcel(Parcel in) {
		name = in.readString();
		int inI = in.readInt();
		if (inI != -1)
			type = TransmissionType.values()[inI];
		inI = in.readInt();
		if (inI != -1)
			tos = TrafficClass.values()[inI];
		//Parcelable(ConfigParametersParcel.class.getClassLoader());
		config = new ConfigParameters(in.readString());
	}

	static ServiceDescriptionParcel wrap(ServiceDescription desc) {
		if (desc instanceof ServiceDescriptionParcel) {
			return (ServiceDescriptionParcel)desc;
		} else if (desc != null) {
			return new ServiceDescriptionParcel(desc);
		} else {
			return null;
		}
	}
}
