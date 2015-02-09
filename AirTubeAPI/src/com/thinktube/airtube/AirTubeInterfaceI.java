package com.thinktube.airtube;

/* Due to the AIDL limitation that it can not subclass or implement interfaces,
 * this needs to be manually kept in sync with android/AirTubeInterfaceAidl.aidl */

/**
 * Main interface to AirTube.
 * <p>
 * Used by clients and services to register and interact with AirTube.
 */
public interface AirTubeInterfaceI {
	/* registering components */

	/**
	 * Register a component with AirTube. The component will get the AirTube
	 * connection events onConnect() and onDisconnect() and should do it's
	 * initialization there.
	 *
	 * Note: This may seem unnecessary for a Pure Java implementation, but
	 * is necessary to handle the Android implementation of AirTube.
	 *
	 * @param comp
	 *            The component, or more specifically its
	 *            AirTubeConnectionCallbackI interface
	 */
	void addComponent(AirTubeConnectionCallbackI comp);

	/**
	 * Unregister a component with AirTube. The component will receive no
	 * further connection events.
	 *
	 * Note: This may seem unnecessary for a Pure Java implementation, but
	 * is necessary to handle the Android implementation of AirTube.
	 *
	 * @param comp
	 *            The component, or more specifically its
	 *            AirTubeConnectionCallbackI interface
	 */
	void removeComponent(AirTubeConnectionCallbackI comp);

	/* services */

	/**
	 * Register a service with AirTube
	 * 
	 * @param desc
	 *            A detailed description of the service
	 * @param cb
	 *            Callback to the service
	 * @return A new unique ID for the service
	 */
	AirTubeID registerService(ServiceDescription desc, AirTubeCallbackI cb);

	/**
	 * Unregister service from AirTube
	 * 
	 * @param serviceId
	 *            The ID we got before from {@link #registerService}
	 * @return Success
	 */
	boolean unregisterService(AirTubeID serviceId);

	/**
	 * Get number of clients subscribed to service
	 * 
	 * @param serviceId
	 *            The ID we got before from {@link #registerService}
	 * @return Number of clients
	 */
	int getNumberOfClients(AirTubeID serviceId);
	
	/**
	 * Send data from service to all clients (publish)
	 * 
	 * @param serviceId
	 *            The ID we got before from {@link #registerService}
	 * @param data
	 * 			  The data to send
	 */
	void sendServiceData(AirTubeID serviceId, ServiceData data);

	/* clients */
	
	/**
	 * Register a client with AirTube
	 *
	 * @return A new unique ID for the client
	 */
	AirTubeID registerClient(AirTubeCallbackI client);
	
	/**
	 * Unregister client from AirTube
	 * 
	 * @param clientId
	 *            The ID we got before from {@link #registerClient}
	 * @return Success
	 */
	boolean unregisterClient(AirTubeID clientId);

	/* any */

	/**
	 * Search for services matching description. When a suitable service is
	 * found, the
	 * {@link AirTubeCallbackI#onServiceFound(AirTubeID, ServiceDescription)}
	 * method is called back
	 * 
	 * @param desc
	 *            service description
	 * @param clientId
	 *            The ID of the client or service
	 */
	void findServices(ServiceDescription desc, AirTubeID clientId);

	/**
	 * Not interested in the services looked up with {@link #findServices}
	 * before any more
	 * 
	 * @param desc
	 *            service description
	 * @param clientId
	 *            The ID of the client or service
	 */
	void unregisterServiceInterest(ServiceDescription desc, AirTubeID clientId);

	/**
	 * Subscribe to a service
	 * 
	 * @param serviceId
	 *            ID of the service (usually found by {@link #findServices}
	 *            before)
	 * @param clientId
	 *            ID of the client or service
	 * @param conf
	 *            Optional configuration parameters
	 */
	void subscribeService(AirTubeID serviceId, AirTubeID clientId, ConfigParameters conf);
	
	/**
	 * Unsubscribe from service
	 * 
	 * @param serviceId
	 *            The ID of the service
	 * @param clientId
	 *            The ID of the client or subscribed service
	 */
	void unsubscribeService(AirTubeID serviceId, AirTubeID clientId);

	/**
	 * Send data from a client to a service or from a service to one client
	 * <p>
	 * Some services may accept data from clients, and the clients can use this
	 * function to send data to the service. Also services may use it to
	 * send some data to only one client.
	 *
	 * @param toId
	 *            The ID of the receiver
	 *
	 * @param fromId
	 *            The ID of the sender
	 * @param data
	 *            The data to send
	 */
	void sendData(AirTubeID toId, AirTubeID fromId, ServiceData data);

	/**
	 * Get detailed description of a service
	 * 
	 * @param serviceID
	 *            The ID of the service
	 * @return A detailed description of the service
	 */
	ServiceDescription getDescription(AirTubeID serviceID);
}
