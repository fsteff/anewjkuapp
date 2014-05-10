package org.voidsink.anewjkuapp.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.voidsink.anewjkuapp.ImportCalendarTask;
import org.voidsink.anewjkuapp.R;
import org.voidsink.anewjkuapp.activity.MainActivity;
import org.voidsink.anewjkuapp.base.BaseFragment;
import org.voidsink.anewjkuapp.calendar.CalendarContractWrapper;
import org.voidsink.anewjkuapp.calendar.CalendarEventAdapter;
import org.voidsink.anewjkuapp.calendar.CalendarListEvent;
import org.voidsink.anewjkuapp.calendar.CalendarListItem;
import org.voidsink.anewjkuapp.calendar.CalendarUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

public class CalendarFragment extends BaseFragment {

	private ListView mListView;
	private CalendarEventAdapter mAdapter;

	long now = 0, then = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_calendar, container,
				false);

		mListView = (ListView) view.findViewById(R.id.calendar_events);
		mAdapter = new CalendarEventAdapter(mContext);
		mListView.setAdapter(mAdapter);

		Button loadMore = (Button) inflater.inflate(R.layout.listview_footer_button, null);
		mListView.addFooterView(loadMore);
		
		loadMore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loadMoreData();
			}
		});
		loadMore.setClickable(true);
		
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init range
		now = System.currentTimeMillis();
		then = now + 14 * DateUtils.DAY_IN_MILLIS;

		loadData();
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.calendar, menu);
	}

	private void loadMoreData() {
		then += 31 * DateUtils.DAY_IN_MILLIS;
		loadData();
	}

	private void loadData() {
		new CalendarLoadTask().execute();
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	private class CalendarLoadTask extends AsyncTask<String, Void, Void> {
		private ProgressDialog progressDialog;
		private List<CalendarListItem> mEvents;
		private Map<String, Integer> mColors;

		@Override
		protected Void doInBackground(String... urls) {
			mEvents = new ArrayList<CalendarListItem>();

			// fetch calendar colors
			this.mColors = new HashMap<String, Integer>();
			ContentResolver cr = mContext.getContentResolver();
			Cursor c = cr
					.query(CalendarContractWrapper.Calendars.CONTENT_URI(),
							new String[] {
									CalendarContractWrapper.Calendars._ID(),
									CalendarContractWrapper.Calendars
											.CALENDAR_COLOR() }, null, null,
							null);
			while (c.moveToNext()) {
				this.mColors.put(c.getString(0), c.getInt(1));
			}
			c.close();

			Account mAccount = MainActivity.getAccount(mContext);
			if (mAccount != null) {
				AccountManager mAm = AccountManager.get(mContext);

				String calIDLva = mAm.getUserData(mAccount,
						CalendarUtils.ARG_CALENDAR_ID_LVA);
				String calIDExam = mAm.getUserData(mAccount,
						CalendarUtils.ARG_CALENDAR_ID_EXAM);

				cr = mContext.getContentResolver();
				c = cr.query(
						CalendarContractWrapper.Events.CONTENT_URI(),
						ImportCalendarTask.EVENT_PROJECTION,
						"(" + CalendarContractWrapper.Events.CALENDAR_ID()
								+ " = ? or "
								+ CalendarContractWrapper.Events.CALENDAR_ID()
								+ " = ? ) and "
								+ CalendarContractWrapper.Events.DTEND()
								+ " >= ? and "
								+ CalendarContractWrapper.Events.DTSTART()
								+ " <= ? and "
								+ CalendarContractWrapper.Events.DELETED()
								+ " != 1", new String[] { calIDExam, calIDLva,
								Long.toString(now), Long.toString(then) },
						CalendarContractWrapper.Events.DTSTART() + " ASC");

				if (c != null) {
					while (c.moveToNext()) {
						mEvents.add(new CalendarListEvent(
								mColors.get(c
										.getString(ImportCalendarTask.COLUMN_EVENT_CAL_ID)),
								c.getString(ImportCalendarTask.COLUMN_EVENT_TITLE),
								c.getString(ImportCalendarTask.COLUMN_EVENT_LOCATION),
								c.getLong(ImportCalendarTask.COLUMN_EVENT_DTSTART),
								c.getLong(ImportCalendarTask.COLUMN_EVENT_DTEND)));

					}
					c.close();
				}
			}

			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(mContext,
					getString(R.string.progress_title),
					getString(R.string.progress_load_calendar), true);
		}

		@Override
		protected void onPostExecute(Void result) {
			mAdapter.clear();
			mAdapter.addAll(CalendarEventAdapter.insertSections(mEvents));
			progressDialog.dismiss();
			super.onPostExecute(result);
		}

	}

}