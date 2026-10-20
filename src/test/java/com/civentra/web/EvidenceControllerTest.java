package com.civentra.web;

import com.civentra.dao.IssueDAO;
import com.civentra.dao.UserDAO;
import com.civentra.model.User;
import com.civentra.service.ImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EvidenceControllerTest {
    private final ImageStorageService storage = mock(ImageStorageService.class);
    private final IssueDAO issues = mock(IssueDAO.class);
    private final UserDAO users = mock(UserDAO.class);
    private final EvidenceController controller = new EvidenceController(storage, issues, users);

    @Test
    void evidenceRequiresASignedInUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        assertEquals(404, controller.getEvidence("11111111-1111-1111-1111-111111111111.png", request)
                .getStatusCode().value());
        verifyNoInteractions(storage, issues);
    }

    @Test
    void citizenCannotFetchAnotherReportEvidence() {
        HttpServletRequest request = authenticatedRequest("CITIZEN", 8);
        when(storage.findStoredImage("11111111-1111-1111-1111-111111111111.png"))
                .thenReturn(Optional.of(Path.of("evidence.png")));
        when(issues.canViewEvidence("/uploads/11111111-1111-1111-1111-111111111111.png", 8)).thenReturn(false);

        assertEquals(404, controller.getEvidence("11111111-1111-1111-1111-111111111111.png", request)
                .getStatusCode().value());
    }

    @Test
    void deactivatedAccountCannotFetchPreviouslyAuthorizedEvidence() throws Exception {
        HttpServletRequest request = authenticatedRequest("CITIZEN", 8);
        when(users.findById(8)).thenReturn(inactiveUser(8));

        assertEquals(404, controller.getEvidence("11111111-1111-1111-1111-111111111111.png", request)
                .getStatusCode().value());
        verifyNoInteractions(storage, issues);
    }

    @Test
    void reportOwnerCanFetchTheirEvidence() {
        HttpServletRequest request = authenticatedRequest("CITIZEN", 8);
        when(storage.findStoredImage("11111111-1111-1111-1111-111111111111.png"))
                .thenReturn(Optional.of(Path.of("evidence.png")));
        when(issues.canViewEvidence("/uploads/11111111-1111-1111-1111-111111111111.png", 8)).thenReturn(true);

        var response = controller.getEvidence("11111111-1111-1111-1111-111111111111.png", request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store", response.getHeaders().getCacheControl());
    }

    private HttpServletRequest authenticatedRequest(String role, int id) {
        User user = new User();
        user.setUserId(id);
        user.setRoleName(role);
        user.setActive(true);
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("civentra.user")).thenReturn(user);
        try {
            when(users.findById(id)).thenReturn(user);
        } catch (java.sql.SQLException ex) {
            throw new AssertionError(ex);
        }
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(session);
        return request;
    }

    private User inactiveUser(int id) {
        User user = new User();
        user.setUserId(id);
        user.setRoleName("CITIZEN");
        user.setActive(false);
        return user;
    }
}
