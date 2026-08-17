package com.plip.video.global.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static com.plip.video.global.config.InternalApiKeyFilter.HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

class InternalApiKeyFilterTest {

	private InternalApiKeyFilter filter;
	private MockHttpServletResponse response;
	private MockFilterChain filterChain;

	@BeforeEach
	void setUp() {
		filter = new InternalApiKeyFilter(new InternalProperties("secret-key"));
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();
	}

	@Test
	void allowsInternalRequestWithValidApiKey() throws Exception {
		MockHttpServletRequest request = internalRequest("/internal/videos/uuid/thumbnail");
		request.addHeader(HEADER_NAME, "secret-key");

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(filterChain.getRequest()).isSameAs(request);
	}

	@Test
	void rejectsInternalRequestWithoutApiKey() throws Exception {
		MockHttpServletRequest request = internalRequest("/internal/videos/uuid/thumbnail");

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(filterChain.getRequest()).isNull();
	}

	@Test
	void rejectsInternalRequestWithInvalidApiKey() throws Exception {
		MockHttpServletRequest request = internalRequest("/internal/videos/uuid/processed");
		request.addHeader(HEADER_NAME, "wrong-key");

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(filterChain.getRequest()).isNull();
	}

	@Test
	void skipsPublicApiPaths() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/videos/uuid");

		filter.doFilter(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(filterChain.getRequest()).isSameAs(request);
	}

	private MockHttpServletRequest internalRequest(String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest("PATCH", uri);
		request.setServletPath(uri);
		return request;
	}
}
