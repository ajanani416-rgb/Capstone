package com.salon.management.service;

import com.salon.management.dto.AppointmentResponse;
import com.salon.management.entity.Appointment;
import com.salon.management.entity.AppointmentStatus;
import com.salon.management.repository.AppointmentRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Day-queue reads (FEATURES.md F-08, decision D5 remainder).
 *
 * <p>Order is immutable: (appointment_time, queue_number). There is no manual
 * reorder — admins influence flow only through status moves (TASK-006).</p>
 *
 * <p>Wait formula (locked): your estimated wait = sum of service durations of
 * same-barber, same-day appointments ahead of you that still occupy the chair
 * (QUEUED or IN_SERVICE). IN_SERVICE rows themselves wait 0 but still count
 * for those behind them; COMPLETED/CANCELLED rows wait 0 and count nothing.
 * Computed on read, never stored, so it can never go stale.</p>
 */
@Service
public class QueueService {

    private final AppointmentRepository appointments;

    public QueueService(AppointmentRepository appointments) {
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> dayQueue(LocalDate date) {
        List<Appointment> rows =
                appointments.findByAppointmentDateOrderByAppointmentTimeAscQueueNumberAsc(date);
        Map<Long, Integer> occupiedAhead = new HashMap<>();
        List<AppointmentResponse> out = new ArrayList<>(rows.size());
        for (Appointment row : rows) {
            Long barberId = row.getBarber().getId();
            int ahead = occupiedAhead.getOrDefault(barberId, 0);
            if (row.getStatus() == AppointmentStatus.QUEUED) {
                out.add(new AppointmentResponse(row, ahead));
                occupiedAhead.put(barberId, ahead + row.getService().getDurationMinutes());
            } else if (row.getStatus() == AppointmentStatus.IN_SERVICE) {
                out.add(new AppointmentResponse(row, 0));
                occupiedAhead.put(barberId, ahead + row.getService().getDurationMinutes());
            } else {
                out.add(new AppointmentResponse(row, 0));
            }
        }
        return out;
    }
}
