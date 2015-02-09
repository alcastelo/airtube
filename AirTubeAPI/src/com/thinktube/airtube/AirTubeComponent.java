package com.thinktube.airtube;

/**
 * Abstract base class to ease implementation of an AirTube component,
 * implementing the {@link AirTubeCallbackI} and
 * {@link AirTubeConnectionCallbackI} interfaces with empty methods.
 * <p>
 * Additionally adds {@link #start()}, {@link #stop()} and {@link #unregister()} methods, which makes it
 * easier to handle and structurize all components the same.
 * <p>
 * Components are not required to extend this base class, but can also implement
 * the {@link AirTubeCallbackI} and {@link AirTubeConnectionCallbackI}
 * interfaces themselves.
 * 
 * @see AirTubeConnectionCallbackI
 * @see AirTubeCallbackI
 */
public abstract class AirTubeComponent implements AirTubeCallbackI,
		AirTubeConnectionCallbackI {

	/* AirTubeConnectionCallbackI */

	@Override
	public abstract void onConnect(AirTubeInterfaceI airtube);

	@Override
	public abstract void onDisconnect();

	/* AirTubeCallbackI */

	@Override
	public void receiveData(AirTubeID fromId, ServiceData data) {
	}

	@Override
	public void onSubscription(AirTubeID serviceId, AirTubeID clientId,
			ConfigParameters config) {
	}

	@Override
	public void onUnsubscription(AirTubeID serviceId, AirTubeID clientId) {
	}

	@Override
	public void onServiceFound(AirTubeID serviceId, ServiceDescription desc) {
	}

	/**
	 * Start the local side, if necessary
	 */
	public void start() {
	}

	/**
	 * Stop the local side, if necessary
	 */
	public void stop() {
	}

	/**
	 * Unregister component from AirTube.
	 */
	public abstract void unregister();
}
