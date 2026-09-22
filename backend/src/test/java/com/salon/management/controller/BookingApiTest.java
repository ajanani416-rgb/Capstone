package com.salon.management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.management.entity.Barber;
import com.salon.management.entity.OtpPurpose;
import com.salon.management.entity.Service;
import com.salon.management.repository.BarberRepository;
import com.salon.management.repository.ServiceRepository;
import com.salon.management.service.OtpService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** TASK-005 contract tests: happy booking (201, QUEUED, per-day queue #),
 * D2 conflict → 409, catalog/date validation → 400/404, owner isolation,
 * and the 401 boundary. */
@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OtpService otpService;

    @Autowired
    private ServiceRepository services;

    @Autowired
    private BarberRepository barbers;

    private Service service;
    private Barber barber;

    @BeforeEach
    void catalog() {
        service = services.save(new Service("Cut-" + UUID.randomUUID(), null, 30,
                new BigDecimal("299.00"), true));
        barber = barbers.save(new Barber("Barber-" + UUID.randomUUID(), null, true));
    }

    private static final AtomicLong PHONE_SEQ = new AtomicLong(9193000000L);

    private static String phone() {
        return "+" + PHONE_SEQ.getAndIncrement();
    }

    private String token() throws Exception {
        String number = phone();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Booker", "phone", number, "password", "Password123"))))
                .andExpect(status().isCreated());
        MvcResult result = mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", number,
                                "code", otpService.lastIssuedCode(number, OtpPurpose.REGISTER),
                                "purpose", "REGISTER"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private Map<String, Object> slot(String date, String time) {
        return Map.of("serviceId", service.getId(), "barberId", barber.getId(),
                "appointmentDate", date, "appointmentTime", time);
    }

    private String futureDate(int plusDays) {
        return LocalDate.now().plusDays(plusDays).toString();
    }

    @Test
    void bookHappyPathAssignsQueuedAndQueueNumberOne() throws Exception {
        String date = futureDate(3);
        mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(date, "10:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("QUEUED")))
                .andExpect(jsonPath("$.queueNumber", is(1)))
                .andExpect(jsonPath("$.serviceName", is(service.getName())))
                .andExpect(jsonPath("$.barberName", is(barber.getName())));

        // Second booking same day, other slot → queue number 2 (per-day sequence).
        mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(date, "11:00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queueNumber", is(2)));
    }

    @Test
    void sameSlotSameBarberIs409() throws Exception {
        String date = futureDate(4);
        String auth = token();
        mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(date, "10:00"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(date, "10:00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already booked")));
    }

    @Test
    void pastDateIs400() throws Exception {
        mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                slot(LocalDate.now().minusDays(1).toString(), "10:00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownServiceIs404AndInactiveIs400() throws Exception {
        String auth = token();
        String date = futureDate(5);
        mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceId", 999999, "barberId", barber.getId(),
                                "appointmentDate", date, "appointmentTime", "10:00"))))
                .andExpect(status().isNotFound());

        service.setActive(false);
        services.save(service);
        mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(date, "12:00"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ownersAreIsolatedAndDetailIsOwnerOrAdmin() throws Exception {
        String date = futureDate(6);
        String mine = token();
        MvcResult created = mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + mine)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot(date, "10:00"))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        String other = token();
        mvc.perform(get("/api/appointments/me")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/appointments/" + id)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/appointments/" + id)
                        .header("Authorization", "Bearer " + mine))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueNumber", is(1)));
    }

    @Test
    void bookingWithoutTokenIs401() throws Exception {
        mvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                slot(futureDate(7), "10:00"))))
                .andExpect(status().isUnauthorized());
    }
}
