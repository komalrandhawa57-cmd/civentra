package com.civentra.dao;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceDAOTransitionTest {
    @Test
    void providerMustSubmitDetailsForCompletionReview() {
        assertThrows(IllegalArgumentException.class,
                () -> ServiceDAO.validateProviderTransition("IN_PROGRESS", "PENDING_VERIFICATION", "  "));
        assertEquals("PENDING_VERIFICATION",
                ServiceDAO.validateProviderTransition("in_progress", "pending_verification", "Work completed"));
    }

    @Test
    void providerCannotMarkRequestCompletedWithoutReview() {
        assertThrows(IllegalStateException.class,
                () -> ServiceDAO.validateProviderTransition("IN_PROGRESS", "COMPLETED", "Done"));
    }

    @Test
    void administratorCanOnlyReviewPendingCompletion() {
        assertEquals("COMPLETED", ServiceDAO.validateAdminReviewTransition("PENDING_VERIFICATION", "COMPLETED"));
        assertEquals("IN_PROGRESS", ServiceDAO.validateAdminReviewTransition("PENDING_VERIFICATION", "IN_PROGRESS"));
        assertThrows(IllegalStateException.class,
                () -> ServiceDAO.validateAdminReviewTransition("IN_PROGRESS", "COMPLETED"));
    }

    @Test
    void providerCannotStartWorkWithoutCitizenApprovalOfLatestEstimate() {
        ServiceDAO.validateEstimateApprovalForStart("APPROVED");
        assertThrows(IllegalStateException.class, () -> ServiceDAO.validateEstimateApprovalForStart("PENDING"));
        assertThrows(IllegalStateException.class, () -> ServiceDAO.validateEstimateApprovalForStart("DECLINED"));
        assertThrows(IllegalStateException.class, () -> ServiceDAO.validateEstimateApprovalForStart(null));
    }
}
