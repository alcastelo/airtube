package com.thinktube.airtube.routing;

import java.net.InetAddress;
import java.util.logging.Logger;

import com.thinktube.airtube.routing.nbr.ProxyLink;
import com.thinktube.net.NetworkInterfaces;
import com.thinktube.net.NetworkInterfaces.NetIf;

public class ProxyManager {
	private final static Logger LOG = Logger.getLogger(ProxyManager.class.getSimpleName());
	private NetworkInterfaces nifs;
	private ProxyLink proxyLink;

	public ProxyManager() {
	}

	public void clear() {
		for (NetIf ni : nifs.getInterfaces()) {
			if (ni instanceof ProxyLink) {
				((ProxyLink)ni).close();
				nifs.removeInterface(ni);
			}
		}
	}

	public void setProxy(InetAddress proxyIp) {
		// close existing connections if IP has changed
		if (proxyLink != null && !proxyLink.getAddress().equals(proxyIp)) {
			LOG.info("proxy changed");
			proxyLink.close();
		}

		if (proxyIp == null) { /* unset */
			LOG.info("proxy unset");
			if (nifs != null) {
				nifs.removeInterface(proxyLink);
				proxyLink.close();
			}
			proxyLink = null;
		} else {
			LOG.info("proxy set to " + proxyIp);
			proxyLink = new ProxyLink(proxyIp);
			if (nifs != null) {
				nifs.addInterface(proxyLink);
				LOG.info("Created upstream proxy interface " + proxyLink);
			}
		}
	}

	public void start(NetworkInterfaces nifs) {
		this.nifs = nifs;
		if (proxyLink != null) {
			nifs.addInterface(proxyLink);
		}
	}
}
