package com.thinktube.android.audio;

import android.util.Log;

import com.thinktube.audio.AudioConfig;
import com.thinktube.audio.JitterBuffer;
import com.thinktube.audio.JitterBuffer.BufferEntry;
import com.thinktube.audio.JitterBuffer3;
import com.thinktube.codec.AudioDecoderI;
import com.thinktube.codec.AudioProtocol;

/**
 * AudioReceiver plugs together some components:
 * 
 * 1) AudioProtocol.Receiver knows the data format. It receives the data first
 * and then passes the frame to the jitter buffer
 * 
 * 2) The jitter buffer buffers and reorders frames and decides when to drop a
 * frame. It can give use the next frame in sequence, if available.
 * 
 * 3) We get frames from the jitter buffer, as available, decode them and pass
 * them to the player to play it. If the player does not get enough frames it
 * will plays out "comfort noise" instead.
 */
public class AudioReceiver {
	private static final String TAG = "AudioReceiver";
	private static final int MAX_DELAY = 256; //ms

	private final AudioDecoderI dec;
	private final AudioProtocol.Receiver proto;
	private final AudioPlayer player;
	private final JitterBuffer jb;

	public AudioReceiver(AudioDecoderI dec, AudioConfig audioConfig) {
		this(dec, AudioSetup.getAudioSessionId(), audioConfig);
	}

	public AudioReceiver(AudioDecoderI dec, int sessionId, AudioConfig audioConfig)
	{
		this.dec = dec;
		this.jb = new JitterBuffer3(audioConfig, MAX_DELAY);
		this.proto = new AudioProtocol.Receiver(jb);
		this.player = new AudioPlayer(sessionId, audioConfig, this);
	}

	public void start() {
		dec.start();
		player.start();
		Log.d(TAG, "Started");
	}

	public void stop() {
		dec.stop();
		player.stop();
		jb.reset();
	}

	public void inputData(byte[] data) {
		proto.handlePacket(data);
	}

	public boolean getAudioFrame(short[] out) {
		boolean res = false;
		BufferEntry be = jb.getPlayBuffer();

		if (be == null) {
			/* no packet available.
			 *
			 * if the decoder supports "packet loss concealing" (PLC) it will
			 * give us some interpolated sound data or silence, if not, the
			 * player will substitute "false" with "silence" (comfort noise)
			 */
			if (dec.hasPLC()) {
				//Log.d(TAG, "Packet loss PLC");
				res = dec.decode(null, 0, 0, out, false);
			}
		} else if (be.fec) {
			/* next packet (seqNo) is not available, but the next-next (seqNo+1)
			 *
			 * Since this packet *may* contain some redundant information about
			 * the previous packet thru "forward error correction" (FEC) the
			 * JitterBuffer has given it to as as a "preview" (peek) - therefore
			 * we don't have to return the buffer. It stays in the queue and we'll
			 * get it again as a "normal" sequence packet next  */
			if (dec.hasFEC()) {
				//Log.d(TAG, "Doing FEC with " + be.seqNo);
				res = dec.decode(be.data, be.offset, be.len, out, true);
			} else if (dec.hasPLC()) {
				Log.d(TAG, "PLC instead of FEC");
				res = dec.decode(null, 0, 0, out, false);
			}
		} else {
			/* "normal" packet with the correct next sequence number */
			//Log.d(TAG, "Normal decode of " + be.seqNo);
			res = dec.decode(be.data, be.offset, be.len, out, false);
			jb.returnPlayBuffer(be);
		}
		return res;
	}

	public JitterBuffer.Statistics getStats() {
		return jb.getStatistics();
	}

	public void setSpeexAEC(boolean on) {
		player.setSpeexAEC(on);
	}
}
