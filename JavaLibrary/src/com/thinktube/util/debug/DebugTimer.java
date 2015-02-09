package com.thinktube.util.debug;

import java.util.logging.Logger;

public class DebugTimer {
	private final static Logger LOG = Logger.getLogger(DebugTimer.class.getSimpleName());
	String name;
	long lastTime;
	long time;
	long sum = 0;
	long count = 0;

	public DebugTimer(String string) {
		name = string;
	}

	public void logElapsedTime() {
		logElapsedTime("time elapsed: ");
	}

	public void logElapsedTime(String string) {
		time = System.currentTimeMillis();
		LOG.info(name + ": " + string + " " + (time - lastTime) + "ms");
		lastTime = time;
	}

	public void takeTime() {
		lastTime = System.currentTimeMillis();
	}

	public void logAverageTime(int every) {
		time = System.currentTimeMillis();
		if (count > 0)
			sum += (time - lastTime);
		count++;
		lastTime = time;

		if (count > 0 && (count % every) == 0)
			LOG.info(name + ": average " + sum/count + "ms");
	}
}
