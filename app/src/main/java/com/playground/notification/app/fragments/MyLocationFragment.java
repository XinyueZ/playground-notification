package com.playground.notification.app.fragments;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.playground.notification.R;
import com.playground.notification.api.Api;
import com.playground.notification.api.ApiNotInitializedException;
import com.playground.notification.app.App;
import com.playground.notification.databinding.MyLocationBinding;
import com.playground.notification.ds.google.Matrix;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.sync.MyLocation;
import com.playground.notification.sync.MyLocationManager;
import com.playground.notification.utils.PlaygroundIdUtils;
import com.playground.notification.utils.Prefs;
import com.playground.notification.utils.Utils;

import java.io.Serializable;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.playground.notification.app.fragments.AppFragment.EXTRAS_GROUND;
import static com.playground.notification.app.fragments.AppFragment.EXTRAS_LAT;
import static com.playground.notification.app.fragments.AppFragment.EXTRAS_LNG;
import static com.playground.notification.utils.Utils.setPlaygroundIcon;

/**
 * Information for my own location.
 *
 * @author Xinyue Zhao
 */
public final class MyLocationFragment extends AppCompatDialogFragment {
	/**
	 * Main layout for this component.
	 */
	private static final int LAYOUT = R.layout.fragment_my_location;
	/**
	 * Data-binding.
	 */
	private MyLocationBinding mBinding;

	private final AppFragment.CommonUIDelegate mCommonUIDelegate = new AppFragment.CommonUIDelegate(this);
	/**
	 * New an instance of {@link MyLocationFragment}.
	 *
	 * @param context    {@link android.content.Context}.
	 * @param fromLat    The latitude of "from" position to {@code playground}.
	 * @param fromLng    The longitude of "from" position to {@code playground}.
	 * @param playground {@link Playground}.
	 * @return An instance of {@link MyLocationFragment}.
	 */
	public static MyLocationFragment newInstance(Context context, double fromLat, double fromLng, Playground playground) {
		Bundle args = new Bundle();
		args.putDouble(EXTRAS_LAT, fromLat);
		args.putDouble(EXTRAS_LNG, fromLng);
		args.putSerializable(EXTRAS_GROUND, (Serializable) playground);
		return (MyLocationFragment) MyLocationFragment.instantiate(context, MyLocationFragment.class.getName(), args);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_AppCompat_Light_Dialog);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mBinding = DataBindingUtil.inflate(inflater, LAYOUT, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mBinding.map.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {
		mBinding.map.onDestroy();
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
		super.onResume();
		EventBus.getDefault()
		        .register(mCommonUIDelegate);
	}

	@Override
	public void onPause() {
		mBinding.map.onPause();
		EventBus.getDefault()
		        .unregister(mCommonUIDelegate);
		super.onPause();
	}

	@Override
	public void onLowMemory() {
		mBinding.map.onLowMemory();
		super.onLowMemory();
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Bundle args = getArguments();
		final Playground playground = (Playground) args.getSerializable(EXTRAS_GROUND);
		if (playground != null) {

			final double lat = args.getDouble(EXTRAS_LAT);
			final double lng = args.getDouble(EXTRAS_LNG);

			MyLocationManager manager = MyLocationManager.getInstance();
			MyLocation myLocation = manager.findInCache(playground);

			Prefs prefs = Prefs.getInstance();
			mBinding.map.onCreate(savedInstanceState);

			if (myLocation != null) {
				mBinding.saveMyLocationIv.setImageResource(R.drawable.ic_action_delete);
				mBinding.shareGroundBtn.setVisibility(View.VISIBLE);
				mBinding.myLocationNameTv.setText(myLocation.getLabel());
				mBinding.myLocationNameTv.setEnabled(false);
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
				dismiss();
			}

			mBinding.map.getLayoutParams().height = (int) App.Instance.getListItemHeight();
			mBinding.map.getMapAsync(mOnMapReadyCallback);
		}
	}

	/**
	 * Map can be loaded successfully or not.
	 */
	private final OnMapReadyCallback mOnMapReadyCallback = new OnMapReadyCallback() {
		@Override
		public void onMapReady(GoogleMap googleMap) {
			mCommonUIDelegate.onMapReady(googleMap);
			googleMap.setOnMapClickListener(mOnMapClickListener);
		}
	};

	/**
	 * Click on map.
	 */
	private final GoogleMap.OnMapClickListener mOnMapClickListener = new GoogleMap.OnMapClickListener() {
		@Override
		public void onMapClick(LatLng latLng) {
			//Ignore here...
		}
	};

	/**
	 * Event-handler for all radio-buttons on UI.
	 */
	public static class EventHandler {
		private double mLat;
		private double mLng;
		private Playground mGround;
		private MyLocationBinding mBinding;

		public EventHandler(double fromLat, double fromLng, Playground playground, MyLocationBinding binding) {
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

		public void onSaveMyLocationClicked(View v) {
			MyLocationManager manager = MyLocationManager.getInstance();
			MyLocation myLocation = manager.findInCache(mGround);
			if (myLocation != null) {
				manager.removeMyLocation(myLocation, mBinding.saveMyLocationIv, mBinding.myLocationVg);
			} else {
				String name = mBinding.myLocationNameTv.getText()
				                                       .toString();
				if (Utils.validateStr(App.Instance, name)) {
					InputMethodManager imm = (InputMethodManager) App.Instance.getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(mBinding.myLocationNameTv.getWindowToken(), 0);
					mGround.setId(PlaygroundIdUtils.getId(mGround));
					manager.addMyLocation(mGround, name, mBinding.saveMyLocationIv, null);
					mBinding.shareGroundBtn.setVisibility(View.VISIBLE);
				}
			}
		}

		public void onShareGround(View v) {
			final String url = Prefs.getInstance()
			                        .getGoogleMapSearchHost() + mGround.getLatitude() + "," + mGround.getLongitude();
			com.tinyurl4j.Api.getTinyUrl(url, new Callback<com.tinyurl4j.data.Response>() {
				@Override
				public void success(com.tinyurl4j.data.Response response, retrofit.client.Response response2) {
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
