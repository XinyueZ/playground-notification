package com.playground.notification.app.adapters;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.location.Address;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import com.chopping.application.LL;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.playground.notification.R;
import com.playground.notification.app.App;
import com.playground.notification.bus.OpenPlaygroundEvent;
import com.playground.notification.bus.SelectedPinOpenEvent;
import com.playground.notification.databinding.ItemPlaygroundBinding;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.sync.Rating;
import com.playground.notification.utils.Prefs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.playground.notification.sync.RatingManager.RatingUI;
import static com.playground.notification.sync.RatingManager.showRatingSummaryOnLocation;
import static com.playground.notification.utils.Utils.setPlaygroundIcon;


/**
 * The adapter for {@link RecyclerView} in {@link com.playground.notification.app.fragments.PlaygroundListFragment}.
 *
 * @author Xinyue Zhao
 */
public final class PlaygroundListAdapter extends RecyclerView.Adapter<PlaygroundListAdapter.PlaygroundListAdapterViewHolder> {
	private static final int ITEM_LAYOUT = R.layout.item_playground_list;
	private final List<Playground> mPlaygroundList = new ArrayList<>();
	private int mLastSelectedPosition = Adapter.NO_SELECTION;

	private Playground mPlaygroundScrolledTo = null;
	private final LinearLayoutManager mLinearLayoutManager;

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
	 * Handler for {@link SelectedPinOpenEvent}.
	 *
	 * @param e Event {@link SelectedPinOpenEvent}.
	 */
	@SuppressWarnings("unused")
	public void onEvent(SelectedPinOpenEvent e) {
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
	                                                                                                  RatingUI {
		private final ItemPlaygroundBinding mBinding;
		private final PlaygroundListAdapter mPlaygroundListAdapter;
		private GoogleMap mGoogleMap;
		private static final Geocoder GEOCODER = new Geocoder(App.Instance, Locale.getDefault());

		private PlaygroundListAdapterViewHolder(PlaygroundListAdapter playgroundListAdapter, ItemPlaygroundBinding binding) {
			super(binding.getRoot());
			mPlaygroundListAdapter = playgroundListAdapter;
			mBinding = binding;
		}

		private void onBindViewHolder() {
			final ViewGroup.LayoutParams layoutParams = mBinding.itemMapview.getLayoutParams();
			layoutParams.width = (int) App.Instance.getListItemWidth();
			layoutParams.height = (int) App.Instance.getListItemHeight();

			mBinding.itemMapview.onCreate(null);
			mBinding.itemMapview.onStart();
			mBinding.itemMapview.onResume();
			mBinding.itemMapview.getMapAsync(this);

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

			showRatingSummaryOnLocation(playground, this);
			mGoogleMap = googleMap;

			setAddress(playground);

			googleMap.getUiSettings()
			         .setMapToolbarEnabled(false);
			googleMap.getUiSettings()
			         .setScrollGesturesEnabled(false);
			googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(playground.getPosition(), 16));
			MarkerOptions options = new MarkerOptions().position(playground.getPosition());
			setPlaygroundIcon(App.Instance, playground, options);
			googleMap.addMarker(options);
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

		private void setAddress(final Playground playground) {
			Observable.just(playground.getPosition())
			          .subscribeOn(Schedulers.newThread())
			          .map(new Function<LatLng, AddressLatLng>() {
				          @Override
				          public AddressLatLng apply(LatLng latLng) throws Exception {
					          Prefs prefs = Prefs.getInstance();
					          if (prefs.isGeocoded(latLng)) {
						          Address address = new Address(Locale.getDefault());
						          address.setAddressLine(0, prefs.getGeocodedLocation(latLng));
						          address.setAddressLine(1, null);
						          return new AddressLatLng(address, latLng);
					          } else {
						          try {
							          List<Address> fromLocation = GEOCODER.getFromLocation(latLng.latitude, latLng.longitude, 1);
							          if (fromLocation == null || fromLocation.size() <= 0) {
								          return new AddressLatLng(new Address(Locale.getDefault()), latLng);
							          } else {
								          return new AddressLatLng(fromLocation.get(0), latLng);
							          }
						          } catch (IOException e) {
							          return new AddressLatLng(new Address(Locale.getDefault()), latLng);
						          }
					          }
				          }
			          })
			          .observeOn(AndroidSchedulers.mainThread())
			          .subscribe(new Consumer<AddressLatLng>() {
				          @Override
				          public void accept(AddressLatLng addressLatLng) throws Exception {
					          Address address = addressLatLng.getAddress();
					          if (address.getAddressLine(0) == null) {
						          mBinding.setAddress(null);
					          } else {
						          mBinding.setAddress(address.getAddressLine(0) + (TextUtils.isEmpty(address.getAddressLine(1)) ?
						                                                           "" :
						                                                           "\n" + address.getAddressLine(1)));
						          Prefs.getInstance()
						               .setGeocodedLocation(addressLatLng.getLatLng(), mBinding.getAddress());
					          }
				          }
			          });
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

				mBinding.itemMapview.onPause();
				mBinding.itemMapview.onStop();
				mBinding.itemMapview.onDestroy();
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

	private static final class AddressLatLng {
		private final Address mAddress;
		private final LatLng mLatLng;

		private AddressLatLng(Address address, LatLng latLng) {
			mAddress = address;
			mLatLng = latLng;
		}

		private Address getAddress() {
			return mAddress;
		}

		private LatLng getLatLng() {
			return mLatLng;
		}
	}
}
