package com.playground.notification.app.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.chopping.activities.BaseActivity;
import com.chopping.application.BasicPrefs;
import com.chopping.bus.CloseDrawerEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.playground.notification.R;
import com.playground.notification.app.App;
import com.playground.notification.app.fragments.AboutDialogFragment.EulaConfirmationDialog;
import com.playground.notification.app.fragments.AppListImpFragment;
import com.playground.notification.bus.BackPressedEvent;
import com.playground.notification.bus.EULAConfirmedEvent;
import com.playground.notification.bus.EULARejectEvent;
import com.playground.notification.bus.FavoriteListLoadingErrorEvent;
import com.playground.notification.bus.FavoriteListLoadingSuccessEvent;
import com.playground.notification.bus.ListDetailClosedEvent;
import com.playground.notification.bus.ListDetailShownEvent;
import com.playground.notification.bus.MyLocationLoadingErrorEvent;
import com.playground.notification.bus.MyLocationLoadingSuccessEvent;
import com.playground.notification.bus.NearRingListLoadingErrorEvent;
import com.playground.notification.bus.NearRingListLoadingSuccessEvent;
import com.playground.notification.bus.RatingOnLocationsLoadingErrorEvent;
import com.playground.notification.bus.RefreshListEvent;
import com.playground.notification.bus.ShowStreetViewEvent;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.MyLocationManager;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.sync.RatingManager;
import com.playground.notification.utils.Prefs;

import java.lang.ref.WeakReference;

import de.greenrobot.event.EventBus;

/**
 * A basic {@link android.app.Activity} for application.
 *
 * @author Xinyue Zhao
 */
public abstract class AppActivity extends BaseActivity {
	public static final int MENU_ITEM_OTHERS = -1;
	private static final int MENU_ITEM_FAVORITE = 0;
	private static final int MENU_ITEM_NEAR_RING = 1;

	/**
	 * Height of App-bar.
	 */
	private int mAppBarHeight;


	private @Nullable CommonUIDelegate mCommonUIDelegate;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		calcAppBarHeight();
	}

	/**
	 * Show  {@link android.support.v4.app.DialogFragment}.
	 *
	 * @param _dlgFrg  An instance of {@link android.support.v4.app.DialogFragment}.
	 * @param _tagName Tag name for dialog, default is "dlg". To grantee that only one instance of {@link android.support.v4.app.DialogFragment} can been seen.
	 */
	public void showDialogFragment(DialogFragment _dlgFrg, String _tagName) {
		try {
			if (_dlgFrg != null) {
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				// Ensure that there's only one dialog to the user.
				Fragment prev = getSupportFragmentManager().findFragmentByTag("dlg");
				if (prev != null) {
					ft.remove(prev);
				}
				try {
					if (TextUtils.isEmpty(_tagName)) {
						_dlgFrg.show(ft, "dlg");
					} else {
						_dlgFrg.show(ft, _tagName);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void onResume() {
		if (mCommonUIDelegate == null) {
			setupCommonUIDelegate(mCommonUIDelegate = new CommonUIDelegate());
		}
		super.onResume();
		checkPlayService();

		if (mCommonUIDelegate != null) {
			EventBus.getDefault()
			        .register(mCommonUIDelegate);


			mCommonUIDelegate.initNavigationAndDrawer();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mCommonUIDelegate != null) {
			EventBus.getDefault()
			        .unregister(mCommonUIDelegate);
		}
	}

	/**
	 * To confirm whether the validation of the Play-service of Google Inc.
	 */
	private void checkPlayService() {
		int resultCode = GoogleApiAvailability.getInstance()
		                                      .isGooglePlayServicesAvailable(this);
		if (resultCode == ConnectionResult.SUCCESS) {//Ignore update.
			//The "End User License Agreement" must be confirmed before you use this application.
			if (!Prefs.getInstance()
			          .isEULAOnceConfirmed()) {
				showDialogFragment(new EulaConfirmationDialog(), null);
			}
		} else {
			new Builder(this).setTitle(R.string.application_name)
			                 .setMessage(R.string.lbl_play_service)
			                 .setCancelable(false)
			                 .setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
				                 public void onClick(DialogInterface dialog, int whichButton) {
					                 dialog.dismiss();
					                 Intent intent = new Intent(Intent.ACTION_VIEW);
					                 intent.setData(Uri.parse(getString(R.string.play_service_url)));
					                 try {
						                 startActivity(intent);
					                 } catch (ActivityNotFoundException e0) {
						                 intent.setData(Uri.parse(getString(R.string.play_service_web)));
						                 try {
							                 startActivity(intent);
						                 } catch (Exception e1) {
							                 //Ignore now.
						                 }
					                 } finally {
						                 finish();
					                 }
				                 }
			                 })
			                 .create()
			                 .show();
		}
	}


	/**
	 * Calculate height of actionbar.
	 */
	private void calcAppBarHeight() {
		int[] abSzAttr;
		abSzAttr = new int[] { android.R.attr.actionBarSize };
		@SuppressLint("Recycle") TypedArray a = obtainStyledAttributes(abSzAttr);
		mAppBarHeight = a.getDimensionPixelSize(0, -1);
	}

	/**
	 * @return Height of App-bar.
	 */
	public int getAppBarHeight() {
		return mAppBarHeight;
	}

	@Override
	protected BasicPrefs getPrefs() {
		return Prefs.getInstance();
	}


	/**
	 * {@link #setupCommonUIDelegate(CommonUIDelegate)} to setup different shared UI elements.
	 *
	 * @param commonUIDelegate A new created {@link CommonUIDelegate} will be setup.
	 */
	protected void setupCommonUIDelegate(@NonNull CommonUIDelegate commonUIDelegate) {
		commonUIDelegate.setActivityWeakReference(this);
	}

	/**
	 * {@link #shouldDoBackPressed()} tells {@link AppActivity} that we should allow process back-press.
	 *
	 * @return {@code true} It's allowed.
	 */
	protected boolean shouldDoBackPressed() {
		if (mCommonUIDelegate != null && mCommonUIDelegate.mItemSelected) {
			EventBus.getDefault()
			        .post(new BackPressedEvent());
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void onBackPressed() {
		if (shouldDoBackPressed() && mCommonUIDelegate != null && !mCommonUIDelegate.onBackPressed()) {
			super.onBackPressed();
		}
	}

	/**
	 * Different {@link AppActivity} might have same UI elements like  {@link NavigationView}, {@link DrawerLayout} and the logical on these elements should be the same , and the
	 * {@link CommonUIDelegate} uses {@link de.greenrobot.event.EventBus} to share the logical.
	 *
	 * @author Xinyue Zhao
	 */
	protected static final class CommonUIDelegate {
		private @Nullable DrawerLayout mDrawerLayout;
		private @Nullable NavigationView mNavigationView;
		private @Nullable WeakReference<Activity> mActivityWeakReference;
		private @Nullable View mAppListView;

		private boolean mItemSelected;

		//------------------------------------------------
		//Subscribes, event-handlers
		//------------------------------------------------

		/**
		 * Handler for {@link com.chopping.bus.CloseDrawerEvent}.
		 *
		 * @param e Event {@link com.chopping.bus.CloseDrawerEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") CloseDrawerEvent e) {
			if (mDrawerLayout != null) {
				mDrawerLayout.closeDrawers();
			}
		}


		/**
		 * Handler for {@link FavoriteListLoadingSuccessEvent}.
		 *
		 * @param e Event {@link FavoriteListLoadingSuccessEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") FavoriteListLoadingSuccessEvent e) {
			if (mNavigationView != null) {
				com.playground.notification.utils.Utils.updateDrawerMenuItem(mNavigationView, R.id.action_favorite, R.string.action_favorite, FavoriteManager.getInstance());
			}
		}

		/**
		 * Handler for {@link NearRingListLoadingSuccessEvent}.
		 *
		 * @param e Event {@link NearRingListLoadingSuccessEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") NearRingListLoadingSuccessEvent e) {
			if (mNavigationView != null) {
				com.playground.notification.utils.Utils.updateDrawerMenuItem(mNavigationView, R.id.action_near_ring, R.string.action_near_ring, NearRingManager.getInstance());
			}
		}

		/**
		 * Handler for {@link MyLocationLoadingSuccessEvent}.
		 *
		 * @param e Event {@link MyLocationLoadingSuccessEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") MyLocationLoadingSuccessEvent e) {
			if (mNavigationView != null) {
				com.playground.notification.utils.Utils.updateDrawerMenuItem(mNavigationView, R.id.action_my_location_list, R.string.action_my_location_list, MyLocationManager.getInstance());
			}
		}


		/**
		 * Handler for {@link ShowStreetViewEvent}.
		 *
		 * @param e Event {@link ShowStreetViewEvent}.
		 */
		public void onEvent(ShowStreetViewEvent e) {
			if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {
				StreetViewActivity.showInstance(mActivityWeakReference.get(), e.getTitle(), e.getLocation());
			}
		}


		/**
		 * Handler for {@link  EULARejectEvent}.
		 *
		 * @param e Event {@link  EULARejectEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") EULARejectEvent e) {
			if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {
				ActivityCompat.finishAffinity(mActivityWeakReference.get());
			}
		}

		/**
		 * Handler for {@link  EULAConfirmedEvent}.
		 *
		 * @param e Event {@link  EULAConfirmedEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") EULAConfirmedEvent e) {
			if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {
				ConnectGoogleActivity.showInstance(mActivityWeakReference.get());
			}
		}


		/**
		 * Handler for {@link FavoriteListLoadingErrorEvent}.
		 *
		 * @param e Event {@link FavoriteListLoadingErrorEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") FavoriteListLoadingErrorEvent e) {
			FavoriteManager.getInstance()
			               .init();
		}

		/**
		 * Handler for {@link NearRingListLoadingErrorEvent}.
		 *
		 * @param e Event {@link NearRingListLoadingErrorEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") NearRingListLoadingErrorEvent e) {
			NearRingManager.getInstance()
			               .init();
		}

		/**
		 * Handler for {@link MyLocationLoadingErrorEvent}.
		 *
		 * @param e Event {@link MyLocationLoadingErrorEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") MyLocationLoadingErrorEvent e) {
			MyLocationManager.getInstance()
			                 .init();
		}


		/**
		 * Handler for {@link RatingOnLocationsLoadingErrorEvent}.
		 *
		 * @param e Event {@link RatingOnLocationsLoadingErrorEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") RatingOnLocationsLoadingErrorEvent e) {
			RatingManager.getInstance()
			             .init();
		}


		/**
		 * Handler for {@link ListDetailShownEvent}.
		 *
		 * @param e Event {@link ListDetailShownEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") ListDetailShownEvent e) {
			mItemSelected = true;
		}


		/**
		 * Handler for {@link ListDetailClosedEvent}.
		 *
		 * @param e Event {@link ListDetailClosedEvent}.
		 */
		public void onEvent(@SuppressWarnings("UnusedParameters") ListDetailClosedEvent e) {
			mItemSelected = false;
		}

		//------------------------------------------------


		void setDrawerLayout(@NonNull DrawerLayout drawerLayout) {
			mDrawerLayout = drawerLayout;
		}

		void setNavigationView(@NonNull NavigationView navigationView) {
			mNavigationView = navigationView;
		}

		void setActivityWeakReference(@NonNull Activity activity) {
			mActivityWeakReference = new WeakReference<>(activity);
		}

		void setAppListView(@Nullable View appListView) {
			mAppListView = appListView;
		}

		boolean onBackPressed() {
			if (mActivityWeakReference == null || mActivityWeakReference.get() == null || mNavigationView == null || mDrawerLayout == null || mAppListView == null) {
				return false;
			}
			mActivityWeakReference.get();
			if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
				mDrawerLayout.closeDrawer(GravityCompat.START);
				return true;
			} else if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
				mDrawerLayout.closeDrawer(GravityCompat.END);
				return true;
			}
			return false;
		}

		void deselectMenuItems() {
			if (mNavigationView == null) {
				return;
			}
			Prefs prefs = Prefs.getInstance();
			if (prefs.getCurrentSelectedMenuItem() != MENU_ITEM_OTHERS) {
				prefs.setCurrentSelectedMenuItem(MENU_ITEM_OTHERS);
				Menu menu = mNavigationView.getMenu();
				MenuItem itemFav = menu.findItem(R.id.action_favorite);
				MenuItem itemNearRing = menu.findItem(R.id.action_near_ring);
				itemFav.setChecked(false);
				itemNearRing.setChecked(false);
			}
		}

		private final NavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				if (mActivityWeakReference == null || mActivityWeakReference.get() == null || mDrawerLayout == null || mNavigationView == null) {
					return false;
				}
				final Menu menu = mNavigationView.getMenu();
				final MenuItem itemFav = menu.findItem(R.id.action_favorite);
				final MenuItem itemNearRing = menu.findItem(R.id.action_near_ring);
				final Prefs prefs = Prefs.getInstance();
				Activity activity = mActivityWeakReference.get();
				mDrawerLayout.closeDrawer(GravityCompat.START);
				switch (menuItem.getItemId()) {
					case R.id.action_favorite:
						prefs.setCurrentSelectedMenuItem(MENU_ITEM_FAVORITE);
						itemFav.setChecked(true);
						itemNearRing.setChecked(false);
						FavoriteManager favoriteManager = FavoriteManager.getInstance();
						if (favoriteManager.getCachedList()
						                   .size() > 0) {
							if (!App.Instance.getResources()
							                 .getBoolean(R.bool.is_small_screen)) {
								EventBus.getDefault()
								        .post(new RefreshListEvent(favoriteManager.getCachedList()));
							} else {
								PlaygroundListActivity.showInstance(activity, favoriteManager.getCachedList());
							}
						}
						break;
					case R.id.action_near_ring:
						prefs.setCurrentSelectedMenuItem(MENU_ITEM_NEAR_RING);
						itemFav.setChecked(false);
						itemNearRing.setChecked(true);
						NearRingManager nearRingManager = NearRingManager.getInstance();
						if (nearRingManager.getCachedList()
						                   .size() > 0) {
							if (!App.Instance.getResources()
							                 .getBoolean(R.bool.is_small_screen)) {
								EventBus.getDefault()
								        .post(new RefreshListEvent(nearRingManager.getCachedList()));
							} else {
								PlaygroundListActivity.showInstance(activity, nearRingManager.getCachedList());
							}
						}
						break;
					case R.id.action_my_location_list:
						MyLocationManager myLocationManager = MyLocationManager.getInstance();
						if (myLocationManager.getCachedList()
						                     .size() > 0) {
							MyLocationListActivity.showInstance(activity);
						}
						break;
					case R.id.action_settings:
						SettingsActivity.showInstance(activity);
						break;
					case R.id.action_more_apps:
						mDrawerLayout.openDrawer(GravityCompat.END);
						break;
					case R.id.action_radar:
						com.playground.notification.utils.Utils.openExternalBrowser(activity, "http://" + App.Instance.getString(R.string.support_spielplatz_radar));
						break;
					case R.id.action_weather:
						com.playground.notification.utils.Utils.openExternalBrowser(activity, "http://" + App.Instance.getString(R.string.support_openweathermap));
						break;
				}
				return true;
			}
		};

		void initNavigationAndDrawer() {
			if (mNavigationView == null) {
				return;
			}
			final Menu menu = mNavigationView.getMenu();
			final MenuItem itemFav = menu.findItem(R.id.action_favorite);
			final MenuItem itemNearRing = menu.findItem(R.id.action_near_ring);
			switch (Prefs.getInstance()
			             .getCurrentSelectedMenuItem()) {
				case AppActivity.MENU_ITEM_FAVORITE:
					itemFav.setChecked(true);
					itemNearRing.setChecked(false);
					break;
				case AppActivity.MENU_ITEM_NEAR_RING:
					itemFav.setChecked(false);
					itemNearRing.setChecked(true);
					break;
				default:
					itemFav.setChecked(false);
					itemNearRing.setChecked(false);
					break;
			}
			mNavigationView.setNavigationItemSelectedListener(onNavigationItemSelectedListener);
		}
	}

	protected void initManagers() {
		FavoriteManager.getInstance()
		               .init();
		NearRingManager.getInstance()
		               .init();
		MyLocationManager.getInstance()
		                 .init();
		RatingManager.getInstance()
		             .init();
	}

	/**
	 * Show all external applications links.
	 */
	protected void showAppList() {
		getSupportFragmentManager().beginTransaction()
		                           .replace(R.id.app_list_fl, AppListImpFragment.newInstance(this))
		                           .commit();
	}

	protected void deselectMenuItems() {
		if (mCommonUIDelegate != null) {
			mCommonUIDelegate.deselectMenuItems();
		}
	}
}
