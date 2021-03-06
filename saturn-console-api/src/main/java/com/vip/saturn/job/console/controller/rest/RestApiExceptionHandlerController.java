package com.vip.saturn.job.console.controller.rest;

import com.vip.saturn.job.console.domain.RestApiErrorResult;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import com.vip.saturn.job.console.exception.SaturnJobConsoleHttpException;
import com.vip.saturn.job.console.utils.SaturnConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Handler for RESTful API Exception. <p> Created by jeff.zhu on 26/05/2017.
 */
@ControllerAdvice
public class RestApiExceptionHandlerController {

	private static final Logger log = LoggerFactory.getLogger(RestApiExceptionHandlerController.class);

	private static final String NOT_EXISTED_PREFIX = "does not exists";

	@ExceptionHandler
	public ResponseEntity<Object> handleSaturnJobConsoleException(SaturnJobConsoleException e) {
		String message = e.getMessage();
		if (StringUtils.isBlank(message)) {
			message = e.toString();
		}

		if (message.contains(NOT_EXISTED_PREFIX)) {
			log.warn("Resource not found while calling REST API:" + message);
			return constructErrorResponse(message, HttpStatus.NOT_FOUND);
		} else if (message.startsWith(SaturnConstants.INVALID_PARAMETER_PREFIX)) {
			String tmpMsg = StringUtils.removeStart(message, SaturnConstants.INVALID_PARAMETER_PREFIX);
			log.warn("Bad request while calling REST API:" + tmpMsg);
			return constructErrorResponse(tmpMsg, HttpStatus.BAD_REQUEST);
		}

		log.error("Internal server error happens while calling REST API:" + message);
		return constructErrorResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler
	public ResponseEntity<Object> handleSaturnJobConsoleHttpException(SaturnJobConsoleHttpException e) {
		HttpHeaders httpHeaders = new HttpHeaders();

		int statusCode = e.getStatusCode();
		if (statusCode == HttpStatus.CREATED.value()) {
			return new ResponseEntity<>(httpHeaders, HttpStatus.CREATED);
		}

		HttpStatus httpStatus;
		try {
			httpStatus = HttpStatus.valueOf(statusCode);
		} catch (IllegalArgumentException e1) {
			httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		String message = e.getMessage();
		if (StringUtils.isBlank(message)) {
			message = e.toString();
		}

		if (httpStatus.is5xxServerError()) {
			log.error("Internal server error happens while calling REST API:" + message);
		} else {
			log.warn("Exception happens while calling REST API:" + message);
		}

		return constructErrorResponse(message, httpStatus);
	}

	@ExceptionHandler
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public RestApiErrorResult handleMessageNotReadableException(HttpMessageNotReadableException e) {
		return new RestApiErrorResult("The http message is not readable. Please check your request.");
	}

	@ExceptionHandler
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public RestApiErrorResult handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
		return new RestApiErrorResult("Method or argument not valid");
	}

	@ExceptionHandler
	@ResponseBody
	@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	public RestApiErrorResult handleMessageNotReadableException(HttpMediaTypeNotSupportedException e) {
		return new RestApiErrorResult("Media type not supported.");
	}

	private ResponseEntity<Object> constructErrorResponse(String errorMsg, HttpStatus status) {
		HttpHeaders httpHeaders = new HttpHeaders();

		RestApiErrorResult restApiErrorResult = new RestApiErrorResult();
		restApiErrorResult.setMessage(errorMsg);

		return new ResponseEntity<Object>(restApiErrorResult, httpHeaders, status);
	}
}
