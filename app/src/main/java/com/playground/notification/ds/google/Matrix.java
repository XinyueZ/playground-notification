package com.playground.notification.ds.google;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class Matrix {
	@SerializedName("destination_addresses")
	private List<String> mDestination;
	@SerializedName("origin_addresses")
	private List<String> mOrigin;
	@SerializedName("rows")
	private List<Row>    mRows;

	public List<String> getDestination() {
		return mDestination;
	}

	public List<String> getOrigin() {
		return mOrigin;
	}

	public List<Row> getRows() {
		return mRows;
	}
}
