package com.thinktube.airtube.android.gui;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

class TabsPagerAdapter extends FragmentPagerAdapter implements
		ActionBar.TabListener, ViewPager.OnPageChangeListener {
	private List<Fragment> fragments;
	private ActionBar actionBar;
	private ViewPager viewPager;

	public TabsPagerAdapter(FragmentManager fm, ActionBar ab, ViewPager vp) {
		super(fm);
		fragments = new ArrayList<Fragment>(5);
		actionBar = ab;
		viewPager = vp;
	}

	@Override
	public Fragment getItem(int index) {
		return fragments.get(index);
	}

	@Override
	public int getCount() {
		return fragments.size();
	}

	public void addTabFragment(String name, Fragment f) {
		fragments.add(f);
		actionBar.addTab(actionBar.newTab().setText(name).setTabListener(this));
	}

	/* ActionBar.TabListener */

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		viewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	/* ViewPager.OnPageChangeListener */

	@Override
	public void onPageSelected(int position) {
		/**
		 * on swiping the viewpager make respective tab selected
		 */
		actionBar.setSelectedNavigationItem(position);
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}
}
