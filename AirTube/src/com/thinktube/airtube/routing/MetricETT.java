package com.thinktube.airtube.routing;

import java.nio.ByteBuffer;

public class MetricETT implements Metric {
	double ett;

	public MetricETT(double val) {
		this.ett = val;
	}

	@Override
	public String toString() {
		return String.format("ETT:%.3f", ett);
	}

	@Override
	public boolean isBetterThan(Metric a) {
		if (a == null)
			return true;
		if (!(a instanceof MetricETT))
			throw new RuntimeException(a + "is not " + getClass());

		if (((MetricETT)a).ett < ett)
			return false;
		return true;
	}

	@Override
	public boolean isInfinite() {
		return Double.isInfinite(ett);
	}

	@Override
	public Metric getInfinite() {
		return new MetricETT(Double.POSITIVE_INFINITY);
	}

	@Override
	public Metric copy() {
		return new MetricETT(ett);
	}

	@Override
	public void add(Metric a) {
		if (a == null)
			return;
		if (!(a instanceof MetricETT))
			throw new RuntimeException(a + " is not " + getClass());
		ett = ett + ((MetricETT)a).ett;
	}

	@Override
	public void subtract(Metric a) {
		if (a == null)
			return;
		if (!(a instanceof MetricETT))
			throw new RuntimeException(a + " is not " + getClass());
		ett = ett - ((MetricETT)a).ett;
	}

	@Override
	public void writeTo(ByteBuffer buf) {
		buf.putDouble(ett);
	}

	public static MetricETT readFrom(ByteBuffer buf) {
		return new MetricETT(buf.getDouble());
	}

	@Override
	public int getDataSize() {
		return 8; /* size of double in ByteBuffer */
	}
}
