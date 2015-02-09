package com.thinktube.airtube;

import java.nio.ByteBuffer;

import com.thinktube.util.ByteBufferUtil;

/**
 * The fields defined here are the mandatory description of a service.
 * <p>
 * A service is mainly identified and looked up by its name.
 * <p>
 * {@link TrafficClass} and {@link TransmissionType} configure the networking
 * details of the service and {@link ConfigParameters} allows for optional and
 * service specific descriptions.
 */
public class ServiceDescription {
	/**
	 * Name of the service (main key)
	 */
	public String name;

	/**
	 * Optional configuration parameters
	 */
	public ConfigParameters config;

	/**
	 * transmission type
	 */
	public TransmissionType type;

	/**
	 * Type of service (TOS) to use for QoS . Priority.
	 */
	public TrafficClass tos;

	public ServiceDescription() {
	}

	/**
	 * Defaults to UDP and normal priority and empty config parameters
	 * 
	 * @param name
	 *            name of service
	 */
	public ServiceDescription(String name) {
		this(name, null, null);
	}

	/**
	 * Default to normal priority and empty config parameters
	 * 
	 * @param name
	 *            name of service
	 * @param type
	 *            transmission type
	 */
	public ServiceDescription(String name, TransmissionType type) {
		this(name, type, null);
	}

	/**
	 * Empty config parameters
	 * 
	 * @param name
	 *            name of service
	 * @param type
	 *            transmission type
	 * @param tos
	 *            TOS
	 */
	public ServiceDescription(String name, TransmissionType type, TrafficClass tos) {
		this.name = name;
		this.type = type;
		this.tos = tos;
		this.config = new ConfigParameters();
	}

	/**
	 * Full constructor specifying all
	 * 
	 * @param name
	 *            name of service
	 * @param type
	 *            transmission type
	 * @param tos
	 *            TOS
	 * @param conf
	 *            configuration parameters
	 */
	public ServiceDescription(String name, TransmissionType type, TrafficClass tos, ConfigParameters conf) {
		this.name = name;
		this.type = type;
		this.tos = tos;
		this.config = conf;
	}

	/**
	 * Copy constructor
	 * 
	 * @param d
	 *            source
	 */
	public ServiceDescription(ServiceDescription d) {
		this(d.name, d.type, d.tos, d.config);
	}

	@Override
	public String toString() {
		if (type == null && tos == null)
			return "ServiceDescription [" + name + "]";
		else if (type != null && tos == null)
			return "ServiceDescription [" + name + ", " + type + "]";
		else if (type == null && tos != null)
			return "ServiceDescription [" + name + ", " + tos + "]";
		else
			return "ServiceDescription [" + name + ", " + type + ", " + tos + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((tos == null) ? 0 : tos.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		/*
		 * Note: remove class check for ServiceDescriptionParcel subclass which
		 * should "equal"
		 */
		// if (getClass() != obj.getClass())
		// return false;
		ServiceDescription other = (ServiceDescription) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != null && other.type != null && type != other.type)
			return false;
		if (tos != null && other.tos != null && tos != other.tos)
			return false;
		return true;
	}

	public void toByteBuffer(ByteBuffer out) {
		ByteBufferUtil.putString(out, name);
		out.putInt(type != null ? type.ordinal() : -1);
		out.putInt(tos != null ? tos.ordinal() : -1);
		config.toByteBuffer(out);
	}

	public ServiceDescription(ByteBuffer in) {
		name = ByteBufferUtil.getString(in);
		int inI = in.getInt();
		if (inI != -1)
			type = TransmissionType.values()[inI];
		inI = in.getInt();
		if (inI != -1)
			tos = TrafficClass.values()[inI];
		config = new ConfigParameters(in);
	}
}
