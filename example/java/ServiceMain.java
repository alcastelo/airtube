package com.thinktube.airtube.example.simple;

import java.io.IOException;

import com.thinktube.airtube.AirTube;


public class ServiceMain {
	public static void main(String[] args) {
		AirTube airtube = new AirTube();
		airtube.start();
		airtube.addComponent(new SimpleService());

		System.out.println("*** Press any key to stop!");

		try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("*** Stopping");
		airtube.stop();
		Thread.yield();

		System.out.println("*** Done");
	}
}
