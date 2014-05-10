package org.voidsink.anewjkuapp;

import java.util.Date;

import org.voidsink.anewjkuapp.kusss.LVA;

import android.database.Cursor;

public class ExamListExam implements ExamListItem {

	private int lvaNr;
	private String info;
	private String title;
	private String description;
	private String term;
	private int skz;
	private Date date;
	private String time;
	private String location;

	public ExamListExam(Cursor c, LvaMap map) {
		this.lvaNr = c.getInt(ImportExamTask.COLUMN_EXAM_LVANR);
		this.info = c.getString(ImportExamTask.COLUMN_EXAM_INFO);
		this.description = c.getString(ImportExamTask.COLUMN_EXAM_DESCRIPTION);
		this.term = c.getString(ImportExamTask.COLUMN_EXAM_TERM);
		this.date = new Date(c.getLong(ImportExamTask.COLUMN_EXAM_DATE));
		this.time = c.getString(ImportExamTask.COLUMN_EXAM_TIME);
		this.location = c.getString(ImportExamTask.COLUMN_EXAM_LOCATION);
		LVA lva = map.getLVA(this.term, this.lvaNr);
		this.title = lva.getTitle();
		this.skz = lva.getSKZ();
	}
	
	@Override
	public int getType() {
		return EXAM_TYPE;
	}

	@Override
	public boolean mark() {
		return !this.info.isEmpty();
	}

	@Override
	public boolean isExam() {
		return true;
	}

	public String getTitle() {
		return this.title;
	}

	public String getDescription() {
		return this.description;
	}

	public String getInfo() {
		return this.info;
	}

	public int getLvaNr() {
		return this.lvaNr;
	}

	public String getTerm() {
		return this.term;
	}

	public int getSkz() {
		return this.skz;
	}

	public Date getDate() {
		return this.date;
	}

	public String getTime() {
		return this.time;
	}

	public String getLocation() {
		return this.location;
	}

}