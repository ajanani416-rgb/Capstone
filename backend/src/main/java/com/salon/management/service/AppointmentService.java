package com.salon.management.service;

import com.salon.management.dto.AppointmentResponse;
import com.salon.management.dto.CreateAppointmentRequest;
import com.salon.management.entity.Appointment;
import com.salon.management.entity.AppointmentStatus;
import com.salon.management.entity.Barber;
import com.salon.management.entity.Service;
import com.salon.management.entity.User;
import com.salon.management.exception.IllegalStatusTransitionException;
import com.salon.management.exception.SlotTakenException;
import com.salon.management.repository.AppointmentRepository;
import com.salon.management.repository.BarberRepository;
import com.salon.management.repository.ServiceRepository;
import com.salon.management.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Customer booking + owner-scoped reads (F-04/F-05) + admin list/status
 * (F-06/F-07).
 *
 * <p>Rules applied here, all traceable to locked decisions:
 * D2 exact-slot block (pre-check + V3 unique backstop → 409),
 * D3 initial status QUEUED + strict forward-only transitions,
 * per-day queue sequence max+1. estimatedWaitMinutes is computed on read
 * (QueueService), never stored.</p>
 *
 * <p>Owner isolation: customers see only their own rows (missing-or-foreign
 * both answer 404 so IDs can't be probed); admins see all.</p>
 */
@org.springframework.stereotype.Service
public class AppointmentService {

    private final AppointmentRepository appointments;
    private final UserRepository users;
    private final ServiceRepository services;
    private final BarberRepository barbers;
    private final QueueService queueService;

    public AppointmentService(AppointmentRepository appointments, UserRepository users,
            ServiceRepository services, BarberRepository barbers, QueueService queueService) {
        this.appointments = appointments;
        this.users = users;
        this.services = services;
        this.barbers = barbers;
        this.queueService = queueService;
    }

    @Transactional
    public AppointmentResponse book(Long userId, CreateAppointmentRequest request) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        Service service = services.findById(request.getServiceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found."));
        if (!Boolean.TRUE.equals(service.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That service is no longer available.");
        }
        Barber barber = barbers.findById(request.getBarberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barber not found."));
        if (!Boolean.TRUE.equals(barber.getActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "That barber is no longer available.");
        }
        if (request.getAppointmentDate().isEqual(LocalDate.now())
                && !request.getAppointmentTime().isAfter(LocalTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Time must be in the future for bookings today.");
        }
        if (appointments.existsByBarberIdAndAppointmentDateAndAppointmentTime(
                barber.getId(), request.getAppointmentDate(), request.getAppointmentTime())) {
            throw new SlotTakenException();
        }
        Appointment appointment = new Appointment(user, barber, service,
                request.getAppointmentDate(), request.getAppointmentTime(),
                AppointmentStatus.QUEUED);
        appointment.setQueueNumber(
                appointments.maxQueueNumberForDate(request.getAppointmentDate()) + 1);
        try {
            return new AppointmentResponse(appointments.saveAndFlush(appointment));
        } catch (DataIntegrityViolationException race) {
            // Lost a same-slot race against the V3 unique constraint.
            throw new SlotTakenException();
        }
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listMine(Long userId) {
        return appointments.findByUserIdOrderByAppointmentDateAscAppointmentTimeAsc(userId)
                .stream().map(AppointmentResponse::new).toList();
    }

    /** Owner-or-admin read. Foreign/missing IDs answer 404 uniformly.
     * QUEUED rows carry the live computed wait (same formula as the day
     * queue); other statuses carry the stored value. */
    @Transactional(readOnly = true)
    public AppointmentResponse getVisibleTo(Long userId, boolean admin, Long id) {
        Appointment appointment = appointments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Appointment not found."));
        if (!admin && !appointment.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found.");
        }
        if (appointment.getStatus() == AppointmentStatus.QUEUED) {
            return queueService.dayQueue(appointment.getAppointmentDate()).stream()
                    .filter(q -> q.getId().equals(id))
                    .findFirst()
                    .orElseGet(() -> new AppointmentResponse(appointment));
        }
        return new AppointmentResponse(appointment);
    }

    /** Admin list with optional filters, backend-ordered (date, time, queue).
     * Null filter = no filtering on that axis. */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAll(LocalDate date, AppointmentStatus status) {
        List<Appointment> rows;
        if (date != null && status != null) {
            rows = appointments.findByAppointmentDateAndStatusOrderByAppointmentTimeAscQueueNumberAsc(
                    date, status);
        } else if (date != null) {
            rows = appointments.findByAppointmentDateOrderByAppointmentTimeAscQueueNumberAsc(date);
        } else if (status != null) {
            rows = appointments.findByStatusOrderByAppointmentDateAscAppointmentTimeAsc(status);
        } else {
            rows = appointments.findAllByOrderByAppointmentDateAscAppointmentTimeAscQueueNumberAsc();
        }
        return rows.stream().map(AppointmentResponse::new).toList();
    }

    /** Strict forward-only transitions (locked): QUEUED → IN_SERVICE/CANCELLED,
     * IN_SERVICE → COMPLETED/CANCELLED, terminal states locked. Anything else
     * (including no-op repeats) is 409. */
    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus to) {
        Appointment appointment = appointments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Appointment not found."));
        AppointmentStatus from = appointment.getStatus();
        boolean legal = switch (from) {
            case QUEUED -> to == AppointmentStatus.IN_SERVICE || to == AppointmentStatus.CANCELLED;
            case IN_SERVICE -> to == AppointmentStatus.COMPLETED || to == AppointmentStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!legal) {
            throw new IllegalStatusTransitionException(from, to);
        }
        appointment.setStatus(to);
        return new AppointmentResponse(appointments.saveAndFlush(appointment));
    }
}
