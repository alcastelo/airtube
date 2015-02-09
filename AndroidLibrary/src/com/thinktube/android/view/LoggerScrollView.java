package com.thinktube.android.view;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;

public class LoggerScrollView extends ScrollView {
	private TextView text;

	// necessary for automatic instantiation from view.xml
	public LoggerScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		text = new TextView(context);
		this.addView(text);

		// add default handler to log to GUI
		Logger.getLogger("").addHandler(new Handler() {
			@Override
			public void publish(LogRecord rec) {
				if (isLoggable(rec)) {
					log(rec.getMessage());
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

	public void log(final String msg) {
		this.post(new Runnable() {
			@Override
			public void run() {
				text.append(msg + "\n");
				smoothScrollTo(0, text.getBottom());
			}
		});
	}

	public void clear() {
		text.setText("");
	}
}
