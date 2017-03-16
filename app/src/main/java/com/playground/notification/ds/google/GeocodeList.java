package com.playground.notification.ds.google;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class GeocodeList {
	@SerializedName("results")
	private List<Geocode> mGeocodeList;
	@SerializedName("status")
	private String        mStatus;

	public List<Geocode> getGeocodeList() {
		return mGeocodeList;
	}

	public String getStatus() {
		return mStatus;
	}
}
