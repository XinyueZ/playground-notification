package com.playground.notification.bus;


public final class PostOpenRouteEvent {
	private final boolean mAddToNearRing;


	public PostOpenRouteEvent(boolean addToNearRing) {
		mAddToNearRing = addToNearRing;
	}


	public boolean isAddToNearRing() {
		return mAddToNearRing;
	}
}
