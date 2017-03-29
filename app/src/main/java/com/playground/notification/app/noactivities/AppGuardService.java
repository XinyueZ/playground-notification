package com.playground.notification.app.noactivities;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.playground.notification.R;
import com.playground.notification.utils.NotifyUtils;


public class AppGuardService extends Service {
	private static final int ONGOING_NOTIFICATION_ID = 0x57;
	private boolean mReg = false;
	private int mJobId;
	private @Nullable JobManager mJobManager;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!mReg) {
			Notification notification = NotifyUtils.buildNotifyWithoutBigImage(this,
			                                                                   ONGOING_NOTIFICATION_ID,
			                                                                   getString(R.string.application_name),
			                                                                   getString(R.string.lbl_notify_content),
			                                                                   R.drawable.ic_balloon,
			                                                                   NotifyUtils.getAppHome(this),
			                                                                   false);
			startForeground(ONGOING_NOTIFICATION_ID, notification);
			mReg = true;
		}

		if (mJobManager == null) {
			prepareScheduleJob();
			mJobId = NotifyUserJob.scheduleJob();
		}
		return START_STICKY;
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		mReg = false;
		if (mJobManager != null) {
			mJobManager.cancel(mJobId);
			mJobManager.removeJobCreator(mJobCreator);
			mJobManager = null;
		}
	}


	/**
	 * Ready for a job that runs background automatically.
	 * See.
	 * <a href="https://github.com/evernote/android-job">android-job</a>
	 */
	private void prepareScheduleJob() {
		try {
			mJobManager = JobManager.instance();
		} catch (IllegalStateException e) {
			mJobManager = JobManager.create(this);
		}
		mJobManager.addJobCreator(mJobCreator);
	}

	/**
	 * Factory of a job that runs background automatically.
	 * See.
	 * <a href="https://github.com/evernote/android-job">android-job</a>
	 */
	private final JobCreator mJobCreator = new JobCreator() {
		@Override
		public Job create(String tag) {
			switch (tag) {
				case NotifyUserJob.TAG:
					return new NotifyUserJob(AppGuardService.this);
				default:
					return null;
			}
		}
	};
}
