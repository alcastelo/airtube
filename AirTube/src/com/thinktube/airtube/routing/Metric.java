package com.thinktube.airtube.routing;

import java.nio.ByteBuffer;

public interface Metric {

	boolean isBetterThan(Metric a);

	boolean isInfinite();

	Metric getInfinite();

	void add(Metric metric);

	void subtract(Metric metric);

	void writeTo(ByteBuffer buf);

	int getDataSize(); /* size in bytes for writeTo */

	Metric copy();

}
