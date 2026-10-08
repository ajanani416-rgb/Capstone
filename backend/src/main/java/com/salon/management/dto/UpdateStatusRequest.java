package com.salon.management.dto;

import com.salon.management.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

/** PATCH /api/appointments/:id/status body. Must be a legal forward move
 * (QUEUED → IN_SERVICE/CANCELLED, IN_SERVICE → COMPLETED/CANCELLED). */
public class UpdateStatusRequest {

    @NotNull(message = "Status is required.")
    private AppointmentStatus status;

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
