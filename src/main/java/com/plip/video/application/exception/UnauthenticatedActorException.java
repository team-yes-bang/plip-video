package com.plip.video.application.exception;

public class UnauthenticatedActorException extends RuntimeException {

	public UnauthenticatedActorException() {
		super("인증이 필요합니다.");
	}
}
