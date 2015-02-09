package com.thinktube.airtube.android.app;

import com.thinktube.airtube.*;
import com.thinktube.airtube.android.AirTubeBaseActivity;
import com.thinktube.airtube.android.AirTubeServiceConnection;
import com.thinktube.airtube.android.VideoClient;
import com.thinktube.android.view.LoggerScrollView;
import com.thinktube.android.view.ViewUtils;
import com.thinktube.service.R;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

public class VideoReceiverMultiActivity extends AirTubeBaseActivity {
	protected static final String TAG = "VideoReceiver";
	private static final int MAX_VIEW = 4;

	private static class VideoRecv {
		VideoClient vc;
		SurfaceView videoView;
		boolean fullscreen;
	}

	private int numSubs = 0;
	private VideoRecv[] clients = new VideoRecv[MAX_VIEW];

	private AirTubeServiceConnection clientIf;

	private RelativeLayout ll;
	private View btn;
	private LoggerScrollView log;

	private AirTubeComponent multiClient = new AirTubeComponent() {
		ServiceDescription desc = new ServiceDescription("video");
		AirTubeID myId;
		AirTubeInterfaceI airtube;

		@Override
		public void onServiceFound(AirTubeID sid, ServiceDescription desc) {
			Log.d(TAG, "service numSubs " + numSubs);

			for (int i=0; i < numSubs; i++) {
				if (clients[i].vc.getService().equals(sid)) {
					Log.d(TAG, "already subscribed to same " + sid + " reusing video slot");
					clients[i].vc.setService(sid, desc);
					return;
				}
			}

			if (numSubs < MAX_VIEW) {
				clients[numSubs].vc.onConnect(airtube);
				clients[numSubs].vc.setService(sid, desc);
				numSubs++;
			}
		}

		@Override
		public void onConnect(AirTubeInterfaceI airtube) {
			this.airtube = airtube;
			myId = airtube.registerClient(this);
			airtube.findServices(desc, myId);
		}

		@Override
		public void onDisconnect() {
			myId = null;
			stop();
		}

		@Override
		public void unregister() {
			if (myId != null && airtube != null) {
				airtube.unregisterServiceInterest(desc, myId);
				airtube.unregisterClient(myId);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_receiver_multi);

		ViewUtils.keepScreenOn(this);

		ll = (RelativeLayout) findViewById(R.id.layout);
		log = (LoggerScrollView) findViewById(R.id.scroller);
		btn = findViewById(R.id.toggleButton1);

		for (int i=0; i < MAX_VIEW; i++) {
			clients[i] = new VideoRecv();
			clients[i].vc = new VideoClient(true);
			clients[i].fullscreen = false;
		}

		clients[0].videoView = (SurfaceView) findViewById(R.id.decoder1);
		clients[0].videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d(TAG, "surface 1 created, try init");
				clients[0].vc.setSurface(holder.getSurface());
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				clients[0].vc.stop();
			}
			public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
				// nothing
			}
		});
		clients[0].videoView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				fullScreen(v, 0, false);
			}
		});

		clients[1].videoView = (SurfaceView) findViewById(R.id.decoder2);
		clients[1].videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d(TAG, "surface 2 created, try init");
				clients[1].vc.setSurface(holder.getSurface());
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				clients[1].vc.stop();
			}
			public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
				// nothing
			}
		});
		clients[1].videoView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				fullScreen(v, 1, false);
			}
		});

		clients[2].videoView = (SurfaceView) findViewById(R.id.decoder3);
		clients[2].videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d(TAG, "surface 3 created, try init");
				clients[2].vc.setSurface(holder.getSurface());
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				clients[2].vc.stop();
			}
			public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
				// nothing
			}
		});
		clients[2].videoView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				fullScreen(v, 2, false);
			}
		});

		clients[3].videoView = (SurfaceView) findViewById(R.id.decoder4);
		clients[3].videoView.getHolder().addCallback(new SurfaceHolder.Callback() {
			public void surfaceCreated(SurfaceHolder holder) {
				Log.d(TAG, "surface 4 created, try init");
				clients[3].vc.setSurface(holder.getSurface());
			}
			public void surfaceDestroyed(SurfaceHolder holder) {
				clients[3].vc.stop();
			}
			public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
				// nothing
			}
		});
		clients[3].videoView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				fullScreen(v, 3, false);
			}
		});

		ll.post(new Runnable() {
			@Override
			public void run() {
				// initialize layout
				for (int i=0; i < MAX_VIEW; i++) {
					fullScreen(clients[i].videoView, i, true);
				}
				ViewGroup.LayoutParams lp = log.getLayoutParams();
				lp.width = ll.getWidth() / 3;
				log.setLayoutParams(lp);
				lp = btn.getLayoutParams();
				lp.width = ll.getWidth() / 3;
				btn.setLayoutParams(lp);
			}
		});

		clientIf = new AirTubeServiceConnection(this, multiClient);
	}

	protected void fullScreen(View v, int i, boolean forceInit) {
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);

		if (clients[i].fullscreen || forceInit) {
			// back to normal position
			if (i == 1) {
				lp.addRule(RelativeLayout.RIGHT_OF, R.id.decoder1);
				lp.leftMargin = 5;
			} else if (i == 2) {
				lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			} else if (i == 3) {
				lp.addRule(RelativeLayout.RIGHT_OF, R.id.decoder3);
				lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				lp.leftMargin = 5;
			}
			int spaceY = ll.getHeight();
			int spaceX = ll.getWidth();
			int h = (spaceY - 5) / 2;
			int w = (spaceX - log.getWidth() - 10) / 2;

			v.setLayoutParams(lp);
			ViewUtils.setAspectRatio(v, 640, 480, w, h);
			clients[i].fullscreen = false;
			for (int n = 0; n < MAX_VIEW; n++) {
				clients[n].videoView.setVisibility(View.VISIBLE);
			}
		} else {
			// go fullscreen (inside App)
			v.setLayoutParams(lp);
			for (int n = 0; n < MAX_VIEW; n++) {
				if (n != i) {
					clients[n].videoView.setVisibility(View.INVISIBLE);
				}
			}
			ViewUtils.setAspectRatio(v, 640, 480, ll.getWidth(), ll.getHeight());
			clients[i].fullscreen = true;
		}
	}

	public void onButtonClicked(View view) {
		boolean on = ((ToggleButton) view).isChecked();
		if (on) {
			start();
		} else {
			stop();
		}
	}

	protected void start() {
		log.clear();
		clientIf.bind();
	}

	protected void stop() {
		for (int i=0; i<4; i++) {
			clients[i].vc.stop();
		}
		multiClient.unregister();
		clientIf.unbind();
		numSubs = 0;
	}
}
