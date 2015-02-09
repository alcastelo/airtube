package com.thinktube.android.view;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

public class ViewUtils {

	public static void createToastLogger(final Context act) {
		Logger.getLogger("").addHandler(new Handler() {
			@Override
			public void publish(LogRecord rec) {
				if (isLoggable(rec)) {
					Toast.makeText(act, rec.getLoggerName() + ": " + rec.getMessage(), Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void close() {
			}

			@Override
			public void flush() {
			}
		});
	}
	
	public static void setAspectRatio(View view, int videoWidth, int videoHeight, int spaceWidth, int spaceHeight) {
		LayoutParams lp = view.getLayoutParams();
		android.util.Log.d("XXX", "SPACE: " + spaceWidth + "x" + spaceHeight);

		lp.height = (int) (((float) videoHeight / (float) videoWidth) * (float) spaceWidth);
		lp.width = spaceWidth;

		if (lp.height > spaceHeight) {
			lp.height = spaceHeight;
			lp.width = (int) (((float) videoWidth / (float) videoHeight) * (float) lp.height);
		}

		android.util.Log.d("XXX", "FINALLY: " + lp.width + "x" + lp.height + " = " + ((float) lp.width) / lp.height);
		view.setLayoutParams(lp);
	}

	public static void keepScreenOn(Activity act) {
		act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
