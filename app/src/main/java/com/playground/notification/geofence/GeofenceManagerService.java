package com.playground.notification.geofence;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.playground.notification.ds.sync.NearRing;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.utils.PlaygroundIdUtils;
import com.playground.notification.utils.Prefs;

import java.util.ArrayList;
import java.util.List;

/**
 * A background {@link Service} that controls "near-ring" geofence transition.
 * <p/>
 * See {@link NearRing}.
 *
 * @author Xinyue Zhao
 */
public final class GeofenceManagerService extends Service implements ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<Status> {
	/**
	 * All created geofence objects.
	 */
	private List<Geofence> mGeofenceList = new ArrayList<>();
	/**
	 * Used when requesting to add or remove geofences.
	 */
	private PendingIntent mGeofencePendingIntent;

	/**
	 * Provides the entry point to Google Play services.
	 */
	private GoogleApiClient mGoogleApiClient;

	/**
	 * A geofence request.
	 */
	private GeofencingRequest mGeofencingRequest;


	@Override
	public void onCreate() {
		super.onCreate();
		buildGoogleApiClient();
		Log.d( "pg:geofence", "onCreate" );
	}

	@Override
	public int onStartCommand( Intent intent, int flags, int startId ) {
		if( mGoogleApiClient != null && !mGoogleApiClient.isConnected() ) {
			mGoogleApiClient.connect();
		}

		if( mGeofenceList != null ) {
			mGeofenceList.clear();
		}

		List<NearRing> rings = NearRingManager.getInstance().getCachedList();
		if( rings.size() > 0 ) {
			for( NearRing nearRing : rings ) {
				addGeofence( nearRing );
			}
			createGeofenceRequest();
			createPendingIntent();
		}

		Log.d( "pg:geofence", "onStartCommand" );
		return super.onStartCommand( intent, flags, startId );
	}

	@Override
	public void onConnected( Bundle bundle ) {
		//Google service is O.K, start transactions from geofence.
		if( mGeofencingRequest != null && mGeofencePendingIntent != null ) {
			LocationServices.GeofencingApi.addGeofences( mGoogleApiClient, mGeofencingRequest, mGeofencePendingIntent ).setResultCallback( this );
		}
	}

	@Override
	public void onDestroy() {
		if( mGeofencePendingIntent != null ) {
			if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
				LocationServices.GeofencingApi.removeGeofences( mGoogleApiClient, mGeofencePendingIntent ).setResultCallback( this );
			}
			mGeofencePendingIntent = null;
		}

		if( mGoogleApiClient != null && !mGoogleApiClient.isConnected() ) {
			mGoogleApiClient.disconnect();
			mGoogleApiClient = null;
		}
		mGeofencingRequest = null;
		mGoogleApiClient = null;
		super.onDestroy();
		Log.d( "pg:geofence", "onDestroy" );
	}


	private void addGeofence( NearRing nearRing ) {
		mGeofenceList.add( new Geofence.Builder().setRequestId(PlaygroundIdUtils.getId(nearRing) ).setCircularRegion(
				nearRing.getLatitude(), nearRing.getLongitude(), Prefs.getInstance().getAlarmArea() ).setExpirationDuration(
				AlarmManager.INTERVAL_DAY ).setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER ).build() );
	}

	private void createGeofenceRequest() {
		GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
		builder.setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER );
		builder.addGeofences( mGeofenceList );
		mGeofencingRequest = builder.build();
	}

	private void createPendingIntent() {
		Intent intent = new Intent( this, GeofenceTransitionsIntentService.class );
		mGeofencePendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.
				FLAG_UPDATE_CURRENT );
	}

	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder( this ).addConnectionCallbacks( this ).addOnConnectionFailedListener( this ).addApi(
				LocationServices.API ).build();
	}


	/**
	 * Runs when the result of calling addGeofences() and removeGeofences() becomes available. Either method can complete successfully or with an
	 * error.
	 * <p/>
	 * Since this activity implements the {@link ResultCallback} interface, we are required to define this method.
	 *
	 * @param status
	 * 		The Status returned through a PendingIntent when addGeofences() or removeGeofences() get called.
	 */
	@Override
	public void onResult( Status status ) {
	}


	@Override
	public void onConnectionSuspended( int i ) {

	}

	@Override
	public void onConnectionFailed( ConnectionResult connectionResult ) {

	}

	@Nullable
	@Override
	public IBinder onBind( Intent intent ) {
		return null;
	}
}
