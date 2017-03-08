package com.playground.notification.bus;


public final class PostOpenRouteEvent {
	private final boolean mFlag;


	public PostOpenRouteEvent(boolean flag) {
		mFlag = flag;
	}


	public boolean isFlag() {
		return mFlag;
	}
}
