package com.thinktube.net;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * a common shared (routing) packet with different sections which can be read an
 * set my different components
 */
public class MultiSectionPacket {
	//private final static Logger LOG = Logger.getLogger(SectionRoutingPacket.class.getSimpleName());
	public final static byte PKT_TYPE = 2;
	private ArrayList<Section> sect = new ArrayList<Section>();

	private static class Section {
		byte code;
		ByteBuffer buf;
		int len;
		
		Section(byte code, ByteBuffer buf) {
			this.code = code;
			this.buf = buf;
			this.len = buf.limit();
		}
		
		Section(byte code, int len) {
			this.code = code;
			this.len = len;
		}
	}

	public void addSection(int code, ByteBuffer buf) {
		sect.add(new Section((byte)code, buf));
	}

	public ByteBuffer getSection(int code) {
		for (Section s : sect) {
			if (s.code == code) {
				return s.buf;
			}
		}
		return null;
	}

	public ByteBuffer[] getBuffers() {
		byte numEntries = (byte)sect.size();
		ByteBuffer[] bufs = new ByteBuffer[numEntries + 1];
		ByteBuffer idx = ByteBuffer.allocate(numEntries*5 + 2);
		int i = 1;

		idx.put(PKT_TYPE);
		idx.put(numEntries); // number of sections
		for (Section s : sect) {
			idx.put(s.code);
			idx.putInt(s.len);
			bufs[i++] = s.buf;
		}
		idx.flip();
		bufs[0] = idx;
		
		return bufs;
	}

	public void receive(ByteBuffer buf) {
		byte numSect = buf.get();
		for (int i=0; i < numSect; i++) {
			byte code = buf.get();
			int len = buf.getInt();
			sect.add(new Section(code, len));
		}

		for (int i=0; i < numSect; i++) {
			ByteBuffer sec = buf.slice();
			Section s = sect.get(i);
			sec.limit(s.len);
			buf.position(buf.position()+s.len);
			s.buf = sec;
		}
	}

	public void clear() {
		sect.clear();
	}
}
