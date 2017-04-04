package com.playground.notification.app.activities;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.playground.notification.R;
import com.playground.notification.bus.ListDetailClosedEvent;
import com.playground.notification.bus.ListDetailShownEvent;
import com.playground.notification.databinding.AppBarLayoutBinding;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.MyLocationManager;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.utils.Prefs;
import com.playground.notification.utils.Utils;


public abstract class AppBarActivity extends AppActivity {

	private static final @LayoutRes int LAYOUT = R.layout.activity_appbar;
	private AppBarLayoutBinding mBinding;
	private ActionBarDrawerToggle mDrawerToggle;

	//------------------------------------------------
	//Subscribes, event-handlers
	//------------------------------------------------


	/**
	 * Handler for {@link ListDetailShownEvent}.
	 *
	 * @param e Event {@link ListDetailShownEvent}.
	 */
	public void onEvent(@SuppressWarnings("UnusedParameters") ListDetailShownEvent e) {
		mBinding.appbar.appbarLayout.setExpanded(true);
		dismissToolbar();
	}


	/**
	 * Handler for {@link ListDetailClosedEvent}.
	 *
	 * @param e Event {@link ListDetailClosedEvent}.
	 */
	public void onEvent(@SuppressWarnings("UnusedParameters") ListDetailClosedEvent e) {
		showToolbar();
	}

	//------------------------------------------------

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		setupMain();
		initDrawer();
		setupContent(mBinding.appbarContent);
	}

	@Override
	protected void onDestroy() {
		if (mDrawerToggle != null) {
			mBinding.drawerLayout.removeDrawerListener(mDrawerToggle);
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}
	}

	/**
	 * Initialize the navigation drawer.
	 */
	private void initDrawer() {
		mDrawerToggle = new ActionBarDrawerToggle(this, mBinding.drawerLayout, R.string.application_name, R.string.app_name) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				Utils.updateDrawerMenuItem(mBinding.navView, R.id.action_favorite, R.string.action_favorite, FavoriteManager.getInstance());
				Utils.updateDrawerMenuItem(mBinding.navView, R.id.action_near_ring, R.string.action_near_ring, NearRingManager.getInstance());
				Utils.updateDrawerMenuItem(mBinding.navView, R.id.action_my_location_list, R.string.action_my_location_list, MyLocationManager.getInstance());
			}
		};
		mBinding.drawerLayout.addDrawerListener(mDrawerToggle);
	}


	private void setupMain() {
		setSupportActionBar(mBinding.appbar.toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	private void showToolbar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.show();
		}
	}

	private void dismissToolbar() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
	}

	protected abstract void setupContent(@NonNull FrameLayout contentLayout);

	protected final void setupFragment(@IdRes int container, @NonNull Fragment fragment) {
		getSupportFragmentManager().beginTransaction()
		                           .replace(container, fragment)
		                           .commit();
	}


	protected final void setupFragment(@NonNull Fragment fragment) {
		setupFragment(R.id.appbar_content, fragment);
	}


	protected AppBarLayoutBinding getBinding() {
		return mBinding;
	}


	@Override
	protected void onAppConfigIgnored() {
		super.onAppConfigIgnored();
		configFinished();
	}

	@Override
	protected void onAppConfigLoaded() {
		super.onAppConfigLoaded();
		configFinished();
	}

	private void configFinished() {
		Prefs prefs = Prefs.getInstance();
		if (prefs.isEULAOnceConfirmed() && TextUtils.isEmpty(prefs.getGoogleId())) {
			ActivityCompat.finishAffinity(this);
			return;
		}

		initManagers();

		if (!TextUtils.isEmpty(prefs.getApiHost())) {
			showAppList();
		}
	}


	@Override
	protected void setupCommonUIDelegate(@NonNull CommonUIDelegate commonUIDelegate) {
		super.setupCommonUIDelegate(commonUIDelegate);
		commonUIDelegate.setDrawerLayout(mBinding.drawerLayout);
		commonUIDelegate.setNavigationView(mBinding.navView);
		commonUIDelegate.setAppListView(mBinding.appListFl);
	}

	@Override
	protected boolean canBackPressedBeDone() {
		deselectMenuItems();
		return true;
	}
}
