package com.salon.management.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.management.entity.Barber;
import com.salon.management.entity.OtpPurpose;
import com.salon.management.entity.Role;
import com.salon.management.entity.Service;
import com.salon.management.entity.User;
import com.salon.management.repository.BarberRepository;
import com.salon.management.repository.ServiceRepository;
import com.salon.management.repository.UserRepository;
import com.salon.management.security.JwtService;
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

/** TASK-007 contract tests: sum-ahead wait formula, per-barber independence,
 * terminal/in-service handling, default-to-today, ADMIN-only boundary, and
 * the live wait on the customer detail view. */
@SpringBootTest
@AutoConfigureMockMvc
class QueueApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository users;
    @Autowired
    private ServiceRepository services;
    @Autowired
    private BarberRepository barbers;
    @Autowired
    private JwtService jwtService;

    @Autowired
    private OtpService otpService;

    private Service svc30;
    private Service svc45;
    private Barber barber;
    private Barber otherBarber;

    @BeforeEach
    void catalog() {
        String tag = UUID.randomUUID().toString();
        svc30 = services.save(new Service("S30-" + tag, null, 30, new BigDecimal("100.00"), true));
        svc45 = services.save(new Service("S45-" + tag, null, 45, new BigDecimal("150.00"), true));
        barber = barbers.save(new Barber("B-" + tag, null, true));
        otherBarber = barbers.save(new Barber("OB-" + tag, null, true));
    }

    private static final AtomicLong PHONE_SEQ = new AtomicLong(9194000000L);

    private static String phone() {
        return "+" + PHONE_SEQ.getAndIncrement();
    }

    private String customerToken() throws Exception {
        String number = phone();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Q", "phone", number))))
                .andExpect(status().isCreated());
        MvcResult r = mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", number,
                                "code", otpService.lastIssuedCode(number, OtpPurpose.REGISTER),
                                "purpose", "REGISTER"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private String adminToken() {
        User admin = users.save(new User("A", phone(), Role.ADMIN));
        return jwtService.generateToken(admin.getId(), "ADMIN");
    }

    private long book(String token, long serviceId, long barberId, String date, String time)
            throws Exception {
        MvcResult r = mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceId", serviceId, "barberId", barberId,
                                "appointmentDate", date, "appointmentTime", time))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private void moveTo(String admin, long id, String status) throws Exception {
        mvc.perform(patch("/api/appointments/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void waitsSumDurationsAhead() throws Exception {
        String cust = customerToken();
        String admin = adminToken();
        String date = LocalDate.now().plusDays(20).toString();
        long first = book(cust, svc30.getId(), barber.getId(), date, "10:00");
        book(cust, svc45.getId(), barber.getId(), date, "11:00");
        book(cust, svc30.getId(), barber.getId(), date, "12:00");

        mvc.perform(get("/api/queue?date=" + date).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].estimatedWaitMinutes", is(0)))
                .andExpect(jsonPath("$[1].estimatedWaitMinutes", is(30)))
                .andExpect(jsonPath("$[2].estimatedWaitMinutes", is(75)));

        // First starts service: still occupies the chair for those behind.
        moveTo(admin, first, "IN_SERVICE");
        mvc.perform(get("/api/queue?date=" + date).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].estimatedWaitMinutes", is(30)));
    }

    @Test
    void inServiceWaitsZeroTerminalCountsNothing() throws Exception {
        String cust = customerToken();
        String admin = adminToken();
        String date = LocalDate.now().plusDays(21).toString();
        long first = book(cust, svc30.getId(), barber.getId(), date, "10:00");
        long second = book(cust, svc45.getId(), barber.getId(), date, "11:00");

        moveTo(admin, first, "IN_SERVICE");
        mvc.perform(get("/api/queue?date=" + date).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estimatedWaitMinutes", is(0)))
                .andExpect(jsonPath("$[1].estimatedWaitMinutes", is(30)));

        moveTo(admin, first, "COMPLETED");
        mvc.perform(get("/api/queue?date=" + date).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estimatedWaitMinutes", is(0)))
                .andExpect(jsonPath("$[1].estimatedWaitMinutes", is(0)));

        // Cancel the second too: everything terminal → all zero.
        moveTo(admin, second, "CANCELLED");
        mvc.perform(get("/api/queue?date=" + date).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].estimatedWaitMinutes", is(0)));
    }

    @Test
    void barbersHaveIndependentLines() throws Exception {
        String cust = customerToken();
        String admin = adminToken();
        String date = LocalDate.now().plusDays(22).toString();
        book(cust, svc45.getId(), barber.getId(), date, "10:00");
        book(cust, svc30.getId(), otherBarber.getId(), date, "10:00");

        mvc.perform(get("/api/queue?date=" + date).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].estimatedWaitMinutes", is(0)))
                .andExpect(jsonPath("$[1].estimatedWaitMinutes", is(0)));
    }

    @Test
    void defaultsToTodayAndCustomerIsForbidden() throws Exception {
        String cust = customerToken();
        String admin = adminToken();
        String today = LocalDate.now().toString();
        // Same-day future slot: use a late hour to stay bookable at any test time.
        book(cust, svc30.getId(), barber.getId(), today, "23:00");

        mvc.perform(get("/api/queue").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].estimatedWaitMinutes", is(0)));
        mvc.perform(get("/api/queue?date=" + today).header("Authorization", "Bearer " + cust))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailCarriesLiveWait() throws Exception {
        String cust = customerToken();
        String date = LocalDate.now().plusDays(23).toString();
        book(cust, svc30.getId(), barber.getId(), date, "10:00");
        long second = book(cust, svc30.getId(), barber.getId(), date, "11:00");

        mvc.perform(get("/api/appointments/" + second).header("Authorization", "Bearer " + cust))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedWaitMinutes", is(30)));
    }
}
