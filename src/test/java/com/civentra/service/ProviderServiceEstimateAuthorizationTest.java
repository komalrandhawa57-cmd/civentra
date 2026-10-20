package com.civentra.service;

import com.civentra.dao.ProviderDAO;
import com.civentra.dao.ServiceDAO;
import com.civentra.dao.ServiceEstimateDAO;
import com.civentra.model.ServiceProvider;
import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProviderServiceEstimateAuthorizationTest {
    private final ProviderDAO providers = mock(ProviderDAO.class);
    private final ServiceDAO requests = mock(ServiceDAO.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final AuthenticatedUser authenticatedUser = mock(AuthenticatedUser.class);
    private final ServiceEstimateDAO estimates = mock(ServiceEstimateDAO.class);
    private final InvoiceService invoices = mock(InvoiceService.class);
    private final ProviderService service = new ProviderService(providers, requests, notifications,
            authenticatedUser, estimates, invoices);

    @Test
    void providerCannotSubmitAnEstimateForAnotherProvider() {
        signIn("PROVIDER", 40);
        ServiceProvider own = new ServiceProvider();
        own.setProviderId(7L);
        when(providers.findByUserId(40L)).thenReturn(Optional.of(own));

        assertThrows(SecurityException.class, () -> service.submitServiceEstimate(11, 8,
                "Repair", BigDecimal.TEN, BigDecimal.ZERO));
        verifyNoInteractions(estimates);
    }

    @Test
    void citizensCannotSubmitProviderEstimates() {
        signIn("CITIZEN", 40);

        assertThrows(SecurityException.class, () -> service.submitServiceEstimate(11, 7,
                "Repair", BigDecimal.TEN, BigDecimal.ZERO));
        verifyNoInteractions(providers, estimates);
    }

    @Test
    void onlyTheAssignedProviderCanUseEstimateSubmission() {
        signIn("PROVIDER", 40);
        ServiceProvider own = new ServiceProvider();
        own.setProviderId(7L);
        when(providers.findByUserId(40L)).thenReturn(Optional.of(own));
        when(estimates.submit(11, 7, 40, "Repair", BigDecimal.TEN, BigDecimal.ZERO)).thenReturn(19L);
        var estimate = new com.civentra.model.ServiceEstimate();
        estimate.setCitizenId(50L);
        when(estimates.latestForRequest(11)).thenReturn(Optional.of(estimate));

        service.submitServiceEstimate(11, 7, "Repair", BigDecimal.TEN, BigDecimal.ZERO);

        verify(estimates).submit(11, 7, 40, "Repair", BigDecimal.TEN, BigDecimal.ZERO);
        verify(notifications).notifyServiceRequest(50, 11, "Service estimate ready",
                "A provider has submitted a cost estimate for your approval.");
    }

    @Test
    void adminCompletionApprovalGeneratesFinalInvoiceFromApprovedEstimateWorkflow() {
        signIn("ADMIN", 3);
        when(requests.verifyCompletion(11, "COMPLETED", 3, "Work verified")).thenReturn(40L);

        service.verifyRequestCompletion(11, 3, true, "Work verified");

        verify(requests).verifyCompletion(11, "COMPLETED", 3, "Work verified");
        verify(invoices).generateServiceInvoiceAfterCompletion(11);
        verify(notifications).notifyServiceRequest(40, 11, "Invoice generated",
                "A final invoice has been generated from the estimate you approved.");
    }

    @Test
    void citizenCannotApproveCompletionOrGenerateInvoice() {
        signIn("CITIZEN", 40);

        assertThrows(SecurityException.class, () -> service.verifyRequestCompletion(11, 40, true, ""));
        verifyNoInteractions(requests, invoices);
    }

    @Test
    void citizenRequestHistoryIsRestrictedToTheirOwnAccount() {
        signIn("CITIZEN", 40);

        assertThrows(SecurityException.class, () -> service.getRequestHistoryForCitizen(11, 41));
        verifyNoInteractions(requests);
    }

    @Test
    void providerRequestHistoryRequiresTheProviderOwnsTheProfile() {
        signIn("PROVIDER", 40);
        ServiceProvider own = new ServiceProvider();
        own.setProviderId(7L);
        when(providers.findByUserId(40L)).thenReturn(Optional.of(own));

        assertThrows(SecurityException.class, () -> service.getRequestHistoryForProvider(11, 8));
        verify(requests, never()).findHistoryForProvider(anyLong(), anyLong());
    }

    private void signIn(String role, int userId) {
        User user = new User();
        user.setUserId(userId);
        user.setRoleName(role);
        user.setActive(true);
        when(authenticatedUser.getUser()).thenReturn(user);
    }
}
