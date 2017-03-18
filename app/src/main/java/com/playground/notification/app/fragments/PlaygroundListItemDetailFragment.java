package com.playground.notification.app.fragments;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.content.res.AppCompatResources;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.chopping.application.LL;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.playground.notification.R;
import com.playground.notification.api.Api;
import com.playground.notification.api.ApiNotInitializedException;
import com.playground.notification.app.App;
import com.playground.notification.bus.BackPressedEvent;
import com.playground.notification.bus.OpenRouteEvent;
import com.playground.notification.bus.ShowLocationRatingEvent;
import com.playground.notification.databinding.PlaygroundListItemDetailBinding;
import com.playground.notification.ds.google.Matrix;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.sync.Rating;
import com.playground.notification.ds.sync.SyncPlayground;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.sync.RatingManager;
import com.playground.notification.utils.Prefs;
import com.playground.notification.utils.Utils;

import java.io.Serializable;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.playground.notification.sync.RatingManager.showPersonalRatingOnLocation;
import static com.playground.notification.sync.RatingManager.showRatingSummaryOnLocation;

/**
 * Show details of a playground, address, rating.
 *
 * @author Xinyue Zhao
 */
public final class PlaygroundListItemDetailFragment extends AppFragment implements RatingManager.RatingUI {
	/**
	 * Main layout for this component.
	 */
	private static final int LAYOUT = R.layout.fragment_playground_list_item_detail;
	/**
	 * Data-binding.
	 */
	private PlaygroundListItemDetailBinding mBinding;

	private final AppFragment.CommonUIDelegate mCommonUIDelegate = new AppFragment.CommonUIDelegate(this);


	/**
	 * New an instance of {@link PlaygroundListItemDetailFragment}.
	 *
	 * @param context    {@link Context}.
	 * @param fromLat    The latitude of "from" position to {@code playground}.
	 * @param fromLng    The longitude of "from" position to {@code playground}.
	 * @param playground {@link Playground}.
	 * @return An instance of {@link PlaygroundListItemDetailFragment}.
	 */
	public static PlaygroundListItemDetailFragment newInstance(Context context, double fromLat, double fromLng, Playground playground) {
		Bundle args = new Bundle();
		args.putDouble(EXTRAS_LAT, fromLat);
		args.putDouble(EXTRAS_LNG, fromLng);
		args.putSerializable(EXTRAS_GROUND, (Serializable) playground);
		return (PlaygroundListItemDetailFragment) PlaygroundListItemDetailFragment.instantiate(context, PlaygroundListItemDetailFragment.class.getName(), args);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mBinding.map.onSaveInstanceState(outState);
		mBinding.streetview.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {
		mBinding.map.onDestroy();
		mBinding.streetview.onDestroy();
		super.onDestroy();
	}

	@Override
	public void onStart() {
		mBinding.map.onStart();
		super.onStart();
	}

	@Override
	public void onResume() {
		mBinding.map.onResume();
		mBinding.streetview.onResume();
		super.onResume();
		EventBus.getDefault()
		        .register(mCommonUIDelegate);
		mCommonUIDelegate.updateWeatherView(mBinding.weatherLayout);
	}

	@Override
	public void onPause() {
		EventBus.getDefault()
		        .unregister(mCommonUIDelegate);
		mBinding.map.onPause();
		mBinding.streetview.onPause();
		super.onPause();
	}

	@Override
	public void onLowMemory() {
		mBinding.map.onLowMemory();
		mBinding.streetview.onLowMemory();
		super.onLowMemory();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mBinding = DataBindingUtil.inflate(inflater, LAYOUT, container, false);
		mBinding.map.onCreate(savedInstanceState);
		mBinding.streetview.onCreate(savedInstanceState);
		return mBinding.getRoot();
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initView();
	}


	private void initView() {
		mBinding.toolbar.setNavigationIcon(AppCompatResources.getDrawable(getContext(), R.drawable.ic_action_close));
		mBinding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				EventBus.getDefault()
				        .post(new BackPressedEvent());
			}
		});


		Bundle args = getArguments();
		final Playground playground = (Playground) args.getSerializable(EXTRAS_GROUND);
		if (playground != null && playground.getPosition() != null) {
			LL.d("Ground ID: " + playground.getId());

			final double lat = args.getDouble(EXTRAS_LAT);
			final double lng = args.getDouble(EXTRAS_LNG);

			Prefs prefs = Prefs.getInstance();

			if (!prefs.isShowcaseShown(Prefs.KEY_SHOWCASE_NEAR_RING)) {
				mBinding.showcaseVg.setVisibility(View.VISIBLE);
				mBinding.closeBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						ViewPropertyAnimator animator = ViewPropertyAnimator.animate(mBinding.showcaseVg);
						animator.alpha(0f)
						        .setDuration(1000)
						        .setListener(new AnimatorListenerAdapter() {
							        @Override
							        public void onAnimationEnd(Animator animation) {
								        super.onAnimationEnd(animation);
								        mBinding.showcaseVg.setVisibility(View.GONE);
							        }
						        })
						        .start();
					}
				});
				prefs.setShowcase(Prefs.KEY_SHOWCASE_NEAR_RING, true);
			}


			final String method;
			switch (prefs.getTransportationMethod()) {
				case "0":
					method = "driving";
					break;
				case "1":
					method = "walking";
					break;
				case "2":
					method = "bicycling";
					break;
				case "3":
					method = "transit";
					break;
				default:
					method = "walking";
					break;
			}
			String units = "metric";
			switch (prefs.getDistanceUnitsType()) {
				case "0":
					units = "metric";
					break;
				case "1":
					units = "imperial";
					break;

			}
			try {
				Api.getMatrix(lat + "," + lng,
				              playground.getLatitude() + "," + playground.getLongitude(),
				              Locale.getDefault()
				                    .getLanguage(),
				              method,
				              App.Instance.getDistanceMatrixKey(),
				              units,
				              new Callback<Matrix>() {
					              @Override
					              public void success(Matrix matrix, Response response) {
						              mBinding.setMatrix(matrix);
						              mBinding.setMode(method);
						              mBinding.setHandler(new EventHandler(lat, lng, playground, mBinding));
					              }

					              @Override
					              public void failure(RetrofitError error) {

					              }
				              });
			} catch (ApiNotInitializedException e) {
				EventBus.getDefault()
				        .post(new BackPressedEvent());
			}


			if (FavoriteManager.getInstance()
			                   .isCached(playground)) {
				mBinding.favIv.setImageResource(R.drawable.ic_favorite);
			}
			if (NearRingManager.getInstance()
			                   .isCached(playground)) {
				mBinding.ringIv.setImageResource(R.drawable.ic_geo_fence);
			}

			showPersonalRatingOnLocation(playground, this);
			showRatingSummaryOnLocation(playground, this);
			setupGoogleTools();
			mBinding.weatherLayout.setWeather(playground.getPosition());
		}
	}

	@Override
	public void setRating(Rating rate) {
		mBinding.setRating(rate);
	}

	@Override
	public void setRating(float rate) {
		mBinding.locationRb.setRating(rate);
	}


	private void setupGoogleTools() {
		mBinding.locationContainer.getLayoutParams().width = (int) App.Instance.getListItemWidth() * 2;
		mBinding.locationContainer.getLayoutParams().height = (int) App.Instance.getListItemHeight() * 2;
		mBinding.loadingImgPb.setVisibility(View.VISIBLE);
		mBinding.map.getMapAsync(mOnMapReadyCallback);
		mBinding.streetview.getStreetViewPanoramaAsync(mOnStreetViewPanoramaReadyCallback);
	}

	/**
	 * Streeview can show photo correctly or not.
	 */
	private final StreetViewPanorama.OnStreetViewPanoramaChangeListener mOnStreetViewPanoramaChangeListener = new StreetViewPanorama.OnStreetViewPanoramaChangeListener() {
		@Override
		public void onStreetViewPanoramaChange(StreetViewPanoramaLocation streetViewPanoramaLocation) {
			if (streetViewPanoramaLocation != null && streetViewPanoramaLocation.links != null) {
				mBinding.toolbar.inflateMenu(R.menu.menu_list_item_detail);
				mBinding.toolbar.getMenu()
				                .findItem(R.id.action_street_view)
				                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					                @Override
					                public boolean onMenuItemClick(MenuItem item) {
						                mCommonUIDelegate.openStreetView(mBinding.getMatrix());
						                return true;
					                }
				                });

				mBinding.streetviewButtonStub.inflate()
				                             .setOnClickListener(new OnClickListener() {
					                             @Override
					                             public void onClick(View v) {
						                             mCommonUIDelegate.openStreetView(mBinding.getMatrix());
					                             }
				                             });

				((ViewGroup.MarginLayoutParams)mBinding.weatherLayout.getLayoutParams()).topMargin = com.chopping.utils.Utils.getActionBarHeight(App.Instance);
			}
			mBinding.loadingImgPb.setVisibility(View.GONE);
		}
	};


	/**
	 * Streeview can be loaded successfully or not.
	 */
	private final OnStreetViewPanoramaReadyCallback mOnStreetViewPanoramaReadyCallback = new OnStreetViewPanoramaReadyCallback() {
		@Override
		public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
			streetViewPanorama.setOnStreetViewPanoramaChangeListener(mOnStreetViewPanoramaChangeListener);
			Playground playground = (Playground) getArguments().getSerializable(EXTRAS_GROUND);
			streetViewPanorama.setPosition(playground.getPosition());
		}
	};

	/**
	 * Map can be loaded successfully or not.
	 */
	private final OnMapReadyCallback mOnMapReadyCallback = new OnMapReadyCallback() {
		@Override
		public void onMapReady(GoogleMap googleMap) {
			mBinding.loadingImgPb.setVisibility(View.GONE);
			if (googleMap != null) {
				mCommonUIDelegate.onMapReady(googleMap);
			}
		}
	};


	/**
	 * Event-handler for all radio-buttons on UI.
	 */
	public static final class EventHandler {
		private final double mLat;
		private final double mLng;
		private final Playground mGround;
		private final PlaygroundListItemDetailBinding mBinding;

		public EventHandler(double fromLat, double fromLng, Playground playground, PlaygroundListItemDetailBinding binding) {
			mLat = fromLat;
			mLng = fromLng;
			mGround = playground;
			mBinding = binding;
		}

		public void onModeSelected(View view) {
			mBinding.setMode(view.getTag()
			                     .toString());
			mBinding.changingPb.setVisibility(View.VISIBLE);

			Prefs prefs = Prefs.getInstance();
			String units = "metric";
			switch (prefs.getDistanceUnitsType()) {
				case "0":
					break;
				case "1":
					break;

			}
			try {
				Api.getMatrix(mLat + "," + mLng,
				              mGround.getLatitude() + "," + mGround.getLongitude(),
				              Locale.getDefault()
				                    .getLanguage(),
				              mBinding.getMode(),
				              App.Instance.getDistanceMatrixKey(),
				              units,
				              new Callback<Matrix>() {
					              @Override
					              public void success(Matrix matrix, Response response) {
						              mBinding.setMatrix(matrix);
						              mBinding.setHandler(EventHandler.this);
						              mBinding.changingPb.setVisibility(View.GONE);
					              }

					              @Override
					              public void failure(RetrofitError error) {
						              mBinding.changingPb.setVisibility(View.GONE);
					              }
				              });
			} catch (ApiNotInitializedException e) {
				//Ignore this request.
				mBinding.changingPb.setVisibility(View.GONE);
			}
		}

		public void onRatingClicked(@SuppressWarnings("UnusedParameters") View view) {
			EventBus.getDefault()
			        .post(new ShowLocationRatingEvent(mGround, mBinding.getRating()));
		}

		public void onSaveFavClicked(@SuppressWarnings("UnusedParameters") View view) {
			FavoriteManager mgr = FavoriteManager.getInstance();
			SyncPlayground favFound = mgr.findInCache(mGround);
			if (favFound == null) {
				mgr.addFavorite(mGround, mBinding.favIv, mBinding.playgroundDetailVg);
			} else {
				mgr.removeFavorite(favFound, mBinding.favIv, mBinding.playgroundDetailVg);
			}
		}

		public void onSaveNearRingClicked(@SuppressWarnings("UnusedParameters") View view) {
			NearRingManager mgr = NearRingManager.getInstance();
			SyncPlayground ringFound = mgr.findInCache(mGround);
			if (ringFound == null) {
				mgr.addNearRing(mGround, mBinding.ringIv, mBinding.playgroundDetailVg);
			} else {
				mgr.removeNearRing(ringFound, mBinding.ringIv, mBinding.playgroundDetailVg);
			}
		}

		public void onGoClicked(@SuppressWarnings("UnusedParameters") View v) {
			EventBus.getDefault()
			        .post(new OpenRouteEvent());
		}


		public void onShareGround(@SuppressWarnings("UnusedParameters") View v) {
			final String url = Prefs.getInstance()
			                        .getGoogleMapSearchHost() + mGround.getLatitude() + "," + mGround.getLongitude();
			com.tinyurl4j.Api.getTinyUrl(url, new Callback<com.tinyurl4j.data.Response>() {
				@Override
				public void success(com.tinyurl4j.data.Response response, Response response2) {
					String subject = App.Instance.getString(R.string.lbl_share_ground_title);
					String content = App.Instance.getString(R.string.lbl_share_ground_content,
					                                        response.getResult(),
					                                        Prefs.getInstance()
					                                             .getAppDownloadInfo());
					mBinding.shareGroundBtn.getContext()
					                       .startActivity(Utils.getShareInformation(subject, content));
				}

				@Override
				public void failure(RetrofitError error) {
					String subject = App.Instance.getString(R.string.lbl_share_ground_title);
					String content = App.Instance.getString(R.string.lbl_share_ground_content,
					                                        url,
					                                        Prefs.getInstance()
					                                             .getAppDownloadInfo());
					mBinding.shareGroundBtn.getContext()
					                       .startActivity(Utils.getShareInformation(subject, content));
				}
			});
		}
	}
}
