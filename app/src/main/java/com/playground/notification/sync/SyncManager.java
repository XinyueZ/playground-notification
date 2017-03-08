package com.playground.notification.sync;

import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.chopping.application.LL;
import com.chopping.utils.Utils;
import com.playground.notification.R;
import com.playground.notification.app.App;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.ds.sync.Favorite;
import com.playground.notification.ds.sync.SyncPlayground;
import com.playground.notification.utils.PlaygroundIdUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;


public abstract class SyncManager<T extends SyncPlayground> {
	private List<T> mCachedList = new LinkedList<>();
	private boolean mInit;

	/**
	 * No one can create this class.
	 */
	protected SyncManager() {
	}


	public boolean isCached(Playground ground) {
		return ground != null && mCachedList.contains(ground);
	}


	@Nullable
	public T findInCache(Playground ground) {
		for (T cached : mCachedList) {
			if (cached.equals(ground)) {
				return cached;
			}
		}
		return null;
	}


	protected void add(T newT, ImageView v, View viewForSnack) {
		newT.setId(PlaygroundIdUtils.getId(newT));
		//Same bookmark should not be added again.
		if (mCachedList.contains(newT)) {
			return;
		}
		mCachedList.add(newT);
		if (v != null) {
			v.setImageResource(getAddedIcon());
			v.setEnabled(false);
		}
		addInternal(newT, v, viewForSnack);
	}


	private void addInternal(final T newT, final ImageView v, View viewForSnack) {
		final WeakReference<View> anchor = new WeakReference<>(viewForSnack);
		final WeakReference<ImageView> actionBtn = new WeakReference<>(v);
		newT.save(new SaveListener<String>() {
			@Override
			public void done(String s, BmobException exp) {
				if (exp != null) {
					onFailure(exp.toString());
					return;
				}

				View anchorV = anchor.get();
				View btn = actionBtn.get();
				if (anchorV != null) {
					Snackbar.make(anchorV, getAddSuccessText(), Snackbar.LENGTH_SHORT)
					        .show();
				}
				Utils.showShortToast(App.Instance, getAddSuccessText());
				if (btn != null) {
					btn.setEnabled(true);
				}
			}

			private void onFailure(String s) {
				LL.e("Cannot do add internal at addInternal(): " + (TextUtils.isEmpty(s) ?
				                                                    "." :
				                                                    s + "."));
				View anchorV = anchor.get();
				ImageView btn = actionBtn.get();
				if (anchorV != null) {
					Snackbar.make(anchorV, R.string.meta_load_error, Snackbar.LENGTH_LONG)
					        .setAction(R.string.btn_retry, new OnClickListener() {
						        @Override
						        public void onClick(View v) {
							        addInternal(newT, actionBtn.get(), anchor.get());
						        }
					        })
					        .show();
				}
				Utils.showLongToast(App.Instance, R.string.meta_load_error);
				if (btn != null) {
					btn.setEnabled(true);
					btn.setImageResource(getRemovedIcon());
				}
			}
		});
	}

	protected void remove(T oldT, ImageView v, View viewForSnack) {
		oldT.setId(PlaygroundIdUtils.getId(oldT));
		boolean isCached = isCached(oldT);
		if (isCached) {
			for (T cached : mCachedList) {
				if (cached.equals(oldT)) {
					mCachedList.remove(cached);
					break;
				}
			}
			if (v != null) {
				v.setImageResource(getRemovedIcon());
				v.setEnabled(false);
			}
			removeInternal(oldT, v, viewForSnack);
		}
	}


	private void removeInternal(final T syncPlayground, ImageView v, View viewForSnack) {
		final WeakReference<View> anchor = new WeakReference<>(viewForSnack);
		final WeakReference<ImageView> actionBtn = new WeakReference<>(v);
		syncPlayground.delete(new UpdateListener() {
			@Override
			public void done(BmobException exp) {
				if (exp != null) {
					onFailure(exp.toString());
					return;
				}
				View anchorV = anchor.get();
				View btn = actionBtn.get();
				if (anchorV != null) {
					Snackbar.make(anchorV, getRemoveSuccessText(), Snackbar.LENGTH_SHORT)
					        .show();
				}
				Utils.showShortToast(App.Instance, getRemoveSuccessText());
				if (btn != null) {
					btn.setEnabled(true);
				}
			}


			private void onFailure(String s) {
				LL.e("Cannot do removal internal at removeInternal():" + (TextUtils.isEmpty(s) ?
				                                                          "." :
				                                                          s + "."));
				View anchorV = anchor.get();
				ImageView btn = actionBtn.get();
				if (anchorV != null) {
					Snackbar.make(anchorV, R.string.meta_load_error, Snackbar.LENGTH_LONG)
					        .setAction(R.string.btn_retry, new OnClickListener() {
						        @Override
						        public void onClick(View v) {
							        removeInternal(syncPlayground, actionBtn.get(), anchor.get());
						        }
					        })
					        .show();
				}
				Utils.showLongToast(App.Instance, R.string.meta_load_error);
				if (btn != null) {
					btn.setEnabled(true);
					btn.setImageResource(getAddedIcon());
				}
			}
		});
	}

	/**
	 * Clean all tabs.
	 */
	public void clean() {
		mCachedList.clear();
	}


	/**
	 * @return All cached  {@link Favorite}s.
	 */
	public List<T> getCachedList() {
		return mCachedList;
	}

	protected abstract int getAddSuccessText();

	protected abstract int getRemoveSuccessText();

	protected abstract int getAddedIcon();

	protected abstract int getRemovedIcon();

	protected void setInit() {
		mInit = true;
	}

	public boolean isInit() {
		return mInit;
	}
}
