package org.voidsink.anewjkuapp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.voidsink.anewjkuapp.base.BaseAsyncTask;
import org.voidsink.anewjkuapp.kusss.ExamGrade;
import org.voidsink.anewjkuapp.kusss.Grade;
import org.voidsink.anewjkuapp.kusss.GradeType;
import org.voidsink.anewjkuapp.kusss.KusssHandler;
import org.voidsink.anewjkuapp.notification.GradesChangedNotification;
import org.voidsink.anewjkuapp.notification.SyncNotification;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class ImportGradeTask extends BaseAsyncTask<Void, Void, Void> {

	private static final String TAG = ImportLvaTask.class.getSimpleName();

	private static final Object sync_lock = new Object();

	private ContentProviderClient mProvider;
	private Account mAccount;
	private SyncResult mSyncResult;
	private Context mContext;
	private ContentResolver mResolver;

	public static final String[] GRADE_PROJECTION = new String[] {
			KusssContentContract.Grade.GRADE_COL_ID,
			KusssContentContract.Grade.GRADE_COL_TERM,
			KusssContentContract.Grade.GRADE_COL_LVANR,
			KusssContentContract.Grade.GRADE_COL_DATE,
			KusssContentContract.Grade.GRADE_COL_SKZ,
			KusssContentContract.Grade.GRADE_COL_TYPE,
			KusssContentContract.Grade.GRADE_COL_GRADE,
			KusssContentContract.Grade.GRADE_COL_TITLE,
			KusssContentContract.Grade.GRADE_COL_CODE};

	// Constants representing column positions from PROJECTION.
	public static final int COLUMN_GRADE_ID = 0;
	public static final int COLUMN_GRADE_TERM = 1;
	public static final int COLUMN_GRADE_LVANR = 2;
	public static final int COLUMN_GRADE_DATE = 3;
	public static final int COLUMN_GRADE_SKZ = 4;
	public static final int COLUMN_GRADE_TYPE = 5;
	public static final int COLUMN_GRADE_GRADE = 6;
	public static final int COLUMN_GRADE_TITLE = 7;
	public static final int COLUMN_GRADE_CODE = 8;

	public ImportGradeTask(Account account, Context context) {
		this(account, null, null, null, null, context);
		this.mProvider = context.getContentResolver().acquireContentProviderClient(KusssContentContract.Exam.CONTENT_URI);
		this.mSyncResult = new SyncResult();
	}
	
	public ImportGradeTask(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult,
			Context context) {
		this.mAccount = account;
		this.mProvider = provider;
		this.mSyncResult = syncResult;
		this.mResolver = context.getContentResolver();
		this.mContext = context;
	}

	@Override
	protected Void doInBackground(Void... params) {
		Log.d(TAG, "Start importing grades");

		SyncNotification mSyncNotification = new SyncNotification(mContext,
				R.string.notification_sync_grade);
		mSyncNotification.show("Import Noten");
		GradesChangedNotification mGradeChangeNotification = new GradesChangedNotification(
				mContext);
		
		synchronized (sync_lock) {
			mSyncNotification.update("");
			mSyncNotification.update("Noten werden geladen");

			try {
				Log.d(TAG, "setup connection");

				if (KusssHandler.handler
						.isAvailable(
								this.mAccount.name,
								AccountManager.get(mContext).getPassword(
										this.mAccount))) {
					Log.d(TAG, "load grades");

					List<ExamGrade> grades = KusssHandler.handler.getGrades();
					if (grades == null) {
						mSyncResult.stats.numParseExceptions++;
					}
					Map<String, ExamGrade> gradeMap = new HashMap<String, ExamGrade>();
					for (ExamGrade grade : grades) {
						gradeMap.put(
								String.format("%s-%d", grade.getCode(),
										grade.getDate().getTime()), grade);
					}

					Log.d(TAG, String.format("got %s grades", grades.size()));

					mSyncNotification.update("Noten werden aktualisiert");

					ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

					Uri examUri = KusssContentContract.Grade.CONTENT_URI;
					Cursor c = mProvider.query(examUri, GRADE_PROJECTION, null,
							null, null);

					if (c != null) {
						Log.d(TAG, "Found " + c.getCount()
								+ " local entries. Computing merge solution...");
					} else {
						Log.w(TAG, "selection failed");
					}

					int gradeId;
					String gradeCode;
					Date gradeDate;
					GradeType gradeType;
					Grade gradeGrade;
					while (c.moveToNext()) {
						gradeId = c.getInt(COLUMN_GRADE_ID);
						gradeCode = c.getString(COLUMN_GRADE_CODE);
						gradeDate = new Date(c.getLong(COLUMN_GRADE_DATE));
						gradeType = GradeType.parseGradeType(c
								.getInt(COLUMN_GRADE_TYPE));
						gradeGrade = Grade.parseGradeType(c
								.getInt(COLUMN_GRADE_GRADE));

						ExamGrade grade = gradeMap.get(String.format("%s-%d",
								gradeCode, gradeDate.getTime()));
						if (grade != null) {
							gradeMap.remove(String.format("%s-%d", gradeCode,
									gradeDate.getTime()));
							// Check to see if the entry needs to be updated
							Uri existingUri = examUri.buildUpon()
									.appendPath(Integer.toString(gradeId))
									.build();
							Log.d(TAG, "Scheduling update: " + existingUri);

							if (!gradeType.equals(grade.getType())
									|| !gradeGrade.equals(grade.getGrade())) {
								mGradeChangeNotification.addUpdate(String
										.format("%s: %s", grade.getTitle(),
												mContext.getString(grade
														.getGrade()
														.getStringResID())));
							}

							batch.add(ContentProviderOperation
									.newUpdate(
											KusssContentContract
													.asEventSyncAdapter(
															existingUri,
															mAccount.name,
															mAccount.type))
									.withValue(
											KusssContentContract.Grade.GRADE_COL_ID,
											Integer.toString(gradeId))
									.withValues(grade.getContentValues())
									.build());
							mSyncResult.stats.numUpdates++;
						}
					}
					c.close();

					for (ExamGrade grade : gradeMap.values()) {
						batch.add(ContentProviderOperation
								.newInsert(
										KusssContentContract
												.asEventSyncAdapter(examUri,
														mAccount.name,
														mAccount.type))
								.withValues(grade.getContentValues()).build());
						Log.d(TAG, "Scheduling insert: " + grade.getTerm()
								+ " " + grade.getLvaNr());

						mGradeChangeNotification.addInsert(String.format(
								"%s: %s", grade.getTitle(), mContext
										.getString(grade.getGrade()
												.getStringResID())));

						mSyncResult.stats.numInserts++;
					}

					if (batch.size() > 0) {
						mSyncNotification.update("Noten werden gespeichert");

						Log.d(TAG, "Applying batch update");
						mProvider.applyBatch(batch);
						Log.d(TAG, "Notify resolver");
						mResolver.notifyChange(
								KusssContentContract.Grade.CONTENT_URI, null, // No
																				// local
																				// observer
								false); // IMPORTANT: Do not sync to network
					} else {
						Log.w(TAG, "No batch operations found! Do nothing");
					}
				} else {
					mSyncResult.stats.numAuthExceptions++;
				}

			} catch (Exception e) {
				Log.e(TAG, "import failed: " + e);
			} finally {
				mSyncNotification.cancel();
				mGradeChangeNotification.show();
				setImportDone(true);
			}
		}

		return null;
	}

}