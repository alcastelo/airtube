package com.thinktube.airtube;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class ServiceRegistry {
	private static final String TAG = "ServReg";
	List<AService> localServ = new ArrayList<AService>();
	List<AService> remoteServ = new ArrayList<AService>();

	public int addLocal(ServiceDescription desc) {
		AService as = new AService(desc);
		localServ.add(as);
		return localServ.indexOf(as);
	}

	public void removeLocal(int id) {
		localServ.remove(id);
	}
	
	public int addRemote(AService as) {
		remoteServ.add(as);
		return remoteServ.indexOf(as);
	}

	public void removeRemote(int id) {
		remoteServ.remove(id);
	}

	public AService findLocalService(String name) {
		for (AService as : localServ) {
			if (as.desc.name.equals(name)) {
				return as;
			}
		}
		return null;
	}
	
	public AService findRemoteService(String name) {
		for (AService as : remoteServ) {
			if (as.desc.name.equals(name)) {
				return as;
			}
		}
		return null;
	}
		
	public List<AService> findAllServices(String name) {
		List<AService> list = new ArrayList<AService>();
		for (AService as : localServ) {
			if (as.desc.name.equals(name)) {
				list.add(as);
			}
		}
		for (AService as : remoteServ) {
			if (as.desc.name.equals(name)) {
				list.add(as);
			}
		}
		return list;
	}
	
	public List<ServiceDescription> getLocalServiceDescriptions() {
		List<ServiceDescription> list = new ArrayList<ServiceDescription>();
		for (AService as : localServ) {
			list.add(as.desc);
		}
		return list;
	}

	public void addClient(String name, AClient ac) {
		Log.d(TAG, "add client " + ac);
		for (AService as : localServ) {
			if (as.desc.name.equals(name)) {
				as.clients.add(ac);
			}
		}
		for (AService as : remoteServ) {
			if (as.desc.name.equals(name)) {
				as.clients.add(ac);
			}
		}
	}

	public void removeClient(String name, int id) {
		for (AService as : localServ) {
			if (as.desc.name.equals(name)) {
				as.clients.remove(id);
			}
		}
		for (AService as : remoteServ) {
			if (as.desc.name.equals(name)) {
				as.clients.remove(id);
			}
		}
	}
}
