package com.thinktube.airtube;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AService {
	ServiceDescription desc;
	List<AClient> clients; // local only
	Set<Peer> providers;

	AService(ServiceDescription desc) {
		this.desc = desc;
		this.clients = new ArrayList<AClient>();
		this.providers = new HashSet<Peer>();
	}
}
