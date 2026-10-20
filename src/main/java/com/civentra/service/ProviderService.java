package com.civentra.service;

import com.civentra.dao.ProviderDAO;
import com.civentra.dao.ServiceDAO;
import com.civentra.dao.ServiceEstimateDAO;
import com.civentra.model.ServiceCategory;
import com.civentra.model.ServiceProvider;
import com.civentra.model.ServiceRequest;
import com.civentra.model.ServiceEstimate;
import com.civentra.model.ServiceRequestStatusHistory;
import com.civentra.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

@Service
public class ProviderService {

    private final ProviderDAO providerDAO;
    private final ServiceDAO serviceDAO;
    private final NotificationService notificationService;
    private final AuthenticatedUser authenticatedUser;
    private final ServiceEstimateDAO estimateDAO;
    private final InvoiceService invoiceService;

    @Autowired
    public ProviderService(ProviderDAO providerDAO, ServiceDAO serviceDAO, NotificationService notificationService,
                           AuthenticatedUser authenticatedUser, ServiceEstimateDAO estimateDAO, InvoiceService invoiceService) {
        this.providerDAO = providerDAO;
        this.serviceDAO = serviceDAO;
        this.notificationService = notificationService;
        this.authenticatedUser = authenticatedUser;
        this.estimateDAO = estimateDAO;
        this.invoiceService = invoiceService;
    }

    public List<ServiceProvider> getAllProviders() {
        return providerDAO.findAll();
    }

    public List<ServiceProvider> getAllVerifiedProviders() {
        return providerDAO.getAllVerifiedProviders();
    }

    public int getVerifiedProviderCount() { return providerDAO.countVerifiedProviders(); }

    public int getActiveRequestCount() { return serviceDAO.countActiveRequests(); }

    public List<ServiceProvider> getAllProfilesForAdmin() {
        return providerDAO.getAllProfilesForAdmin();
    }

    public Map<String, Integer> getProviderOnboardingCounts() {
        Map<String, Integer> counts = providerDAO.getProviderOnboardingCounts();
        return counts != null ? counts : Collections.emptyMap();
    }

    public List<ServiceCategory> getActiveServiceCategories() {
        return providerDAO.getActiveServiceCategories();
    }

    public long saveProviderProfile(ServiceProvider provider) {
        return providerDAO.saveProviderProfile(provider);
    }

    public List<ServiceProvider> getByCategory(int categoryId) {
        return providerDAO.getByCategory(categoryId);
    }

    public List<ServiceProvider> getByCategory(long categoryId) {
        return providerDAO.findByCategory(categoryId);
    }

    public long createServiceRequest(ServiceRequest request) {
        if (request == null || request.getCitizenId() == null) {
            throw new SecurityException("A citizen can only create a service request for their own account.");
        }
        long requestId = serviceDAO.create(request);
        try {
            providerDAO.getById(request.getProviderId()).map(ServiceProvider::getUserId)
                    .filter(java.util.Objects::nonNull)
                    .ifPresent(providerUserId -> notificationService.notifyServiceRequest(providerUserId, requestId,
                            "New service request", "A citizen sent you a service request."));
        } catch (Exception ignored) {}
        return requestId;
    }

    public List<ServiceRequest> getRequestsForProvider(long providerId) {
        return serviceDAO.findByProviderId(providerId);
    }

    public List<ServiceRequest> getRequestsForCitizen(long citizenId) {
        return serviceDAO.findByCitizenId(citizenId);
    }

    public List<ServiceRequest> getRequestsPendingVerification() {
        try {
            List<ServiceRequest> list = serviceDAO.findPendingVerification();
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<ServiceRequestStatusHistory> getRequestHistoryForCitizen(long requestId, long citizenId) {
        return serviceDAO.findHistoryForCitizen(requestId, citizenId);
    }

    public List<ServiceRequestStatusHistory> getRequestHistoryForProvider(long requestId, long providerId) {
        return serviceDAO.findHistoryForProvider(requestId, providerId);
    }

    public List<ServiceRequestStatusHistory> getRequestHistoryForAdmin(long requestId) {
        return serviceDAO.findHistoryForAdmin(requestId);
    }

    public long submitServiceEstimate(long requestId, long providerId, String description,
                                     BigDecimal subtotal, BigDecimal tax) {
        long estimateId = estimateDAO.submit(requestId, providerId, 1L, description, subtotal, tax);
        try {
            long citizenId = estimateDAO.latestForRequest(requestId)
                    .map(ServiceEstimate::getCitizenId).filter(java.util.Objects::nonNull)
                    .orElse(1L);
            notificationService.notifyServiceRequest(citizenId, requestId, "Service estimate ready",
                    "A provider has submitted a cost estimate for your approval.");
        } catch (Exception ignored) {}
        return estimateId;
    }

    public void reviewServiceEstimate(long requestId, boolean approve, String remarks) {
        ServiceEstimate estimate = estimateDAO.review(requestId, 1L, 1L, approve, remarks);
        try {
            providerDAO.getById(estimate.getProviderId()).map(ServiceProvider::getUserId)
                    .filter(java.util.Objects::nonNull)
                    .ifPresent(providerUserId -> notificationService.notifyServiceRequest(providerUserId, requestId,
                            "Estimate " + estimate.getStatus().toLowerCase(java.util.Locale.ROOT),
                            "The citizen has " + estimate.getStatus().toLowerCase(java.util.Locale.ROOT) + " your estimate."));
        } catch (Exception ignored) {}
    }

    public void updateRequestStatus(long requestId, long providerId, String status, String completionNotes) {
        long citizenId = serviceDAO.updateStatusForProvider(requestId, providerId, status, completionNotes);
        try {
            notificationService.notifyServiceRequest(citizenId, requestId, "Service request updated",
                    "Your service request is now " + status.toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + ".");
        } catch (Exception ignored) {}
    }

    public void verifyRequestCompletion(long requestId, long adminId, boolean approve, String remarks) {
        String targetStatus = approve ? "COMPLETED" : "IN_PROGRESS";
        long citizenId = serviceDAO.verifyCompletion(requestId, targetStatus, adminId, remarks);

        if (approve) {
            try {
                invoiceService.generateServiceInvoiceAfterCompletion(requestId);
            } catch (Exception ignored) {}
        }

        try {
            notificationService.notifyServiceRequest(citizenId, requestId, "Service completion reviewed",
                    approve ? "The provider's completion report was approved." :
                            "The completion report was returned to the provider for further work.");
        } catch (Exception ignored) {}
    }

    public Optional<ServiceProvider> getByUserId(int userId) {
        return providerDAO.getByUserId(userId);
    }

    public Optional<ServiceProvider> getByUserId(long userId) {
        return providerDAO.getByUserId(userId);
    }

    public Optional<ServiceProvider> getProviderByUserId(long userId) {
        return providerDAO.getByUserId(userId);
    }

    public Optional<ServiceProvider> getProviderByUserId(int userId) {
        return providerDAO.getByUserId(userId);
    }

    public Optional<ServiceProvider> getById(int providerId) {
        return providerDAO.getById(providerId);
    }

    public Optional<ServiceProvider> getById(long providerId) {
        return providerDAO.getById(providerId);
    }

    public Optional<ServiceProvider> getProviderById(long providerId) {
        return providerDAO.getById(providerId);
    }

    public boolean updateStatus(int providerId, String status) {
        return providerDAO.updateStatus((long) providerId, null, status);
    }

    public boolean updateStatus(long providerId, String status) {
        return providerDAO.updateStatus(providerId, null, status);
    }

    public boolean updateStatus(Long providerId, Long userId, String status) {
        return providerDAO.updateStatus(providerId, userId, status);
    }

    public boolean updateVerificationStatus(int providerId, String verificationStatus) {
        return providerDAO.updateVerificationStatus((long) providerId, null, verificationStatus);
    }

    public boolean updateVerificationStatus(long providerId, String verificationStatus) {
        return providerDAO.updateVerificationStatus(providerId, null, verificationStatus);
    }

    public boolean updateVerificationStatus(Long providerId, Long userId, String verificationStatus) {
        return providerDAO.updateVerificationStatus(providerId, userId, verificationStatus);
    }

    public long createProvider(ServiceProvider provider) {
        return providerDAO.create(provider);
    }

    public void updateProvider(ServiceProvider provider) {
        providerDAO.update(provider);
    }
}