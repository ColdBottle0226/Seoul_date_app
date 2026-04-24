package com.seouldate.user.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 개인정보 암호화 / 복호화 / 해시 유틸
 *
 * <p>암호화 알고리즘: AES-256-GCM (인증 암호화, 무결성 보장)
 * <ul>
 *   <li>IV(Nonce) : 12 bytes (암호문 앞에 prepend)</li>
 *   <li>GCM Tag  : 128 bits</li>
 *   <li>출력 형태 : Base64(IV + Ciphertext + GCMTag)</li>
 * </ul>
 *
 * <p>해시 알고리즘: SHA-256 (검색용 단방향 해시 — 이메일/전화번호 조회에 사용)
 *
 * <p>환경변수 설정 예시:
 * <pre>
 *   CRYPTO_AES_KEY=32자리_Base64인코딩된_256비트_키
 * </pre>
 */
@Component
public class CryptoUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;

    public CryptoUtil(@Value("${crypto.aes.key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256 키는 반드시 32 bytes (Base64 인코딩) 이어야 합니다.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    // ──────────────────────────────────────────────────────────────────────
    // 암호화 (AES-256-GCM)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 평문을 AES-256-GCM 으로 암호화하여 Base64 문자열로 반환.
     *
     * @param plaintext 암호화할 평문 (null 이면 null 반환)
     * @return Base64 인코딩된 암호문 (IV + Ciphertext + GCMTag)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // IV를 앞에 붙여서 Base64 인코딩
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("암호화 실패", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 복호화 (AES-256-GCM)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * Base64 암호문을 복호화하여 평문 반환.
     *
     * @param ciphertextBase64 Base64 인코딩된 암호문 (null 이면 null 반환)
     * @return 복호화된 평문
     */
    public String decrypt(String ciphertextBase64) {
        if (ciphertextBase64 == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertextBase64);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("복호화 실패", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // SHA-256 해시 (DB 검색용 단방향 해시)
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 평문을 SHA-256 으로 해시하여 Hex 문자열로 반환.
     *
     * <p>이메일, 전화번호 등 암호화 저장 컬럼의 검색용 해시 컬럼 생성에 사용.
     * (암호화 컬럼은 검색 불가 → 해시 컬럼으로 UNIQUE/INDEX 조회)
     *
     * @param plaintext 해시할 평문 (소문자 정규화 후 해시 권장)
     * @return SHA-256 Hex 문자열 (64자)
     */
    public String hash(String plaintext) {
        if (plaintext == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(
                    plaintext.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8)
            );
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 해시 실패", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────────────────────────────

    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
