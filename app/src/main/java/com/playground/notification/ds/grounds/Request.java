package com.playground.notification.ds.grounds;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class Request {
	@SerializedName("left")
	private double       mWest;
	@SerializedName("bottom")
	private double       mSouth;
	@SerializedName("right")
	private double       mEast;
	@SerializedName("top")
	private double       mNorth;
	@SerializedName("width")
	private int          mWidth;
	@SerializedName("height")
	private int          mHeight;
	@SerializedName("filter")
	private List<String> mFilter;
	@SerializedName("ts")
	private long         mTimestamps;
	@SerializedName("result")
	private List<String> mResult;


	public double getWest() {
		return mWest;
	}

	public void setWest( double west ) {
		mWest = west;
	}

	public double getSouth() {
		return mSouth;
	}

	public void setSouth( double south ) {
		mSouth = south;
	}

	public double getEast() {
		return mEast;
	}

	public void setEast( double east ) {
		mEast = east;
	}

	public double getNorth() {
		return mNorth;
	}

	public void setNorth( double north ) {
		mNorth = north;
	}

	public int getWidth() {
		return mWidth;
	}

	public void setWidth( int width ) {
		mWidth = width;
	}

	public int getHeight() {
		return mHeight;
	}

	public void setHeight( int height ) {
		mHeight = height;
	}

	public List<String> getFilter() {
		return mFilter;
	}

	public void setFilter( List<String> filter ) {
		mFilter = filter;
	}

	public long getTimestamps() {
		return mTimestamps;
	}

	public void setTimestamps( long timestamps ) {
		mTimestamps = timestamps;
	}

	public List<String> getResult() {
		return mResult;
	}

	public void setResult( List<String> result ) {
		mResult = result;
	}
}
