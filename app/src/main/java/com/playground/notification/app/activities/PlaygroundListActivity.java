package com.playground.notification.app.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.chopping.utils.Utils;
import com.playground.notification.R;
import com.playground.notification.app.App;
import com.playground.notification.app.fragments.AboutDialogFragment;
import com.playground.notification.app.fragments.PlaygroundListFragment;
import com.playground.notification.ds.grounds.Playground;
import com.playground.notification.utils.Prefs;

import java.io.Serializable;
import java.util.List;

/**
 * {@link PlaygroundListActivity} shows the list-mode of search result.
 * It works only for phone.
 *
 * @author Xinyue Zhao
 */
public final class PlaygroundListActivity extends AppBarActivity {

	private static final String EXTRAS_PLAYGROUND_LIST = PlaygroundListActivity.class.getName() + ".EXTRAS.playground.list";
	/**
	 * View's menu.
	 */
	private static final int MENU = R.menu.menu_list;

	private PlaygroundListFragment mPlaygroundListFragment;


	/**
	 * Show single instance of {@link PlaygroundListActivity}
	 *
	 * @param cxt            {@link Activity}.
	 * @param playgroundList A list of {@link Playground}.
	 */
	public static void showInstance(@NonNull Activity cxt, @Nullable List<? extends Playground> playgroundList) {
		if (playgroundList == null) {
			return;
		}
		Intent intent = new Intent(cxt, PlaygroundListActivity.class);
		intent.putExtra(EXTRAS_PLAYGROUND_LIST, (Serializable) playgroundList);
		ActivityCompat.startActivity(cxt, intent, Bundle.EMPTY);
	}

	@Override
	protected void setupContent(@NonNull FrameLayout contentLayout) {
		Intent intent = getIntent();
		if (intent == null) {
			return;
		}
		List<? extends Playground> playgroundList = (List<? extends Playground>) intent.getSerializableExtra(EXTRAS_PLAYGROUND_LIST);
		getSupportFragmentManager().beginTransaction()
		                           .replace(contentLayout.getId(), mPlaygroundListFragment = PlaygroundListFragment.newInstance(App.Instance, playgroundList))
		                           .commit();

		setHasShownDataOnUI(true);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		if (mPlaygroundListFragment != null) {
			List<? extends Playground> playgroundList = (List<? extends Playground>) intent.getSerializableExtra(EXTRAS_PLAYGROUND_LIST);
			mPlaygroundListFragment.refresh(playgroundList);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(MENU, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		//Share application.
		MenuItem menuAppShare = menu.findItem(R.id.action_share_app);
		android.support.v7.widget.ShareActionProvider provider = (android.support.v7.widget.ShareActionProvider) MenuItemCompat.getActionProvider(menuAppShare);
		String subject = getString(R.string.lbl_share_app_title);
		String text = getString(R.string.lbl_share_app_content,
		                        getString(R.string.application_name),
		                        Prefs.getInstance()
		                             .getAppDownloadInfo());
		provider.setShareIntent(Utils.getDefaultShareIntent(provider, subject, text));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_map_mode:
				supportFinishAfterTransition();
				break;
			case R.id.action_about:
				showDialogFragment(AboutDialogFragment.newInstance(this), null);
				break;
		}
		return super.onOptionsItemSelected(item);
	}


}
