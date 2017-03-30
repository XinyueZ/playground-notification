package com.playground.notification.bus;


import com.playground.notification.ds.grounds.Playground;

public final class SelectedPinOpenEvent {
	private final Playground mPlayground;


	public SelectedPinOpenEvent(Playground playground) {
		mPlayground = playground;
	}


	public Playground getPlayground() {
		return mPlayground;
	}
}
