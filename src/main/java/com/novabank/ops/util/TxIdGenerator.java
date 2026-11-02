package com.novabank.ops.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import jakarta.xml.bind.DatatypeConverter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 거래번호 생성기.
 * 형식: 접두어(2) + 무작위 영숫자(6) + 체크 문자(2) = 10자. 예) NBA7Q2C9K3
 */
@Component
public class TxIdGenerator {

    private static final int RANDOM_LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    @Value("${tx.id.prefix:NB}")
    private String prefix = "NB";

    public String generate() {
        String body = randomAlphaNumeric(RANDOM_LENGTH);
        String candidate = prefix.toUpperCase() + body;
        return candidate + checksum(candidate);
    }

    /** 무작위 바이트를 Base64 로 바꾼 뒤 영숫자만 추려 쓴다 */
    private String randomAlphaNumeric(int length) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            byte[] bytes = new byte[12];
            random.nextBytes(bytes);
            String encoded = Base64.getEncoder().encodeToString(bytes);
            for (int i = 0; i < encoded.length() && sb.length() < length; i++) {
                char ch = Character.toUpperCase(encoded.charAt(i));
                if (Character.isLetterOrDigit(ch) && ch < 128) {
                    sb.append(ch);
                }
            }
        }
        return sb.toString();
    }

    /** MD5 앞 1바이트를 16진수 2자로 — 입력 오타 검출용 */
    static String checksum(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes("UTF-8"));
            String hex = DatatypeConverter.printHexBinary(digest);
            return hex.substring(0, 2);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 를 사용할 수 없습니다.", e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 을 사용할 수 없습니다.", e);
        }
    }

    public boolean isValid(String txId) {
        if (txId == null || txId.length() != prefix.length() + RANDOM_LENGTH + 2) {
            return false;
        }
        String body = txId.substring(0, txId.length() - 2);
        return txId.endsWith(checksum(body));
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
