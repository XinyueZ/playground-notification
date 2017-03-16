package com.playground.notification.app.activities;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;

import com.chopping.activities.BaseActivity;
import com.chopping.application.BasicPrefs;
import com.chopping.bus.CloseDrawerEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.playground.notification.R;
import com.playground.notification.app.fragments.AboutDialogFragment.EulaConfirmationDialog;
import com.playground.notification.bus.BackPressedEvent;
import com.playground.notification.bus.ListDetailClosedEvent;
import com.playground.notification.bus.ListDetailShownEvent;
import com.playground.notification.bus.EULAConfirmedEvent;
import com.playground.notification.bus.EULARejectEvent;
import com.playground.notification.bus.FavoriteListLoadingErrorEvent;
import com.playground.notification.bus.FavoriteListLoadingSuccessEvent;
import com.playground.notification.bus.MyLocationLoadingErrorEvent;
import com.playground.notification.bus.MyLocationLoadingSuccessEvent;
import com.playground.notification.bus.NearRingListLoadingErrorEvent;
import com.playground.notification.bus.NearRingListLoadingSuccessEvent;
import com.playground.notification.bus.RatingOnLocationsLoadingErrorEvent;
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
	/**
	 * Height of App-bar.
	 */
	private int mAppBarHeight;


	private CommonUIDelegate mCommonUIDelegate;

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
				DialogFragment dialogFragment = _dlgFrg;
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				// Ensure that there's only one dialog to the user.
				Fragment prev = getSupportFragmentManager().findFragmentByTag("dlg");
				if (prev != null) {
					ft.remove(prev);
				}
				try {
					if (TextUtils.isEmpty(_tagName)) {
						dialogFragment.show(ft, "dlg");
					} else {
						dialogFragment.show(ft, _tagName);
					}
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkPlayService();
		if (needCommonUIDelegate() && mCommonUIDelegate == null) {
			setupCommonUIDelegate(mCommonUIDelegate = new CommonUIDelegate());
		}
		if (mCommonUIDelegate != null) {
			EventBus.getDefault()
			        .register(mCommonUIDelegate);
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
		final int isFound = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (isFound == ConnectionResult.SUCCESS) {//Ignore update.
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
	protected void calcAppBarHeight() {
		int[] abSzAttr;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			abSzAttr = new int[] { android.R.attr.actionBarSize };
		} else {
			abSzAttr = new int[] { R.attr.actionBarSize };
		}
		TypedArray a = obtainStyledAttributes(abSzAttr);
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
	 * {@link #needCommonUIDelegate()} tells {@link AppActivity} whether to use common logical on some shared UIs.
	 *
	 * @return {@code false} if don't has same common logical.
	 */
	protected boolean needCommonUIDelegate() {
		return true;
	}

	/**
	 * {@link #setupCommonUIDelegate(CommonUIDelegate)} to setup different shared UI elements.
	 *
	 * @param commonUIDelegate A new created {@link CommonUIDelegate} will be setup. Ignore to override this if {@link #needCommonUIDelegate()} returns {@code false}.
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
		if (mCommonUIDelegate.mItemSelected) {
			EventBus.getDefault()
			        .post(new BackPressedEvent());
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void onBackPressed() {
		if (shouldDoBackPressed()) {
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

		private boolean mItemSelected;

		//------------------------------------------------
		//Subscribes, event-handlers
		//------------------------------------------------

		/**
		 * Handler for {@link com.chopping.bus.CloseDrawerEvent}.
		 *
		 * @param e Event {@link com.chopping.bus.CloseDrawerEvent}.
		 */
		public void onEvent(CloseDrawerEvent e) {
			if (mDrawerLayout != null) {
				mDrawerLayout.closeDrawers();
			}
		}


		/**
		 * Handler for {@link FavoriteListLoadingSuccessEvent}.
		 *
		 * @param e Event {@link FavoriteListLoadingSuccessEvent}.
		 */
		public void onEvent(FavoriteListLoadingSuccessEvent e) {
			if (mNavigationView != null) {
				com.playground.notification.utils.Utils.updateDrawerMenuItem(mNavigationView, R.id.action_favorite, R.string.action_favorite, FavoriteManager.getInstance());
			}
		}

		/**
		 * Handler for {@link NearRingListLoadingSuccessEvent}.
		 *
		 * @param e Event {@link NearRingListLoadingSuccessEvent}.
		 */
		public void onEvent(NearRingListLoadingSuccessEvent e) {
			if (mNavigationView != null) {
				com.playground.notification.utils.Utils.updateDrawerMenuItem(mNavigationView, R.id.action_near_ring, R.string.action_near_ring, NearRingManager.getInstance());
			}
		}

		/**
		 * Handler for {@link MyLocationLoadingSuccessEvent}.
		 *
		 * @param e Event {@link MyLocationLoadingSuccessEvent}.
		 */
		public void onEvent(MyLocationLoadingSuccessEvent e) {
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
		public void onEvent(EULARejectEvent e) {
			if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {
				ActivityCompat.finishAffinity(mActivityWeakReference.get());
			}
		}

		/**
		 * Handler for {@link  EULAConfirmedEvent}.
		 *
		 * @param e Event {@link  EULAConfirmedEvent}.
		 */
		public void onEvent(EULAConfirmedEvent e) {
			if (mActivityWeakReference != null && mActivityWeakReference.get() != null) {
				ConnectGoogleActivity.showInstance(mActivityWeakReference.get());
			}
		}


		/**
		 * Handler for {@link FavoriteListLoadingErrorEvent}.
		 *
		 * @param e Event {@link FavoriteListLoadingErrorEvent}.
		 */
		public void onEvent(FavoriteListLoadingErrorEvent e) {
			FavoriteManager.getInstance()
			               .init();
		}

		/**
		 * Handler for {@link NearRingListLoadingErrorEvent}.
		 *
		 * @param e Event {@link NearRingListLoadingErrorEvent}.
		 */
		public void onEvent(NearRingListLoadingErrorEvent e) {
			NearRingManager.getInstance()
			               .init();
		}

		/**
		 * Handler for {@link MyLocationLoadingErrorEvent}.
		 *
		 * @param e Event {@link MyLocationLoadingErrorEvent}.
		 */
		public void onEvent(MyLocationLoadingErrorEvent e) {
			MyLocationManager.getInstance()
			                 .init();
		}


		/**
		 * Handler for {@link RatingOnLocationsLoadingErrorEvent}.
		 *
		 * @param e Event {@link RatingOnLocationsLoadingErrorEvent}.
		 */
		public void onEvent(RatingOnLocationsLoadingErrorEvent e) {
			RatingManager.getInstance()
			             .init();
		}


		/**
		 * Handler for {@link ListDetailShownEvent}.
		 *
		 * @param e Event {@link ListDetailShownEvent}.
		 */
		public void onEvent(ListDetailShownEvent e) {
			mItemSelected = true;
		}


		/**
		 * Handler for {@link ListDetailClosedEvent}.
		 *
		 * @param e Event {@link ListDetailClosedEvent}.
		 */
		public void onEvent(ListDetailClosedEvent e) {
			mItemSelected = false;
		}

		//------------------------------------------------


		protected void setDrawerLayout(@NonNull DrawerLayout drawerLayout) {
			mDrawerLayout = drawerLayout;
		}

		protected void setNavigationView(@NonNull NavigationView navigationView) {
			mNavigationView = navigationView;
		}

		public void setActivityWeakReference(@NonNull Activity activity) {
			mActivityWeakReference = new WeakReference<Activity>(activity);
		}
	}
}
