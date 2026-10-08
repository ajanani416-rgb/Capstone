package com.salon.management.dto;

import com.salon.management.entity.Appointment;
import com.salon.management.entity.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;

/** Appointment view: flat names included so the frontend never has to resolve
 * internal IDs (PRODUCT.md success criteria). */
public class AppointmentResponse {

    private final Long id;
    private final Long serviceId;
    private final String serviceName;
    private final Long barberId;
    private final String barberName;
    private final LocalDate appointmentDate;
    private final LocalTime appointmentTime;
    private final Integer queueNumber;
    private final AppointmentStatus status;
    private final Integer estimatedWaitMinutes;

    public AppointmentResponse(Appointment appointment) {
        this(appointment, appointment.getEstimatedWaitMinutes());
    }

    /** Queue views override the stored (MVP: always null) value with the
     * live computed wait. The stored column stays untouched — waits are
     * derived on read so they can never go stale. */
    public AppointmentResponse(Appointment appointment, Integer estimatedWaitMinutes) {
        this.id = appointment.getId();
        this.serviceId = appointment.getService().getId();
        this.serviceName = appointment.getService().getName();
        this.barberId = appointment.getBarber().getId();
        this.barberName = appointment.getBarber().getName();
        this.appointmentDate = appointment.getAppointmentDate();
        this.appointmentTime = appointment.getAppointmentTime();
        this.queueNumber = appointment.getQueueNumber();
        this.status = appointment.getStatus();
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public Long getId() {
        return id;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Long getBarberId() {
        return barberId;
    }

    public String getBarberName() {
        return barberName;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public Integer getQueueNumber() {
        return queueNumber;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public Integer getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }
}
