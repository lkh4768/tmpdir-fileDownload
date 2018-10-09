package xyz.swwarehouse.tmpdir.entity;

import java.util.Date;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "fileinfos")
public class FileInfo {
	@Field("id")
	private String id;
	private Date submissionTime;
	private Date expireTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getSubmissionTime() {
		return submissionTime;
	}

	public void setSubmissionTime(Date submissionTime) {
		this.submissionTime = submissionTime;
	}

	public long getExpireTime() {
		return expireTime.getTime();
	}

	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public String toString() {
		return "id: " + this.id + ", submisstionTime: " + this.submissionTime + ", expireTime: " + this.expireTime;
	}
}
