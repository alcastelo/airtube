package com.thinktube.airtube;

/**
 * TransmissionType is the type of data transmission services can use. Currently
 * UDP, TCP and UDP Broadcast are defined, but more advanced transmission types
 * like "flooding" and "delayed delivery" are to follow.
 */

public enum TransmissionType {
	/**
	 * User Datagram Protocol (UDP) does not guarantee delivery. Packets are
	 * unicast between service and clients but may be lost or re-ordered.
	 */
	UDP,

	/**
	 * Transmission Control Protocol (TCP) guarantees delivery of data in the
	 * right order. It may however suffer from delays and congestion and is not
	 * the right choice for real-time audio/video data. Packets are unicast
	 * between service and clients.
	 */
	TCP,

	/**
	 * UDP broadcasts packets between service and clients, but without any
	 * delivery guarantees. This means that not all clients of a service may
	 * receive the broadcast, especially in ad-hoc networks, depending on their
	 * radio reception. Also all other clients of the same service within range
	 * will hear packets which a client sends to the service.
	 */
	UDP_BROADCAST
}
