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

import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * A basic {@link android.app.Activity} for application.
 *
 * @author Xinyue Zhao
 */
public abstract class AppActivity extends BaseActivity {
	public static final int MENU_ITEM_OTHERS = -1;
	private static final int MENU_ITEM_FAVORITE = 0;
	private static final int MENU_ITEM_NEAR_RING = 1;
	protected static final int REQ_APP_ACTIVITY = 0x123;
	protected static final String RES_APP_ACTIVITY = AppActivity.class.getName() + ".EXTRAS.playground.res";
	protected static final String EXTRAS_MENU_ITEM = AppActivity.class.getName() + ".EXTRAS.playground.menu.item";

	/**
	 * Height of App-bar.
	 */
	private int mAppBarHeight;


	private @Nullable CommonUIDelegate mCommonUIDelegate;

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setErrorHandlerAvailable(true);
	}

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
	 * {@link #canBackPressedBeDone()} tells {@link AppActivity} that we should allow process back-press.
	 *
	 * @return {@code true} It's allowed.
	 */
	protected boolean canBackPressedBeDone() {
		return true;
	}

	@Override
	public void onBackPressed() {
		if (mCommonUIDelegate != null && mCommonUIDelegate.mDrawerLayout != null && mCommonUIDelegate.isDrawerOpened()) {
			mCommonUIDelegate.mDrawerLayout.closeDrawers();
		} else if (mCommonUIDelegate != null && mCommonUIDelegate.mItemSelected) {
			EventBus.getDefault()
			        .post(new BackPressedEvent());
		} else {
			if (canBackPressedBeDone()) {
				super.onBackPressed();
			}
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
		@SuppressWarnings("unused")
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
		@SuppressWarnings("unused")
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
		@SuppressWarnings("unused")
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
		@SuppressWarnings("unused")
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
		@SuppressWarnings("unused")
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
		@SuppressWarnings("unused")
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
		@SuppressWarnings("unused")
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
		@SuppressWarnings("unused")
		public void onEvent(@SuppressWarnings("UnusedParameters") FavoriteListLoadingErrorEvent e) {
			FavoriteManager.getInstance()
			               .init();
		}

		/**
		 * Handler for {@link NearRingListLoadingErrorEvent}.
		 *
		 * @param e Event {@link NearRingListLoadingErrorEvent}.
		 */
		@SuppressWarnings("unused")
		public void onEvent(@SuppressWarnings("UnusedParameters") NearRingListLoadingErrorEvent e) {
			NearRingManager.getInstance()
			               .init();
		}

		/**
		 * Handler for {@link MyLocationLoadingErrorEvent}.
		 *
		 * @param e Event {@link MyLocationLoadingErrorEvent}.
		 */
		@SuppressWarnings("unused")
		public void onEvent(@SuppressWarnings("UnusedParameters") MyLocationLoadingErrorEvent e) {
			MyLocationManager.getInstance()
			                 .init();
		}


		/**
		 * Handler for {@link RatingOnLocationsLoadingErrorEvent}.
		 *
		 * @param e Event {@link RatingOnLocationsLoadingErrorEvent}.
		 */
		@SuppressWarnings("unused")
		public void onEvent(@SuppressWarnings("UnusedParameters") RatingOnLocationsLoadingErrorEvent e) {
			RatingManager.getInstance()
			             .init();
		}


		/**
		 * Handler for {@link ListDetailShownEvent}.
		 *
		 * @param e Event {@link ListDetailShownEvent}.
		 */
		@SuppressWarnings("unused")
		public void onEvent(@SuppressWarnings("UnusedParameters") ListDetailShownEvent e) {
			mItemSelected = true;
		}


		/**
		 * Handler for {@link ListDetailClosedEvent}.
		 *
		 * @param e Event {@link ListDetailClosedEvent}.
		 */
		@SuppressWarnings("unused")
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

		private boolean onBackPressed() {
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

		private boolean isDrawerOpened() {
			if (mActivityWeakReference == null || mActivityWeakReference.get() == null || mNavigationView == null || mDrawerLayout == null || mAppListView == null) {
				return false;
			}
			mActivityWeakReference.get();
			return mDrawerLayout.isDrawerOpen(GravityCompat.START) || mDrawerLayout.isDrawerOpen(GravityCompat.END);
		}

		private void deselectMenuItems() {
			if (mActivityWeakReference == null || mActivityWeakReference.get() == null || mNavigationView == null) {
				return;
			}
			Activity activity = mActivityWeakReference.get();
			Prefs prefs = Prefs.getInstance();
			if (prefs.getCurrentSelectedMenuItem() != MENU_ITEM_OTHERS) {
				prefs.setCurrentSelectedMenuItem(MENU_ITEM_OTHERS);
				Menu menu = mNavigationView.getMenu();
				MenuItem itemFav = menu.findItem(R.id.action_favorite);
				MenuItem itemNearRing = menu.findItem(R.id.action_near_ring);
				itemFav.setChecked(false);
				itemNearRing.setChecked(false);
				activity.setTitle(App.Instance.getString(R.string.title_activity_maps));
			}
		}

		private final NavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
				if (mActivityWeakReference == null || mActivityWeakReference.get() == null || mDrawerLayout == null || mNavigationView == null) {
					return false;
				}
				Activity activity = mActivityWeakReference.get();
				final Menu menu = mNavigationView.getMenu();
				final MenuItem itemFav = menu.findItem(R.id.action_favorite);
				final MenuItem itemNearRing = menu.findItem(R.id.action_near_ring);
				final Prefs prefs = Prefs.getInstance();
				mDrawerLayout.closeDrawer(GravityCompat.START);
				switch (menuItem.getItemId()) {
					case R.id.action_favorite:
						if (prefs.getCurrentSelectedMenuItem() == MENU_ITEM_FAVORITE) {
							return true;
						}
						FavoriteManager favoriteManager = FavoriteManager.getInstance();
						if (favoriteManager.getCachedList()
						                   .size() > 0) {
							prefs.setCurrentSelectedMenuItem(MENU_ITEM_FAVORITE);
							itemFav.setChecked(true);
							itemNearRing.setChecked(false);
							if (!App.Instance.getResources()
							                 .getBoolean(R.bool.is_small_screen)) {
								EventBus.getDefault()
								        .post(new RefreshListEvent(favoriteManager.getCachedList()));
								activity.setTitle(App.Instance.getString(R.string.action_favorite,
								                                         FavoriteManager.getInstance()
								                                                        .getCachedList()
								                                                        .size()));
							} else {
								PlaygroundListActivity.showInstance(activity,
								                                    favoriteManager.getCachedList(),
								                                    itemNearRing);
							}
						} else {
							prefs.setCurrentSelectedMenuItem(MENU_ITEM_OTHERS);
							itemFav.setChecked(false);
							itemNearRing.setChecked(false);
						}
						break;
					case R.id.action_near_ring:
						if (prefs.getCurrentSelectedMenuItem() == MENU_ITEM_NEAR_RING) {
							return true;
						}
						NearRingManager nearRingManager = NearRingManager.getInstance();
						if (nearRingManager.getCachedList()
						                   .size() > 0) {
							prefs.setCurrentSelectedMenuItem(MENU_ITEM_NEAR_RING);
							itemFav.setChecked(false);
							itemNearRing.setChecked(true);
							if (!App.Instance.getResources()
							                 .getBoolean(R.bool.is_small_screen)) {
								EventBus.getDefault()
								        .post(new RefreshListEvent(nearRingManager.getCachedList()));
								activity.setTitle(App.Instance.getString(R.string.action_near_ring,
								                                         NearRingManager.getInstance()
								                                                        .getCachedList()
								                                                        .size()));
							} else {
								PlaygroundListActivity.showInstance(activity,
								                                    nearRingManager.getCachedList(),
								                                    itemFav);
							}
						} else {
							prefs.setCurrentSelectedMenuItem(MENU_ITEM_OTHERS);
							itemFav.setChecked(false);
							itemNearRing.setChecked(false);
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

		private void initNavigationAndDrawer() {
			if (mActivityWeakReference == null || mActivityWeakReference.get() == null || mNavigationView == null) {
				return;
			}
			Activity activity = mActivityWeakReference.get();
			Menu menu = mNavigationView.getMenu();
			MenuItem itemFav = menu.findItem(R.id.action_favorite);
			MenuItem itemNearRing = menu.findItem(R.id.action_near_ring);
			switch (Prefs.getInstance()
			             .getCurrentSelectedMenuItem()) {
				case AppActivity.MENU_ITEM_FAVORITE:
					itemFav.setChecked(true);
					itemNearRing.setChecked(false);
					activity.setTitle(App.Instance.getString(R.string.action_favorite,
					                                         FavoriteManager.getInstance()
					                                                        .getCachedList()
					                                                        .size()));
					break;
				case AppActivity.MENU_ITEM_NEAR_RING:
					itemFav.setChecked(false);
					itemNearRing.setChecked(true);
					activity.setTitle(App.Instance.getString(R.string.action_near_ring,
					                                         NearRingManager.getInstance()
					                                                        .getCachedList()
					                                                        .size()));
					break;
				default:
					itemFav.setChecked(false);
					itemNearRing.setChecked(false);
					activity.setTitle(App.Instance.getString(R.string.title_activity_maps));
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


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(getIntent().getIntExtra(EXTRAS_MENU_ITEM, NO_POSITION) == NO_POSITION) {
			Prefs.getInstance()
			     .setCurrentSelectedMenuItem(MENU_ITEM_OTHERS);
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}
		if (requestCode == AppActivity.REQ_APP_ACTIVITY && resultCode == RESULT_OK) {
			int menuItem = data.getIntExtra(RES_APP_ACTIVITY, NO_POSITION);
			switch (menuItem) {
				case R.id.action_favorite:
					Prefs.getInstance()
					     .setCurrentSelectedMenuItem(MENU_ITEM_FAVORITE);
					break;
				case R.id.action_near_ring:
					Prefs.getInstance()
					     .setCurrentSelectedMenuItem(MENU_ITEM_NEAR_RING);
					break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
