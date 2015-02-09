package com.thinktube.airtube;

import java.nio.ByteBuffer;

import org.json.JSONException;
import org.json.JSONObject;

import com.thinktube.util.Base64;
import com.thinktube.util.ByteBufferUtil;

/**
 * Configuration parameters of a service or client.
 * <p>
 * Used to specify and transmit optional configuration parameters, which are
 * service specific key-value pairs expressed in JSON, e.g.:
 * 
 * <pre>{ "codec": "opus", "frameSize": 160 }</pre>
 * 
 * For flexibility reasons you are supposed to use the getter and setter methods
 * instead of accessing the JSON object itself.
 */
public class ConfigParameters {
	/*
	 * We warp around JSON, so we can change it later
	 */
	protected JSONObject json;

	/**
	 * New empty ConfigParameters
	 */
	public ConfigParameters() {
		json = new JSONObject();
	}

	/**
	 * Copy constructor
	 * 
	 * @param c
	 *            config parameters to copy
	 */
	public ConfigParameters(ConfigParameters c) {
		if (c != null) {
			this.json = c.json;
		} else {
			this.json = new JSONObject();
		}
	}

	/**
	 * Config parameters from JSON String
	 * 
	 * @param s
	 *            JSON sting
	 */
	public ConfigParameters(String s) {
		if (s != null && !s.isEmpty()) {
			try {
				json = new JSONObject(s);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		return "ConfigParameters: " + json.toString();
	}

	public String getString() {
		if (json != null) {
			return json.toString();
		} else {
			return "";
		}
	}

	public void toByteBuffer(ByteBuffer out) {
		ByteBufferUtil.putString(out, this.getString());
	}

	public ConfigParameters(ByteBuffer in) {
		String s = ByteBufferUtil.getString(in);
		if (s != null && !s.isEmpty()) {
			try {
				json = new JSONObject(s);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	/* wrapper methods */
	public boolean getBoolean(String name) throws Exception {
		return json.getBoolean(name);
	}

	public double getDouble(String name) throws Exception {
		return json.getDouble(name);
	}

	public int getInt(String name) throws Exception {
		return json.getInt(name);
	}

	public long getLong(String name) throws Exception {
		return json.getLong(name);
	}

	public String getString(String name) throws Exception {
		return json.getString(name);
	}

	public byte[] getBytes(String name) throws Exception {
		return Base64.decode(json.getString(name), Base64.DEFAULT);
	}

	public boolean put(String name, boolean value) {
		try {
			json.put(name, value);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}

	public boolean put(String name, double value) {
		try {
			json.put(name, value);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}

	public boolean put(String name, int value) {
		try {
			json.put(name, value);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}

	public boolean put(String name, long value) {
		try {
			json.put(name, value);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}

	public boolean put(String name, String value) {
		try {
			json.put(name, value);
			return true;
		} catch (JSONException e) {
			return false;
		}
	}

	public boolean put(String name, byte[] value) {
		try {
			json.put(name, new String(Base64.encode(value, Base64.NO_WRAP)));
			return true;
		} catch (JSONException e) {
			return false;
		}
	}

	public boolean has(String name) {
		return json.has(name);
	}
}
