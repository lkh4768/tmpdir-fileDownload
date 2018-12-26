package xyz.swwarehouse.tmpdir.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Not found ID")
public class NotFoundIDException extends RuntimeException {

	public NotFoundIDException(String message) {
		super(message);
	}

	public NotFoundIDException(String message, Throwable cause) {
		super(message, cause);
	}
}
