package com.playground.notification.app.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.content.res.AppCompatResources;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.chopping.bus.CloseDrawerEvent;
import com.chopping.utils.Utils;
import com.playground.notification.R;
import com.playground.notification.app.App;
import com.playground.notification.bus.LogoutEvent;
import com.playground.notification.databinding.FragmentGplusBinding;
import com.playground.notification.geofence.GeofenceManagerService;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.MyLocationManager;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.sync.RatingManager;
import com.playground.notification.utils.Prefs;

import de.greenrobot.event.EventBus;

/**
 * The fragment that controls user information of g+, logout etc.
 *
 * @author Xinyue Zhao
 */
public final class GPlusFragment extends AppFragment {
	/**
	 * Main layout for this component.
	 */
	private static final int LAYOUT = R.layout.fragment_gplus;

	private FragmentGplusBinding mBinding;

	/**
	 * New an instance of {@link GPlusFragment}.
	 *
	 * @param context {@link android.content.Context}.
	 * @return An instance of {@link GPlusFragment}.
	 */
	public static Fragment newInstance(Context context) {
		return GPlusFragment.instantiate(context, GPlusFragment.class.getName());
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mBinding = DataBindingUtil.inflate(inflater, LAYOUT, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mBinding.logoutBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EventBus.getDefault()
				        .post(new CloseDrawerEvent());

				//Logout and delete all userdata.
				logout();
				Activity activity = getActivity();
				if (activity != null) {
					EventBus.getDefault().postSticky(new LogoutEvent());
//					ConnectGoogleActivity.showInstance(activity);
				}
			}
		});


	}

	/**
	 * Doing logout from app.
	 */
	public static void logout() {
		Prefs prefs = Prefs.getInstance();
		if (!TextUtils.isEmpty(prefs.getGoogleId())) {
			prefs.setGoogleId(null);
			prefs.setGoogleDisplayName(null);
			prefs.setGoogleThumbUrl(null);
			FavoriteManager.getInstance()
			               .clean();
			NearRingManager.getInstance()
			               .clean();
			MyLocationManager.getInstance()
			                 .clean();
			RatingManager.getInstance()
			             .clean();
			App.Instance.stopService(new Intent(App.Instance, GeofenceManagerService.class));
		}
	}


	@Override
	public void onResume() {
		Prefs prefs = Prefs.getInstance();
		if (!TextUtils.isEmpty(prefs.getGoogleThumbUrl())) {
			Glide.with(App.Instance)
			     .load(Utils.uriStr2URI(prefs.getGoogleThumbUrl())
			                .toASCIIString())
			     .placeholder(AppCompatResources.getDrawable(getContext(), R.drawable.ic_person_default))
			     .into(mBinding.peoplePhotoIv);
		} else {
			mBinding.peoplePhotoIv.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_person_default));
		}
		mBinding.peopleNameTv.setText(getString(R.string.lbl_hello, prefs.getGoogleDisplayName()));
		super.onResume();
	}

}
