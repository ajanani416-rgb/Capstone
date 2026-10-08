package com.salon.management.repository;

import com.salon.management.entity.Appointment;
import com.salon.management.entity.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Appointment}. Queue ordering stays a backend concern:
 * callers use these ordered queries and must not re-sort client-side
 * (ARCHITECTURE.md §1). Owner-scoping (user_id = caller) is enforced in the
 * service layer, not by these methods alone.
 */
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByUserIdOrderByAppointmentDateAscAppointmentTimeAsc(Long userId);

    List<Appointment> findByAppointmentDateOrderByAppointmentTimeAscQueueNumberAsc(
            LocalDate appointmentDate);

    List<Appointment> findByStatusOrderByAppointmentDateAscAppointmentTimeAsc(
            AppointmentStatus status);

    List<Appointment> findByAppointmentDateAndStatusOrderByAppointmentTimeAscQueueNumberAsc(
            LocalDate appointmentDate, AppointmentStatus status);

    List<Appointment> findAllByOrderByAppointmentDateAscAppointmentTimeAscQueueNumberAsc();

    boolean existsByServiceId(Long serviceId);

    boolean existsByBarberId(Long barberId);

    /** D2 pre-check: exact-slot block before insert (the V3 unique constraint
     * is the backstop for races). */
    boolean existsByBarberIdAndAppointmentDateAndAppointmentTime(
            Long barberId, LocalDate appointmentDate, LocalTime appointmentTime);

    /** Per-day queue sequence (D5 numbering): next number = max + 1. */
    @Query("SELECT COALESCE(MAX(a.queueNumber), 0) FROM Appointment a "
            + "WHERE a.appointmentDate = :date")
    int maxQueueNumberForDate(@Param("date") LocalDate date);
}
