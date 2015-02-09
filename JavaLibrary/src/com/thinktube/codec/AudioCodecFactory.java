package com.thinktube.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class AudioCodecFactory {
	public enum Codec {AAC, G711, SPEEX, OPUS};

	public static AudioDecoderI getDecoder(Codec c) {
		switch (c) {
		case AAC:
			return (AudioDecoderI) instantiate("com.thinktube.android.codec.AACDecoder");
		case G711:
			return (AudioDecoderI) instantiate("com.thinktube.codec.G711Decoder");
		case SPEEX:
			return (AudioDecoderI) instantiate("com.thinktube.codec.SpeexDecoder");
		case OPUS:
			return (AudioDecoderI) instantiate("com.thinktube.codec.OpusDecoder");
		default:
			return null;
		}
	}

	public static AudioEncoderI getEncoder(Codec c) {
		switch (c) {
		case AAC:
			return (AudioEncoderI)instantiate("com.thinktube.android.codec.AACEncoder");
		case G711:
			return (AudioEncoderI)instantiate("com.thinktube.codec.G711Encoder");
		case SPEEX:
			return (AudioEncoderI)instantiateWithIntConstructor("com.thinktube.codec.SpeexEncoder", 9);
		case OPUS:
			return (AudioEncoderI)instantiateWithIntConstructor("com.thinktube.codec.OpusEncoder", 9);
		default:
			return null;
		}
	}

	private static Object instantiate(String name) {
		try {
			return Class.forName(name).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Object instantiateWithIntConstructor(String name, int val) {
		try {
			Class<?> clazz = Class.forName(name);
			Constructor<?> ctor = clazz.getConstructor(int.class);
			return ctor.newInstance(val);
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
}
