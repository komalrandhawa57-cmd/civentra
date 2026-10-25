package com.civentra.security;

import com.civentra.dao.IssueDAO;
import com.civentra.dao.UserDAO;
import com.civentra.model.User;
import com.civentra.service.ImageStorageService;
import com.civentra.service.UserService;
import com.civentra.ui.LoginView;
import com.civentra.ui.admin.AdminDashboard;
import com.civentra.ui.admin.UserManagementView;
import com.civentra.ui.citizen.CitizenDashboard;
import com.civentra.ui.officer.OfficerDashboard;
import com.civentra.ui.provider.ProviderDashboard;
import com.civentra.util.SecurityUtil;
import com.civentra.web.EvidenceController;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleBasedAuthenticationAndRoutingTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private IssueDAO issueDAO;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private VaadinSession mockVaadinSession;

    @Mock
    private WrappedSession mockWrappedSession;

    @Mock
    private UI mockUI;

    private AuthenticatedUser authenticatedUser;
    private UserService userService;
    private SecurityServiceInitListener securityListener;
    private EvidenceController evidenceController;

    private final Map<String, Object> vaadinSessionAttributes = new HashMap<>();
    private final Map<String, Object> httpSessionAttributes = new HashMap<>();

    @BeforeEach
    void setUp() {
        vaadinSessionAttributes.clear();
        httpSessionAttributes.clear();

        // Configure mock VaadinSession
        when(mockVaadinSession.getAttribute(anyString())).thenAnswer(inv -> vaadinSessionAttributes.get(inv.getArgument(0)));
        doAnswer(inv -> {
            vaadinSessionAttributes.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(mockVaadinSession).setAttribute(anyString(), any());
        when(mockVaadinSession.getSession()).thenReturn(mockWrappedSession);

        // Configure mock WrappedSession
        when(mockWrappedSession.getAttribute(anyString())).thenAnswer(inv -> httpSessionAttributes.get(inv.getArgument(0)));
        doAnswer(inv -> {
            httpSessionAttributes.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(mockWrappedSession).setAttribute(anyString(), any());
        doAnswer(inv -> {
            httpSessionAttributes.remove(inv.getArgument(0));
            return null;
        }).when(mockWrappedSession).removeAttribute(anyString());
        doAnswer(inv -> {
            httpSessionAttributes.clear();
            return null;
        }).when(mockWrappedSession).invalidate();

        VaadinSession.setCurrent(mockVaadinSession);
        UI.setCurrent(mockUI);

        authenticatedUser = new AuthenticatedUser(userDAO);
        authenticatedUser.setUserDAO(userDAO);

        userService = new UserService();
        ReflectionTestUtils.setField(userService, "userDAO", userDAO);
        ReflectionTestUtils.setField(userService, "authenticatedUser", authenticatedUser);

        securityListener = new SecurityServiceInitListener(authenticatedUser);
        evidenceController = new EvidenceController(imageStorageService, issueDAO, userDAO);
    }

    @AfterEach
    void tearDown() {
        VaadinSession.setCurrent(null);
        UI.setCurrent(null);
    }

    // 1. Valid Citizen Login
    @Test
    void validCitizenLoginRoutesToCitizenDashboard() throws Exception {
        User citizen = createTestUser(1, "citizen@civentra.com", "CITIZEN", "Password123");
        when(userDAO.findByEmail("citizen@civentra.com")).thenReturn(citizen);
        when(userDAO.findById(1)).thenReturn(citizen);

        User loggedIn = userService.login("citizen@civentra.com", "Password123");
        assertNotNull(loggedIn);
        assertEquals("CITIZEN", loggedIn.getRoleName());
        assertEquals("citizen", LoginView.getDashboardRoute(loggedIn.getRoleName()));
        assertTrue(authenticatedUser.isLoggedIn());
        assertEquals(1, authenticatedUser.getUserId());
    }

    // 2. Valid Admin Login
    @Test
    void validAdminLoginRoutesToAdminDashboard() throws Exception {
        User admin = createTestUser(4, "admin@civentra.com", "ADMIN", "AdminPass123");
        when(userDAO.findByEmail("admin@civentra.com")).thenReturn(admin);
        when(userDAO.findById(4)).thenReturn(admin);

        User loggedIn = userService.login("admin@civentra.com", "AdminPass123");
        assertNotNull(loggedIn);
        assertEquals("ADMIN", loggedIn.getRoleName());
        assertEquals("admin", LoginView.getDashboardRoute(loggedIn.getRoleName()));
        assertTrue(authenticatedUser.isAdmin());
    }

    // 3. Valid Officer Login
    @Test
    void validOfficerLoginRoutesToOfficerDashboard() throws Exception {
        User officer = createTestUser(2, "officer@civentra.com", "MUNICIPAL_OFFICER", "OfficerPass123");
        when(userDAO.findByEmail("officer@civentra.com")).thenReturn(officer);
        when(userDAO.findById(2)).thenReturn(officer);

        User loggedIn = userService.login("officer@civentra.com", "OfficerPass123");
        assertNotNull(loggedIn);
        assertEquals("OFFICER", authenticatedUser.getRoleName());
        assertEquals("officer", LoginView.getDashboardRoute(authenticatedUser.getRoleName()));
        assertTrue(authenticatedUser.isOfficer());
    }

    // 4. Valid Provider Login
    @Test
    void validProviderLoginRoutesToProviderDashboard() throws Exception {
        User provider = createTestUser(3, "provider@civentra.com", "SERVICE_PROVIDER", "ProviderPass123");
        when(userDAO.findByEmail("provider@civentra.com")).thenReturn(provider);
        when(userDAO.findById(3)).thenReturn(provider);

        User loggedIn = userService.login("provider@civentra.com", "ProviderPass123");
        assertNotNull(loggedIn);
        assertEquals("PROVIDER", authenticatedUser.getRoleName());
        assertEquals("provider", LoginView.getDashboardRoute(authenticatedUser.getRoleName()));
        assertTrue(authenticatedUser.isProvider());
    }

    // 5. Invalid Credentials (both wrong password and non-existent email return same message)
    @Test
    void invalidCredentialsReturnConsistentGenericErrorMessage() throws Exception {
        User existing = createTestUser(10, "user@civentra.com", "CITIZEN", "CorrectPass123");
        when(userDAO.findByEmail("user@civentra.com")).thenReturn(existing);
        when(userDAO.findByEmail("nonexistent@civentra.com")).thenReturn(null);

        // Wrong password
        IllegalArgumentException wrongPasswordEx = assertThrows(IllegalArgumentException.class, () ->
                userService.login("user@civentra.com", "WrongPassword!")
        );
        assertEquals("Invalid email or password.", wrongPasswordEx.getMessage());

        // Non-existent email
        IllegalArgumentException nonExistentEx = assertThrows(IllegalArgumentException.class, () ->
                userService.login("nonexistent@civentra.com", "AnyPassword123")
        );
        assertEquals("Invalid email or password.", nonExistentEx.getMessage());
    }

    // 6. Citizen Attempting to Access Admin APIs & Admin Views
    @Test
    void citizenAttemptingToAccessAdminApisAndViewsIsDenied() {
        User citizen = createTestUser(1, "citizen@civentra.com", "CITIZEN", "Pass123");
        authenticatedUser.login(citizen);

        // A. Citizen attempting to call Admin service API directly
        assertThrows(SecurityException.class, () -> userService.getAllUsers());
        assertThrows(SecurityException.class, () -> userService.getTotalUsers());
        assertThrows(SecurityException.class, () -> userService.setUserActive(2, false));

        // B. Citizen navigating to Admin View
        BeforeEnterEvent beforeEnterEvent = mockBeforeEnterEvent(AdminDashboard.class, "admin");
        ReflectionTestUtils.invokeMethod(securityListener, "beforeEnter", beforeEnterEvent);

        verify(beforeEnterEvent).forwardTo("citizen");
    }

    // 7. Direct URL Access to a Protected Dashboard While Unauthenticated
    @Test
    void unauthenticatedDirectAccessToProtectedDashboardRedirectsToLogin() {
        // No user logged in
        assertFalse(authenticatedUser.isLoggedIn());

        // Attempting to access Admin Dashboard
        BeforeEnterEvent adminEvent = mockBeforeEnterEvent(AdminDashboard.class, "admin");
        ReflectionTestUtils.invokeMethod(securityListener, "beforeEnter", adminEvent);
        verify(adminEvent).forwardTo(LoginView.class);

        // Attempting to access Officer Dashboard
        BeforeEnterEvent officerEvent = mockBeforeEnterEvent(OfficerDashboard.class, "officer");
        ReflectionTestUtils.invokeMethod(securityListener, "beforeEnter", officerEvent);
        verify(officerEvent).forwardTo(LoginView.class);

        // Attempting to access Provider Dashboard
        BeforeEnterEvent providerEvent = mockBeforeEnterEvent(ProviderDashboard.class, "provider");
        ReflectionTestUtils.invokeMethod(securityListener, "beforeEnter", providerEvent);
        verify(providerEvent).forwardTo(LoginView.class);

        // Attempting to access Citizen Dashboard
        BeforeEnterEvent citizenEvent = mockBeforeEnterEvent(CitizenDashboard.class, "citizen");
        ReflectionTestUtils.invokeMethod(securityListener, "beforeEnter", citizenEvent);
        verify(citizenEvent).forwardTo(LoginView.class);
    }

    // 8. Logout and Subsequent Access
    @Test
    void logoutInvalidatesSessionAndSubsequentAccessRedirectsToLogin() {
        User citizen = createTestUser(1, "citizen@civentra.com", "CITIZEN", "Pass123");
        authenticatedUser.login(citizen);
        assertTrue(authenticatedUser.isLoggedIn());

        // Perform logout
        authenticatedUser.logout();

        // Verify session state cleared
        assertFalse(authenticatedUser.isLoggedIn());
        assertNull(authenticatedUser.getUser());
        assertNull(vaadinSessionAttributes.get("CIVENTRA_CURRENT_USER"));
        assertNull(httpSessionAttributes.get("civentra.user"));

        // Subsequent access to dashboard redirects to login
        BeforeEnterEvent navEvent = mockBeforeEnterEvent(CitizenDashboard.class, "citizen");
        ReflectionTestUtils.invokeMethod(securityListener, "beforeEnter", navEvent);
        verify(navEvent).forwardTo(LoginView.class);
    }

    // 9. Refreshing a Dashboard After Login
    @Test
    void refreshingDashboardPreservesAuthenticatedRoleAndSession() throws Exception {
        User officer = createTestUser(2, "officer@civentra.com", "MUNICIPAL_OFFICER", "Pass123");
        when(userDAO.findById(2)).thenReturn(officer);
        authenticatedUser.login(officer);

        assertEquals("OFFICER", authenticatedUser.getRoleName());

        // Simulate page refresh (getUser called on refreshed UI context)
        User refreshedUser = authenticatedUser.getUser();
        assertNotNull(refreshedUser);
        assertEquals(2, refreshedUser.getUserId());
        assertEquals("OFFICER", refreshedUser.getRoleName());

        // Navigation to OfficerDashboard on refresh is allowed without redirection to login
        BeforeEnterEvent navEvent = mockBeforeEnterEvent(OfficerDashboard.class, "officer");
        ReflectionTestUtils.invokeMethod(securityListener, "beforeEnter", navEvent);
        verify(navEvent, never()).forwardTo(LoginView.class);
        verify(navEvent, never()).forwardTo(anyString());
    }

    // 10. Public Registration Not Allowing Privilege Escalation
    @Test
    void publicRegistrationDoesNotAllowPrivilegeEscalation() throws Exception {
        when(userDAO.findRoleIdByName("CITIZEN")).thenReturn(1);
        when(userDAO.emailExists("newcitizen@civentra.com")).thenReturn(false);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        when(userDAO.createUser(userCaptor.capture())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(99);
            return 99;
        });
        when(userDAO.findById(99)).thenAnswer(inv -> userCaptor.getValue());

        User registered = userService.registerCitizen("New Citizen", "newcitizen@civentra.com", "9876543210", "Password123");

        assertNotNull(registered);
        // Public registration always assigns CITIZEN role (roleId = 1)
        assertEquals(1, registered.getRoleId());
        assertEquals(1, userCaptor.getValue().getRoleId());
        assertTrue(SecurityUtil.isEncodedPassword(userCaptor.getValue().getPasswordHash()));
        assertTrue(SecurityUtil.verifyPassword("Password123", userCaptor.getValue().getPasswordHash()));
    }

    @Test
    void unknownDatabaseRoleCannotFallBackToCitizenAccess() {
        User unknown = createTestUser(77, "unknown@civentra.com", "DEPARTMENT_OFFICER", "SafePass123");
        authenticatedUser.login(unknown);

        assertEquals("UNKNOWN", authenticatedUser.getRoleName());
        assertFalse(authenticatedUser.isCitizen());
        assertEquals("login", LoginView.getDashboardRoute("DEPARTMENT_OFFICER"));

        BeforeEnterEvent navEvent = mockBeforeEnterEvent(CitizenDashboard.class, "citizen");
        ReflectionTestUtils.invokeMethod(securityListener, "beforeEnter", navEvent);
        verify(navEvent).forwardTo(LoginView.class);
    }

    private User createTestUser(int id, String email, String role, String rawPassword) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setFullName("Test " + role);
        user.setPhone("9876543210");
        user.setRoleName(role);
        user.setPasswordHash(SecurityUtil.hashPassword(rawPassword));
        user.setActive(true);
        user.setStatus("ACTIVE");
        return user;
    }

    @SuppressWarnings("unchecked")
    private BeforeEnterEvent mockBeforeEnterEvent(Class<?> targetClass, String routePath) {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        doReturn(targetClass).when(event).getNavigationTarget();
        Location location = new Location(routePath);
        when(event.getLocation()).thenReturn(location);
        return event;
    }
}
