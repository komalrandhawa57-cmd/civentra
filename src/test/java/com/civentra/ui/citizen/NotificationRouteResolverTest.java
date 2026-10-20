package com.civentra.ui.citizen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationRouteResolverTest {
    @Test
    void issueNotificationsOpenTheRecipientWorkspace() {
        assertEquals("citizen/my-issues", NotificationRouteResolver.issueRoute("citizen"));
        assertEquals("officer", NotificationRouteResolver.issueRoute("OFFICER"));
        assertEquals("provider", NotificationRouteResolver.issueRoute("PROVIDER"));
        assertEquals("admin/assignments", NotificationRouteResolver.issueRoute("ADMIN"));
    }

    @Test
    void serviceNotificationsOpenRoleAppropriateRequests() {
        assertEquals("citizen/service-requests", NotificationRouteResolver.serviceRequestRoute("citizen"));
        assertEquals("provider", NotificationRouteResolver.serviceRequestRoute("provider"));
        assertEquals("admin/service-requests", NotificationRouteResolver.serviceRequestRoute("admin"));
    }

    @Test
    void unknownRoleDoesNotReceiveARestrictedDestination() {
        assertEquals("login", NotificationRouteResolver.issueRoute("UNKNOWN"));
        assertEquals("login", NotificationRouteResolver.serviceRequestRoute(null));
    }
}
