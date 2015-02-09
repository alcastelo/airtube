package com.thinktube.util;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * This implements a fixed-size circular queue (first element is dropped if queue gets full)
 * which is ordered by priority (Comparable). Also the "buffer" elements are re-used.
 *
 * @author br1
 *
 * @param <T> Type of the Elements which implements Comparable
 */

public class CircularPriorityQueueBuffer<T extends Comparable<T>> {
	//private static final String TAG = "PrioQueue";
	private final PriorityQueue<T> queue;
	private final Queue<T> free;
	private final int maxSize;
	private int numQueued;

	public CircularPriorityQueueBuffer(T[] elements) {
		this.maxSize = elements.length;
		queue = new PriorityQueue<T>(maxSize);
		free = new LinkedList<T>();

		for (int i=0; i<elements.length; i++) {
			free.add(elements[i]);
		}
	}

	/** Returns the next buffer. If there are no free buffers, the oldest
	 * queue item is dropped and re-used (this is why it's a "circular" buffer) */
	public synchronized T getNextFree() {
		T e = free.poll();
		// if there are no free buffers, remove oldest entry and reuse it
		if (e == null) {
			e = this.poll();
			//Log.d(TAG, "overwrite head");
			if (e == null) {
				//Log.e(TAG, "both null?");
			}
		}
		return e;
	}

	/** Return a buffer to the pool of free buffers */
	public synchronized void returnFree(T item) {
		free.add(item);
	}

	/** Put a buffer in the queue, discarding duplicate items.
	 * If the buffer was first got with getNextFree() this will always succeed */
	public synchronized boolean put(T item) {
		if (queue.contains(item)) {
			//Log.d(TAG, "**** discarding duplicate item");
			return false;
		} else if (numQueued < maxSize) {
			numQueued++;
			return queue.add(item);
		}
		//Log.d(TAG, "queue full");
		return false;
	}

	public synchronized T peek() {
		return queue.peek();
	}

	public synchronized T poll() {
		numQueued--;
		return queue.poll();
	}

	public synchronized void clear() {
		T e;
		while ((e = queue.poll()) != null) {
			numQueued--;
			free.add(e);
		}
	}

	public synchronized int length() {
		return numQueued;
	}

	public int maxLength() {
		return maxSize;
	}
}
