package xyz.swwarehouse.tmpdir.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class FileInfo {
	@Id
	private String id;
	private long submissionTime;
	private long expireTime;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getSubmissionTime() {
		return submissionTime;
	}

	public void setSubmissionTime(long submissionTime) {
		this.submissionTime = submissionTime;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public String toString() {
		return "id: " + this.id + ", submisstionTime: " + new Date(this.submissionTime) + ", expireTime: " + new Date(this.expireTime);
	}
}
