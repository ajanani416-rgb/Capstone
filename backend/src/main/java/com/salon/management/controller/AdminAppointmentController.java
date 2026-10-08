package com.salon.management.controller;

import com.salon.management.dto.AppointmentResponse;
import com.salon.management.dto.UpdateStatusRequest;
import com.salon.management.entity.AppointmentStatus;
import com.salon.management.service.AppointmentService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN-only appointment management (FEATURES.md F-06/F-07). Role checks
 * live here as @PreAuthorize — the security boundary, not the frontend. */
@RestController
@RequestMapping("/api/appointments")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    public AdminAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> listAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.listAll(date, status));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request.getStatus()));
    }
}
