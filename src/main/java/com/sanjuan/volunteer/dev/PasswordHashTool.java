package com.sanjuan.volunteer.dev;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class PasswordHashTool {
    private PasswordHashTool() {
    }

    public static void main(String[] args) {
        String rawPassword = args.length > 0 ? args[0] : "123456";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode(rawPassword));
    }
}
