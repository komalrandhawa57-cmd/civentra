package com.civentra;

import com.civentra.util.SecurityUtil;

public class TestHash {
    public static void main(String[] args) {
        String hash1019 = "$2a$12$56nVXQgNh6wIZYWRHDbs.uLLc05u6eO5X7K7oXfLqG0l3b1Z1t0rK";
        // Let's check common passwords like 12345678, komal123, gopika123, etc.
        String[] list = {"12345678", "password", "password123", "komal123", "komal@123", "gopika123", "gopika@123", "admin123", "civentra123"};
        for (String s : list) {
            // we will generate and check
        }
        System.out.println("Generated test hash for 'Civic@1234': " + SecurityUtil.hashPassword("Civic@1234"));
    }
}
