package com.thinktube.android.video;

/*
 * See http://www.fourcc.org/yuv.php
 *
 * I420: 8 bit Y plane followed by 8 bit 2x2 subsampled U and V planes.
 * YV12: 8 bit Y plane followed by 8 bit 2x2 subsampled V and U planes.
 * NV12: 8-bit Y plane followed by an interleaved U/V plane with 2x2 subsampling
 * NV21: As NV12 with U and V reversed in the interleaved plane
 *
 * YUV420Planar is I420
 * YUV420PackedSemiPlanar is NV12
 */

public class PixelFormat {
	/**
	 * convert pixel format from YV12 to YUV420 packed semi-planar (NV12)
	 * @param input	in YV12 format
	 * @param output in YUV240 packed semi-planar (NV12)
	 * @param width
	 * @param height
	 */
	public static byte[] YV12toYUV420PackedSemiPlanar(final byte[] input,
			final byte[] output, final int width, final int height) {
		/*
		 * COLOR_TI_FormatYUV420PackedSemiPlanar is NV12
		 * We convert by putting the corresponding U and V bytes together (interleaved).
		 */
		final int frameSize = width * height;
		final int qFrameSize = frameSize / 4;

		System.arraycopy(input, 0, output, 0, frameSize); // Y

		for (int i = 0; i < qFrameSize; i++) {
			output[frameSize + i * 2] = input[frameSize + i + qFrameSize]; // Cb (U)
			output[frameSize + i * 2 + 1] = input[frameSize + i]; // Cr (V)
		}
		return output;
	}

	/**
	 * convert pixel format from YV12 to YUV420 planar
	 * @param input	in YV12 format
	 * @param output in YUV420 planar (I420)
	 * @param width
	 * @param height
	 */
	public static byte[] YV12toYUV420Planar(byte[] input, byte[] output,
			int width, int height) {
		/*
		 * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
		 * So we just have to reverse U and V.
		 */
		final int frameSize = width * height;
		final int qFrameSize = frameSize / 4;

		System.arraycopy(input, 0, output, 0, frameSize); // Y
		System.arraycopy(input, frameSize, output, frameSize + qFrameSize,
				qFrameSize); // Cr (V)
		System.arraycopy(input, frameSize + qFrameSize, output, frameSize,
				qFrameSize); // Cb (U)

		return output;
	}

	public static void rotatePlane90(byte[] source, byte[] dest, int startPos, int width, int height) {
		/*for (int y = 0; y < height; y++) {
		    for (int x = 0; x < width; x++)
		        out[startPos + x * height + height - y - 1] = in[startPos + x + y * width];
		}*/
		for (int h = 0, dest_col = height - 1; h < height; ++h, --dest_col) {
			for (int w = 0; w < width; w++) {
				dest[(w * height) + dest_col] = source[h * width + w];
			}
		}
	}

	public static void rotatePlane270(byte[] source, byte[] dest, int startPos, int width, int height) {
		for (int h = 0, dest_col = 0; h < height; ++dest_col, ++h) {
			for (int w = 0, dest_row = width - 1; w < width; --dest_row, ++w) {
				dest[startPos + (dest_row * height) + dest_col] = source[startPos + h * width + w];
			}
		}
	}

	public static void rotateYV12_270(byte[] source, byte[] dest, int width, int height) {
		final int frameSize = width * height;
		final int qFrameSize = frameSize / 4;

		rotatePlane270(source, dest, 0, width, height);
		rotatePlane270(source, dest, frameSize, width / 2, height / 2);
		rotatePlane270(source, dest, frameSize + qFrameSize, width / 2, height / 2);
	}

	public static int getYV12PreviewSize(int width, int height) {
		// from http://developer.android.com/reference/android/hardware/Camera.Parameters.html#setPreviewFormat%28int%29
		int yStride = (int) Math.ceil(width / 16.0) * 16;
		int uvStride = (int) Math.ceil((yStride / 2) / 16.0) * 16;
		int ySize = yStride * height;
		int uvSize = uvStride * height / 2;
		int size = ySize + uvSize * 2;
		//int yRowIndex = yStride * y;
		//int uRowIndex = ySize + uvSize + uvStride * c;
		//int vRowIndex = ySize + uvStride * c;

		// from http://developer.android.com/reference/android/graphics/ImageFormat.html
		// it's the same like above, except for:
		//int cr_offset = ySize;
		//int cb_offset = ySize + uvSize;
		return size;
	}

	/* unused methods:

	// just a test for Nexus S
	public static byte[] NV21toYUV420Planar(final byte[] input,
			final byte[] output, final int width, final int height) {

		final int frameSize = width * height;
		final int qFrameSize = frameSize / 4;

		System.arraycopy(input, 0, output, 0, frameSize); // Y

		for (int i = 0; i < qFrameSize; i++) {
			output[frameSize + i + qFrameSize] = input[frameSize + i * 2]; // Cb (U)
			output[frameSize + i] = input[frameSize + i * 2 + 1]; // Cr (V)
		}
		return output;
	}

	//Method from Ketai project YUV420 to RGB
	void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

		final int frameSize = width * height;

		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}

				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);

				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;

				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
						| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}
	*/
}
