package com.plip.video.adapter.out.storage;

import com.plip.video.global.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
@ConditionalOnProperty(prefix = "plip.storage", name = "type", havingValue = "local")
public class LocalObjectTokenService {

	private static final String HMAC_ALG = "HmacSHA256";

	private final String tokenSecret;

	public LocalObjectTokenService(StorageProperties storageProperties) {
		String secret = storageProperties.local() != null ? storageProperties.local().tokenSecret() : null;
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"plip.storage.local.token-secret is required when plip.storage.type=local");
		}
		this.tokenSecret = secret;
	}

	public String sign(String method, String objectKey, long expiresEpochSeconds) {
		return hmacHex(canonical(method, objectKey, expiresEpochSeconds));
	}

	public void verifyOrThrow(String method, String objectKey, String expiresRaw, String signature) {
		if (expiresRaw == null || expiresRaw.isBlank() || signature == null || signature.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing local object token");
		}
		long expires;
		try {
			expires = Long.parseLong(expiresRaw.trim());
		} catch (NumberFormatException ex) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid expires");
		}
		if (Instant.now().getEpochSecond() > expires) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Local object token expired");
		}
		String expected = sign(method, objectKey, expires).toLowerCase();
		String provided = signature.trim().toLowerCase();
		if (!MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8),
				provided.getBytes(StandardCharsets.UTF_8)
		)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid local object signature");
		}
	}

	private static String canonical(String method, String objectKey, long expiresEpochSeconds) {
		return method.toUpperCase() + "|" + objectKey + "|" + expiresEpochSeconds;
	}

	private String hmacHex(String payload) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALG);
			mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
			byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to sign local object URL", ex);
		}
	}
}
