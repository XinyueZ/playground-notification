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

import static android.support.v7.widget.RecyclerView.NO_POSITION;

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


	/**
	 * Show single instance of {@link PlaygroundListActivity}
	 *
	 * @param cxt            {@link Activity}.
	 * @param playgroundList A list of {@link Playground}.
	 * @param menuItem       The menu to start this {@link Activity}.
	 */
	public static void showInstance(@NonNull Activity cxt, @Nullable List<? extends Playground> playgroundList, @Nullable MenuItem menuItem) {
		if (playgroundList == null) {
			return;
		}
		Intent intent = new Intent(cxt, PlaygroundListActivity.class);
		intent.putExtra(EXTRAS_PLAYGROUND_LIST, (Serializable) playgroundList);
		intent.putExtra(EXTRAS_MENU_ITEM,
		                menuItem == null ?
		                NO_POSITION :
		                menuItem.getItemId());
		ActivityCompat.startActivityForResult(cxt, intent, REQ_APP_ACTIVITY, Bundle.EMPTY);
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//When this activity returns, the parent's menu-item must selected again.
		int menuItem = getIntent().getIntExtra(EXTRAS_MENU_ITEM, NO_POSITION);
		Intent result = new Intent();
		result.putExtra(RES_APP_ACTIVITY, menuItem);
		setResult(RESULT_OK, result);
	}

	@Override
	protected void setupContent(@NonNull FrameLayout contentLayout) {
		Intent intent = getIntent();
		if (intent == null) {
			return;
		}
		List<? extends Playground> playgroundList = (List<? extends Playground>) intent.getSerializableExtra(EXTRAS_PLAYGROUND_LIST);
		getSupportFragmentManager().beginTransaction()
		                           .replace(contentLayout.getId(), PlaygroundListFragment.newInstance(App.Instance, playgroundList))
		                           .commit();

		setHasShownDataOnUI(true);
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
				MapActivity.showInstance(this);
				break;
			case R.id.action_about:
				showDialogFragment(AboutDialogFragment.newInstance(this), null);
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
