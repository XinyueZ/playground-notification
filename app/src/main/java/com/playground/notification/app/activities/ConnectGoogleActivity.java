package com.playground.notification.app.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.playground.notification.R;
import com.playground.notification.app.App;
import com.playground.notification.databinding.ActivityConnectGoogleBinding;
import com.playground.notification.utils.Prefs;

/**
 * Login on Google.
 *
 * @author Xinyue Zhao
 */
public final class ConnectGoogleActivity extends AppActivity {
	/**
	 * Main layout for this component.
	 */
	private static final int LAYOUT = R.layout.activity_connect_google;
	/**
	 * Request-id of this  {@link Activity}.
	 */
	public static final int REQ = 0x91;
	/**
	 * Data-binding.
	 */
	private ActivityConnectGoogleBinding mBinding;
	/**
	 * The Google-API.
	 */
	private GoogleApiClient mGoogleApiClient;

	private boolean mVisible;

	/**
	 * Show single instance of {@link ConnectGoogleActivity}
	 *
	 * @param cxt {@link Context}.
	 */
	public static void showInstance(Activity cxt) {
		Intent intent = new Intent(cxt, ConnectGoogleActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		ActivityCompat.startActivityForResult(cxt, intent, REQ, Bundle.EMPTY);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mVisible = false;
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		setUpErrorHandling((ViewGroup) findViewById(R.id.error_content));
		mBinding.googleLoginBtn.setSize(SignInButton.SIZE_WIDE);
		mBinding.helloTv.setText(getString(R.string.lbl_welcome, getString(R.string.application_name)));
		ViewCompat.setElevation(mBinding.sloganVg, getResources().getDimension(R.dimen.common_elevation));
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
		                                                                                              .build();
		mGoogleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this /* FragmentActivity */, new GoogleApiClient.OnConnectionFailedListener() {
			@Override
			public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
				Snackbar.make(mBinding.loginContentLl, R.string.meta_load_error, Snackbar.LENGTH_LONG)
				        .setAction(R.string.btn_close_app, new OnClickListener() {
					        @Override
					        public void onClick(View v) {
						        ActivityCompat.finishAffinity(ConnectGoogleActivity.this);
					        }
				        })
				        .show();
			}
		}).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();


		mBinding.googleLoginBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mBinding.googleLoginBtn.setVisibility(View.GONE);
				mBinding.loginPb.setVisibility(View.VISIBLE);
				mBinding.helloTv.setText(R.string.lbl_connect_google);
				ViewPropertyAnimator.animate(mBinding.thumbIv)
				                    .cancel();
				ViewPropertyAnimator.animate(mBinding.thumbIv)
				                    .alpha(0.3f)
				                    .setDuration(500)
				                    .start();
				loginGPlus();
			}
		});


		mBinding.closeBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_OK);
				ActivityCompat.finishAfterTransition(ConnectGoogleActivity.this);
			}
		});
	}


	@Override
	protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
		if (requestCode == REQ) {
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(intent);
			handleSignInResult(result);
		}
	}

	private void handleSignInResult(GoogleSignInResult result) {
		if (result.isSuccess()) {
			// Signed in successfully, show authenticated UI.
			GoogleSignInAccount acct = result.getSignInAccount();
			if (!mVisible) {
				Prefs prefs = Prefs.getInstance();
				if (acct != null) {
					prefs.setGoogleId(acct.getId());
					prefs.setGoogleDisplayName(acct.getDisplayName());

					if (acct.getPhotoUrl() != null) {
						Glide.with(App.Instance)
						     .load(acct.getPhotoUrl())
						     .into(mBinding.thumbIv);
						prefs.setGoogleThumbUrl(acct.getPhotoUrl()
						                            .toString());
					}
					ViewPropertyAnimator.animate(mBinding.thumbIv)
					                    .cancel();
					ViewPropertyAnimator.animate(mBinding.thumbIv)
					                    .alpha(1)
					                    .setDuration(500)
					                    .start();


					mBinding.helloTv.setText(getString(R.string.lbl_hello, acct.getDisplayName()));
					mBinding.loginPb.setVisibility(View.GONE);
					mBinding.closeBtn.setVisibility(View.VISIBLE);
					Animation shake = AnimationUtils.loadAnimation(App.Instance, R.anim.shake);
					mBinding.closeBtn.startAnimation(shake);
				}
			}

		} else {
			mBinding.helloTv.setText(getString(R.string.lbl_welcome, getString(R.string.application_name)));
			mBinding.loginPb.setVisibility(View.GONE);
			ViewPropertyAnimator.animate(mBinding.thumbIv)
			                    .cancel();
			ViewPropertyAnimator.animate(mBinding.thumbIv)
			                    .alpha(1)
			                    .setDuration(500)
			                    .start();
			mBinding.googleLoginBtn.setVisibility(View.VISIBLE);
		}
	}


	/**
	 * Login Google+
	 */
	private void loginGPlus() {
		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
		startActivityForResult(signInIntent, REQ);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mVisible = true;
	}
}
