package com.civentra.model;

/** Canonical service-request workflow values stored in Oracle SERVICE_REQUESTS.STATUS. */
public enum ServiceRequestStatus {
    REQUESTED, ACCEPTED, REJECTED, IN_PROGRESS, COMPLETED
}
