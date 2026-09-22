package com.salon.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create/update body for barbers. Same delete policy as services:
 * blocked while appointments reference the row (409). */
public class BarberRequest {

    @NotBlank(message = "Name is required.")
    @Size(max = 255, message = "Name must be at most 255 characters.")
    private String name;

    @Size(max = 255, message = "Specialization must be at most 255 characters.")
    private String specialization;

    private Boolean active;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
