package com.salon.management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** TASK-006 contract tests: admin list/filters, strict transitions,
 * catalog CRUD + referenced-delete block, role enforcement, users list. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminApiTest {

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

    private Service service;
    private Barber barber;

    @BeforeEach
    void catalog() {
        service = services.save(new Service("Cut-" + UUID.randomUUID(), null, 30,
                new BigDecimal("299.00"), true));
        barber = barbers.save(new Barber("Barber-" + UUID.randomUUID(), null, true));
    }

    private static final AtomicLong PHONE_SEQ = new AtomicLong(9195000000L);

    private static String phone() {
        return "+" + PHONE_SEQ.getAndIncrement();
    }

    private String customerToken() throws Exception {
        String number = phone();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Cust", "phone", number, "password", "Password123"))))
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
        User admin = users.save(new User("Admin", phone(),
                "unused-hash", Role.ADMIN));
        return jwtService.generateToken(admin.getId(), "ADMIN");
    }

    private long book(String token, String date, String time) throws Exception {
        MvcResult r = mvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "serviceId", service.getId(), "barberId", barber.getId(),
                                "appointmentDate", date, "appointmentTime", time))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private String future(int plusDays) {
        return LocalDate.now().plusDays(plusDays).toString();
    }

    @Test
    void adminListFiltersByDateAndStatus() throws Exception {
        String cust = customerToken();
        String admin = adminToken();
        String d1 = future(10);
        String d2 = future(11);
        book(cust, d1, "10:00");
        long id2 = book(cust, d2, "10:00");
        mvc.perform(patch("/api/appointments/" + id2 + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_SERVICE\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/appointments?date=" + d1)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        // Combined filters narrow to this test's own row (the shared H2 keeps
        // other tests' IN_SERVICE rows, so a status-only filter would see them).
        mvc.perform(get("/api/appointments?date=" + d2 + "&status=IN_SERVICE")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is((int) id2)));
        mvc.perform(get("/api/appointments")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    void customerCannotReachAdminEndpoints() throws Exception {
        String cust = customerToken();
        mvc.perform(get("/api/appointments").header("Authorization", "Bearer " + cust))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/appointments/1/status")
                        .header("Authorization", "Bearer " + cust)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + cust)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "X", "durationMinutes", 10, "price", 50))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + cust))
                .andExpect(status().isForbidden());
    }

    @Test
    void legalTransitionsSucceed() throws Exception {
        String admin = adminToken();
        long id = book(customerToken(), future(12), "10:00");
        mvc.perform(patch("/api/appointments/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_SERVICE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_SERVICE")));
        mvc.perform(patch("/api/appointments/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void illegalTransitionsAre409() throws Exception {
        String admin = adminToken();
        long id = book(customerToken(), future(13), "10:00");
        // Skip-ahead QUEUED -> COMPLETED.
        mvc.perform(patch("/api/appointments/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Cannot move")));
        // Reach terminal COMPLETED, then try to move again.
        mvc.perform(patch("/api/appointments/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_SERVICE\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/appointments/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/appointments/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict());
        // Missing status field is 400, not 409.
        mvc.perform(patch("/api/appointments/" + id + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogCrudAndReferencedDeleteBlock() throws Exception {
        String admin = adminToken();
        String auth = "Bearer " + admin;

        MvcResult created = mvc.perform(post("/api/services")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Shave", "durationMinutes", 15, "price", 149.00))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active", is(true)))
                .andReturn();
        long sid = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/api/services/" + sid)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Shave+", "durationMinutes", 20,
                                "price", 199.00, "active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Shave+")));

        // Public list hides the deactivated row; admin list shows it.
        mvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + sid + ")]", hasSize(0)));
        mvc.perform(get("/api/services").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + sid + ")]", hasSize(1)));

        // Referenced service cannot be hard-deleted.
        book(customerToken(), future(14), "10:00");
        mvc.perform(delete("/api/services/" + service.getId())
                        .header("Authorization", auth))
                .andExpect(status().isConflict());
        // Unreferenced row deletes cleanly.
        MvcResult b = mvc.perform(post("/api/barbers")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Temp"))))
                .andExpect(status().isCreated())
                .andReturn();
        long bid = objectMapper.readTree(b.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(delete("/api/barbers/" + bid).header("Authorization", auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void usersListHasNoSecrets() throws Exception {
        String cust = customerToken();
        String number = phone();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Listed", "phone", number, "password", "Password123"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/users").header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.phone=='" + number + "')]", hasSize(1)))
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[*].password").doesNotExist());
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + cust))
                .andExpect(status().isForbidden());
    }
}
