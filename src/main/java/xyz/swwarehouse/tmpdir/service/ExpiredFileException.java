package xyz.swwarehouse.tmpdir.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.PRECONDITION_FAILED, reason="Expired File")
public class ExpiredFileException extends RuntimeException {

	public ExpiredFileException(String message) {
		super(message);
	}

	public ExpiredFileException(String message, Throwable cause) {
		super(message, cause);
	}
}
