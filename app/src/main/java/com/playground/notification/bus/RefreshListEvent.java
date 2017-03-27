package com.playground.notification.bus;


import com.playground.notification.ds.grounds.Playground;

import java.util.List;

public final class RefreshListEvent {
	private final List<? extends Playground> mList;

	public RefreshListEvent(List<? extends Playground> list) {
		mList = list;
	}


	public List<? extends Playground> getList() {
		return mList;
	}
}
