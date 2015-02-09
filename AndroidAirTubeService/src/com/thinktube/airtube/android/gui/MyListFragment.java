package com.thinktube.airtube.android.gui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;

public class MyListFragment<T extends MyArrayAdapter.ListItem> extends
		ListFragment {

	MyArrayAdapter<T> adapter;
	List<T> list = new ArrayList<T>();

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		adapter = new MyArrayAdapter<T>(activity, list);
		this.setListAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	public void addItem(T it) {
		list.add(it);

		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	public void removeItem(T it) {
		boolean removed = false;
		Iterator<T> iter = list.iterator();
		while (iter.hasNext()) {
			if (iter.next().equals(it)) {
				iter.remove();
				removed = true;
				break;
			}
		}

		if (adapter != null && removed) {
			adapter.notifyDataSetChanged();
		}
	}

	public void updateItem(T it, Bundle b) {
		boolean updated = false;
		T listItem;
		Iterator<T> iter = list.iterator();
		while (iter.hasNext()) {
			listItem = iter.next();
			if (listItem.equals(it)) {
				listItem.setFromBundle(b);
				updated = true;
				break;
			}
		}

		if (adapter != null && updated) {
			adapter.notifyDataSetChanged();
		}
	}

	public void clear() {
		list.clear();
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}
}
