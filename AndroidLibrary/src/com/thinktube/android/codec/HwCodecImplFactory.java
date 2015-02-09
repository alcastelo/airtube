/**
 *
 */
package com.thinktube.android.codec;

import com.thinktube.android.video.PixelFormat;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.os.Build;
import android.util.Log;

/**
 * @author Rafael Sierra
 *
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class HwCodecImplFactory {

	static final String TAG = "CodecImplFactory";

	/**
	 * Interface for hardware dependent codec parts
	 * @author Rafael Sierra
	 *
	 */
	public interface HwCodecImplI {
		MediaCodec getVideoEncoder();
		MediaCodec getVideoDecoder();
		int getColorFormat();
		byte[] cvtPixelFormat(byte[] input, byte[] output, int width, int height);
		MediaCodec getAudioEncoder();
		MediaCodec getAudioDecoder();
	}

	/**
	 * Galaxy Nexus Implementation
	 * @author Rafael Sierra
	 *
	 */
	public class CodecImpl_GalaxyNexus implements HwCodecImplI {
		@Override
		public MediaCodec getVideoEncoder() {
			return MediaCodec.createByCodecName("OMX.TI.DUCATI1.VIDEO.H264E");
		}

		@Override
		public MediaCodec getVideoDecoder() {
			return MediaCodec.createByCodecName("OMX.TI.DUCATI1.VIDEO.DECODER");
		}

		@Override
		public int getColorFormat() {
			return MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar;
		}

		@Override
		public byte[] cvtPixelFormat(final byte[] input, final byte[] output,
				final int width, final int height) {
			/* COLOR_TI_FormatYUV420PackedSemiPlanar is NV12.
			 * We could set the preview to NV21, which is alike but with U and V reversed and
			 * convert from there by just flipping 2 bytes.
			 * What we do now is we take the common YV12 preview format and have a slightly
			 * more expensive conversion.
			 */
			return PixelFormat.YV12toYUV420PackedSemiPlanar(input,
					output, width, height);
		}

		@Override
		public MediaCodec getAudioEncoder() {
			return MediaCodec.createByCodecName("OMX.google.aac.encoder");
			//"AACEncoder" crashes in native code
		}

		@Override
		public MediaCodec getAudioDecoder() {
			return MediaCodec.createByCodecName("OMX.google.aac.decoder");
		}
	}

	/**
	 * Nexus 7 Implementation
	 * @author Rafael Sierra
	 *
	 */
	public class CodecImpl_Nexus7 implements HwCodecImplI {
		@Override
		public MediaCodec getVideoEncoder() {
			return MediaCodec.createByCodecName("OMX.Nvidia.h264.encoder");
		}

		@Override
		public MediaCodec getVideoDecoder() {
			return MediaCodec.createByCodecName("OMX.Nvidia.h264.decode");
		}

		@Override
		public int getColorFormat() {
			return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
		}

		@Override
		public byte[] cvtPixelFormat(byte[] input, byte[] output, int width,
				int height) {
			return PixelFormat.YV12toYUV420Planar(input, output, width,
					height);
		}

		@Override
		public MediaCodec getAudioEncoder() {
			return MediaCodec.createByCodecName("OMX.google.aac.encoder");
			//"AACEncoder" crashes in native code
		}

		@Override
		public MediaCodec getAudioDecoder() {
			return MediaCodec.createByCodecName("OMX.google.aac.decoder");
		}
	}

	/**
	 * Nexus S Implementation
	 * @author Rafael Sierra
	 *
	 */
	public class CodecImpl_NexusS implements HwCodecImplI {
		@Override
		public MediaCodec getVideoEncoder() {
			return MediaCodec.createByCodecName("OMX.SEC.AVC.Encoder");
		}

		@Override
		public MediaCodec getVideoDecoder() {
			return MediaCodec.createByCodecName("OMX.SEC.AVC.Decoder");
		}

		@Override
		public int getColorFormat() {
			return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
		}

		@Override
		public byte[] cvtPixelFormat(byte[] input, byte[] output, int width,
				int height) {
			/* TODO: Not sure about the color format, something is wrong... */
			return input;
		}

		@Override
		public MediaCodec getAudioEncoder() {
			return MediaCodec.createByCodecName("OMX.google.aac.encoder");
		}

		@Override
		public MediaCodec getAudioDecoder() {
			return MediaCodec.createByCodecName("OMX.google.aac.decoder");
		}
	}

	/**
	 * Generic Implementation
	 * @author Rafael Sierra
	 *
	 */
	public class CodecImpl_Generic implements HwCodecImplI {
		@Override
		public MediaCodec getVideoEncoder() {
			return MediaCodec.createEncoderByType("video/avc");
		}

		@Override
		public MediaCodec getVideoDecoder() {
			return MediaCodec.createDecoderByType("video/avc");
		}

		@Override
		public int getColorFormat() {
			return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
		}

		@Override
		public byte[] cvtPixelFormat(byte[] input, byte[] output, int width,
				int height) {
			return input;
		}

		@Override
		public MediaCodec getAudioEncoder() {
			return MediaCodec.createEncoderByType("audio/mp4a-latm");
		}

		@Override
		public MediaCodec getAudioDecoder() {
			return MediaCodec.createDecoderByType("audio/mp4a-latm");
		}
	}

	static HwCodecImplFactory cif = new HwCodecImplFactory();

	public static HwCodecImplI createCodec() {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			Log.e(TAG, "MediaCodec not available in this version of Android! Expect errors!");
		}

		Log.i(TAG, "model: " + android.os.Build.MODEL);
		if (android.os.Build.MODEL.compareTo("Galaxy Nexus") == 0) {
			return cif.new CodecImpl_GalaxyNexus();
		}
		else if (android.os.Build.MODEL.compareTo("Nexus 7") == 0) {
			return cif.new CodecImpl_Nexus7();
		}
		else if (android.os.Build.MODEL.compareTo("Nexus S") == 0) {
			return cif.new CodecImpl_NexusS();
		}
		else {
			Log.e(TAG, "*** unknown device model ***");
			return cif.new CodecImpl_Generic();
		}
	}
}
