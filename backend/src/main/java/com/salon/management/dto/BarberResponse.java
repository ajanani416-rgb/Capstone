package com.salon.management.dto;

import com.salon.management.entity.Barber;

/** Barber view — the entity itself is never serialized. */
public class BarberResponse {

    private final Long id;
    private final String name;
    private final String specialization;
    private final Boolean active;

    public BarberResponse(Barber barber) {
        this.id = barber.getId();
        this.name = barber.getName();
        this.specialization = barber.getSpecialization();
        this.active = barber.getActive();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public Boolean getActive() {
        return active;
    }
}
