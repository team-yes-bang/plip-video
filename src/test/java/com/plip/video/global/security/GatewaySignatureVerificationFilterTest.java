package com.plip.video.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.plip.video.global.config.GatewayHmacProperties;
import com.plip.video.global.web.RequestHeaders;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class GatewaySignatureVerificationFilterTest {

	private static final String SECRET = "phase5-gateway-hmac-secret";
	private static final String USER_UUID = "0195aaaa-bbbb-7ccc-dddd-eeeeeeeeeeee";

	@Mock
	private FilterChain filterChain;

	private GatewaySignatureVerificationFilter filter;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		filter = new GatewaySignatureVerificationFilter(new GatewayHmacProperties(SECRET, 300L));
		response = new MockHttpServletResponse();
	}

	@Test
	void acceptsValidSignature() throws Exception {
		long timestamp = System.currentTimeMillis();
		String path = "/api/v1/videos";
		String signature = GatewayHmacVerifier.sign(
				SECRET,
				GatewayHmacVerifier.buildCanonicalPayload(timestamp, "GET", path, USER_UUID)
		);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		request.addHeader(RequestHeaders.USER_UUID_HEADER, USER_UUID);
		request.addHeader(RequestHeaders.GATEWAY_TIMESTAMP, Long.toString(timestamp));
		request.addHeader(RequestHeaders.GATEWAY_SIGNATURE, signature);

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(200);
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void rejectsSpoofedSignature() throws Exception {
		long timestamp = System.currentTimeMillis();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/videos");
		request.addHeader(RequestHeaders.USER_UUID_HEADER, USER_UUID);
		request.addHeader(RequestHeaders.GATEWAY_TIMESTAMP, Long.toString(timestamp));
		request.addHeader(RequestHeaders.GATEWAY_SIGNATURE, "deadbeef");

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(401);
		verify(filterChain, never()).doFilter(request, response);
	}

	@Test
	void skipsVerificationWhenSecretDisabled() throws Exception {
		filter = new GatewaySignatureVerificationFilter(new GatewayHmacProperties("", 300L));
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/videos");

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
	}

}
