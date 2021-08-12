/*
 * Copyright (C) 2011 Creendo
 */

package com.creendo.mundart;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TabHost;

import com.creendo.mundart.data.MundartDb;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class Mundart extends TabActivity {

	private TabHost mTabHost;
	private TabHost.TabSpec mSpec;
	private Intent mIntent;
	private Resources mRes;

	private MundartDb mDb;

	public static AdRequest mAdRequest;
	public static final String MY_AD_UNIT_ID = "a14d87a54c83851";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mRes = getResources();
		
		// load the adRequest. we do this as soon as possible as we want
		// the banner to be loaded when the other views are showed
		mAdRequest = new AdRequest();
		AdView adView = new AdView(this, AdSize.BANNER, MY_AD_UNIT_ID);
		LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
		layout.addView(adView);
		adView.loadAd(new AdRequest());

		//open the database
		mDb = new MundartDb(this);
		mDb.open();

		mTabHost = getTabHost();

		// the word list tab ("Wortschatz")
		mIntent = new Intent().setClass(this, WordList.class);
		mSpec = mTabHost.newTabSpec("wordlist");
		mSpec.setIndicator("Wortschatz",
				mRes.getDrawable(R.drawable.ic_tab_wordlist));
		mSpec.setContent(mIntent);
		mTabHost.addTab(mSpec);

		// the myWords tab ("Meine Wšrter")
		mIntent = new Intent().setClass(this, UserWordList.class);
		mSpec = mTabHost.newTabSpec("wordlist2");
		mSpec.setIndicator("Meine Wšrter",
				mRes.getDrawable(R.drawable.ic_tab_userwordlist));
		mSpec.setContent(mIntent);
		mTabHost.addTab(mSpec);

	}

	/**
	 * Close the Database when Mundart
	 * is distroyed.
	 * 
	 * @see android.app.ActivityGroup#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mDb.close();
	}

}