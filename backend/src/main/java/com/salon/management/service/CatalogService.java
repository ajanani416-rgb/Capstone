package com.salon.management.service;

import com.salon.management.dto.BarberRequest;
import com.salon.management.dto.BarberResponse;
import com.salon.management.dto.ServiceRequest;
import com.salon.management.dto.ServiceResponse;
import com.salon.management.entity.Barber;
import com.salon.management.entity.Service;
import com.salon.management.repository.AppointmentRepository;
import com.salon.management.repository.BarberRepository;
import com.salon.management.repository.ServiceRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Catalog administration (FEATURES.md F-09/F-10). Reads are role-aware:
 * anonymous/CUSTOMER callers see active rows only; ADMIN sees all.
 * Writes are ADMIN-only (enforced at the controller with @PreAuthorize).
 * Hard DELETE is blocked while appointments reference the row (409) —
 * deactivation via update with active=false is the reversible path.
 */
@org.springframework.stereotype.Service
public class CatalogService {

    private final ServiceRepository services;
    private final BarberRepository barbers;
    private final AppointmentRepository appointments;

    public CatalogService(ServiceRepository services, BarberRepository barbers,
            AppointmentRepository appointments) {
        this.services = services;
        this.barbers = barbers;
        this.appointments = appointments;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> listServices(boolean admin) {
        List<Service> rows = admin ? services.findAll() : services.findByActiveTrue();
        return rows.stream().map(ServiceResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public ServiceResponse getService(Long id) {
        return new ServiceResponse(services.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Service not found.")));
    }

    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        Service service = new Service(request.getName().trim(),
                request.getDescription(), request.getDurationMinutes(),
                request.getPrice(), request.getActive() == null || request.getActive());
        return new ServiceResponse(services.save(service));
    }

    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        Service service = services.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Service not found."));
        service.setName(request.getName().trim());
        service.setDescription(request.getDescription());
        service.setDurationMinutes(request.getDurationMinutes());
        service.setPrice(request.getPrice());
        if (request.getActive() != null) {
            service.setActive(request.getActive());
        }
        return new ServiceResponse(services.save(service));
    }

    @Transactional
    public void deleteService(Long id) {
        Service service = services.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Service not found."));
        if (appointments.existsByServiceId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Service has appointments and cannot be deleted. Deactivate it instead.");
        }
        services.delete(service);
    }

    @Transactional(readOnly = true)
    public List<BarberResponse> listBarbers(boolean admin) {
        List<Barber> rows = admin ? barbers.findAll() : barbers.findByActiveTrue();
        return rows.stream().map(BarberResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public BarberResponse getBarber(Long id) {
        return new BarberResponse(barbers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Barber not found.")));
    }

    @Transactional
    public BarberResponse createBarber(BarberRequest request) {
        Barber barber = new Barber(request.getName().trim(), request.getSpecialization(),
                request.getActive() == null || request.getActive());
        return new BarberResponse(barbers.save(barber));
    }

    @Transactional
    public BarberResponse updateBarber(Long id, BarberRequest request) {
        Barber barber = barbers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Barber not found."));
        barber.setName(request.getName().trim());
        barber.setSpecialization(request.getSpecialization());
        if (request.getActive() != null) {
            barber.setActive(request.getActive());
        }
        return new BarberResponse(barbers.save(barber));
    }

    @Transactional
    public void deleteBarber(Long id) {
        Barber barber = barbers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Barber not found."));
        if (appointments.existsByBarberId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Barber has appointments and cannot be deleted. Deactivate them instead.");
        }
        barbers.delete(barber);
    }
}
