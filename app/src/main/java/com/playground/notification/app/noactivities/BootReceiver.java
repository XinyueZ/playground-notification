package com.playground.notification.app.noactivities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.playground.notification.app.App;

/**
 * Handling device boot by {@link BroadcastReceiver}.
 *
 * @author Xinyue Zhao
 */
public final class BootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive( Context context, Intent intent ) {
		App.Instance.startService( new Intent( App.Instance, AppGuardService.class ) );
	}
}

