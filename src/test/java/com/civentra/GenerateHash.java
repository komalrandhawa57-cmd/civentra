package com.civentra;

import com.civentra.util.SecurityUtil;

public class GenerateHash {
    public static void main(String[] args) {
        String hash = SecurityUtil.hashPassword("Civic@12345");
        System.out.println("BCRYPT_HASH: " + hash);
    }
}
