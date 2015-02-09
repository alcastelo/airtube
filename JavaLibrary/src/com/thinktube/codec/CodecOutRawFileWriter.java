package com.thinktube.codec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class CodecOutRawFileWriter implements CodecOutCallbackI {
	private FileChannel out;

	public CodecOutRawFileWriter(String filename) {
		File file = new File(filename);
		//File file = new File(Environment.getExternalStorageDirectory(), filename);
		try {
			out = new FileOutputStream(file).getChannel();
		} catch (FileNotFoundException e) {
			//Log.e("TAG", "file " + file + " can not be created");
		}
	}

	public CodecOutRawFileWriter(File file) {
		try {
			out = new FileOutputStream(file).getChannel();
		} catch (FileNotFoundException e) {
			//Log.e("TAG", "file " + file + " can not be created");
		}
	}

	public void handleFrame(byte[] buf, int size) {
		write(buf, size);
	}

	public void handleFrame(short[] buf, int size) {
		write(buf, size);
	}

	public void handleFrame(ByteBuffer buf, int size, int flags) {
		buf.limit(size);
		try {
			out.write(buf);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write(byte[] buf, int size) {
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.limit(size);
		try {
			out.write(bb);
		} catch (IOException e) {
			//Log.e(TAG, "Cannot write" + e);
		}
	}

	public void write(short[] buf, int size) {
		ByteBuffer bb = ByteBuffer.allocate(buf.length*2);
		bb.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(buf, 0, size);
		try {
			out.write(bb);
		} catch (IOException e) {
			//Log.e(TAG, "Cannot write" + e);
		}
	}

	@Override
	public void stop() {
		if (out != null) {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int getNecessaryHeadroom() {
		return 0;
	}
}
