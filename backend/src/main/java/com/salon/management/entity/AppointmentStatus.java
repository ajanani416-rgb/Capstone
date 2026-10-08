package com.salon.management.entity;

/**
 * Closed status set (decision D3). New bookings start QUEUED; admins advance
 * QUEUED → IN_SERVICE → COMPLETED, or mark CANCELLED. Strict forward-only
 * transitions enforced in AppointmentService (TASK-006).
 */
public enum AppointmentStatus {
    QUEUED,
    IN_SERVICE,
    COMPLETED,
    CANCELLED
}
