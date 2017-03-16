package com.playground.notification.bus;


import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.sync.Rating;

public final class ShowLocationRatingEvent {
	private final Playground mPlayground;
	private final Rating mRating;

	public ShowLocationRatingEvent(Playground ground, Rating rating) {
		mPlayground = ground;
		mRating = rating;
	}

	public Playground getPlayground() {
		return mPlayground;
	}

	public Rating getRating() {
		return mRating;
	}
}
