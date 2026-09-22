package com.salon.management.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/** POST /api/appointments body. ISO-8601 date (YYYY-MM-DD) and time (HH:mm).
 * Date must be today or later; a same-day time in the past is rejected by the
 * service with 400. IDs must reference active catalog rows. */
public class CreateAppointmentRequest {

    @NotNull(message = "Service is required.")
    private Long serviceId;

    @NotNull(message = "Barber is required.")
    private Long barberId;

    @NotNull(message = "Date is required.")
    @FutureOrPresent(message = "Date must be today or in the future.")
    private LocalDate appointmentDate;

    @NotNull(message = "Time is required.")
    private LocalTime appointmentTime;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getBarberId() {
        return barberId;
    }

    public void setBarberId(Long barberId) {
        this.barberId = barberId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
}
