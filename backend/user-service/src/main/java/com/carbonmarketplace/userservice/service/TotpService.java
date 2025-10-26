package com.carbonmarketplace.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TotpService {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int CODE_DIGITS = 6;
    private static final long TIME_STEP = TimeUnit.SECONDS.toMillis(30);
    private static final int WINDOW_SIZE = 1; // Allow 1 time step before/after current
    private static final String ISSUER = "Carbon Credit Marketplace";

    /**
     * Generate a new TOTP secret
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20]; // 160 bits
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    /**
     * Generate QR code URL for Google Authenticator
     */
    public static String generateQRCodeUrl(String email, String secret) {
        try {
            String format = "otpauth://totp/%s:%s?secret=%s&issuer=%s";
            return String.format(format,
                    URLEncoder.encode(ISSUER, StandardCharsets.UTF_8),
                    URLEncoder.encode(email, StandardCharsets.UTF_8),
                    secret,
                    URLEncoder.encode(ISSUER, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to generate QR code URL", e);
            throw new RuntimeException("Failed to generate QR code URL");
        }
    }

    /**
     * Validate TOTP code
     */
    public boolean validateCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }

        try {
            int userCode = Integer.parseInt(code);
            long currentTime = System.currentTimeMillis() / TIME_STEP;

            // Check current time step and surrounding windows
            for (int i = -WINDOW_SIZE; i <= WINDOW_SIZE; i++) {
                long timeStep = currentTime + i;
                int generatedCode = generateCode(secret, timeStep);

                if (generatedCode == userCode) {
                    log.debug("TOTP code validated successfully");
                    return true;
                }
            }

            log.debug("TOTP code validation failed");
            return false;

        } catch (NumberFormatException e) {
            log.error("Invalid TOTP code format: {}", code);
            return false;
        } catch (Exception e) {
            log.error("Error validating TOTP code", e);
            return false;
        }
    }

    /**
     * Generate TOTP code for a specific time step
     */
    private int generateCode(String secret, long timeStep) {
        try {
            Base32 base32 = new Base32();
            byte[] decodedKey = base32.decode(secret);
            byte[] timeBytes = longToBytes(timeStep);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(decodedKey, HMAC_ALGORITHM);
            mac.init(keySpec);

            byte[] hash = mac.doFinal(timeBytes);

            // Dynamic truncation
            int offset = hash[hash.length - 1] & 0xf;
            int binary = ((hash[offset] & 0x7f) << 24) |
                    ((hash[offset + 1] & 0xff) << 16) |
                    ((hash[offset + 2] & 0xff) << 8) |
                    (hash[offset + 3] & 0xff);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);

            return otp;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating TOTP code", e);
            throw new RuntimeException("Error generating TOTP code");
        }
    }

    /**
     * Convert long to byte array
     */
    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    /**
     * Generate a TOTP code for the current time (for testing)
     */
    public String generateCurrentCode(String secret) {
        try {
            long currentTime = System.currentTimeMillis() / TIME_STEP;
            int code = generateCode(secret, currentTime);
            return String.format("%0" + CODE_DIGITS + "d", code);
        } catch (Exception e) {
            log.error("Error generating current TOTP code", e);
            throw new RuntimeException("Error generating current TOTP code");
        }
    }
}
