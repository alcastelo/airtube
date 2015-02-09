package com.thinktube.util;

import java.util.LinkedList;
import java.util.Queue;

/*
 * From http://rosettacode.org/wiki/Averages/Simple_moving_average#Java
 * and slightly adapted
 */
public class SimpleMovingAverage {
	private final Queue<Double> window = new LinkedList<Double>();
	private final int windowSize;
	private double sum;

	public SimpleMovingAverage(int windowSize) {
		assert windowSize > 0 : "Window size must be a positive integer";
		this.windowSize = windowSize;
	}

	public void newNum(double num) {
		sum += num;
		window.add(num);
		if (window.size() > windowSize) {
			sum -= window.remove();
		}
	}

	public double getAvg() {
		if (window.isEmpty())
			return 0; // technically the average is undefined
		return sum / window.size();
	}

	public static void main(String[] args) {
		double[] testData = { 1, 2, 3, 4, 5, 5, 4, 3, 2, 1 };
		int[] windowSizes = { 3, 5 };
		for (int windSize : windowSizes) {
			SimpleMovingAverage ma = new SimpleMovingAverage(windSize);
			for (double x : testData) {
				ma.newNum(x);
				System.out.println("Next number = " + x + ", SMA = "
						+ ma.getAvg());
			}
			System.out.println();
		}
	}
}
