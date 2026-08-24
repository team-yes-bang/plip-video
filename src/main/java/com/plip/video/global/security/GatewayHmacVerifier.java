package com.plip.video.global.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class GatewayHmacVerifier {

	private GatewayHmacVerifier() {
	}

	public static String sign(String secret, String canonicalPayload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException exception) {
			throw new IllegalStateException("Failed to compute gateway HMAC", exception);
		}
	}

	public static String buildCanonicalPayload(
			long timestampMillis,
			String method,
			String path,
			String userUuid
	) {
		return timestampMillis + "\n"
				+ method.toUpperCase() + "\n"
				+ path + "\n"
				+ userUuid;
	}

	public static boolean constantTimeEquals(String expected, String actual) {
		if (expected == null || actual == null) {
			return false;
		}
		return MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8),
				actual.getBytes(StandardCharsets.UTF_8)
		);
	}

}
