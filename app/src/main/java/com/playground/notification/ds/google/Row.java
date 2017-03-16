package com.playground.notification.ds.google;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class Row {
	@SerializedName("elements")
	private List<Element> mElements;

	public List<Element> getElements() {
		return mElements;
	}
}
