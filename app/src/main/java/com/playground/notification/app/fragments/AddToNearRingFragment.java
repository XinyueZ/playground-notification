package com.playground.notification.app.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;

import com.playground.notification.R;
import com.playground.notification.bus.PostOpenRouteEvent;
import com.playground.notification.ds.grounds.Playground;

import java.io.Serializable;

import de.greenrobot.event.EventBus;

import static com.playground.notification.app.fragments.AppFragment.EXTRAS_GROUND;
import static com.playground.notification.app.fragments.AppFragment.EXTRAS_LAT;
import static com.playground.notification.app.fragments.AppFragment.EXTRAS_LNG;

/**
 * Created by xzhao on 16.03.17.
 */
public final class AddToNearRingFragment extends AppCompatDialogFragment { ;

	public static AddToNearRingFragment newInstance(Context cxt, double fromLat, double fromLng, Playground playground) {
		Bundle args = new Bundle();
		args.putDouble(EXTRAS_LAT, fromLat);
		args.putDouble(EXTRAS_LNG, fromLng);
		args.putSerializable(EXTRAS_GROUND, (Serializable) playground);
		return (AddToNearRingFragment) AddToNearRingFragment.instantiate(cxt, AddToNearRingFragment.class.getName(), args);
	}


	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the Builder class for convenient dialog construction
		android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.add_to_near_ring_before_route_title)
		       .setMessage(R.string.add_to_near_ring_before_route)
		       .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
				       EventBus.getDefault()
				               .post(new PostOpenRouteEvent(true));
			       }
		       })
		       .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int id) {
				       EventBus.getDefault()
				               .post(new PostOpenRouteEvent(false));
			       }
		       });
		return builder.create();
	}
}
