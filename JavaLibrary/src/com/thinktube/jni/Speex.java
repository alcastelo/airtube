package com.thinktube.jni;

public class Speex {
	/* compression/quality
	 * 1 : 4kbps (very noticeable artifacts, usually intelligible)
	 * 2 : 6kbps (very noticeable artifacts, good intelligibility)
	 * 4 : 8kbps (noticeable artifacts sometimes)
	 * 6 : 11kpbs (artifacts usually only noticeable with headphones)
	 * 8 : 15kbps (artifacts not usually noticeable)
	 */
	public native int encoderCreate(int compression);
	public native int encode(short input[], int inOffset, int inSize, byte output[], int outOffset, int outSize);
	public native void encoderDestroy();

	public native int decoderCreate();
	public native int decode(byte input[], int inOffset, int inSize, short output[], int outOffset, int outSize);
	public native void decoderDestroy();

	/* echo	*/
	public native int echoCreate(int sampleRate, int frameSize, int filterLength);
	public native int echoPlayback(short input[], int inSize);
	public native int echoCapture(short input[], short[] output, int inOutSize);
	public native int echoCancelation(short input[], short echo[], short[] output, int inOutSize);
	public native void echoDestroy();

	static {
        System.loadLibrary("speex");
    }
}
