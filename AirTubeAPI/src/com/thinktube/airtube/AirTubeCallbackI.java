package com.thinktube.airtube;

/* Due to the AIDL limitation that it can not subclass or implement interfaces,
 * this needs to be manually kept in sync with android/AirTubeCallback.aidl */

/**
 * Interface used by AirTube to call back into the component (service or client)
 * to notify it of relevant events, like subscription or data reception.
 * <p>
 * This interface defines the essential callbacks for AirTube components, but if
 * you want to write components which are usable on Android as well, it is
 * advisable to implement the {@link AirTubeConnectionCallbackI} interface as
 * well, most conveniently by extending {@link AirTubeComponent}.
 * 
 * @see AirTubeConnectionCallbackI
 * @see AirTubeComponent
 */
public interface AirTubeCallbackI {
	/**
	 * Receive data from service or client
	 * 
	 * @param fromId
	 *            Sender
	 * @param data
	 *            Data received
	 */
	void receiveData(AirTubeID fromId, ServiceData data);

	/**
	 * A client subscribed to a service. This is called on the service component
	 * as well as on the client component.
	 * 
	 * @param serviceId
	 *            The service ID
	 * @param clientId
	 *            The client ID
	 * @param config
	 *            Optional configuration parameters
	 */
	void onSubscription(AirTubeID serviceId, AirTubeID clientId,
			ConfigParameters config);

	/**
	 * A client unsubscribed from a service. This is called on the service
	 * component as well as on the client component.
	 * 
	 * @param serviceId
	 *            The service ID
	 * @param clientId
	 *            The client ID
	 */
	void onUnsubscription(AirTubeID serviceId, AirTubeID clientId);

	/**
	 * A service matching the description which was looked up before with
	 * {@link AirTubeInterfaceI#findServices} was found.
	 * 
	 * @param serviceId
	 *            The ID of the service
	 * @param desc
	 *            A more detailed service description
	 */
	void onServiceFound(AirTubeID serviceId, ServiceDescription desc);
}
