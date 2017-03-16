package com.playground.notification.app.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.content.res.AppCompatResources;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.chopping.application.BasicPrefs;
import com.chopping.application.LL;
import com.chopping.fragments.BaseFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.playground.notification.R;
import com.playground.notification.api.Api;
import com.playground.notification.api.ApiNotInitializedException;
import com.playground.notification.app.App;
import com.playground.notification.app.activities.AppActivity;
import com.playground.notification.app.activities.MapActivity;
import com.playground.notification.bus.BackPressedEvent;
import com.playground.notification.bus.OpenRouteEvent;
import com.playground.notification.bus.PostOpenRouteEvent;
import com.playground.notification.bus.ShowLocationRatingEvent;
import com.playground.notification.bus.ShowStreetViewEvent;
import com.playground.notification.databinding.PlaygroundListItemDetailBinding;
import com.playground.notification.databinding.RatingDialogBinding;
import com.playground.notification.ds.google.Matrix;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.sync.Rating;
import com.playground.notification.ds.sync.SyncPlayground;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.sync.RatingManager;
import com.playground.notification.ui.RouteCalcClientPicker;
import com.playground.notification.utils.PlaygroundIdUtils;
import com.playground.notification.utils.Prefs;
import com.playground.notification.utils.Utils;

import java.io.Serializable;
import java.util.Locale;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.playground.notification.sync.RatingManager.showPersonalRatingOnLocation;
import static com.playground.notification.sync.RatingManager.showRatingSummaryOnLocation;
import static com.playground.notification.utils.Utils.setPlaygroundIcon;

/**
 * Show details of a playground, address, rating.
 *
 * @author Xinyue Zhao
 */
public final class PlaygroundListItemDetailFragment extends BaseFragment implements RatingManager.RatingUI {
	private static final String EXTRAS_GROUND = PlaygroundListItemDetailFragment.class.getName() + ".EXTRAS.playground";
	private static final String EXTRAS_LAT = PlaygroundListItemDetailFragment.class.getName() + ".EXTRAS.lat";
	private static final String EXTRAS_LNG = PlaygroundListItemDetailFragment.class.getName() + ".EXTRAS.lng";
	/**
	 * Main layout for this component.
	 */
	private static final int LAYOUT = R.layout.fragment_playground_list_item_detail;
	/**
	 * Data-binding.
	 */
	private PlaygroundListItemDetailBinding mBinding;


	//------------------------------------------------
	//Subscribes, event-handlers
	//------------------------------------------------

	/**
	 * Handler for {@link OpenRouteEvent}.
	 *
	 * @param e Event {@link OpenRouteEvent}.
	 */
	public void onEvent(OpenRouteEvent e) {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		final Bundle arguments = getArguments();
		NearRingManager mgr = NearRingManager.getInstance();
		SyncPlayground ringFound = mgr.findInCache((Playground) arguments.getSerializable(EXTRAS_GROUND));
		if (ringFound == null) {
			AddToNearRingFragment.newInstance(activity, arguments.getDouble(EXTRAS_LAT), arguments.getDouble(EXTRAS_LNG), ((Playground) arguments.getSerializable(EXTRAS_GROUND)))
			                     .show(getChildFragmentManager(), null);
		} else {
			EventBus.getDefault()
			        .post(new PostOpenRouteEvent(false));
		}
	}

	/**
	 * Handler for {@link PostOpenRouteEvent}.
	 *
	 * @param e Event {@link PostOpenRouteEvent}.
	 */
	public void onEvent(PostOpenRouteEvent e) {
		if (e.isFlag()) {
			NearRingManager.getInstance()
			               .addNearRing(((Playground) getArguments().getSerializable(EXTRAS_GROUND)), mBinding.ringIv, mBinding.playgroundDetailVg);
		}

		openRoute();
	}

	/**
	 * Handler for {@link ShowLocationRatingEvent}.
	 *
	 * @param e Event {@link ShowLocationRatingEvent}.
	 */
	public void onEvent(ShowLocationRatingEvent e) {
		if (!getUserVisibleHint()) {
			return;
		}
		AppActivity activity = (AppActivity) getActivity();
		if (activity != null) {
			activity.showDialogFragment(RatingDialogFragment.newInstance(activity, e.getPlayground(), mBinding.getRating()), "rating");
		}
	}
	//------------------------------------------------

	//A dialog to update current rating status of a ground for you.
	public static final class RatingDialogFragment extends DialogFragment {
		/**
		 * Data-binding.
		 */
		private RatingDialogBinding mBinding;

		public static RatingDialogFragment newInstance(Context cxt, Playground playground, Rating rating) {
			Bundle args = new Bundle();
			args.putSerializable("rating", (Serializable) rating);
			args.putSerializable("ground", (Serializable) playground);
			return (RatingDialogFragment) RatingDialogFragment.instantiate(cxt, RatingDialogFragment.class.getName(), args);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return inflater.inflate(R.layout.fragment_rating, container, false);
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			mBinding = DataBindingUtil.bind(view.findViewById(R.id.rating_dialog_vg));
			getDialog().setTitle(R.string.lbl_rating);
			Rating rating = ((Rating) getArguments().getSerializable("rating"));
			if (rating != null) {
				mBinding.setRating(rating);
			}
			view.findViewById(R.id.close_iv)
			    .setOnClickListener(new OnClickListener() {
				    @Override
				    public void onClick(View v) {
					    dismiss();
					    Playground playground = (Playground) getArguments().getSerializable("ground");
					    playground.setId(PlaygroundIdUtils.getId(playground));
					    Rating rating = ((Rating) getArguments().getSerializable("rating"));
					    if (rating == null) {
						    Rating newRating = new Rating(Prefs.getInstance()
						                                       .getGoogleId(), playground);
						    newRating.setValue(mBinding.locationRb.getRating());
						    newRating.save(new SaveListener<String>() {
							    @Override
							    public void done(String s, BmobException exp) {
								    if (exp != null) {
									    LL.d("newRating failed");
									    return;
								    }
								    LL.d("newRating success");
							    }
						    });
					    } else {
						    Rating updateRating = new Rating(Prefs.getInstance()
						                                          .getGoogleId(), playground);
						    updateRating.setValue(mBinding.locationRb.getRating());
						    updateRating.update(rating.getObjectId(), new UpdateListener() {
							    @Override
							    public void done(BmobException exp) {
								    if (exp != null) {
									    LL.d("updateRating failed");
									    return;
								    }
								    LL.d("updateRating success");
							    }
						    });
					    }
				    }
			    });
		}
	}


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
	}

	@Override
	public void onPause() {
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
		if (playground != null) {
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

	private void openRoute() {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}

		Bundle arguments = getArguments();
		RouteCalcClientPicker.show(activity,
		                           Utils.getMapWeb(new LatLng(arguments.getDouble(EXTRAS_LAT), arguments.getDouble(EXTRAS_LNG)),
		                                           ((Playground) arguments.getSerializable(EXTRAS_GROUND)).getPosition()));
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
						                openStreetView();
						                return true;
					                }
				                });

				mBinding.streetviewButtonStub.inflate()
				                             .setOnClickListener(new OnClickListener() {
					                             @Override
					                             public void onClick(View v) {
						                             openStreetView();
					                             }
				                             });
			}
			mBinding.loadingImgPb.setVisibility(View.GONE);
		}
	};

	private void openStreetView() {
		Matrix matrix = mBinding.getMatrix();
		Playground playground = (Playground) getArguments().getSerializable(EXTRAS_GROUND);
		if (playground.getPosition() != null && matrix != null && matrix.getDestination() != null && matrix.getDestination()
		                                                                                                   .size() > 0 && matrix.getDestination()
		                                                                                                                        .get(0) != null) {
			EventBus.getDefault()
			        .post(new ShowStreetViewEvent(matrix.getDestination()
			                                            .get(0), playground.getPosition()));
		}
	}


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
			Playground playground = (Playground) getArguments().getSerializable(EXTRAS_GROUND);
			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(playground.getPosition(), 16));
			MarkerOptions markerOptions = new MarkerOptions().position(playground.getPosition());
			setPlaygroundIcon(App.Instance, playground, markerOptions);
			googleMap.addMarker(markerOptions);
			googleMap.setOnMapClickListener(mOnMapClickListener);
			mBinding.loadingImgPb.setVisibility(View.GONE);
		}
	};


	/**
	 * Click on map.
	 */
	private final GoogleMap.OnMapClickListener mOnMapClickListener = new GoogleMap.OnMapClickListener() {
		@Override
		public void onMapClick(LatLng latLng) {
			if (getResources().getBoolean(R.bool.is_small_screen)) {
				Activity activity = getActivity();
				if (activity == null) {
					return;
				}
				Playground playground = (Playground) getArguments().getSerializable(EXTRAS_GROUND);
				MapActivity.showInstance(activity, playground);
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
						              mBinding.setHandler(new EventHandler(mLat, mLng, mGround, mBinding));
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
			        .post(new ShowLocationRatingEvent(mGround));
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


	public static final class AddToNearRingFragment extends AppCompatDialogFragment {
		private static final String EXTRAS_GROUND = PlaygroundListItemDetailFragment.class.getName() + ".EXTRAS.playground";
		private static final String EXTRAS_LAT = PlaygroundListItemDetailFragment.class.getName() + ".EXTRAS.lat";
		private static final String EXTRAS_LNG = PlaygroundListItemDetailFragment.class.getName() + ".EXTRAS.lng";

		public static AddToNearRingFragment newInstance(Context cxt, double fromLat, double fromLng, Playground playground) {
			Bundle args = new Bundle();
			args.putDouble(EXTRAS_LAT, fromLat);
			args.putDouble(EXTRAS_LNG, fromLng);
			args.putSerializable(EXTRAS_GROUND, (Serializable) playground);
			return (AddToNearRingFragment) AddToNearRingFragment.instantiate(cxt, AddToNearRingFragment.class.getName(), args);
		}


		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Use the Builder class for convenient dialog construction
			android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.add_to_near_ring_before_route_title)
			       .setMessage(R.string.add_to_near_ring_before_route)
			       .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				       public void onClick(DialogInterface dialog, int id) {
					       EventBus.getDefault()
					               .post(new PostOpenRouteEvent(true));
				       }
			       })
			       .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
				       public void onClick(DialogInterface dialog, int id) {
					       EventBus.getDefault()
					               .post(new PostOpenRouteEvent(false));
				       }
			       });
			return builder.create();
		}
	}

	@Override
	protected BasicPrefs getPrefs() {
		return Prefs.getInstance();
	}
}
