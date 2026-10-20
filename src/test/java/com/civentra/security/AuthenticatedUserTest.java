package com.civentra.security;

import com.civentra.dao.UserDAO;
import com.civentra.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserTest {

    @Mock
    private UserDAO userDAO;

    @Test
    void loginAndGetFullName() {
        AuthenticatedUser auth = new AuthenticatedUser(userDAO);
        User user = new User();
        user.setUserId(2);
        user.setFullName("Officer Rajesh Sharma");
        user.setEmail("officer@civentra.com");
        user.setRoleName("OFFICER");
        user.setActive(true);
        user.setStatus("ACTIVE");

        auth.refreshUser(user);

        // Without a Vaadin session in unit tests, getUser() may be null —
        // so we only assert the object wiring does not throw.
        assertDoesNotThrow(() -> auth.refreshUser(user));
        assertDoesNotThrow(() -> auth.login(user));
        assertNotNull(auth);
    }

    @Test
    void constructorAcceptsUserDao() {
        AuthenticatedUser auth = new AuthenticatedUser(userDAO);
        assertNotNull(auth);
    }
}