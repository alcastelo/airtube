package com.thinktube.airtube.android.gui;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.thinktube.airtube.android.R;

public class MyArrayAdapter<T extends MyArrayAdapter.ListItem> extends BaseAdapter {
	private final LayoutInflater inflater;
	private final List<T> list;

	public interface ListItem {
		String toString();
		String toStringSmall();
		int getImageResource();
		void setFromBundle(Bundle b);
	}

	static class ViewHolder {
		public ImageView icon;
		public TextView text;
		public TextView text2;
	}

	public MyArrayAdapter(Activity act, List<T> list) {
		super();
		this.inflater = (LayoutInflater) act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.list = list;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.row, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
			viewHolder.text = (TextView) convertView.findViewById(R.id.text);
			viewHolder.text2 = (TextView) convertView.findViewById(R.id.textSmall);
		    convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		T li = list.get(position);

		viewHolder.text.setText(li.toString());
		viewHolder.text2.setText(li.toStringSmall());
		viewHolder.icon.setImageResource(li.getImageResource());

		return convertView;
	}

	@Override
	public int getCount() {
	    return list.size();
	}

	@Override
	public Object getItem(final int position) {
	  return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
}
