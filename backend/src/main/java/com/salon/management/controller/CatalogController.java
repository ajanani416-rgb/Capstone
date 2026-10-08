package com.salon.management.controller;

import com.salon.management.dto.BarberRequest;
import com.salon.management.dto.BarberResponse;
import com.salon.management.dto.ServiceRequest;
import com.salon.management.dto.ServiceResponse;
import com.salon.management.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Catalog endpoints. Reads are public but role-aware (admins see inactive
 * rows); writes are ADMIN-only. */
@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/api/services")
    public ResponseEntity<List<ServiceResponse>> listServices(Authentication authentication) {
        return ResponseEntity.ok(catalogService.listServices(isAdmin(authentication)));
    }

    @GetMapping("/api/services/{id}")
    public ResponseEntity<ServiceResponse> getService(@PathVariable("id") Long id) {
        return ResponseEntity.ok(catalogService.getService(id));
    }

    @PostMapping("/api/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> createService(
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createService(request));
    }

    @PutMapping("/api/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> updateService(@PathVariable("id") Long id,
            @Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(catalogService.updateService(id, request));
    }

    @DeleteMapping("/api/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable("id") Long id) {
        catalogService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/barbers")
    public ResponseEntity<List<BarberResponse>> listBarbers(Authentication authentication) {
        return ResponseEntity.ok(catalogService.listBarbers(isAdmin(authentication)));
    }

    @GetMapping("/api/barbers/{id}")
    public ResponseEntity<BarberResponse> getBarber(@PathVariable("id") Long id) {
        return ResponseEntity.ok(catalogService.getBarber(id));
    }

    @PostMapping("/api/barbers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BarberResponse> createBarber(
            @Valid @RequestBody BarberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createBarber(request));
    }

    @PutMapping("/api/barbers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BarberResponse> updateBarber(@PathVariable("id") Long id,
            @Valid @RequestBody BarberRequest request) {
        return ResponseEntity.ok(catalogService.updateBarber(id, request));
    }

    @DeleteMapping("/api/barbers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBarber(@PathVariable("id") Long id) {
        catalogService.deleteBarber(id);
        return ResponseEntity.noContent().build();
    }
}
