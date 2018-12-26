package xyz.swwarehouse.tmpdir.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import xyz.swwarehouse.tmpdir.service.NotFoundIDException;

public class ExceptionController {

	public ResponseEntity handleNotFoundIDException(NotFoundIDException e) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not Found ID");
	}
}
