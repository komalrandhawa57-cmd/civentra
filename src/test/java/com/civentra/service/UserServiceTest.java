package com.civentra.service;

import com.civentra.dao.UserDAO;
import com.civentra.model.User;
import com.civentra.util.SecurityUtil;
import com.civentra.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.atomic.AtomicReference;
import java.sql.SQLException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserDAO userDAO;
    @Mock AuthenticatedUser authenticatedUser;
    @InjectMocks UserService userService;

    @Test
    void registrationPersistsEncodedCitizenAccount() throws Exception {
        when(userDAO.findRoleIdByName("CITIZEN")).thenReturn(3);
        when(userDAO.emailExists("citizen@example.com")).thenReturn(false);
        AtomicReference<User> savedUser = new AtomicReference<>();
        when(userDAO.createUser(any(User.class))).thenAnswer(invocation -> {
            User created = invocation.getArgument(0);
            created.setUserId(42);
            savedUser.set(created);
            return 42;
        });
        when(userDAO.findById(42)).thenAnswer(invocation -> savedUser.get());

        User created = userService.registerCitizen("Asha Rao", "citizen@example.com", "9876543210", "SafePass123");

        assertEquals(42, created.getUserId());
        verify(userDAO).createUser(argThat(user -> user.getRoleId() == 3
                && SecurityUtil.isEncodedPassword(user.getPasswordHash())
                && SecurityUtil.verifyPassword("SafePass123", user.getPasswordHash())));
    }

    @Test
    void duplicateEmailIsRejectedBeforeInsert() throws Exception {
        when(userDAO.findRoleIdByName("CITIZEN")).thenReturn(3);
        when(userDAO.emailExists("citizen@example.com")).thenReturn(true);

        Exception error = assertThrows(Exception.class, () -> userService.registerCitizen(
                "Asha Rao", "citizen@example.com", "9876543210", "SafePass123"));

        assertEquals("Email is already registered.", error.getMessage());
        verify(userDAO, never()).createUser(any());
    }

    @Test
    void concurrentDuplicateEmailIsRejectedByDatabaseConstraint() throws Exception {
        when(userDAO.findRoleIdByName("CITIZEN")).thenReturn(3);
        when(userDAO.emailExists("citizen@example.com")).thenReturn(false);
        when(userDAO.createUser(any(User.class))).thenThrow(new SQLException(
                "ORA-00001: unique constraint (CIVENTRA_USER.UX_USERS_EMAIL_NORMALIZED) violated", null, 1));

        Exception error = assertThrows(Exception.class, () -> userService.registerCitizen(
                "Asha Rao", "citizen@example.com", "9876543210", "SafePass123"));

        assertEquals("Email is already registered.", error.getMessage());
        verify(userDAO).createUser(any(User.class));
        verify(userDAO, never()).findById(anyInt());
    }

    @Test
    void invalidEmailIsRejected() throws Exception {
        assertThrows(Exception.class, () -> userService.registerCitizen(
                "Asha Rao", "not-an-email", "9876543210", "SafePass123"));
        verify(userDAO).findRoleIdByName("CITIZEN");
        verify(userDAO, never()).emailExists(anyString());
        verify(userDAO, never()).createUser(any());
    }

    @Test
    void missingAccountCannotLogInOrBeProvisioned() throws Exception {
        when(userDAO.findByEmail("missing@example.com")).thenReturn(null);

        Exception error = assertThrows(Exception.class, () -> userService.login("missing@example.com", "SafePass123"));

        assertEquals("Invalid email or password.", error.getMessage());
        verify(userDAO, never()).createUser(any());
    }

    @Test
    void validLoginVerifiesEncodedPassword() throws Exception {
        User account = new User();
        account.setUserId(7);
        account.setActive(true);
        account.setPasswordHash(SecurityUtil.hashPassword("SafePass123"));
        when(userDAO.findByEmail("citizen@example.com")).thenReturn(account);

        assertSame(account, userService.login("citizen@example.com", "SafePass123"));
        assertThrows(Exception.class, () -> userService.login("citizen@example.com", "wrongpass"));
    }

    @Test
    void citizenCannotProvisionStaffAccounts() {
        User citizen = new User();
        citizen.setUserId(19);
        citizen.setRoleName("CITIZEN");
        citizen.setActive(true);
        when(authenticatedUser.getUser()).thenReturn(citizen);

        assertThrows(SecurityException.class, () -> userService.createStaffAccount(
                "Asha Rao", "officer@example.com", "9876543210", "SafePass123", "OFFICER"));
        verifyNoInteractions(userDAO);
    }

    @Test
    void accountOwnerCannotChangeAnotherUsersPassword() {
        User citizen = new User();
        citizen.setUserId(19);
        citizen.setRoleName("CITIZEN");
        citizen.setActive(true);
        when(authenticatedUser.getUser()).thenReturn(citizen);

        assertThrows(SecurityException.class, () -> userService.changeOwnPassword(
                20, "oldpass123", "newpass123", "newpass123"));
        verifyNoInteractions(userDAO);
    }
}
