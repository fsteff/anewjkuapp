package org.voidsink.anewjkuapp.update;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.voidsink.anewjkuapp.KusssContentContract;
import org.voidsink.anewjkuapp.R;
import org.voidsink.anewjkuapp.base.BaseAsyncTask;
import org.voidsink.anewjkuapp.kusss.Course;
import org.voidsink.anewjkuapp.kusss.KusssHandler;
import org.voidsink.anewjkuapp.kusss.KusssHelper;
import org.voidsink.anewjkuapp.kusss.Term;
import org.voidsink.anewjkuapp.notification.SyncNotification;
import org.voidsink.anewjkuapp.provider.KusssContentProvider;
import org.voidsink.anewjkuapp.utils.Analytics;
import org.voidsink.anewjkuapp.utils.AppUtils;
import org.voidsink.anewjkuapp.utils.Consts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportLvaTask extends BaseAsyncTask<Void, Void, Void> {

    private static final String TAG = ImportLvaTask.class.getSimpleName();

    private static final Object sync_lock = new Object();

    private ContentProviderClient mProvider;
    private Account mAccount;
    private SyncResult mSyncResult;
    private Context mContext;
    private ContentResolver mResolver;

    private boolean mShowProgress;
    private SyncNotification mUpdateNotification;

    public static final String[] LVA_PROJECTION = new String[]{
            KusssContentContract.Lva.LVA_COL_ID,
            KusssContentContract.Lva.LVA_COL_TERM,
            KusssContentContract.Lva.LVA_COL_LVANR,
            KusssContentContract.Lva.LVA_COL_TITLE,
            KusssContentContract.Lva.LVA_COL_SKZ,
            KusssContentContract.Lva.LVA_COL_TYPE,
            KusssContentContract.Lva.LVA_COL_TEACHER,
            KusssContentContract.Lva.LVA_COL_SWS,
            KusssContentContract.Lva.LVA_COL_ECTS,
            KusssContentContract.Lva.LVA_COL_CODE};

    public static final int COLUMN_LVA_ID = 0;
    public static final int COLUMN_LVA_TERM = 1;
    public static final int COLUMN_LVA_LVANR = 2;
    public static final int COLUMN_LVA_TITLE = 3;
    public static final int COLUMN_LVA_SKZ = 4;
    public static final int COLUMN_LVA_TYPE = 5;
    public static final int COLUMN_LVA_TEACHER = 6;
    public static final int COLUMN_LVA_SWS = 7;
    public static final int COLUMN_LVA_ECTS = 8;
    public static final int COLUMN_LVA_CODE = 9;

    public ImportLvaTask(Account account, Context context) {
        this(account, null, null, null, null, context);
        this.mProvider = context.getContentResolver()
                .acquireContentProviderClient(
                        KusssContentContract.Lva.CONTENT_URI);
        this.mSyncResult = new SyncResult();
        this.mShowProgress = true;
    }

    public ImportLvaTask(Account account, Bundle extras, String authority,
                         ContentProviderClient provider, SyncResult syncResult,
                         Context context) {
        this.mAccount = account;
        this.mProvider = provider;
        this.mSyncResult = syncResult;
        this.mResolver = context.getContentResolver();
        this.mContext = context;
        this.mShowProgress = (extras != null && extras.getBoolean(Consts.SYNC_SHOW_PROGRESS, false));
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Log.d(TAG, "prepare importing LVA");

        if (mShowProgress) {
            mUpdateNotification = new SyncNotification(mContext,
                    R.string.notification_sync_lva);
            mUpdateNotification.show(mContext.getString(R.string.notification_sync_lva_loading));
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.d(TAG, "Start importing LVA");

        synchronized (sync_lock) {
            try {
                Log.d(TAG, "setup connection");

                updateNotify(mContext.getString(R.string.notification_sync_connect));

                if (KusssHandler.getInstance().isAvailable(mContext,
                        AppUtils.getAccountAuthToken(mContext, mAccount),
                        AppUtils.getAccountName(mContext, mAccount),
                        AppUtils.getAccountPassword(mContext, mAccount))) {

                    updateNotify(mContext.getString(R.string.notification_sync_lva_loading));

                    Log.d(TAG, "load lvas");

                    List<Term> terms = KusssContentProvider.getTerms(mContext);
                    List<Course> courses = KusssHandler.getInstance().getLvas(mContext, terms);
                    if (courses == null) {
                        mSyncResult.stats.numParseExceptions++;
                    } else {
                        Map<String, Course> lvaMap = new HashMap<>();
                        for (Course course : courses) {
                            lvaMap.put(KusssHelper.getLvaKey(course.getTerm(), course.getLvaNr()), course);
                        }
                        Map<String, Term> termMap = new HashMap<>();
                        for (Term term : terms) {
                            termMap.put(term.getTerm(), term);
                        }

                        Log.d(TAG, String.format("got %s lvas", courses.size()));

                        updateNotify(mContext.getString(R.string.notification_sync_lva_updating));

                        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

                        Uri lvaUri = KusssContentContract.Lva.CONTENT_URI;
                        Cursor c = mProvider.query(lvaUri, LVA_PROJECTION,
                                null, null, null);

                        if (c == null) {
                            Log.w(TAG, "selection failed");
                        } else {
                            Log.d(TAG,
                                    "Found "
                                            + c.getCount()
                                            + " local entries. Computing merge solution...");

                            int lvaId;
                            String lvaTerm;
                            String lvaNr;

                            while (c.moveToNext()) {
                                lvaId = c.getInt(COLUMN_LVA_ID);
                                lvaTerm = c.getString(COLUMN_LVA_TERM);
                                lvaNr = c.getString(COLUMN_LVA_LVANR);

                                // update only lvas from loaded terms, ignore all other
                                Term term = termMap.get(lvaTerm);
                                if (term != null && term.isLoaded()) {
                                    Course course = lvaMap
                                            .get(KusssHelper.getLvaKey(lvaTerm, lvaNr));
                                    if (course != null) {
                                        lvaMap.remove(KusssHelper.getLvaKey(lvaTerm, lvaNr));
                                        // Check to see if the entry needs to be
                                        // updated
                                        Uri existingUri = lvaUri
                                                .buildUpon()
                                                .appendPath(Integer.toString(lvaId))
                                                .build();
                                        Log.d(TAG, "Scheduling update: "
                                                + existingUri);

                                        batch.add(ContentProviderOperation
                                                .newUpdate(
                                                        KusssContentContract
                                                                .asEventSyncAdapter(
                                                                        existingUri,
                                                                        mAccount.name,
                                                                        mAccount.type))
                                                .withValue(
                                                        KusssContentContract.Lva.LVA_COL_ID,
                                                        Integer.toString(lvaId))
                                                .withValues(KusssHelper.getLvaContentValues(course))
                                                .build());
                                        mSyncResult.stats.numUpdates++;
                                    } else {
                                        // delete
                                        Log.d(TAG,
                                                "delete: "
                                                        + KusssHelper.getLvaKey(lvaTerm, lvaNr));
                                        // Entry doesn't exist. Remove only
                                        // newer
                                        // events from the database.
                                        Uri deleteUri = lvaUri
                                                .buildUpon()
                                                .appendPath(Integer.toString(lvaId))
                                                .build();
                                        Log.d(TAG, "Scheduling delete: "
                                                + deleteUri);

                                        batch.add(ContentProviderOperation
                                                .newDelete(
                                                        KusssContentContract
                                                                .asEventSyncAdapter(
                                                                        deleteUri,
                                                                        mAccount.name,
                                                                        mAccount.type))
                                                .build());
                                        mSyncResult.stats.numDeletes++;
                                    }
                                } else {
                                    mSyncResult.stats.numSkippedEntries++;
                                }
                            }
                            c.close();

                            for (Course course : lvaMap.values()) {
                                // insert only lvas from loaded terms, ignore all other
                                Term term = termMap.get(course.getTerm());
                                if (term != null && term.isLoaded()) {
                                    batch.add(ContentProviderOperation
                                            .newInsert(
                                                    KusssContentContract
                                                            .asEventSyncAdapter(
                                                                    lvaUri,
                                                                    mAccount.name,
                                                                    mAccount.type))
                                            .withValues(KusssHelper.getLvaContentValues(course))
                                            .build());
                                    Log.d(TAG,
                                            "Scheduling insert: " + course.getTerm()
                                                    + " " + course.getLvaNr());
                                    mSyncResult.stats.numInserts++;
                                } else {
                                    mSyncResult.stats.numSkippedEntries++;
                                }
                            }

                            if (batch.size() > 0) {
                                updateNotify(mContext.getString(R.string.notification_sync_lva_saving));

                                Log.d(TAG, "Applying batch update");
                                mProvider.applyBatch(batch);
                                Log.d(TAG, "Notify resolver");
                                mResolver
                                        .notifyChange(
                                                KusssContentContract.Lva.CONTENT_CHANGED_URI,
                                                null, // No
                                                // local
                                                // observer
                                                false); // IMPORTANT: Do not
                                // sync to
                                // network
                            } else {
                                Log.w(TAG,
                                        "No batch operations found! Do nothing");
                            }
                        }
                    }
                } else {
                    mSyncResult.stats.numAuthExceptions++;
                }
            } catch (Exception e) {
                Analytics.sendException(mContext, e, true);
                Log.e(TAG, "import failed", e);
            }
        }

        setImportDone();

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        if (mUpdateNotification != null) {
            mUpdateNotification.cancel();
        }
    }

    private void updateNotify(String string) {
        if (mUpdateNotification != null) {
            mUpdateNotification.update(string);
        }
    }
}
