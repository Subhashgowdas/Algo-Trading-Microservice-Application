package com.algotrade.broker_service.security.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class uses a 256 bit key to encrypt plaintext strings.
 * The encrypted output is Base64‑encoded and contains the 12‑byte IV
 * prepended to the ciphertext, followed by the 16‑byte GCM tag.
 *
 * Never hardcode the key – inject it via environment variable.
 */
public final class AesGcmEncryptionUtil {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 16;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private AesGcmEncryptionUtil() {}

	public static String encrypt(String plaintext, byte[] key) throws Exception {
		if (plaintext == null) return null;

		byte[] iv = new byte[GCM_IV_LENGTH];
		SECURE_RANDOM.nextBytes(iv);

		SecretKey secretKey = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

		byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

		ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
		byteBuffer.put(iv);
		byteBuffer.put(cipherText);

		return Base64.getEncoder().encodeToString(byteBuffer.array());
	}

	public static String decrypt(String encrypted, byte[] key) throws Exception {
		if (encrypted == null) return null;

		byte[] data = Base64.getDecoder().decode(encrypted);
		ByteBuffer byteBuffer = ByteBuffer.wrap(data);
		byte[] iv = new byte[GCM_IV_LENGTH];
		byteBuffer.get(iv);

		byte[] cipherText = new byte[byteBuffer.remaining()];
		byteBuffer.get(cipherText);

		SecretKey secretKey = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance(ALGORITHM);
		GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
		cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

		return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
	}

	public static String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

			// Java native hex formatting (No Apache Commons required)
			return HexFormat.of().formatHex(hashBytes);

		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available in this JVM", e);
		}
	}
}