package com.playground.notification.app.noactivities;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.playground.notification.R;
import com.playground.notification.api.Api;
import com.playground.notification.api.ApiNotInitializedException;
import com.playground.notification.app.App;
import com.playground.notification.app.activities.MapActivity;
import com.playground.notification.ds.weather.Weather;
import com.playground.notification.ds.weather.WeatherDetail;
import com.playground.notification.utils.Prefs;
import com.playground.notification.utils.Utils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.app.PendingIntent.getActivity;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;
import static com.google.android.gms.location.LocationRequest.PRIORITY_LOW_POWER;
import static com.google.android.gms.location.LocationRequest.PRIORITY_NO_POWER;
import static com.google.android.gms.location.LocationRequest.create;
import static com.google.android.gms.location.LocationServices.API;
import static com.google.android.gms.location.LocationServices.FusedLocationApi;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.FEBRUARY;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.JUNE;
import static java.util.Calendar.MARCH;
import static java.util.Calendar.MAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.NOVEMBER;
import static java.util.Calendar.OCTOBER;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;
import static java.util.Calendar.getInstance;

public final class NotifyUserJob extends Job implements LocationListener {
	public static final String TAG = "NotifyUserJob";

	/**
	 * Connect to google-api.
	 */
	private GoogleApiClient mGoogleApiClient;
	/**
	 * Ask current location.
	 */
	private LocationRequest mLocationRequest;
	private boolean mWeekendNotify = false;
	private final NotificationManager mNotificationManager;

	static int scheduleJob() {
		return new JobRequest.Builder(NotifyUserJob.TAG).setPersisted(false)
		                                                .setBackoffCriteria(TimeUnit.MINUTES.toMillis(1), JobRequest.BackoffPolicy.EXPONENTIAL)
		                                                .setExact(TimeUnit.MINUTES.toMillis(1))
		                                                .build()
		                                                .schedule();
	}

	NotifyUserJob(@NonNull Context cxt) {
		mNotificationManager = (NotificationManager) cxt.getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	@NonNull
	protected Job.Result onRunJob(Params params) {
		Prefs prefs = Prefs.getInstance();
		if (prefs.isEULAOnceConfirmed()) {
			Calendar calendar = getInstance();
			int hour = calendar.get(HOUR_OF_DAY);
			int min = calendar.get(MINUTE);
			int month = calendar.get(MONTH);
			int day = calendar.get(DAY_OF_WEEK);
			if (prefs.notificationWeekendCall() && (day == SATURDAY || day == SUNDAY)) {
				if ((hour == 9 && min == 30) || (hour == 12 && min == 0) || (hour == 14 && min == 30)) {
					mWeekendNotify = true;
					startLocatingAndNotify();
				}
			}
			if (prefs.notificationWarmTips()) {
				if (month >= NOVEMBER && month <= FEBRUARY) {
					//Fall ~ Winter
					if ((hour == 15 && min == 0) || (hour == 15 && min == 15)) {
						startLocatingAndNotify();
					}
				} else if (month >= MARCH && month <= MAY) {
					//Spring
					if ((hour == 15 && min == 30) || (hour == 16 && min == 0)) {
						startLocatingAndNotify();
					}
				} else if (month >= JUNE && month <= OCTOBER) {
					//Summer
					if ((hour == 15 && min == 40) || (hour == 16 && min == 0)) {
						startLocatingAndNotify();
					}
				}
			}

		}

		return Result.SUCCESS;
	}


	/**
	 * Notify user.
	 */
	private void notify(String title, String desc, PendingIntent contentIntent) {
		Context context = getContext();
		android.support.v4.app.NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(context).setWhen(System.currentTimeMillis())
		                                                                                                         .setSmallIcon(R.drawable.ic_balloon)
		                                                                                                         .setTicker(title)
		                                                                                                         .setContentTitle(title)
		                                                                                                         .setContentText(desc)
		                                                                                                         .setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().bigText(desc)
		                                                                                                                                                                               .setBigContentTitle(
				                                                                                                                                                                               title)
		                                                                                                                                                                               .setSummaryText(
				                                                                                                                                                                               desc))
		                                                                                                         .setAutoCancel(true);
		notifyBuilder.setContentIntent(contentIntent);
		Utils.vibrateSound(context, notifyBuilder);
		mNotificationManager.notify((int) System.currentTimeMillis(), notifyBuilder.build());
	}


	private boolean isGoodWeatherCondition(WeatherDetail weatherDetail) {
		return weatherDetail.getId() == 800 || weatherDetail.getId() == 801 || weatherDetail.getId() == 802 || weatherDetail.getId() == 803;
	}


	private void startLocatingAndNotify() {
		//Location request.
		if (mLocationRequest == null) {
			mLocationRequest = create();
			mLocationRequest.setInterval(300);
			mLocationRequest.setFastestInterval(300);
			int ty = 0;
			switch (Prefs.getInstance()
			             .getBatteryLifeType()) {
				case "0":
					ty = PRIORITY_BALANCED_POWER_ACCURACY;
					break;
				case "1":
					ty = PRIORITY_HIGH_ACCURACY;
					break;
				case "2":
					ty = PRIORITY_LOW_POWER;
					break;
				case "3":
					ty = PRIORITY_NO_POWER;
					break;
			}
			mLocationRequest.setPriority(ty);
		}

		if (mGoogleApiClient == null) {
			mGoogleApiClient = new GoogleApiClient.Builder(App.Instance).addApi(API)
			                                                            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
				                                                            @Override
				                                                            public void onConnected(Bundle bundle) {
					                                                            if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && mLocationRequest != null) {
						                                                            Context context = getContext();
						                                                            if (ActivityCompat.checkSelfPermission(context,
						                                                                                                   ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat
								                                                            .checkSelfPermission(
								                                                            context,
								                                                            ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
						                                                            	return;
						                                                            }
						                                                            FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, NotifyUserJob.this);
					                                                            }
				                                                            }

				                                                            @Override
				                                                            public void onConnectionSuspended(int i) {
					                                                            com.chopping.utils.Utils.showShortToast(App.Instance, "onConnectionSuspended");

				                                                            }
			                                                            })
			                                                            .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
				                                                            @Override
				                                                            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
					                                                            com.chopping.utils.Utils.showShortToast(App.Instance, "onConnectionFailed: " + connectionResult.getErrorCode());
				                                                            }
			                                                            })
			                                                            .build();

			mGoogleApiClient.connect();
		}
	}


	@Override
	public void onLocationChanged(Location location) {
		if(location == null)
			return;
		Prefs prefs = Prefs.getInstance();
		if (!prefs.isEULAOnceConfirmed()) {
			return;
		}
		String units = "metric";
		switch (prefs.getWeatherUnitsType()) {
			case "0":
				units = "metric";
				break;
			case "1":
				units = "imperial";
				break;
		}
		try {
			Api.getWeather(location.getLatitude(),
			               location.getLongitude(),
			               Locale.getDefault()
			                     .getLanguage(),
			               units,
			               App.Instance.getWeatherKey(),
			               new Callback<Weather>() {
				               @Override
				               public void success(Weather weather, Response response) {
					               Prefs prefs = Prefs.getInstance();
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
							               Context cxt = getContext();
							               String temp = weather.getTemperature() != null ?
							                             cxt.getString(units,
							                                           weather.getTemperature()
							                                                  .getValue()) :
							                             cxt.getString(units, 0f);

							               Intent i = new Intent(App.Instance, MapActivity.class);
							               i.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP);
							               String title = mWeekendNotify ?
							                              App.Instance.getString(R.string.notify_weekend_title) :
							                              App.Instance.getString(R.string.notify_warm_tips_title);
							               if (isGoodWeatherCondition(weatherDetail)) {
								               PendingIntent pi = getActivity(App.Instance, (int) System.currentTimeMillis(), i, FLAG_UPDATE_CURRENT);
								               NotifyUserJob.this.notify(title, App.Instance.getString(R.string.notify_content, weatherDetail.getDescription(), temp), pi);
							               }
						               }
					               }
					               //Ignore...
				               }

				               @Override
				               public void failure(RetrofitError error) {
					               //Ignore...
				               }
			               });
		} catch (ApiNotInitializedException e) {
			//Ignore...
		} finally {
			stopLocating();
		}
	}


	private void stopLocating() {
		if (mGoogleApiClient != null) {
			FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
			mGoogleApiClient = null;
		}
		if (mLocationRequest != null) {
			mLocationRequest = null;
		}
	}
}
