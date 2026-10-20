package com.civentra.service;

import com.civentra.dao.IssueDAO;
import com.civentra.dao.ProviderDAO;
import com.civentra.model.CommunityIssue;
import com.civentra.model.ServiceProvider;
import com.civentra.model.User;
import com.civentra.security.AuthenticatedUser;
import com.civentra.model.IssueStatus;
import java.nio.file.Path;
import com.civentra.service.ImageStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceAuthorizationTest {
    @Mock IssueDAO issueDAO;
    @Mock NotificationService notificationService;
    @Mock ProviderDAO providerDAO;
    @Mock AuthenticatedUser authenticatedUser;
    @Mock ImageStorageService imageStorageService;
    @InjectMocks IssueService issueService;

    @Test
    void anonymousCannotReadCommunityIssueFeed() {
        when(authenticatedUser.getUser()).thenReturn(null);
        assertThrows(SecurityException.class, () -> issueService.getAllIssues());
        verifyNoInteractions(issueDAO);
    }

    @Test
    void citizenCanReadCommunityIssueFeed() {
        setCurrentUser(31, "CITIZEN");
        assertDoesNotThrow(() -> issueService.getAllIssues());
        verify(issueDAO).findAll();
    }

    @Test
    void citizenCannotReadAdminStatusAggregates() {
        setCurrentUser(31, "CITIZEN");
        assertThrows(SecurityException.class, () -> issueService.getIssuesByStatus("PENDING"));
        assertThrows(SecurityException.class, () -> issueService.countByStatus("PENDING"));
        verifyNoInteractions(issueDAO);
    }

    @Test
    void citizenCanCheckRecentOpenIssueMatches() {
        setCurrentUser(31, "CITIZEN");
        when(issueDAO.getRecentIssuesForDuplicateCheck("Pothole", null, null)).thenReturn(List.of());

        assertDoesNotThrow(() -> issueService.getRecentIssuesForDuplicateCheck("Pothole", null, null));
        verify(issueDAO).getRecentIssuesForDuplicateCheck("Pothole", null, null);
    }

    @Test
    void citizenCannotReadAnotherCitizensIssueById() {
        setCurrentUser(31, "CITIZEN");
        CommunityIssue issue = new CommunityIssue();
        issue.setReportedBy(32L);
        when(issueDAO.findById(5L)).thenReturn(Optional.of(issue));

        assertThrows(SecurityException.class, () -> issueService.getIssueById(5L));
    }

    @Test
    void citizenCannotSetInitialIssueStatusInRequestPayload() {
        setCurrentUser(31, "CITIZEN");
        CommunityIssue issue = validIssue();
        issue.setStatus("RESOLVED");
        when(issueDAO.create(issue)).thenReturn(42L);

        assertEquals(42L, issueService.reportIssue(issue));
        verify(issueDAO).create(issue);
        assertEquals(IssueStatus.REPORTED, issue.getStatus());
    }

    @Test
    void citizenCannotAttachAnImageThatIsNotInManagedStorage() {
        setCurrentUser(31, "CITIZEN");
        CommunityIssue issue = validIssue();
        issue.setEvidencePath("/uploads/00000000-0000-0000-0000-000000000001.png");
        when(imageStorageService.findStoredImage("00000000-0000-0000-0000-000000000001.png"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> issueService.reportIssue(issue));
        verify(issueDAO, never()).create(any());
    }

    @Test
    void citizenCannotReuseEvidenceAlreadyAttachedToAnotherIssue() {
        setCurrentUser(31, "CITIZEN");
        CommunityIssue issue = validIssue();
        String evidence = "/uploads/00000000-0000-0000-0000-000000000001.png";
        issue.setEvidencePath(evidence);
        when(imageStorageService.findStoredImage("00000000-0000-0000-0000-000000000001.png"))
                .thenReturn(Optional.of(Path.of("evidence.png")));
        when(issueDAO.isEvidencePathAttached(evidence)).thenReturn(true);

        assertThrows(SecurityException.class, () -> issueService.reportIssue(issue));
        verify(issueDAO, never()).create(any());
    }

    @Test
    void citizenCannotAssignIssueToAnOfficer() {
        setCurrentUser(31, "CITIZEN");
        assertThrows(SecurityException.class,
                () -> issueService.assignIssueToOfficer(5, 7, 31, "notes"));
        verifyNoInteractions(issueDAO);
    }

    @Test
    void providerCannotUpdateAnotherProvidersIssue() {
        setCurrentUser(31, "PROVIDER");
        ServiceProvider ownProvider = new ServiceProvider();
        ownProvider.setProviderId(44L);
        when(providerDAO.findByUserId(31L)).thenReturn(Optional.of(ownProvider));

        assertThrows(SecurityException.class,
                () -> issueService.updateStatusAsProvider(5, "IN_PROGRESS", 45L, "work"));
        verifyNoInteractions(issueDAO);
    }

    @Test
    void providerStatusHistoryUsesAuthenticatedUserId() {
        setCurrentUser(31, "PROVIDER");
        ServiceProvider ownProvider = new ServiceProvider();
        ownProvider.setProviderId(44L);
        when(providerDAO.findByUserId(31L)).thenReturn(Optional.of(ownProvider));

        issueService.updateStatusAsProvider(5, "IN_PROGRESS", 44L, "Work started");

        verify(issueDAO).transitionStatus(5L, "IN_PROGRESS", 31L, 44L, "Work started");
    }

    private void setCurrentUser(int id, String role) {
        User user = new User();
        user.setUserId(id);
        user.setRoleName(role);
        user.setActive(true);
        when(authenticatedUser.getUser()).thenReturn(user);
    }

    private CommunityIssue validIssue() {
        CommunityIssue issue = new CommunityIssue();
        issue.setReportedBy(31L);
        issue.setTitle("Blocked storm drain");
        issue.setDescription("Water cannot drain after rain.");
        issue.setCategoryId(4L);
        issue.setLocationAddress("Ward 3, Main Street");
        return issue;
    }
}
