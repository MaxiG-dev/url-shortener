package dev.maxig.ms_shorten.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortenUrl {
    @Value("${config.application.short-url-length}")
    private int urlLength;

    private static final String URL_SAFE_BASE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_";
    private static final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(this.urlLength);
        for (int i = 0; i < urlLength; i++) {
            sb.append(URL_SAFE_BASE.charAt(random.nextInt(URL_SAFE_BASE.length())));
        }
        return sb.toString();
    }
}