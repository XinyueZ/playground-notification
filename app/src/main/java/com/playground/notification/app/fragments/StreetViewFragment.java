package com.playground.notification.app.fragments;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chopping.application.BasicPrefs;
import com.chopping.fragments.BaseFragment;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.playground.notification.R;
import com.playground.notification.databinding.FragmentStreetViewBinding;
import com.playground.notification.utils.Prefs;


/**
 * Show street-view for given location.
 *
 * @author Xinyue Zhao
 */
public final class StreetViewFragment extends BaseFragment implements OnStreetViewPanoramaReadyCallback {
	private static final String EXTRAS_LOCATION = StreetViewFragment.class.getName() + ".EXTRAS.location";
	private static final int LAYOUT = R.layout.fragment_street_view;
	private FragmentStreetViewBinding mBinding;
	private StreetViewPanorama mStreetViewPanorama;

	public static StreetViewFragment newInstance(@NonNull Context cxt, @NonNull LatLng location) {
		Bundle args = new Bundle();
		args.putParcelable(EXTRAS_LOCATION, location);
		return (StreetViewFragment) StreetViewFragment.instantiate(cxt, StreetViewFragment.class.getName(), args);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mBinding = DataBindingUtil.inflate(inflater, LAYOUT, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		SupportStreetViewPanoramaFragment streetViewPanoramaFragment = (SupportStreetViewPanoramaFragment) getChildFragmentManager().findFragmentById(R.id.panorama);
		streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);
	}

	@Override
	public void onStreetViewPanoramaReady(StreetViewPanorama streetViewPanorama) {
		mStreetViewPanorama = streetViewPanorama;
		LatLng location = getArguments().getParcelable(EXTRAS_LOCATION);
		setStreetView(location);

	}

	/**
	 * Set current location on street-view.
	 *
	 * @param location The current location.
	 */
	public void setStreetView(@NonNull LatLng location) {
		if (mStreetViewPanorama != null) {
			mStreetViewPanorama.setPosition(location);
		}
	}


	@Override
	protected BasicPrefs getPrefs() {
		return Prefs.getInstance();
	}
}
