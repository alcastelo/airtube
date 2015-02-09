package com.thinktube.airtube;

/**
 * Interface for receiving "connection" callbacks, which notify the component of
 * the availability of the AirTube service.
 * <p>
 * The AirTube instance passed over in {@link #onConnect} may be used until
 * {@link #onDisconnect} is called.
 * <p>
 * Note: This interface exists mainly for Android services where a service
 * connection needs to be used which has an independent lifecycle and
 * availability. Even though not strictly necessary on pure Java implementations
 * it is advisable to implement this interface as well, most conveniently by
 * extending {@link AirTubeComponent}.
 * 
 * @see AirTubeCallbackI
 * @see AirTubeComponent
 */
public interface AirTubeConnectionCallbackI {

	/**
	 * Connected to the AirTube service, provided by the parameter. From now on,
	 * until {@link #onDisconnect} the provided {@link AirTubeInterfaceI} may be
	 * used.
	 * 
	 * @param airtube
	 *            Interface to AirTube
	 */
	void onConnect(AirTubeInterfaceI airtube);

	/**
	 * Disconnected from the AirTube service. The interface provided before in
	 * {@link #onConnect} may not be used any more.
	 */
	void onDisconnect();
}
