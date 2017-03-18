package com.playground.notification.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.android.gms.maps.model.LatLng;
import com.playground.notification.R;
import com.playground.notification.app.fragments.StreetViewFragment;


/**
 * Show street-view for given location.
 *
 * @author Xinyue Zhao
 */
public final class StreetViewActivity extends AppBarActivity {

	private static final String EXTRAS_TITLE = StreetViewActivity.class.getName() + ".EXTRAS.";
	private static final String EXTRAS_LOCATION = StreetViewActivity.class.getName() + ".EXTRAS.location";

	/**
	 * View's menu.
	 */
	private static final int MENU = R.menu.menu_streetview;

	/**
	 * Show single instance of {@link}
	 *
	 * @param cxt {@link Activity}.
	 */
	public static void showInstance(@NonNull Activity cxt, @NonNull String title, @NonNull LatLng location) {
		Intent intent = new Intent(cxt, StreetViewActivity.class);
		intent.putExtra(EXTRAS_TITLE, title);
		intent.putExtra(EXTRAS_LOCATION, location);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		ActivityCompat.startActivity(cxt, intent, Bundle.EMPTY);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

		updateUI(intent);
	}

	private void updateUI(Intent intent) {
		String title = intent.getStringExtra(EXTRAS_TITLE);
		LatLng latLng = intent.getParcelableExtra(EXTRAS_LOCATION);
		showLocationStreetView(latLng);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(title);
		}
	}

	private void showLocationStreetView(LatLng location) {
		Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.appbar_content);
		StreetViewFragment dialogFragment;
		if (fragment == null) {
			dialogFragment = StreetViewFragment.newInstance(getApplicationContext(), location);
			setupFragment(dialogFragment);
		} else {
			dialogFragment = (StreetViewFragment) fragment;
			dialogFragment.setStreetView(location);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(MENU, menu);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_my_location:
				LatLng location = getIntent().getParcelableExtra(EXTRAS_LOCATION);
				showLocationStreetView(location);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void setupContent(@NonNull FrameLayout contentLayout) {
		Intent intent = getIntent();
		updateUI(intent);
	}
}
