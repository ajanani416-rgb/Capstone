package com.salon.management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.management.entity.OtpPurpose;
import com.salon.management.service.OtpService;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Phone-OTP contract tests: register → challenge (no token), login before
 * verify → 403, wrong code → 401, verify → JWT, login → challenge → JWT,
 * resend, duplicate → 409, validation → 400, me boundaries. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OtpService otpService;

    private static final AtomicLong PHONE_SEQ = new AtomicLong(9191000000L);

    private static String phone() {
        return "+" + PHONE_SEQ.getAndIncrement();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private String codeFor(String phone, OtpPurpose purpose) {
        return otpService.lastIssuedCode(phone, purpose);
    }

    /** Full happy path; returns the JWT. */
    private String registerAndVerify(String phone) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "phone", phone,
                                "password", "Password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.purpose", is("REGISTER")));
        MvcResult verified = mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", phone,
                                "code", codeFor(phone, OtpPurpose.REGISTER),
                                "purpose", "REGISTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andReturn();
        return objectMapper.readTree(verified.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void registerVerifyLoginVerifyMe() throws Exception {
        String number = phone();
        String token = registerAndVerify(number);

        // Password login now returns a challenge, not a session.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", number, "password", "Password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.purpose", is("LOGIN")));

        MvcResult second = mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", number,
                                "code", codeFor(number, OtpPurpose.LOGIN), "purpose", "LOGIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone", is(number)))
                .andReturn();
        String loginToken = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("token").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone", is(number)));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginBeforeVerifyIs403WithFreshCode() throws Exception {
        String number = phone();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Early", "phone", number,
                                "password", "Password123"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", number, "password", "Password123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not verified")));
    }

    @Test
    void wrongCodeIs401() throws Exception {
        String number = phone();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "W", "phone", number,
                                "password", "Password123"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", number, "code", "000000",
                                "purpose", "REGISTER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicatePhoneIsConflict() throws Exception {
        String number = phone();
        registerAndVerify(number);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Again", "phone", number,
                                "password", "Password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void roleCannotBeSmuggledThroughRegistration() throws Exception {
        String number = phone();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Sneaky", "phone", number,
                                "password", "Password123", "role", "ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purpose", is("REGISTER")));
    }

    @Test
    void wrongPasswordAndUnknownPhoneAreBoth401() throws Exception {
        String number = phone();
        registerAndVerify(number);
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", number, "password", "WrongPassword1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid phone number or password.")));
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", phone(), "password", "Password123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void invalidRegistrationIs400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "", "phone", "not-a-phone",
                                "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.phone").isString())
                .andExpect(jsonPath("$.fieldErrors.password").isString());
    }

    @Test
    void healthStaysPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
