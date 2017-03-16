package com.playground.notification.app.fragments;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chopping.application.LL;
import com.playground.notification.R;
import com.playground.notification.databinding.RatingDialogBinding;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.sync.Rating;
import com.playground.notification.utils.PlaygroundIdUtils;
import com.playground.notification.utils.Prefs;

import java.io.Serializable;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;

/**
 * Created by xzhao on 16.03.17.
 */ //A dialog to update current rating status of a ground for you.
public final class RatingDialogFragment extends AppCompatDialogFragment {
	/**
	 * Data-binding.
	 */
	private RatingDialogBinding mBinding;

	public static RatingDialogFragment newInstance(Context cxt, Playground playground, Rating rating) {
		Bundle args = new Bundle();
		args.putSerializable("rating", (Serializable) rating);
		args.putSerializable("ground", (Serializable) playground);
		return (RatingDialogFragment) RatingDialogFragment.instantiate(cxt, RatingDialogFragment.class.getName(), args);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_rating, container, false);
		return mBinding.getRoot();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getDialog().setTitle(R.string.lbl_rating);
		Rating rating = ((Rating) getArguments().getSerializable("rating"));
		if (rating != null) {
			mBinding.setRating(rating);
		}
		view.findViewById(R.id.close_iv)
		    .setOnClickListener(new View.OnClickListener() {
			    @Override
			    public void onClick(View v) {
				    dismiss();
				    Playground playground = (Playground) getArguments().getSerializable("ground");
				    playground.setId(PlaygroundIdUtils.getId(playground));
				    Rating rating = ((Rating) getArguments().getSerializable("rating"));
				    if (rating == null) {
					    Rating newRating = new Rating(Prefs.getInstance()
					                                       .getGoogleId(), playground);
					    newRating.setValue(mBinding.locationRb.getRating());
					    newRating.save(new SaveListener<String>() {
						    @Override
						    public void done(String s, BmobException exp) {
							    if (exp != null) {
								    LL.d("newRating failed");
								    return;
							    }
							    LL.d("newRating success");
						    }
					    });
				    } else {
					    Rating updateRating = new Rating(Prefs.getInstance()
					                                          .getGoogleId(), playground);
					    updateRating.setValue(mBinding.locationRb.getRating());
					    updateRating.update(rating.getObjectId(), new UpdateListener() {
						    @Override
						    public void done(BmobException exp) {
							    if (exp != null) {
								    LL.d("updateRating failed");
								    return;
							    }
							    LL.d("updateRating success");
						    }
					    });
				    }
			    }
		    });
	}
}
