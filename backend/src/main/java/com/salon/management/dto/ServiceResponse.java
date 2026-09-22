package com.salon.management.dto;

import com.salon.management.entity.Service;
import java.math.BigDecimal;

/** Catalog service view — the entity itself is never serialized. */
public class ServiceResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final Integer durationMinutes;
    private final BigDecimal price;
    private final Boolean active;

    public ServiceResponse(Service service) {
        this.id = service.getId();
        this.name = service.getName();
        this.description = service.getDescription();
        this.durationMinutes = service.getDurationMinutes();
        this.price = service.getPrice();
        this.active = service.getActive();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Boolean getActive() {
        return active;
    }
}
