package com.civentra.service;

import com.civentra.dao.IssueDAO;
import com.civentra.dao.ProviderDAO;
import com.civentra.model.CommunityIssue;
import com.civentra.model.IssueCategory;
import com.civentra.model.IssueStatus;
import com.civentra.model.IssueStatusHistory;
import com.civentra.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class IssueService {

    private final IssueDAO issueDAO;
    private final NotificationService notificationService;
    private final ProviderDAO providerDAO;
    private final AuthenticatedUser authenticatedUser;
    private final ImageStorageService imageStorageService;

    @Autowired
    public IssueService(IssueDAO issueDAO, NotificationService notificationService, ProviderDAO providerDAO,
                        AuthenticatedUser authenticatedUser, ImageStorageService imageStorageService) {
        this.issueDAO = issueDAO;
        this.notificationService = notificationService;
        this.providerDAO = providerDAO;
        this.authenticatedUser = authenticatedUser;
        this.imageStorageService = imageStorageService;
    }

    /** Community map feed is available only to signed-in citizens and administrators. */
    public List<CommunityIssue> getAllIssues() {
        requireRole("CITIZEN", "ADMIN");
        return issueDAO.findAll();
    }

    public List<IssueCategory> getIssueCategories() { return issueDAO.findIssueCategories(); }

    public Optional<CommunityIssue> getIssueById(long id) {
        com.civentra.model.User user = requireCurrentUser("CITIZEN", "OFFICER", "PROVIDER", "ADMIN");
        Optional<CommunityIssue> result = issueDAO.findById(id);
        result.ifPresent(issue -> {
            boolean allowed = switch (user.getRoleName().toUpperCase(java.util.Locale.ROOT)) {
                case "ADMIN" -> true;
                case "CITIZEN" -> issue.getReportedBy() != null && issue.getReportedBy() == user.getUserId();
                case "OFFICER" -> issue.getAssignedTo() != null && issue.getAssignedTo() == user.getUserId();
                case "PROVIDER" -> {
                    long providerId = providerDAO.findByUserId(user.getUserId())
                            .map(com.civentra.model.ServiceProvider::getProviderId)
                            .orElseThrow(() -> new SecurityException("This account has no provider profile."));
                    yield issue.getProviderId() != null && issue.getProviderId() == providerId;
                }
                default -> false;
            };
            if (!allowed) throw new SecurityException("You are not authorized to view this issue.");
        });
        return result;
    }
    
    public Optional<CommunityIssue> getIssueById(int id) { return getIssueById((long) id); }

    public List<CommunityIssue> getIssuesByCitizen(long userId) {
        requireOwnerOrRole(userId, "CITIZEN", "ADMIN");
        return issueDAO.findByReporter(userId);
    }
    
    public List<CommunityIssue> getIssuesByCitizen(int userId) { return getIssuesByCitizen((long) userId); }

    public List<CommunityIssue> getIssuesByStatus(String status) {
        requireRole("ADMIN");
        return issueDAO.findByStatus(status);
    }
    
    public List<CommunityIssue> getIssuesByStatus(IssueStatus status) {
        requireRole("ADMIN");
        return issueDAO.findByStatus(status.name());
    }

    public List<CommunityIssue> getIssuesForOfficer(long officerId) {
        requireOwnerOrRole(officerId, "OFFICER", "ADMIN");
        return issueDAO.findByAssignee(officerId);
    }
    
    public List<CommunityIssue> getIssuesForProvider(long providerId) {
        Long userId = requireRole("PROVIDER", "ADMIN");
        if (!"ADMIN".equalsIgnoreCase(authenticatedUser.getRoleName())) {
            long ownProviderId = providerDAO.findByUserId(userId)
                    .map(com.civentra.model.ServiceProvider::getProviderId)
                    .orElseThrow(() -> new SecurityException("This account has no provider profile."));
            if (ownProviderId != providerId) throw new SecurityException("This issue is not assigned to your provider account.");
        }
        return issueDAO.findByProvider(providerId);
    }
    
    public List<CommunityIssue> getUnassignedIssues() {
        requireRole("ADMIN");
        return issueDAO.findUnassigned();
    }

    @Transactional
    public long reportIssue(CommunityIssue issue) {
        Long currentUserId = requireRole("CITIZEN");
        if (issue == null || issue.getReportedBy() == null || issue.getTitle() == null || issue.getTitle().isBlank()
                || issue.getTitle().length() > 150 || issue.getDescription() == null || issue.getDescription().isBlank()
                || issue.getCategoryId() == null || issue.getLocationAddress() == null || issue.getLocationAddress().isBlank()) {
            throw new IllegalArgumentException("Title, description, category, location, and signed-in citizen are required.");
        }
        if (!issue.getReportedBy().equals(currentUserId)) {
            throw new SecurityException("A citizen can only submit an issue for their own account.");
        }
        if ((issue.getLatitude() == null) != (issue.getLongitude() == null)) {
            throw new IllegalArgumentException("Latitude and longitude must both be provided.");
        }
        if (issue.getLatitude() != null && (issue.getLatitude().doubleValue() < -90 || issue.getLatitude().doubleValue() > 90
                || issue.getLongitude().doubleValue() < -180 || issue.getLongitude().doubleValue() > 180)) {
            throw new IllegalArgumentException("The issue coordinates are outside valid map bounds.");
        }
        validateNewEvidence(issue.getEvidencePath());
        
        issue.setStatus(IssueStatus.REPORTED);
        long issueId = issueDAO.create(issue);
        if (issueId > 0 && issue.getReportedBy() != null) {
            notificationService.notifyIssueUpdate(issue.getReportedBy(), issueId,
                    "Issue submitted", "Your report “" + issue.getTitle() + "” was submitted.");
        }
        return issueId;
    }

    @Transactional
    public long saveIssue(CommunityIssue issue) { return reportIssue(issue); }

    @Transactional
    public void updateStatusAsOfficer(long issueId, String status, long officerId, String notes) {
        Long currentUserId = requireRole("OFFICER");
        if (currentUserId != officerId) throw new SecurityException("The active officer account is required.");
        issueDAO.transitionStatus(issueId, status, officerId, null, notes);
        issueDAO.findById(issueId).ifPresent(issue -> notificationService.notifyIssueUpdate(issue.getReportedBy(), issueId,
                "Issue status updated", "Your report “" + issue.getTitle() + "” is now " + status + "."));
    }

    @Transactional
    public void updateStatusAsProvider(long issueId, String status, long providerId, String notes) {
        Long currentUserId = requireRole("PROVIDER");
        long ownProviderId = providerDAO.findByUserId(currentUserId)
                .map(com.civentra.model.ServiceProvider::getProviderId)
                .orElseThrow(() -> new SecurityException("This account has no provider profile."));
        if (ownProviderId != providerId) throw new SecurityException("This issue is not assigned to your provider account.");
        issueDAO.transitionStatus(issueId, status, currentUserId, providerId, notes);
        issueDAO.findById(issueId).ifPresent(issue -> notificationService.notifyIssueUpdate(issue.getReportedBy(), issueId,
                "Work update", "Work on “" + issue.getTitle() + "” was submitted for verification."));
    }

    public List<IssueStatusHistory> getStatusHistoryForCitizen(long issueId, long citizenId) {
        requireOwnerOrRole(citizenId, "CITIZEN", "ADMIN");
        return issueDAO.getStatusHistoryForCitizen(issueId, citizenId);
    }

    @Transactional
    public void assignIssueToOfficer(long issueId, long officerId, long adminId, String notes) {
        Long currentUserId = requireRole("ADMIN");
        if (currentUserId != adminId) throw new SecurityException("The active administrator account is required.");
        issueDAO.assignIssue(issueId, officerId, null, notes, adminId);
        issueDAO.findById(issueId).ifPresent(issue -> {
            if (issue.getReportedBy() != null) notificationService.notifyIssueUpdate(issue.getReportedBy(), issueId,
                    "Issue assigned", "An officer has been assigned to your report “" + issue.getTitle() + "”.");
        });
        notificationService.notifyIssueUpdate(officerId, issueId, "New issue assignment",
                "Issue “" + issueDAO.findById(issueId).map(CommunityIssue::getTitle).orElse("Civic report") + "” was assigned to you.");
    }

    @Transactional
    public long createIssueCategory(IssueCategory category) {
        requireRole("ADMIN");
        return issueDAO.createIssueCategory(category);
    }

    @Transactional
    public long createIssueCategory(String name, String desc) {
        requireRole("ADMIN");
        return issueDAO.createIssueCategory(name, desc);
    }

    public List<CommunityIssue> getRecentIssuesForDuplicateCheck(String title, Double lat, Double lng) {
        requireRole("CITIZEN");
        return issueDAO.getRecentIssuesForDuplicateCheck(title, lat, lng);
    }

    public int countByStatus(String status) {
        requireRole("ADMIN");
        return issueDAO.countByStatus(status);
    }

    private void validateNewEvidence(String evidencePath) {
        if (evidencePath == null || evidencePath.isBlank()) return;
        String prefix = "/uploads/";
        if (!evidencePath.startsWith(prefix)) {
            throw new IllegalArgumentException("Evidence must be uploaded through the issue form.");
        }
        String filename = evidencePath.substring(prefix.length());
        if (imageStorageService.findStoredImage(filename).isEmpty()) {
            throw new IllegalArgumentException("The uploaded evidence is missing or invalid. Attach it again.");
        }
        if (issueDAO.isEvidencePathAttached(evidencePath)) {
            throw new SecurityException("This evidence file is already attached to another issue.");
        }
    }

    private com.civentra.model.User requireCurrentUser(String... allowedRoles) {
        com.civentra.model.User current = authenticatedUser.getUser();
        if (current == null || !current.isActive() || current.getUserId() <= 0
                || java.util.Arrays.stream(allowedRoles).noneMatch(role -> role.equalsIgnoreCase(current.getRoleName()))) {
            throw new SecurityException("An authorized account is required for this action.");
        }
        return current;
    }

    private Long requireRole(String... allowedRoles) {
        return (long) requireCurrentUser(allowedRoles).getUserId();
    }

    private void requireOwnerOrRole(long requestedUserId, String ownerRole, String privilegedRole) {
        Long currentUserId = requireRole(ownerRole, privilegedRole);
        if (currentUserId != requestedUserId && !privilegedRole.equalsIgnoreCase(authenticatedUser.getRoleName())) {
            throw new SecurityException("You may only access records belonging to your account.");
        }
    }
}