package com.thinktube.airtube;

public class ServiceInfo {
	public AirTubeID id;
	public ServiceDescription desc;
	public MonitorCallbackI.Location type;

	public ServiceInfo(AirTubeID id, ServiceDescription desc) {
		this.id = id;
		this.desc = desc;
	}
	
	public ServiceInfo(AirTubeID id, ServiceDescription desc, MonitorCallbackI.Location type) {
		this.id = id;
		this.desc = desc;
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((desc == null) ? 0 : desc.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		ServiceInfo other = (ServiceInfo) obj;
		if (desc == null) {
			if (other.desc != null)
				return false;
		} else if (!desc.equals(other.desc))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return (type == MonitorCallbackI.Location.LOCAL ? "L " : "R ") + id.getString() + " " + desc.name;
	}
}
