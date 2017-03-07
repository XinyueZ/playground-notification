package com.playground.notification.app.adapters;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import com.chopping.application.LL;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.playground.notification.R;
import com.playground.notification.app.App;
import com.playground.notification.bus.OpenPlaygroundEvent;
import com.playground.notification.bus.PinSelectedEvent;
import com.playground.notification.databinding.ItemPlaygroundBinding;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.sync.Rating;
import com.playground.notification.sync.FavoriteManager;
import com.playground.notification.sync.NearRingManager;
import com.playground.notification.sync.RatingManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

import static com.playground.notification.utils.Utils.getBitmapDescriptor;


/**
 * The adapter for {@link RecyclerView} in {@link com.playground.notification.app.fragments.PlaygroundListFragment}.
 *
 * @author Xinyue Zhao
 */
public final class PlaygroundListAdapter extends RecyclerView.Adapter<PlaygroundListAdapter.PlaygroundListAdapterViewHolder> {
	private static final int ITEM_LAYOUT = R.layout.item_playground_list;
	private List<Playground> mPlaygroundList = new ArrayList<>();
	private int mLastSelectedPosition = Adapter.NO_SELECTION;

	private Playground mPlaygroundScrolledTo = null;
	private LinearLayoutManager mLinearLayoutManager;

	public RecyclerView.OnScrollListener recyclerViewOnScrollListener = new RecyclerView.OnScrollListener() {
		public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
			if (mPlaygroundScrolledTo == null) {
				LL.w("ignore onScrolled because mPlaygroundScrolledTo is null");
				return;
			}
			int visiPosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
			View visiView = mLinearLayoutManager.findViewByPosition(visiPosition);
			if (visiView != null) {
				EventBus.getDefault()
				        .post(new OpenPlaygroundEvent(mPlaygroundScrolledTo, visiPosition, new WeakReference<>(visiView)));
				LL.i("open detail at onScrolled because mPlaygroundScrolledTo");
			} else {
				LL.w("visiView is null");
			}
			mPlaygroundScrolledTo = null;
		}
	};

	//------------------------------------------------
	//Subscribes, event-handlers
	//------------------------------------------------

	/**
	 * Handler for {@link PinSelectedEvent}.
	 *
	 * @param e Event {@link PinSelectedEvent}.
	 */
	public void onEvent(PinSelectedEvent e) {
		mPlaygroundScrolledTo = e.getPlayground();
		for (int i = 0, cnt = getItemCount();
				i < cnt;
				i++) {
			final Playground item = mPlaygroundList.get(i);
			if (item.equals(mPlaygroundScrolledTo)) {
				notifySelectedItemChanged(i);
				mLinearLayoutManager.scrollToPositionWithOffset(i, 1);
				return;
			}
		}
	}

	//------------------------------------------------
	public PlaygroundListAdapter(@NonNull LinearLayoutManager linearLayoutManager, @Nullable List<? extends Playground> playgroundList) {
		mLinearLayoutManager = linearLayoutManager;
		if (playgroundList == null) {
			playgroundList = new ArrayList<>();
		}
		mPlaygroundList.addAll(playgroundList);
	}

	@Override
	public PlaygroundListAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		Context cxt = parent.getContext();
		ItemPlaygroundBinding binding = DataBindingUtil.inflate(LayoutInflater.from(cxt), ITEM_LAYOUT, parent, false);
		return new PlaygroundListAdapterViewHolder(this, binding);
	}

	@Override
	public void onBindViewHolder(final PlaygroundListAdapterViewHolder holder, int position) {
		holder.onBindViewHolder();
	}

	@Override
	public void onViewRecycled(PlaygroundListAdapterViewHolder holder) {
		holder.onViewRecycled();
	}


	@Override
	public int getItemCount() {
		return mPlaygroundList == null ?
		       0 :
		       mPlaygroundList.size();
	}

	public void refresh(@Nullable List<? extends Playground> data) {
		if (mPlaygroundList.size() > 0) {
			mPlaygroundList.clear();
		}
		if (data == null) {
			data = new ArrayList<>();
		}
		mPlaygroundList.addAll(data);
		notifyDataSetChanged();
	}

	private void notifySelectedItemChanged(int newPosition) {
		int previousLastSelectedPosition = mLastSelectedPosition;
		mLastSelectedPosition = newPosition;
		if (previousLastSelectedPosition != Adapter.NO_SELECTION) {
			notifyItemChanged(previousLastSelectedPosition);
		}
		if (mLastSelectedPosition != Adapter.NO_SELECTION) {
			notifyItemChanged(mLastSelectedPosition);
		}
	}

	protected static class PlaygroundListAdapterViewHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback,
	                                                                                                  RatingManager.RatingUI {
		private final ItemPlaygroundBinding mBinding;
		private final PlaygroundListAdapter mPlaygroundListAdapter;
		private GoogleMap mGoogleMap;

		private PlaygroundListAdapterViewHolder(PlaygroundListAdapter playgroundListAdapter, ItemPlaygroundBinding binding) {
			super(binding.getRoot());
			mPlaygroundListAdapter = playgroundListAdapter;
			mBinding = binding;
		}

		private void onBindViewHolder() {
			final ViewGroup.LayoutParams layoutParams = mBinding.itemMapRecyclerview.getLayoutParams();
			layoutParams.width = (int) App.Instance.getListItemWidth();
			layoutParams.height = (int) App.Instance.getListItemHeight();

			mBinding.itemMapRecyclerview.onCreate(null);
			mBinding.itemMapRecyclerview.onStart();
			mBinding.itemMapRecyclerview.onResume();
			mBinding.itemMapRecyclerview.getMapAsync(this);

			mBinding.executePendingBindings();
		}


		@Override
		public void onMapReady(GoogleMap googleMap) {
			showData(googleMap);
		}

		private void showData(GoogleMap googleMap) {
			if (getAdapterPosition() < 0 || mPlaygroundListAdapter.mPlaygroundList == null || mPlaygroundListAdapter.mPlaygroundList.size() <= 0) {
				return;
			}
			Playground playground = mPlaygroundListAdapter.mPlaygroundList.get(getAdapterPosition());

			RatingManager.showRatingSummaryOnLocation(playground, this);
			mBinding.setFavorited(FavoriteManager.getInstance()
			                                     .isCached(playground));
			mBinding.setNearRing(NearRingManager.getInstance()
			                                     .isCached(playground));
			mGoogleMap = googleMap;
			mGoogleMap.setBuildingsEnabled(false);
			mGoogleMap.setIndoorEnabled(false);
			googleMap.getUiSettings()
			         .setMapToolbarEnabled(false);
			googleMap.getUiSettings()
			         .setScrollGesturesEnabled(false);
			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(playground.getPosition(), 16));
			Marker marker = googleMap.addMarker(new MarkerOptions().position(playground.getPosition()));
			marker.setIcon(getBitmapDescriptor(App.Instance, R.drawable.ic_pin_500));
			googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
				@Override
				public void onMapClick(LatLng latLng) {
					openItem();
				}
			});
			mBinding.itemContainerFl.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					openItem();
				}
			});
			mBinding.itemBarFl.setSelected(getAdapterPosition() == mPlaygroundListAdapter.mLastSelectedPosition);
			mBinding.loadingPb.setVisibility(View.GONE);
		}

		private void openItem() {
			if (getAdapterPosition() >= 0) {
				Playground playground = mPlaygroundListAdapter.mPlaygroundList.get(getAdapterPosition());
				EventBus.getDefault()
				        .post(new OpenPlaygroundEvent(playground, getAdapterPosition(), new WeakReference<>(itemView)));

				mPlaygroundListAdapter.notifySelectedItemChanged(getAdapterPosition());
			}
		}

		private void onViewRecycled() {
			if (mGoogleMap != null) {
				mGoogleMap.clear();
				mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NONE);

				mBinding.itemMapRecyclerview.onPause();
				mBinding.itemMapRecyclerview.onStop();
				mBinding.itemMapRecyclerview.onDestroy();
			}
			mBinding.locationRb.setRating(0f);
			mBinding.loadingPb.setVisibility(View.VISIBLE);
			mBinding.itemBarFl.setSelected(false);
		}

		@Override
		public void setRating(Rating rate) {
		}

		@Override
		public void setRating(float rate) {
			mBinding.locationRb.setRating(rate);
		}
	}
}
