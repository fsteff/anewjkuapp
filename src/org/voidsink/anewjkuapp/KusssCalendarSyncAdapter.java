package org.voidsink.anewjkuapp;

import net.fortuna.ical4j.data.CalendarBuilder;

import org.voidsink.anewjkuapp.calendar.CalendarUtils;
import org.voidsink.anewjkuapp.kusss.KusssHandler;
import org.voidsink.anewjkuapp.notification.CalendarChangedNotification;
import org.voidsink.anewjkuapp.notification.KusssNotificationBuilder;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

public class KusssCalendarSyncAdapter extends AbstractThreadedSyncAdapter {

	private static final String TAG = KusssCalendarSyncAdapter.class
			.getSimpleName();

	// Global variables
	// Define a variable to contain a content resolver instance
	private boolean mSyncCancled;
	private Context mContext;
	private CalendarBuilder mCalendarBuilder;

	/**
	 * Set up the sync adapter
	 */
	public KusssCalendarSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		/*
		 * If your app uses a content resolver, get an instance of it from the
		 * incoming Context
		 */
		this.mSyncCancled = false;
		this.mContext = context;
		this.mCalendarBuilder = new CalendarBuilder(); // must create in main
														// thread, intialization
														// crashes in Tasks
	}

	/**
	 * Set up the sync adapter. This form of the constructor maintains
	 * compatibility with Android 3.0 and later platform versions
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public KusssCalendarSyncAdapter(Context context, boolean autoInitialize,
			boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
		/*
		 * If your app uses a content resolver, get an instance of it from the
		 * incoming Context
		 */
		this.mSyncCancled = false;
		this.mContext = context;
		this.mCalendarBuilder = new CalendarBuilder();
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {

		if (account == null || account.name == null) {
			KusssNotificationBuilder.showErrorNotification(mContext,
					R.string.notification_error_account_is_null, null);
			syncResult.stats.numAuthExceptions++;
			return;
		}

		final AccountManager am = AccountManager.get(getContext());
		// Lets give another try to authenticate the user
		final String password = am.getPassword(account);
		
		Log.d(TAG, "starting sync of account: " + account.name);

		if (!KusssHandler.handler.isAvailable(account.name, password)) {
			KusssNotificationBuilder.showErrorNotification(mContext,
					R.string.notification_error_account_not_available, null);
			syncResult.stats.numAuthExceptions++;
			return;
		}

		CalendarChangedNotification mNotification = new CalendarChangedNotification(mContext);
		try {
			Looper.prepare();
			
			Log.d(TAG, "importing: " + CalendarUtils.ARG_CALENDAR_ID_EXAM);

			ImportCalendarTask task = new ImportCalendarTask(account, extras,
					authority, provider, syncResult, getContext(),
					CalendarUtils.ARG_CALENDAR_ID_EXAM, mCalendarBuilder, mNotification);
			task.execute();
			while (!task.isDone() && !mSyncCancled) {
				try {
					Thread.sleep(600);
				} catch (Exception e) {
				}
				if (mSyncCancled) {
					task.cancel(true);
				}
			}

			Log.d(TAG, "importing: " + CalendarUtils.ARG_CALENDAR_ID_LVA);

			task = new ImportCalendarTask(account, extras, authority, provider,
					syncResult, getContext(),
					CalendarUtils.ARG_CALENDAR_ID_LVA, mCalendarBuilder, mNotification);

			task.execute();
			while (!task.isDone() && !mSyncCancled) {
				try {
					Thread.sleep(600);
				} catch (Exception e) {
				}
				if (mSyncCancled) {
					task.cancel(true);
				}
			}
		} catch (Exception e) {
			KusssNotificationBuilder.showErrorNotification(mContext,
					R.string.notification_error, e);
		} finally {
			mNotification.show();
		}
	}

	@Override
	public void onSyncCanceled() {
		Log.d(TAG, "Canceled Sync");
		mSyncCancled = true;
	}

}