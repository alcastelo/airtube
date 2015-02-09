package com.thinktube.airtube.routing;

import com.thinktube.util.SimpleMovingAverage;

public class MetricETTProducer {
	private static final int WINDOW_SIZE = 20;
	private SimpleMovingAverage pktLoss = new SimpleMovingAverage(WINDOW_SIZE);
	private SimpleMovingAverage pktLossRev = new SimpleMovingAverage(WINDOW_SIZE);
	private int linkSpeed = 1; /* Link speed in Mbit/sec (default 1Mbps) */
	private MetricETT value = new MetricETT(-1);

	public MetricETTProducer() {
	}

	public double getPktLoss() {
		return pktLoss.getAvg();
	}

	public double getPktLossRev() {
		return pktLossRev.getAvg();
	}

	public double getETX() {
		return 1 / (getPktLoss() * getPktLossRev());
	}

	public MetricETT getETT() {
			value.ett = getETX() / linkSpeed;
			return value;
	}

	private void _updatePktLoss(SimpleMovingAverage avg, int lost) {
		for (int i=0; i<lost; i++) {
			avg.newNum(0);
		}
		avg.newNum(1);
	}

	public void updatePktLoss(int lost) {
		_updatePktLoss(pktLoss, lost);
	}

	public void updatePktLossRev(int lost) {
		_updatePktLoss(pktLossRev, lost);
	}

	public void setLinkSpeed(int i) {
		linkSpeed = i;
	}

	public String toString() {
		return String.format("%s [%.2f/%.2f ETX:%.2f]",
				getETT().toString(), getPktLoss(), getPktLossRev(), getETX());
	}
}
