package com.civentra.service;

import com.civentra.dao.InvoiceDAO;
import com.civentra.dao.ServiceEstimateDAO;
import com.civentra.model.Invoice;
import com.civentra.model.InvoiceLineItem;
import com.civentra.model.InvoicePayment;
import com.civentra.model.ServiceEstimate;
import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class InvoiceService {

    private final InvoiceDAO invoiceDAO;
    private final AuthenticatedUser authenticatedUser;
    private final ServiceEstimateDAO estimateDAO;

    @Autowired
    public InvoiceService(InvoiceDAO invoiceDAO, AuthenticatedUser authenticatedUser, ServiceEstimateDAO estimateDAO) {
        this.invoiceDAO = invoiceDAO;
        this.authenticatedUser = authenticatedUser;
        this.estimateDAO = estimateDAO;
    }

    @Transactional
    public long createInvoice(Invoice invoice) {
        if (invoice == null || invoice.getServiceRequestId() == null || invoice.getServiceRequestId() <= 0
                || !"PERSONAL_SERVICE".equalsIgnoreCase(invoice.getInvoiceType())) {
            throw new IllegalArgumentException("Invoices can only be created for a personal service request.");
        }
        // Treat the supplied invoice as a request only. All ownership, amounts, dates,
        // status, and line item data are rebuilt from the approved estimate.
        return generateServiceInvoiceAfterCompletion(invoice.getServiceRequestId());
    }

    @Transactional
    public long generateServiceInvoiceAfterCompletion(long requestId) {
        User user = requireSignedInUser();
        if (!"ADMIN".equalsIgnoreCase(user.getRoleName())) {
            throw new SecurityException("Administrator approval is required to generate a service invoice.");
        }
        Optional<Invoice> existing = invoiceDAO.findByServiceRequestId(requestId);
        if (existing.isPresent()) return existing.get().getInvoiceId();
        ServiceEstimate estimate = estimateDAO.approvedForCompletedRequest(requestId)
                .orElseThrow(() -> new IllegalStateException("A final invoice requires an approved estimate and verified completion."));

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("CIV-SVC-" + String.format(java.util.Locale.ROOT, "%08d", requestId));
        invoice.setCitizenId(estimate.getCitizenId());
        invoice.setProviderId(estimate.getProviderId());
        invoice.setServiceRequestId(requestId);
        invoice.setInvoiceType("PERSONAL_SERVICE");
        invoice.setStatus("UNPAID");
        invoice.setSubtotal(estimate.getSubtotal());
        invoice.setTax(estimate.getTax());
        invoice.setAmountPaid(java.math.BigDecimal.ZERO.setScale(2));
        invoice.setTotalAmount(estimate.getTotal());
        invoice.setBalanceDue(estimate.getTotal());
        return invoiceDAO.createServiceInvoice(invoice, estimate.getDescription());
    }

    public Optional<Invoice> getInvoiceById(long id) {
        User user = requireSignedInUser();
        Optional<Invoice> invoice = invoiceDAO.findById(id);
        invoice.ifPresent(value -> requireInvoiceAccess(user, value));
        return invoice;
    }
    public Optional<Invoice> getInvoiceById(int id) { return getInvoiceById((long) id); }

    public List<Invoice> getInvoicesForCitizen(long citizenId) {
        User user = requireSignedInUser();
        if (user.getUserId() != citizenId && !"ADMIN".equalsIgnoreCase(user.getRoleName())) {
            throw new SecurityException("You may only view invoices for your account.");
        }
        return invoiceDAO.findByCitizenId(citizenId);
    }

    public List<Invoice> getInvoicesForCitizen(int citizenId) {
        return getInvoicesForCitizen((long) citizenId);
    }

    public List<Invoice> getAllInvoices() {
        User user = requireSignedInUser();
        if (!"ADMIN".equalsIgnoreCase(user.getRoleName())) {
            throw new SecurityException("Administrator access is required to list all invoices.");
        }
        return invoiceDAO.findAll();
    }

    public List<InvoiceLineItem> getLineItemsByInvoice(int invoiceId) {
        return getLineItemsByInvoice((long) invoiceId);
    }

    public List<InvoiceLineItem> getLineItemsByInvoice(long invoiceId) {
        getInvoiceById(invoiceId).orElseThrow(() -> new IllegalArgumentException("Invoice was not found."));
        return invoiceDAO.getLineItemsByInvoice(invoiceId);
    }

    @Transactional
    public long recordPayment(long invoiceId, BigDecimal amount, String paymentMethod, String ref, long recordedBy) {
        return invoiceDAO.recordPayment(invoiceId, amount, paymentMethod, ref, recordedBy);
    }

    @Transactional
    public long recordPayment(long invoiceId, BigDecimal amount, String paymentMethod, String ref, int recordedBy) {
        return invoiceDAO.recordPayment(invoiceId, amount, paymentMethod, ref, recordedBy);
    }

    @Transactional
    public boolean markPaid(long invoiceId, long recordedBy) {
        throw new UnsupportedOperationException("Payment processing is not configured; invoice status was not changed.");
    }

    private User requireSignedInUser() {
        User user = authenticatedUser.getUser();
        if (user == null || !user.isActive()) throw new SecurityException("Sign in to view invoices.");
        return user;
    }

    private void requireInvoiceAccess(User user, Invoice invoice) {
        if (!"ADMIN".equalsIgnoreCase(user.getRoleName())
                && (invoice.getCitizenId() == null || invoice.getCitizenId() != user.getUserId())) {
            throw new SecurityException("You may only view invoices for your account.");
        }
    }
}
