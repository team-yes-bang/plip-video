package com.plip.video.global.web;

/**
 * Gateway·프론트 Server Action 연동 HTTP 헤더.
 * <p>클라이언트는 {@code Authorization: Bearer}로 JWT를 전달하고,
 * Gateway가 검증 후 {@link #USER_UUID_HEADER}를 downstream에 주입한다.</p>
 */
public final class RequestHeaders {

	/** plip-gateway {@code UserUuidHeader#NAME} 와 동일해야 한다. */
	public static final String USER_UUID_HEADER = "X-User-UUID";

	/** Next.js Server Action 등 레거시 클라이언트 호환 */
	public static final String USER_UUID_LEGACY = "X-User-Uuid";

	/** plip-gateway가 JWT 검증 후 downstream에 서명하는 HMAC 헤더 */
	public static final String GATEWAY_SIGNATURE = "X-Gateway-Signature";

	/** HMAC canonical payload에 포함되는 epoch millis */
	public static final String GATEWAY_TIMESTAMP = "X-Gateway-Timestamp";

	private RequestHeaders() {
	}
}
