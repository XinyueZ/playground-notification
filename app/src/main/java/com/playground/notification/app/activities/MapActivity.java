package com.playground.notification.app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;

import com.chopping.application.LL;
import com.chopping.utils.Utils;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.playground.notification.R;
import com.playground.notification.api.Api;
import com.playground.notification.api.ApiNotInitializedException;
import com.playground.notification.app.App;
import com.playground.notification.app.SearchSuggestionProvider;
import com.playground.notification.app.fragments.AboutDialogFragment;
import com.playground.notification.app.fragments.MyLocationFragment;
import com.playground.notification.app.fragments.PlaygroundDetailFragment;
import com.playground.notification.app.fragments.PlaygroundListFragment;
import com.playground.notification.bus.OpenPlaygroundEvent;
import com.playground.notification.bus.PinSelectedEvent;
import com.playground.notification.bus.RefreshListEvent;
import com.playground.notification.bus.SelectedPinOpenEvent;
import com.playground.notification.databinding.MainBinding;
import com.playground.notification.ds.google.Geobound;
import com.playground.notification.ds.google.Geocode;
import com.playground.notification.ds.google.GeocodeList;
import com.playground.notification.ds.google.Geolocation;
import com.playground.notification.ds.google.Geometry;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.grounds.Playgrounds;
import com.playground.notification.ds.grounds.Request;
import com.playground.notification.geofence.GeofenceManagerService;
import com.playground.notification.map.PlaygroundClusterManager;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.MyLocationManager;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.utils.Prefs;
import com.readystatesoftware.viewbadger.BadgeView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;
import static pub.devrel.easypermissions.AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE;


public final class MapActivity extends AppActivity implements LocationListener,
                                                              EasyPermissions.PermissionCallbacks {
	public static final String EXTRAS_GROUND = MapActivity.class.getName() + ".EXTRAS.ground";

	private static final int REQ = 0x98;

	/**
	 * Main layout for this component.
	 */
	private static final int LAYOUT = R.layout.activity_map;
	/**
	 * View's menu.
	 */
	private static final int MENU = R.menu.menu_main;

	private GoogleMap mMap; // Might be null if Google Play services APK is not available.
	private SupportMapFragment mMapFragment;
	/**
	 * {@code true}: force to load markers, ignore the touch&move effect of map. Default is {@code true}, because as initializing the map, the markers
	 * should be loaded.
	 */
	private volatile boolean mForcedToLoad = true;
	/**
	 * Use navigation-drawer for this fork.
	 */
	private ActionBarDrawerToggle mDrawerToggle;
	/**
	 * Data-binding.
	 */
	private MainBinding mBinding;
	/**
	 * Ask current location.
	 */
	private LocationRequest mLocationRequest;
	/**
	 * Connect to google-api.
	 */
	private GoogleApiClient mGoogleApiClient;
	/**
	 * {@code true} if the map is forground.
	 */
	private boolean mVisible;
	/**
	 * Suggestion list while tipping.
	 */
	protected SearchRecentSuggestions mSuggestions;
	/**
	 * The search.
	 */
	private SearchView mSearchView;
	/**
	 * Search menu.
	 */
	private MenuItem mSearchMenu;
	/**
	 * Keyword that will be searched.
	 */
	private String mKeyword = "";
	/**
	 * The interstitial ad.
	 */
	private InterstitialAd mInterstitialAd;
	/**
	 * Current available {@link Playground}s you can see.
	 */
	private @Nullable List<Playground> mAvailablePlaygroundList;
	/**
	 * Manager for all "pin"s on map.
	 */
	private PlaygroundClusterManager mPlaygroundClusterManager;
	/**
	 * The  {@link MenuItem} that can open list of {@link Playground}s., on itself shows count of current count of {@link Playground}s.
	 */
	private @Nullable MenuItem menuLocationList;
	/**
	 * {@link #mBadgeView} shows count of current {@link Playground}s.
	 */
	private @Nullable BadgeView mBadgeView;
	/**
	 * For large-screen, we show all list of {@link Playground}s over map directly.
	 */
	private @Nullable PlaygroundListFragment mPlaygroundListFragment;

	/**
	 * {@link #mShouldIgnoreLoadingFeeds} flags {@code true} when feeds should not load feeds.
	 */
	private boolean mShouldIgnoreLoadingFeeds;

	/**
	 * Mark for selection on a pin.
	 */
	private PinSelectedEvent mPinSelectedEvent;

	//------------------------------------------------
	//Subscribes, event-handlers
	//------------------------------------------------

	/**
	 * Handler for {@link PinSelectedEvent}.
	 *
	 * @param e Event {@link PinSelectedEvent}.
	 */
	@SuppressWarnings("unused")
	public void onEvent(PinSelectedEvent e) {
		mPinSelectedEvent = e;
	}

	/**
	 * Handler for {@link OpenPlaygroundEvent}.
	 *
	 * @param e Event {@link OpenPlaygroundEvent}.
	 */
	public void onEvent(OpenPlaygroundEvent e) {
		if (getResources().getBoolean(R.bool.is_small_screen)) {
			Location location = App.Instance.getCurrentLocation();
			LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
			PlaygroundDetailFragment.newInstance(this, currentLatLng.latitude, currentLatLng.longitude, e.getPlayground())
			                        .show(getSupportFragmentManager(), null);
			return;
		}

		MapActivity.showInstance(this, e.getPlayground());
	}


	/**
	 * Handler for {@link RefreshListEvent}.
	 *
	 * @param e Event {@link RefreshListEvent}.
	 */
	public void onEvent(RefreshListEvent e) {
		refreshPlaygroundList(e.getList());
	}
	//------------------------------------------------

	/**
	 * Show single instance of {@link MapActivity}
	 *
	 * @param cxt {@link Activity}.
	 */
	public static void showInstance(Activity cxt, Playground ground) {
		Intent intent = new Intent(cxt, MapActivity.class);
		intent.putExtra(EXTRAS_GROUND, ground);
		intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
		ActivityCompat.startActivity(cxt, intent, null);
	}

	/**
	 * Show single instance of {@link MapActivity}
	 *
	 * @param cxt {@link Activity}.
	 */
	public static void showInstance(Activity cxt) {
		Intent intent = new Intent(cxt, MapActivity.class);
		intent.setFlags(FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
		ActivityCompat.startActivity(cxt, intent, null);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case DEFAULT_SETTINGS_REQ_CODE:
				permissionsDeniedOpenSetting();
				break;
			case ConnectGoogleActivity.REQ:
				if (resultCode == RESULT_OK) {
				} else {
					ActivityCompat.finishAffinity(this);
				}
				break;
			case REQ:
				switch (resultCode) {
					case Activity.RESULT_OK:
						break;
					case Activity.RESULT_CANCELED:
						exitAppDialog();
						break;
				}
			case SettingsActivity.REQ:
				askWeatherBoard(App.Instance.getCurrentLocation());
				break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapsInitializer.initialize(App.Instance);
		Prefs.getInstance()
		     .setSelectedPlayground(null);

		//Init data-binding.
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		//Init application basic elements.
		setUpErrorHandling((ViewGroup) findViewById(R.id.error_content));

		initDrawer();
		initAddMyLocation();
		initPlaygroundsListIfNeeds();
		//For search and suggestions.
		mSuggestions = new SearchRecentSuggestions(this, getString(R.string.suggestion_auth), SearchSuggestionProvider.MODE);

		//Ads
		buildAds();
	}

	private void initPlaygroundsListIfNeeds() {
		boolean isSmall = App.Instance.getResources()
		                              .getBoolean(R.bool.is_small_screen);
		if (!isSmall) {
			mBinding.playGroundsListContainer.getLayoutParams().width = (int) App.Instance.getListItemWidth();
			mBinding.playGroundsListContainer.requestLayout();
			mBinding.geocodeLv.getLayoutParams().width = (int) App.Instance.getListItemWidth();
			((FrameLayout.LayoutParams) mBinding.geocodeLv.getLayoutParams()).leftMargin = (int) App.Instance.getListItemWidth();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		App.Instance.stopService(new Intent(App.Instance, GeofenceManagerService.class));
	}

	@Override
	protected void onStop() {
		super.onStop();
		App.Instance.startService(new Intent(App.Instance, GeofenceManagerService.class));
	}

	/**
	 * Build Admob.
	 */
	private void buildAds() {
		int curTime = Prefs.getInstance()
		                   .getAds();
		int adsTimes = 5;
		if (curTime % adsTimes == 0) {
			// Create an ad.
			mInterstitialAd = new InterstitialAd(this);
			mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ad_unit_id));
			// Create ad request.
			AdRequest adRequest = new AdRequest.Builder().build();
			// Begin loading your interstitial.
			mInterstitialAd.setAdListener(new AdListener() {
				@Override
				public void onAdLoaded() {
					super.onAdLoaded();
					displayInterstitial();
				}
			});
			mInterstitialAd.loadAd(adRequest);
		}
		curTime++;
		Prefs.getInstance()
		     .setAds(curTime);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (mMap == null) {
			return;
		}
		setIntent(intent);
		if (!TextUtils.equals(intent.getAction(), Intent.ACTION_SEARCH)) {
			if (intent.getSerializableExtra(EXTRAS_GROUND) != null) {
				Playground playground = (Playground) intent.getSerializableExtra(EXTRAS_GROUND);
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(playground.getPosition(), 18));
				Prefs.getInstance()
				     .setSelectedPlayground(playground.getPosition());
				deselectMenuItems();
				mMap.clear();
				mPlaygroundClusterManager = PlaygroundClusterManager.showAvailablePlaygrounds(MapActivity.this, mMap, mAvailablePlaygroundList);
				mShouldIgnoreLoadingFeeds = true;
			}
		} else {
			mKeyword = intent.getStringExtra(SearchManager.QUERY);
			if (!TextUtils.isEmpty(mKeyword) && com.playground.notification.utils.Utils.validateStr(App.Instance, mKeyword)) {
				mKeyword = mKeyword.trim();
				mSearchView.setQueryHint(Html.fromHtml("<font color = #ffffff>" + mKeyword + "</font>"));

				mKeyword = intent.getStringExtra(SearchManager.QUERY);
				mKeyword = mKeyword.trim();
				resetSearchView();

				//No save for suggestions.
				mSuggestions.saveRecentQuery(mKeyword, null);

				//Move map to searched location.
				doSearch(mKeyword);
			}
		}
	}

	/**
	 * Search a location.
	 */
	private void doSearch(String search) {
		try {
			mBinding.loadPinPb.setVisibility(View.VISIBLE);
			Api.getGeocode(search, App.Instance.getDistanceMatrixKey(), new Callback<GeocodeList>() {
				@Override
				public void success(GeocodeList geocodeList, Response response) {
					List<Geocode> geocodes = geocodeList.getGeocodeList();
					if (geocodes != null && geocodes.size() > 0) {
						Geocode geocode = geocodes.get(0);
						movedToUpdatedLocation(geocode);
					}
					mBinding.loadPinPb.setVisibility(View.GONE);
				}

				@Override
				public void failure(RetrofitError error) {
					mBinding.loadPinPb.setVisibility(View.GONE);
				}
			});
		} catch (ApiNotInitializedException e) {
			//Ignore this request.
		}
	}


	/**
	 * Show a list of geocodes suggestions.
	 */
	private void completeGeocodeList(String address) {
		if (TextUtils.isEmpty(address)) {
			mBinding.geocodeLv.setVisibility(View.GONE);
		} else {
			mBinding.geocodeLv.setVisibility(View.VISIBLE);
			try {
				mBinding.loadPinPb.setVisibility(View.VISIBLE);
				Api.getGeocode(address, App.Instance.getDistanceMatrixKey(), new Callback<GeocodeList>() {
					@Override
					public void success(GeocodeList geocodeList, Response response) {
						final List<Geocode> geocodes = geocodeList.getGeocodeList();
						if (geocodes != null) {
							ArrayAdapter<Geocode> adapter = new ArrayAdapter<>(MapActivity.this, R.layout.search_item, geocodes);
							mBinding.geocodeLv.setAdapter(adapter);
							mBinding.geocodeLv.setOnItemClickListener(new OnItemClickListener() {
								@Override
								public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
									resetSearchView();
									Geocode geocode = geocodes.get(position);
									movedToUpdatedLocation(geocode);
									parent.setVisibility(View.GONE);
								}
							});
						}
						mBinding.loadPinPb.setVisibility(View.GONE);
					}

					@Override
					public void failure(RetrofitError error) {
						mBinding.loadPinPb.setVisibility(View.GONE);
					}
				});
			} catch (ApiNotInitializedException e) {
				//Ignore this request.
			}
		}
	}

	/**
	 * Define UI for add my-location.
	 */
	private void initAddMyLocation() {
		updateMyLocationCurrentPosition();
		mBinding.addPaneV.hide();
		mBinding.exitAddBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mBinding.currentBtn.setVisibility(View.GONE);
				mBinding.addBtn.show();
				mBinding.addPaneV.hide();
				if (mShowcaseMyLocationV != null) {
					closeShowcaseMyLocation();
				}
			}
		});
		mBinding.addBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mBinding.currentBtn.setVisibility(View.VISIBLE);
				mBinding.addBtn.hide();
				mBinding.addPaneV.show();
				showcaseAddMyLocation();
			}
		});
		mBinding.currentBtn.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Location location = App.Instance.getCurrentLocation();
				if (location == null) {
					mBinding.currentBtn.setVisibility(View.GONE);
					mBinding.addBtn.show();
					mBinding.addPaneV.hide();
					Snackbar.make(mBinding.drawerLayout, R.string.lbl_no_current_location, Snackbar.LENGTH_LONG)
					        .show();
					return true;
				}
				try {
					final LatLng center = mMap.getProjection()
					                          .getVisibleRegion().latLngBounds.getCenter();
					showDialogFragment(MyLocationFragment.newInstance(App.Instance, location.getLatitude(), location.getLongitude(), new Playground(center.latitude, center.longitude)), null);
				} catch (Exception e) {
					LL.e("Cannot get center point to create my-location. " + (!TextUtils.isEmpty(e.getMessage()) ?
					                                                          e.getMessage() :
					                                                          ""));
				}
				return true;
			}
		});
	}

	/**
	 * {@link View} for showcase my-location.
	 */
	private View mShowcaseMyLocationV = null;

	/**
	 * A showcase for adding my location.
	 */
	private void showcaseAddMyLocation() {
		Prefs prefs = Prefs.getInstance();
		if (!prefs.isShowcaseShown(Prefs.KEY_SHOWCASE_MY_LOCATION)) {
			mShowcaseMyLocationV = getLayoutInflater().inflate(R.layout.showcase_add_my_location, mBinding.drawerLayout, false);
			View cling = mShowcaseMyLocationV.findViewById(R.id.cling_iv);
			ViewHelper.setAlpha(cling, 0f);
			mBinding.drawerLayout.addView(mShowcaseMyLocationV);
			ViewPropertyAnimator animator = ViewPropertyAnimator.animate(cling);
			animator.alpha(1f)
			        .setDuration(1000)
			        .start();
			View close = mShowcaseMyLocationV.findViewById(R.id.close_btn);
			close.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					closeShowcaseMyLocation();
				}
			});
			prefs.setShowcase(Prefs.KEY_SHOWCASE_MY_LOCATION, true);
		}
	}

	private void updateMyLocationCurrentPosition() {
		Resources resources = getResources();
		boolean isSmall = resources.getBoolean(R.bool.is_small_screen);
		MarginLayoutParamsCompat.setMarginStart(((ViewGroup.MarginLayoutParams) mBinding.addPaneV.getLayoutParams()),
		                                        !isSmall ?
		                                        (int) App.Instance.getListItemWidth() + resources.getDimensionPixelSize(R.dimen.list_padding_left) :
		                                        0);
		MarginLayoutParamsCompat.setMarginStart(((ViewGroup.MarginLayoutParams) mBinding.currentBtn.getLayoutParams()),
		                                        !isSmall ?
		                                        (int) App.Instance.getListItemWidth() - resources.getDimensionPixelSize(R.dimen.list_padding_left) - resources.getDimensionPixelSize(R.dimen
				                                                                                                                                                                             .common_padding) * 5 :

		                                        0);
		mBinding.addPaneV.requestLayout();
	}

	@Override
	public void onBackPressed() {
		Prefs.getInstance()
		     .setCurrentSelectedMenuItem(MENU_ITEM_OTHERS);
		if (shouldDoBackPressed()) {
			if (mShowcaseMyLocationV != null) {
				closeShowcaseMyLocation();
			} else {
				super.onBackPressed();
			}
		}
	}

	/**
	 * Close current showcase of my-location.
	 */
	private void closeShowcaseMyLocation() {
		View cling = mShowcaseMyLocationV.findViewById(R.id.cling_iv);
		ViewPropertyAnimator animator = ViewPropertyAnimator.animate(cling);
		animator.alpha(0f)
		        .setDuration(1000)
		        .setListener(new AnimatorListenerAdapter() {
			        @Override
			        public void onAnimationEnd(Animator animation) {
				        super.onAnimationEnd(animation);
				        mBinding.drawerLayout.removeView(mShowcaseMyLocationV);
				        mShowcaseMyLocationV = null;
			        }
		        })
		        .start();
	}

	/**
	 * Ready to use application.
	 */
	private void initUseApp() {
		LL.d("initUseApp");
		//User that have used this application and done clear(logout), should go back to login-page.
		Prefs prefs = Prefs.getInstance();
		if (prefs.isEULAOnceConfirmed() && TextUtils.isEmpty(prefs.getGoogleId())) {
			ConnectGoogleActivity.showInstance(this);
		} else if (prefs.isEULAOnceConfirmed() && !TextUtils.isEmpty(prefs.getGoogleId())) {
			onYouCanUseApp();
		}
	}

	/**
	 * Callback for available using of application.
	 */
	private void onYouCanUseApp() {
		initManagers();
		LL.d("onYouCanUseApp");
		requirePermissions();
		askWeatherBoard(App.Instance.getCurrentLocation());
	}

	private static final int RC_PERMISSIONS = 124;


	private void gotPermissions() {
		initGoogle();
		populateGrounds();
	}

	@SuppressLint("InlinedApi")
	private boolean hasPermissions() {
		return EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
	}


	@SuppressLint("InlinedApi")
	@AfterPermissionGranted(RC_PERMISSIONS)
	private void requirePermissions() {
		if (hasPermissions()) {
			gotPermissions();
		} else {
			// Ask for one permission
			EasyPermissions.requestPermissions(this,
			                                   getString(R.string.rationale_permissions_location),
			                                   RC_PERMISSIONS,
			                                   Manifest.permission.ACCESS_FINE_LOCATION,
			                                   Manifest.permission.ACCESS_COARSE_LOCATION);
		}
	}


	@Override
	public void onPermissionsGranted(int requestCode, List<String> perms) {
		gotPermissions();
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
	}


	@Override
	public void onPermissionsDenied(int requestCode, List<String> perms) {
		permissionsDeniedOpenSetting();
	}


	private void permissionsDeniedOpenSetting() {
		if (!hasPermissions()) {
			new AppSettingsDialog.Builder(this, getString(R.string.app_settings_dialog_rationale_ask_again)).setTitle(getString(R.string.app_settings_dialog_title_settings_dialog))
			                                                                                                .setPositiveButton(getString(R.string.app_settings_dialog_setting))
			                                                                                                .setNegativeButton(getString(R.string.app_settings_dialog_cancel),
			                                                                                                                   new DialogInterface.OnClickListener() {
				                                                                                                                   @Override
				                                                                                                                   public void onClick(DialogInterface dialogInterface, int i) {
					                                                                                                                   ActivityCompat.finishAffinity(MapActivity.this);
				                                                                                                                   }
			                                                                                                                   })
			                                                                                                .build()
			                                                                                                .show();
		}
	}


	/**
	 * Get weather status.
	 */
	private void askWeatherBoard(@Nullable Location l) {
		if (l == null) {
			return;
		}
		mBinding.weatherLayout.setWeather(l);
	}


	/**
	 * Initialize all map infrastructures, location request etc.
	 */
	private void initGoogle() {
		// Try to obtain the map from the SupportMapFragment.
		mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mMapFragment.getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(GoogleMap googleMap) {
				mMap = googleMap;
				setUpMap();
				mapSettings();

				//Location request.
				if (mLocationRequest == null) {
					mLocationRequest = LocationRequest.create();
					mLocationRequest.setInterval(AlarmManager.INTERVAL_HALF_HOUR);
					mLocationRequest.setFastestInterval(AlarmManager.INTERVAL_FIFTEEN_MINUTES);
					int ty = 0;
					switch (Prefs.getInstance()
					             .getBatteryLifeType()) {
						case "0":
							ty = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
							break;
						case "1":
							ty = LocationRequest.PRIORITY_HIGH_ACCURACY;
							break;
						case "2":
							ty = LocationRequest.PRIORITY_LOW_POWER;
							break;
						case "3":
							ty = LocationRequest.PRIORITY_NO_POWER;
							break;
					}
					mLocationRequest.setPriority(ty);
				}

				if (mGoogleApiClient == null) {
					mGoogleApiClient = new GoogleApiClient.Builder(App.Instance).addApi(LocationServices.API)
					                                                            .addConnectionCallbacks(new ConnectionCallbacks() {
						                                                            @Override
						                                                            public void onConnected(Bundle bundle) {
							                                                            if (ActivityCompat.checkSelfPermission(App.Instance,
							                                                                                                   ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat
									                                                            .checkSelfPermission(
									                                                            App.Instance,
									                                                            ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
								                                                            return;
							                                                            }
							                                                            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
							                                                            App.Instance.setCurrentLocation(location);
							                                                            startLocationUpdate();
						                                                            }

						                                                            @Override
						                                                            public void onConnectionSuspended(int i) {
							                                                            Utils.showShortToast(App.Instance, "onConnectionSuspended");

						                                                            }
					                                                            })
					                                                            .addOnConnectionFailedListener(new OnConnectionFailedListener() {
						                                                            @Override
						                                                            public void onConnectionFailed(ConnectionResult connectionResult) {
							                                                            Utils.showShortToast(App.Instance, "onConnectionFailed: " + connectionResult.getErrorCode());
						                                                            }
					                                                            })
					                                                            .build();

					mGoogleApiClient.connect();
				}

				//Setting turn/off location service of system.
				LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
				builder.setAlwaysShow(true);
				builder.setNeedBle(true);
				PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
				result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
					@Override
					public void onResult(LocationSettingsResult result) {
						final Status status = result.getStatus();
						//				final LocationSettingsStates states = result.getLocationSettingsStates();
						switch (status.getStatusCode()) {
							case LocationSettingsStatusCodes.SUCCESS:
								break;
							case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
								try {
									status.startResolutionForResult(MapActivity.this, REQ);
								} catch (SendIntentException e) {
									exitAppDialog();
								}
								break;
							case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
								exitAppDialog();
								break;
						}
					}
				});
			}
		});
	}

	/**
	 * Locating begin.
	 */
	private void startLocationUpdate() {
		if (ActivityCompat.checkSelfPermission(App.Instance, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(App.Instance,
		                                                                                                                                       ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
			return;
		}
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
		}
	}


	@Override
	public void onLocationChanged(Location location) {
		App.Instance.setCurrentLocation(location);
		LL.d("method: onLocationChanged -> mCurrentLocation changed");
		movedToUpdatedLocation(location);
		askWeatherBoard(location);
	}

	/**
	 * Stop locating.
	 */
	protected void stopLocationUpdates() {
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
		}
	}

	@Override
	protected void onDestroy() {
		Prefs.getInstance()
		     .setSelectedPlayground(null);
		stopLocationUpdates();
		if (mMap != null) {
			mMap.clear();
			mMap = null;
		}
		if (mPlaygroundClusterManager != null) {
			mPlaygroundClusterManager.clearItems();
			mPlaygroundClusterManager = null;
		}
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}

		if (mDrawerToggle != null) {
			mBinding.drawerLayout.removeDrawerListener(mDrawerToggle);
		}
		super.onDestroy();
	}

	private AlertDialog mExitAppDlg;

	/**
	 * Force to exit application for no location-service.
	 */
	private void exitAppDialog() {
		mExitAppDlg = new AlertDialog.Builder(MapActivity.this).setCancelable(false)
		                                                       .setTitle(R.string.application_name)
		                                                       .setMessage(R.string.lbl_no_location_service)
		                                                       .setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
			                                                       public void onClick(DialogInterface dialog, int whichButton) {
				                                                       ActivityCompat.finishAfterTransition(MapActivity.this);
			                                                       }
		                                                       })
		                                                       .create();
		mExitAppDlg.show();
	}


	/**
	 * Initialize the navigation drawer.
	 */
	private void initDrawer() {
		setSupportActionBar(mBinding.toolbar);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		mDrawerToggle = new ActionBarDrawerToggle(this, mBinding.drawerLayout, R.string.application_name, R.string.app_name) {
			@Override
			public void onDrawerOpened(View drawerView) {
				super.onDrawerOpened(drawerView);
				com.playground.notification.utils.Utils.updateDrawerMenuItem(mBinding.navView, R.id.action_favorite, R.string.action_favorite, FavoriteManager.getInstance());
				com.playground.notification.utils.Utils.updateDrawerMenuItem(mBinding.navView, R.id.action_near_ring, R.string.action_near_ring, NearRingManager.getInstance());
				com.playground.notification.utils.Utils.updateDrawerMenuItem(mBinding.navView, R.id.action_my_location_list, R.string.action_my_location_list, MyLocationManager.getInstance());
			}
		};
		mBinding.drawerLayout.addDrawerListener(mDrawerToggle);


	}


	@Override
	protected void onResume() {
		if (mCfgLoadDlg != null && mCfgLoadDlg.isShowing()) {
			mCfgLoadDlg.dismiss();
		}
		mCfgLoadDlg = ProgressDialog.show(this, getString(R.string.application_name), getString(R.string.lbl_load_cfg));

		super.onResume();

		if (mDrawerToggle != null) {
			mDrawerToggle.syncState();
		}


		mVisible = true;
	}

	@Override
	protected void onPause() {
		mVisible = false;
		super.onPause();
	}


	/**
	 * This is where we can add markers or lines, add listeners or move the camera. In this case, we just add a marker near Africa.
	 * <p/>
	 * This should only be called once and when we are sure that {@link #mMap} is not null.
	 */
	private void setUpMap() {
		if (ActivityCompat.checkSelfPermission(App.Instance, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(App.Instance,
		                                                                                                                                       ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
			mMap.setMyLocationEnabled(true);
		}
		mMap.setIndoorEnabled(true);
		mMap.setBuildingsEnabled(false);

		boolean isSmall = getResources().getBoolean(R.bool.is_small_screen);
		mMap.setPadding(!isSmall ?
		                (int) App.Instance.getListItemWidth() + getResources().getDimensionPixelSize(R.dimen.list_padding_left) :
		                0, getAppBarHeight(), 0, 0);
		mMap.setOnMyLocationButtonClickListener(new OnMyLocationButtonClickListener() {
			@Override
			public boolean onMyLocationButtonClick() {
				mForcedToLoad = true;
				startLocationUpdate();
				return true;
			}
		});
		mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
			@Override
			public void onCameraIdle() {
				if (mPinSelectedEvent != null) {
					LL.d("Ignore loading new feeds for pin");
					EventBus.getDefault()
					        .post(new SelectedPinOpenEvent(mPinSelectedEvent.getPlayground()));
					mPinSelectedEvent = null;
					return;
				}

				if (mShouldIgnoreLoadingFeeds) {
					LL.d("Ignore loading new feeds.");
					mShouldIgnoreLoadingFeeds = false;
					return;
				}
				mForcedToLoad = true;
				populateGrounds();
			}
		});

	}

	/**
	 * Extra settings on map.
	 */
	private void mapSettings() {
		Prefs prefs = Prefs.getInstance();
		mMap.setTrafficEnabled(prefs.isTrafficShowing());
		mMap.setMapType(prefs.getMapType()
		                     .equals("0") ?
		                GoogleMap.MAP_TYPE_NORMAL :
		                GoogleMap.MAP_TYPE_SATELLITE);
	}


	/**
	 * Draw grounds on map.
	 */
	private void populateGrounds() {
		if (mForcedToLoad && mMap != null) {
			mForcedToLoad = false;
			mBinding.loadPinPb.setVisibility(View.VISIBLE);

			LatLngBounds bounds = mMap.getProjection()
			                          .getVisibleRegion().latLngBounds;
			List<String> filter = new ArrayList<>();
			filter.add("playground");
			Request request = new Request();
			request.setTimestamps(System.currentTimeMillis());
			request.setWidth(App.Instance.getScreenSize().Width);
			request.setHeight(App.Instance.getScreenSize().Height);
			request.setFilter(filter);
			request.setResult(new ArrayList<String>());
			request.setNorth(bounds.northeast.latitude);
			request.setEast(bounds.northeast.longitude);
			request.setSouth(bounds.southwest.latitude);
			request.setWest(bounds.southwest.longitude);

			try {
				Api.getPlaygrounds(Prefs.getInstance()
				                        .getApiSearch(), request, new Callback<Playgrounds>() {
					@Override
					public void success(Playgrounds playgrounds, Response response) {
						mBinding.loadPinPb.setVisibility(View.GONE);
						if (App.Instance.getCurrentLocation() == null) {
							Snackbar.make(mBinding.drawerLayout, R.string.lbl_no_current_location, Snackbar.LENGTH_LONG)
							        .show();
							return;
						}
						if (mMap != null) {
							mMap.clear();
							mAvailablePlaygroundList = playgrounds.getPlaygroundList() == null ?
							                           new ArrayList<Playground>() :
							                           playgrounds.getPlaygroundList();
							if (MyLocationManager.getInstance()
							                     .isInit()) {
								mAvailablePlaygroundList.addAll(MyLocationManager.getInstance()
								                                                 .getCachedList());
							}
							mPlaygroundClusterManager = PlaygroundClusterManager.showAvailablePlaygrounds(MapActivity.this, mMap, mAvailablePlaygroundList);
							if (!getResources().getBoolean(R.bool.is_small_screen)) {
								deselectMenuItems();
								refreshPlaygroundList(mAvailablePlaygroundList);
							} else {
								updateBadgeTextOnAppBar();
							}
						}
					}

					@Override
					public void failure(RetrofitError error) {
						mBinding.loadPinPb.setVisibility(View.GONE);
					}
				});
			} catch (ApiNotInitializedException e) {
				//Ignore this request.
				mBinding.loadPinPb.setVisibility(View.GONE);
			}
		}
	}

	/**
	 * {@link #refreshPlaygroundList(List)} Only works for large screen like tablet to get list of {@link Playground}s.
	 */
	private void refreshPlaygroundList(List<? extends Playground> list) {
		if (mPlaygroundListFragment == null) {
			mBinding.playGroundsListContainer.setVisibility(View.VISIBLE);
			mPlaygroundListFragment = (PlaygroundListFragment) (getSupportFragmentManager().findFragmentById(R.id.play_grounds_list));
			mMap.setOnCameraMoveStartedListener(mPlaygroundListFragment);
		}
		if (mPlaygroundListFragment != null) {
			mPlaygroundListFragment.refresh(list);
		}
	}


	/**
	 * Refresh count of playground on app-bar on menu.
	 */
	private void updateBadgeTextOnAppBar() {
		if (menuLocationList == null) {
			return;
		}
		if (mBadgeView == null) {
			return;
		}
		if (mAvailablePlaygroundList == null || mAvailablePlaygroundList.size() <= 0) {
			mBadgeView.hide(true);
			return;
		}
		mBadgeView.show(true);
		mBadgeView.setText(String.valueOf(mAvailablePlaygroundList.size()));
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(MENU, menu);

		//Search
		mSearchMenu = menu.findItem(R.id.action_search);
		MenuItemCompat.setOnActionExpandListener(mSearchMenu, new MenuItemCompat.OnActionExpandListener() {
			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				mKeyword = "";
				return true;
			}
		});
		mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenu);
		mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				if (TextUtils.isEmpty(newText)) {
					completeGeocodeList(null);
				} else {
					completeGeocodeList(newText);
				}
				return false;
			}

			@Override
			public boolean onQueryTextSubmit(String s) {
				InputMethodManager mgr = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
				resetSearchView();
				return false;
			}
		});
		mSearchView.setIconifiedByDefault(true);
		SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
		if (searchManager != null) {
			SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
			mSearchView.setSearchableInfo(info);
		}

		initBadgeMenuItemsOnAppBar(menu);
		return true;
	}

	/**
	 * Initialize the  {@link MenuItem} that can open list of {@link Playground}s., on itself shows count of current count of {@link Playground}s.
	 *
	 * @param menu The  {@link MenuItem} that can open list of {@link Playground}s., on itself shows count of current count of {@link Playground}s.
	 */
	private void initBadgeMenuItemsOnAppBar(Menu menu) {
		menuLocationList = menu.findItem(R.id.action_list_mode);
		if (menuLocationList == null) {
			return;
		}
		View menuLayoutV = MenuItemCompat.getActionView(menuLocationList);
		mBadgeView = new BadgeView(this, menuLayoutV.findViewById(R.id.badge_v));
		mBadgeView.setText(String.valueOf("0"));
		mBadgeView.setTextColor(Color.BLUE);
		mBadgeView.setBadgePosition(BadgeView.POSITION_BOTTOM_RIGHT);
		menuLayoutV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				PlaygroundListActivity.showInstance(MapActivity.this, mAvailablePlaygroundList);
			}
		});
	}


	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		//Share application.
		MenuItem menuAppShare = menu.findItem(R.id.action_share_app);
		android.support.v7.widget.ShareActionProvider provider = (android.support.v7.widget.ShareActionProvider) MenuItemCompat.getActionProvider(menuAppShare);
		String subject = getString(R.string.lbl_share_app_title);
		String text = getString(R.string.lbl_share_app_content,
		                        getString(R.string.application_name),
		                        Prefs.getInstance()
		                             .getAppDownloadInfo());
		provider.setShareIntent(Utils.getDefaultShareIntent(provider, subject, text));
		return super.onPrepareOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		switch (item.getItemId()) {
			case R.id.action_about:
				showDialogFragment(AboutDialogFragment.newInstance(this), null);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * An indicator dialog for loading app-config
	 */
	private ProgressDialog mCfgLoadDlg;

	@Override
	protected void onAppConfigLoaded() {
		super.onAppConfigLoaded();
		cfgFinished();
	}

	@Override
	protected void onAppConfigIgnored() {
		super.onAppConfigIgnored();
		cfgFinished();
	}

	private void cfgFinished() {
		LL.d("cfgFinished");
		if (mCfgLoadDlg != null && mCfgLoadDlg.isShowing()) {
			mCfgLoadDlg.dismiss();
		}
		if (mExitAppDlg != null && mExitAppDlg.isShowing()) {
			mExitAppDlg.dismiss();
		}
		Prefs prefs = Prefs.getInstance();
		if (!TextUtils.isEmpty(prefs.getApiHost())) {
			showAppList();
			Api.initialize(App.Instance, prefs.getApiHost(), prefs.getWeatherApiHost());
			initUseApp();
		} else {
			exitAppDialog();
		}
	}


	/**
	 * Update current position on the map.
	 *
	 * @param location Current location.
	 */
	private void movedToUpdatedLocation(Location location) {
		if (mMap != null && mVisible) {
			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 14);
			mMap.moveCamera(update);
			LL.d("method: movedToUpdatedLocation");
		}
	}


	/**
	 * Update current position on the map.
	 *
	 * @param geobound {@link Geobound}
	 */
	private void movedToUpdatedLocation(Geobound geobound) {
		if (mMap != null && mVisible) {
			LatLngBounds bounds = new LatLngBounds(new LatLng(geobound.getSouthwest()
			                                                          .getLatitude(),
			                                                  geobound.getSouthwest()
			                                                          .getLongitude()),
			                                       new LatLng(geobound.getNortheast()
			                                                          .getLatitude(),
			                                                  geobound.getNortheast()
			                                                          .getLongitude()));
			CameraUpdate update = CameraUpdateFactory.newLatLngBounds(bounds, 0);
			mMap.moveCamera(update);
			LL.d("method: movedToUpdatedLocation");
		}
	}

	/**
	 * Update current position on the map.
	 *
	 * @param geolocation {@link com.playground.notification.ds.google.Geolocation}
	 */
	private void movedToUpdatedLocation(Geolocation geolocation) {
		if (mMap != null && mVisible) {
			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new LatLng(geolocation.getLatitude(), geolocation.getLongitude()), 16);
			mMap.moveCamera(update);
			LL.d("method: movedToUpdatedLocation");
		}
	}

	/**
	 * Update current position on the map.
	 *
	 * @param geocode {@link Geocode}
	 */
	private void movedToUpdatedLocation(Geocode geocode) {
		if (geocode.getGeometry() != null) {
			Geometry geometry = geocode.getGeometry();
			Geobound geobound = geometry.getBound();
			Geolocation geolocation = geometry.getLocation();
			if (geobound != null) {
				movedToUpdatedLocation(geobound);
			} else {
				if (geolocation != null) {
					movedToUpdatedLocation(geolocation);
				}
			}
			if (geolocation != null) {
				Location location = new Location("mock");
				location.setLatitude(geolocation.getLatitude());
				location.setLongitude(geolocation.getLongitude());
				askWeatherBoard(location);
			}
		}
	}


	/**
	 * Reset the UI status of searchview.
	 */
	protected void resetSearchView() {
		if (mSearchView != null) {
			mSearchView.clearFocus();
		}
	}


	/**
	 * Invoke displayInterstitial() when you are ready to display an interstitial.
	 */
	public void displayInterstitial() {
		if (mInterstitialAd.isLoaded()) {
			mInterstitialAd.show();
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
	protected boolean shouldDoBackPressed() {
		boolean should = super.shouldDoBackPressed();
		final Prefs prefs = Prefs.getInstance();
		LatLng latLng = prefs.getSelectedPlayground();
		if (latLng != null) {
			prefs.setSelectedPlayground(null);
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
			should = false;
		}
		return should;
	}
}
