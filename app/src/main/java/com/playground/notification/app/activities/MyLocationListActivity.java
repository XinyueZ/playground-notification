package com.playground.notification.app.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.support.v7.widget.GridLayoutManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.chopping.utils.DeviceUtils;
import com.playground.notification.R;
import com.playground.notification.app.App;
import com.playground.notification.app.adapters.MyLocationListAdapter;
import com.playground.notification.bus.OpenPlaygroundEvent;
import com.playground.notification.bus.SelectItemEvent;
import com.playground.notification.bus.StartActionModeEvent;
import com.playground.notification.databinding.MyLocationListBinding;
import com.playground.notification.ds.sync.MyLocation;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.MyLocationManager;
import com.playground.notification.utils.Prefs;

import java.util.ArrayList;
import java.util.List;

import static com.bumptech.glide.load.engine.DiskCacheStrategy.SOURCE;

/**
 * A list of all my-locations.
 *
 * @author Xinyue Zhao
 */
public final class MyLocationListActivity extends AppActivity {
	public static final int REQ = 0x91;
	private static final int GRID_COL_COUNT = 4;
	/**
	 * Main layout for this component.
	 */
	private static final int LAYOUT = R.layout.activity_my_location_list;
	/**
	 * Action-mode menu.
	 */
	private static final int MENU_ACTIONMODE = R.menu.menu_actionmode;
	/**
	 * An adapter for the list
	 */
	private MyLocationListAdapter mAdp;
	/**
	 * User action-mode for delete images.
	 */
	private ActionMode mActionMode;
	/**
	 * Data-binding.
	 */
	private MyLocationListBinding mBinding;
	//------------------------------------------------
	//Subscribes, event-handlers
	//------------------------------------------------


	/**
	 * Handler for {@link SelectItemEvent}.
	 *
	 * @param e Event {@link SelectItemEvent}.
	 */
	public void onEvent(SelectItemEvent e) {
		toggleSelection(e.getPosition());
	}


	/**
	 * Handler for {@link StartActionModeEvent}.
	 *
	 * @param e Event {@link  StartActionModeEvent}.
	 */
	public void onEvent(StartActionModeEvent e) {
		//See more about action-mode.
		//http://databasefaq.com/index.php/answer/19065/android-android-fragments-recyclerview-android-actionmode-problems-with-implementing-contextual-action-mode-in-recyclerview-fragment
		mActionMode = startSupportActionMode(new Callback() {
			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.getMenuInflater()
				    .inflate(MENU_ACTIONMODE, menu);
				mBinding.toolbar.setVisibility(View.GONE);

				mAdp.setActionMode(true);
				mAdp.notifyDataSetChanged();
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				//It has only "delete".
				List<Integer> selectedPositions = mAdp.getSelectedItems();
				List<MyLocation> myLocations = new ArrayList<>();
				for (Integer pos : selectedPositions) {
					myLocations.add(mAdp.getData()
					                    .get(pos));
				}
				//Remove on remote.
				for (MyLocation location : myLocations) {
					MyLocation del = new MyLocation(Prefs.getInstance()
					                                     .getGoogleId(), location.getLabel(), location);
					del.setObjectId(location.getObjectId());
					del.delete();
				}
				//Remove on cache.
				for (MyLocation alreadyRemoved : myLocations) {
					mAdp.getData()
					    .remove(alreadyRemoved);
				}
				mAdp.notifyDataSetChanged();
				mActionMode.finish();
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				mActionMode = null;
				mBinding.toolbar.setVisibility(View.VISIBLE);

				mAdp.clearSelection();
				mAdp.setActionMode(false);
				mAdp.notifyDataSetChanged();
			}
		});
	}

	/**
	 * Handler for {@link com.playground.notification.bus.OpenPlaygroundEvent}.
	 *
	 * @param e Event {@link com.playground.notification.bus.OpenPlaygroundEvent}.
	 */
	public void onEvent(OpenPlaygroundEvent e) {
		MapActivity.showInstance(this, e.getPlayground());
	}
	//------------------------------------------------

	/**
	 * Show single instance of {@link MyLocationListActivity}
	 *
	 * @param cxt {@link Context}.
	 */
	public static void showInstance(Activity cxt) {
		Intent intent = new Intent(cxt, MyLocationListActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		ActivityCompat.startActivity(cxt, intent, null);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//Init data-binding.
		mBinding = DataBindingUtil.setContentView(this, LAYOUT);
		//Init application basic elements.
		setUpErrorHandling((ViewGroup) findViewById(R.id.error_content));

		//App-bar.
		setSupportActionBar(mBinding.toolbar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setDefaultDisplayHomeAsUpEnabled(true);
		}

		setTitle(App.Instance.getString(R.string.action_my_location_list,
		                                MyLocationManager.getInstance()
		                                                 .getCachedList()
		                                                 .size()));

		mBinding.listRv.setLayoutManager(new GridLayoutManager(this, GRID_COL_COUNT));
		if (MyLocationManager.getInstance()
		                     .getCachedList()
		                     .size() > 0) {
			final MyLocation myLocation = MyLocationManager.getInstance()
			                                               .getCachedList()
			                                               .get(0);
			Prefs prefs = Prefs.getInstance();
			String latlng = myLocation.getLatitude() + "," + myLocation.getLongitude();
			String maptype = Prefs.getInstance()
			                      .getMapType()
			                      .equals("0") ?
			                 "roadmap" :
			                 "hybrid";
			final String url = prefs.getGoogleApiHost() + "maps/api/staticmap?center=" + latlng + "&zoom=16&size=" + prefs.getMyLocationPreviewSize() + "&markers=color:red%7Clabel:S%7C" + latlng +
					"&key=" + App.Instance.getDistanceMatrixKey() + "&sensor=true&maptype=" + maptype;
			Glide.with(App.Instance)
			     .load(url)
			     .asBitmap()
			     .diskCacheStrategy(SOURCE) // override default RESULT cache and apply transform always
			     .skipMemoryCache(false)
			     .centerCrop()
			     .into(new SimpleTarget<Bitmap>() {
				     @Override
				     public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
					     float cellWidth = DeviceUtils.getScreenSize(App.Instance).Width / (GRID_COL_COUNT + 0.f);
					     float cellHeight = cellWidth * (resource.getHeight() / (resource.getWidth() + 0.f));
					     mAdp = new MyLocationListAdapter(MyLocationManager.getInstance()
					                                                       .getCachedList(), (int) cellWidth, (int) cellHeight);
					     mBinding.listRv.setAdapter(mAdp);
				     }
			     });
		}
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				ActivityCompat.finishAfterTransition(this);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Select items on view when opened action-mode.
	 *
	 * @param position The select position.
	 */
	private void toggleSelection(int position) {
		mAdp.toggleSelection(position);
		int count = mAdp.getSelectedItemCount();

		if (count == 0) {
			mActionMode.finish();
		} else {
			mActionMode.setTitle(String.valueOf(count));
			mActionMode.invalidate();
		}
	}
}
