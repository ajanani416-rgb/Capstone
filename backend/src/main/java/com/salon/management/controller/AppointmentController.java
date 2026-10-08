package com.salon.management.controller;

import com.salon.management.dto.AppointmentResponse;
import com.salon.management.dto.CreateAppointmentRequest;
import com.salon.management.service.AppointmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authenticated: POST book, GET own list, GET one (owner-or-admin).
 * Any authenticated role may book for themselves; admin management
 * (filters, status writes) lives in AdminAppointmentController. */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(Authentication authentication,
            @Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse created = appointmentService.book(
                Long.parseLong(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/me")
    public ResponseEntity<List<AppointmentResponse>> listMine(Authentication authentication) {
        return ResponseEntity.ok(appointmentService
                .listMine(Long.parseLong(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> get(Authentication authentication,
            @PathVariable("id") Long id) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(appointmentService.getVisibleTo(
                Long.parseLong(authentication.getName()), admin, id));
    }
}
