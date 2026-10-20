package com.civentra.util;

import java.time.Year;

public class IdGenerator {

    /**
     * Generate issue code like CIV-2026-00124
     */
    public static String generateIssueCode(int sequenceNumber) {
        int year = Year.now().getValue();
        return String.format("CIV-%d-%05d", year, sequenceNumber);
    }

    /**
     * Generate service request code like SVC-2026-00042
     */
    public static String generateServiceCode(int sequenceNumber) {
        int year = Year.now().getValue();
        return String.format("SVC-%d-%05d", year, sequenceNumber);
    }
}