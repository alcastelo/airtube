package com.thinktube.airtube;

/**
 * Network packet priority, in ascending order: BACKGROUND, NORMAL, VIDEO, VOICE
 * <p>
 * The traffic class is mapped to the IP header TOS (type of service) field
 * a.k.a. QoS (quality of service) or 802.1D priority of the networking packets.
 * This is most relevant for Wifi networks where the TOS itself will be
 * classified into four different "Access Categories", with similar semantics.
 */
public enum TrafficClass {
	/**
	 * Least priority (0x20)
	 */
	BACKGROUND,
	/**
	 * Standard, or "best effort" (0x00)
	 */
	NORMAL,
	/**
	 * Video priority (0xa0)
	 */
	VIDEO,
	/**
	 * Voice, low latency (0xc0)
	 */
	VOICE;

	private int tos[] = { 0x20, // or 0x40 ("spare")
			0x00, // or 0x60 ("excellent effort")
			0x00, // should be 0xa0 or 0x80 ("controlled load"), but we avoid it
					// due to bcmdhd driver bugs
			0xc0 // or 0xe0 ("network control")
	};

	public int getTOSValue() {
		return tos[this.ordinal()];
	}
}
