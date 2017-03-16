package com.playground.notification.app.fragments;

import android.app.Activity;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.playground.notification.R;
import com.playground.notification.app.App;
import com.playground.notification.app.adapters.PlaygroundListAdapter;
import com.playground.notification.bus.BackPressedEvent;
import com.playground.notification.bus.ListDetailClosedEvent;
import com.playground.notification.bus.ListDetailShownEvent;
import com.playground.notification.bus.OpenPlaygroundEvent;
import com.playground.notification.databinding.PlaygroundListBinding;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ui.ib.IBBackgroundRecyclerView;
import com.playground.notification.ui.ib.IBLayoutBase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Show list of {@link com.playground.notification.ds.grounds.Playground}s.
 *
 * @author Xinyue Zhao
 */
public final class PlaygroundListFragment extends AppFragment implements GoogleMap.OnCameraMoveStartedListener {
	private static final String EXTRAS_PLAYGROUND_LIST = PlaygroundListFragment.class.getName() + ".EXTRAS.playground.list";
	private static final int LAYOUT = R.layout.fragment_playground_list;
	private PlaygroundListBinding mBinding;
	private PlaygroundListAdapter mPlaygroundListAdapter;

	//------------------------------------------------
	//Subscribes, event-handlers
	//------------------------------------------------

	/**
	 * Handler for {@link com.playground.notification.bus.OpenPlaygroundEvent}.
	 *
	 * @param e Event {@link com.playground.notification.bus.OpenPlaygroundEvent}.
	 */
	public void onEvent(OpenPlaygroundEvent e) {
		Activity activity = getActivity();
		if (activity == null) {
			return;
		}
		if (e.getSelectedV() != null) {
			mBinding.playgroundListRv.setSelectedPosition(e.getPosition(), e.getPosition());
			mBinding.playgroundDetailContainerIbLayout.openWithAnim(e.getSelectedV()
			                                                         .get());
			Playground playground = e.getPlayground();
			Location location = App.Instance.getCurrentLocation();
			LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
			getChildFragmentManager().beginTransaction()
			                         .replace(R.id.playground_detail_container, PlaygroundListItemDetailFragment.newInstance(activity, currentLatLng.latitude, currentLatLng.longitude, playground))
			                         .addToBackStack(null)
			                         .commit();
		}
	}

	/**
	 * Handler for {@link BackPressedEvent}.
	 *
	 * @param e Event {@link BackPressedEvent}.
	 */
	public void onEvent(BackPressedEvent e) {
		getChildFragmentManager().popBackStack();
		mBinding.playgroundDetailContainerIbLayout.close();
	}
	//------------------------------------------------

	public static PlaygroundListFragment newInstance(Context cxt, List<? extends Playground> playgroundList) {
		Bundle args = new Bundle();
		args.putSerializable(EXTRAS_PLAYGROUND_LIST, (Serializable) playgroundList);
		return (PlaygroundListFragment) PlaygroundListFragment.instantiate(cxt, PlaygroundListFragment.class.getName(), args);
	}


	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		mBinding = DataBindingUtil.inflate(inflater, LAYOUT, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mBinding.rl.setColorSchemeResources(R.color.primary_color, R.color.primary_dark_color, R.color.brown_500, R.color.yellow, R.color.common_blue_50);
		mBinding.rl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				if (mPlaygroundListAdapter != null) {
					mPlaygroundListAdapter.notifyDataSetChanged();
					mBinding.rl.setRefreshing(false);
				}
			}
		});
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
		mBinding.playgroundListRv.setLayoutManager(linearLayoutManager);
		mBinding.playgroundListRv.setHasFixedSize(true);
		if (getArguments() != null) {
			mBinding.playgroundListRv.setAdapter(mPlaygroundListAdapter = new PlaygroundListAdapter(linearLayoutManager,
			                                                                                        (List<? extends Playground>) getArguments().getSerializable(EXTRAS_PLAYGROUND_LIST)));
		} else {
			mBinding.playgroundListRv.setAdapter(mPlaygroundListAdapter = new PlaygroundListAdapter(linearLayoutManager, new ArrayList<Playground>()));
		}
		mBinding.playgroundListRv.addOnScrollListener(mPlaygroundListAdapter.recyclerViewOnScrollListener);
		final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
		dividerItemDecoration.setDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.divider_drawable));
		mBinding.playgroundListRv.addItemDecoration(dividerItemDecoration);

		mBinding.playgroundListRv.setCallback(new IBBackgroundRecyclerView.Callback() {
			@Override
			public void onClose(IBBackgroundRecyclerView v) {
				EventBus.getDefault()
				        .post(new ListDetailClosedEvent());
			}

			@Override
			public void onOpen(IBBackgroundRecyclerView v) {
				EventBus.getDefault()
				        .post(new ListDetailShownEvent());
			}

		});
		mBinding.playgroundDetailContainerIbLayout.setIBBackground(mBinding.playgroundListRv);
		mBinding.playgroundDetailContainerIbLayout.setCloseDistance((int) getResources().getDimension(R.dimen.drag_close_distance));
		mBinding.playgroundDetailContainerIbLayout.setOnDragStateChangeListener(new IBLayoutBase.OnDragStateChangeListener() {
			@Override
			public void dragStateChange(IBLayoutBase.DragState state) {
				switch (state) {
					case CANCLOSE:
						//..
						break;
					case CANNOTCLOSE:
						//..
						break;
				}
			}
		});

	}

	public void refresh(List<? extends Playground> data) {
		mPlaygroundListAdapter.refresh(data);
	}

	@Override
	public void onResume() {
		super.onResume();
		EventBus.getDefault()
		        .register(mPlaygroundListAdapter);
	}

	@Override
	public void onPause() {
		EventBus.getDefault()
		        .unregister(mPlaygroundListAdapter);
		super.onPause();
	}

	@Override
	public void onDestroyView() {
		if (mPlaygroundListAdapter != null) {
			mBinding.playgroundListRv.removeOnScrollListener(mPlaygroundListAdapter.recyclerViewOnScrollListener);
		}
		super.onDestroyView();
	}

	@Override
	public void onCameraMoveStarted(int i) {
		if (mBinding.playgroundListRv.isOpened()) {
			mBinding.playgroundDetailContainerIbLayout.close();
		}
	}
}
