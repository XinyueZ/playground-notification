package com.playground.notification.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.playground.notification.R;
import com.playground.notification.api.Api;
import com.playground.notification.api.ApiNotInitializedException;
import com.playground.notification.app.App;
import com.playground.notification.databinding.LayoutWeatherBinding;
import com.playground.notification.ds.weather.Weather;
import com.playground.notification.ds.weather.WeatherDetail;
import com.playground.notification.utils.Prefs;

import java.util.List;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public final class WeatherLayout extends FrameLayout {
	private LayoutWeatherBinding mLayoutWeatherBinding;

	public WeatherLayout(Context context) {
		super(context);
		init(context);
	}

	public WeatherLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public WeatherLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(@NonNull Context cxt) {
		LayoutInflater inflater = LayoutInflater.from(cxt);
		mLayoutWeatherBinding = LayoutWeatherBinding.inflate(inflater, this, true);
	}

	public WeatherLayout setWeather(@NonNull LatLng latLng) {
		Prefs prefs = Prefs.getInstance();
		if (!prefs.showWeatherBoard()) {
			setVisibility(View.GONE);
			return this;
		}
		try {
			String units = "metric";
			switch (prefs.getWeatherUnitsType()) {
				case "0":
					units = "metric";
					break;
				case "1":
					units = "imperial";
					break;
			}
			Api.getWeather(latLng.latitude,
			               latLng.longitude,
			               Locale.getDefault()
			                     .getLanguage(),
			               units,
			               App.Instance.getWeatherKey(),
			               new Callback<Weather>() {
				               @Override
				               public void success(Weather weather, Response response) {
					               setVisibility(View.VISIBLE);
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
									               mLayoutWeatherBinding.weatherTv.setText(weatherDesc);
								               }
								               String url = !TextUtils.isEmpty(weatherDetail.getIcon()) ?
								                            prefs.getWeatherIconUrl(weatherDetail.getIcon()) :
								                            prefs.getWeatherIconUrl("50d");
								               Glide.with(App.Instance)
								                    .load(url)
								                    .into(mLayoutWeatherBinding.weatherIconIv);
							               }
						               }
					               }
				               }

				               @Override
				               public void failure(RetrofitError error) {
					               setVisibility(View.GONE);
				               }
			               });
		} catch (ApiNotInitializedException e) {
			//Ignore this request.
		}
		return this;
	}
}
