package com.playground.notification.app.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.chopping.application.BasicPrefs;
import com.chopping.fragments.BaseFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.playground.notification.R;
import com.playground.notification.api.Api;
import com.playground.notification.api.ApiNotInitializedException;
import com.playground.notification.app.App;
import com.playground.notification.app.activities.AppActivity;
import com.playground.notification.app.activities.MapActivity;
import com.playground.notification.bus.OpenRouteEvent;
import com.playground.notification.bus.PostOpenRouteEvent;
import com.playground.notification.bus.ShowLocationRatingEvent;
import com.playground.notification.bus.ShowStreetViewEvent;
import com.playground.notification.ds.google.Matrix;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.sync.SyncPlayground;
import com.playground.notification.ds.weather.Weather;
import com.playground.notification.ds.weather.WeatherDetail;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.utils.Prefs;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.playground.notification.utils.Utils.openRoute;
import static com.playground.notification.utils.Utils.setPlaygroundIcon;

abstract class AppFragment extends BaseFragment {
	static final String EXTRAS_GROUND = AppFragment.class.getName() + ".EXTRAS.playground";
	static final String EXTRAS_LAT = AppFragment.class.getName() + ".EXTRAS.lat";
	static final String EXTRAS_LNG = AppFragment.class.getName() + ".EXTRAS.lng";

	/**
	 * App that use this Chopping should know the preference-storage.
	 *
	 * @return An instance of {@link com.chopping.application.BasicPrefs}.
	 */
	@Override
	protected BasicPrefs getPrefs() {
		return Prefs.getInstance();
	}


	protected static final class CommonUIDelegate {
		private @Nullable WeakReference<Fragment> mFragmentWeakReference;


		CommonUIDelegate(@NonNull Fragment fragment) {
			mFragmentWeakReference = new WeakReference<>(fragment);
		}


		//------------------------------------------------
		//Subscribes, event-handlers
		//------------------------------------------------

		/**
		 * Handler for {@link OpenRouteEvent}.
		 *
		 * @param e Event {@link OpenRouteEvent}.
		 */
		public void onEvent(OpenRouteEvent e) {
			if (mFragmentWeakReference.get() == null) {
				return;
			}
			Fragment fragment = mFragmentWeakReference.get();
			FragmentActivity activity = fragment.getActivity();
			if (activity == null) {
				return;
			}
			final Bundle arguments = fragment.getArguments();
			NearRingManager mgr = NearRingManager.getInstance();
			SyncPlayground ringFound = mgr.findInCache((Playground) arguments.getSerializable(EXTRAS_GROUND));
			if (ringFound == null) {
				AddToNearRingFragment.newInstance(activity, arguments.getDouble(EXTRAS_LAT), arguments.getDouble(EXTRAS_LNG), ((Playground) arguments.getSerializable(EXTRAS_GROUND)))
				                     .show(fragment.getChildFragmentManager(), null);
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
			if (mFragmentWeakReference.get() == null) {
				return;
			}
			Fragment fragment = mFragmentWeakReference.get();
			ImageView mRingIv;
			View mDetailVg;

			if (fragment instanceof DialogFragment) {
				mRingIv = (ImageView) ((DialogFragment) fragment).getDialog()
				                                                 .findViewById(R.id.ring_iv);
				mDetailVg = ((DialogFragment) fragment).getDialog()
				                                       .findViewById(R.id.playground_detail_vg);
			} else {
				View view = fragment.getView();
				if (view == null) {
					return;
				}
				mRingIv = (ImageView) view.findViewById(R.id.ring_iv);
				mDetailVg = view.findViewById(R.id.playground_detail_vg);
			}
			if (e.isAddToNearRing()) {
				NearRingManager.getInstance()
				               .addNearRing((Playground) fragment.getArguments()
				                                                 .getSerializable(EXTRAS_GROUND), mRingIv, mDetailVg);
			}

			FragmentActivity activity = fragment.getActivity();
			if (activity == null) {
				return;
			}

			Bundle arguments = fragment.getArguments();
			openRoute(activity, new LatLng(arguments.getDouble(EXTRAS_LAT), arguments.getDouble(EXTRAS_LNG)), ((Playground) arguments.getSerializable(EXTRAS_GROUND)).getPosition());
		}

		/**
		 * Handler for {@link com.playground.notification.bus.ShowLocationRatingEvent}.
		 *
		 * @param e Event {@link com.playground.notification.bus.ShowLocationRatingEvent}.
		 */
		public void onEvent(ShowLocationRatingEvent e) {
			if (mFragmentWeakReference.get() == null) {
				return;
			}
			Fragment fragment = mFragmentWeakReference.get();
			if (!fragment.getUserVisibleHint()) {
				return;
			}
			AppActivity activity = (AppActivity) fragment.getActivity();
			if (activity != null) {
				activity.showDialogFragment(RatingDialogFragment.newInstance(activity, e.getPlayground(), e.getRating()), "rating");
			}
		}

		//------------------------------------------------


		void openStreetView(@Nullable Matrix matrix) {
			if (mFragmentWeakReference.get() == null) {
				return;
			}
			Fragment fragment = mFragmentWeakReference.get();
			Playground playground = (Playground) fragment.getArguments()
			                                             .getSerializable(EXTRAS_GROUND);
			if (playground.getPosition() != null && matrix != null && matrix.getDestination() != null && matrix.getDestination()
			                                                                                                   .size() > 0 && matrix.getDestination()
			                                                                                                                        .get(0) != null) {
				EventBus.getDefault()
				        .post(new ShowStreetViewEvent(matrix.getDestination()
				                                            .get(0), playground.getPosition()));
			}
		}

		final GoogleMap.OnMapClickListener mOnMapClickListener = new GoogleMap.OnMapClickListener() {
			@Override
			public void onMapClick(LatLng latLng) {
				if (mFragmentWeakReference.get() == null) {
					return;
				}
				Fragment fragment = mFragmentWeakReference.get();
				if (App.Instance.getResources()
				                .getBoolean(R.bool.is_small_screen)) {
					Activity activity = fragment.getActivity();
					if (activity == null) {
						return;
					}
					Playground playground = (Playground) fragment.getArguments()
					                                             .getSerializable(EXTRAS_GROUND);
					MapActivity.showInstance(activity, playground);
				}
			}
		};


		void onMapReady(@NonNull GoogleMap googleMap) {
			if (mFragmentWeakReference.get() == null) {
				return;
			}
			Fragment fragment = mFragmentWeakReference.get();
			Playground playground = (Playground) fragment.getArguments()
			                                             .getSerializable(EXTRAS_GROUND);
			if (playground == null || playground.getPosition() == null) {
				return;
			}
			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(playground.getPosition(), 16));
			MarkerOptions markerOptions = new MarkerOptions().position(playground.getPosition());
			setPlaygroundIcon(App.Instance, playground, markerOptions);
			googleMap.addMarker(markerOptions);
			googleMap.setOnMapClickListener(mOnMapClickListener);
		}


		 void showWeather(@NonNull final ImageView weatherIv, @NonNull final TextView weatherTv, @NonNull  LatLng location) {
			if (mFragmentWeakReference.get() == null) {
				return;
			}
			try {
				String units = "metric";
				switch (Prefs.getInstance()
				             .getWeatherUnitsType()) {
					case "0":
						units = "metric";
						break;
					case "1":
						units = "imperial";
						break;
				}
				Api.getWeather(location.latitude,
				               location.longitude,
				               Locale.getDefault()
				                     .getLanguage(),
				               units,
				               App.Instance.getWeatherKey(),
				               new Callback<Weather>() {
					               @Override
					               public void success(Weather weather, Response response) {
						               Prefs prefs = Prefs.getInstance();
						               if (prefs.showWeatherBoard()) {
							               List<WeatherDetail> details = weather.getDetails();
							               if (details != null && details.size() > 0) {
								               WeatherDetail weatherDetail = details.get(0);
								               if (weatherDetail != null) {
									               int units = R.string.lbl_c;
									               switch (prefs.getWeatherUnitsType()) {
										               case "0":
											               units = R.string.lbl_c;
											               break;
										               case "1":
											               units = R.string.lbl_f;
											               break;
									               }
									               String temp = weather.getTemperature() != null ?
									                             App.Instance.getString(units,
									                                                    weather.getTemperature()
									                                                           .getValue()) :
									                             App.Instance.getString(units, 0f);
									               String weatherDesc = String.format("%s", temp);
									               if (!TextUtils.isEmpty(weatherDesc)) {
										               weatherTv.setText(weatherDesc);
									               }
									               String url = !TextUtils.isEmpty(weatherDetail.getIcon()) ?
									                            prefs.getWeatherIconUrl(weatherDetail.getIcon()) :
									                            prefs.getWeatherIconUrl("50d");
									               Glide.with(App.Instance)
									                    .load(url)
									                    .into(weatherIv);
								               }
							               }
						               }
					               }

					               @Override
					               public void failure(RetrofitError error) {
						               ((View) weatherTv.getParent()).setVisibility(View.GONE);
					               }
				               });
			} catch (ApiNotInitializedException e) {
				//Ignore this request.
			}
		}
	}
}
