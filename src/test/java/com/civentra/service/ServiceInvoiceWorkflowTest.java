package com.civentra.service;

import com.civentra.dao.InvoiceDAO;
import com.civentra.dao.ServiceEstimateDAO;
import com.civentra.model.Invoice;
import com.civentra.model.ServiceEstimate;
import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServiceInvoiceWorkflowTest {
    private final InvoiceDAO invoiceDAO = mock(InvoiceDAO.class);
    private final AuthenticatedUser authenticatedUser = mock(AuthenticatedUser.class);
    private final ServiceEstimateDAO estimateDAO = mock(ServiceEstimateDAO.class);
    private final InvoiceService invoiceService = new InvoiceService(invoiceDAO, authenticatedUser, estimateDAO);

    @Test
    void invoiceUsesApprovedEstimateOnlyAfterVerifiedCompletion() {
        signIn("ADMIN", 5);
        ServiceEstimate estimate = new ServiceEstimate();
        estimate.setRequestId(9L);
        estimate.setCitizenId(14L);
        estimate.setProviderId(22L);
        estimate.setDescription("Replace kitchen tap");
        estimate.setSubtotal(new BigDecimal("1250.00"));
        estimate.setTax(new BigDecimal("225.00"));
        estimate.setTotal(new BigDecimal("1475.00"));
        when(invoiceDAO.findByServiceRequestId(9)).thenReturn(Optional.empty());
        when(estimateDAO.approvedForCompletedRequest(9)).thenReturn(Optional.of(estimate));
        when(invoiceDAO.createServiceInvoice(any(Invoice.class), eq("Replace kitchen tap"))).thenReturn(77L);

        assertEquals(77L, invoiceService.generateServiceInvoiceAfterCompletion(9));

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceDAO).createServiceInvoice(captor.capture(), eq("Replace kitchen tap"));
        Invoice invoice = captor.getValue();
        assertEquals("CIV-SVC-00000009", invoice.getInvoiceNumber());
        assertEquals("PERSONAL_SERVICE", invoice.getInvoiceType());
        assertEquals("UNPAID", invoice.getStatus());
        assertEquals(14L, invoice.getCitizenId());
        assertEquals(22L, invoice.getProviderId());
        assertEquals(9L, invoice.getServiceRequestId());
        assertEquals(new BigDecimal("1250.00"), invoice.getSubtotal());
        assertEquals(new BigDecimal("225.00"), invoice.getTax());
        assertEquals(new BigDecimal("1475.00"), invoice.getTotalAmount());
        assertEquals(new BigDecimal("1475.00"), invoice.getBalanceDue());
    }

    @Test
    void repeatedGenerationReturnsExistingInvoiceAndDoesNotDuplicate() {
        signIn("ADMIN", 5);
        Invoice saved = new Invoice();
        saved.setInvoiceId(31L);
        when(invoiceDAO.findByServiceRequestId(9)).thenReturn(Optional.of(saved));

        assertEquals(31L, invoiceService.generateServiceInvoiceAfterCompletion(9));
        verifyNoInteractions(estimateDAO);
        verify(invoiceDAO, never()).createServiceInvoice(any(), anyString());
    }

    @Test
    void invoiceCannotBeGeneratedFromPendingOrDeclinedEstimate() {
        signIn("ADMIN", 5);
        when(invoiceDAO.findByServiceRequestId(9)).thenReturn(Optional.empty());
        when(estimateDAO.approvedForCompletedRequest(9)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> invoiceService.generateServiceInvoiceAfterCompletion(9));
        verify(invoiceDAO, never()).createServiceInvoice(any(), anyString());
    }

    @Test
    void genericCreatePathRequiresPersonalServiceAndUsesVerifiedWorkflow() {
        signIn("ADMIN", 5);
        Invoice request = new Invoice();
        request.setInvoiceType("PERSONAL_SERVICE");
        request.setServiceRequestId(9L);
        when(invoiceDAO.findByServiceRequestId(9)).thenReturn(Optional.empty());
        when(estimateDAO.approvedForCompletedRequest(9)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> invoiceService.createInvoice(request));
        verify(estimateDAO).approvedForCompletedRequest(9);
        verify(invoiceDAO, never()).createServiceInvoice(any(), anyString());

        Invoice publicComplaint = new Invoice();
        publicComplaint.setIssueId(15L);
        assertThrows(IllegalArgumentException.class, () -> invoiceService.createInvoice(publicComplaint));
    }

    @Test
    void citizenCannotGenerateFinalInvoice() {
        signIn("CITIZEN", 14);

        assertThrows(SecurityException.class, () -> invoiceService.generateServiceInvoiceAfterCompletion(9));
        verifyNoInteractions(invoiceDAO, estimateDAO);
    }

    private void signIn(String role, int userId) {
        User user = new User();
        user.setUserId(userId);
        user.setRoleName(role);
        user.setActive(true);
        when(authenticatedUser.getUser()).thenReturn(user);
    }
}
