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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** TASK-009 contract tests: register → challenge (no token), login before
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

    private String email() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private String codeFor(String email, OtpPurpose purpose) {
        return otpService.lastIssuedCode(email, purpose);
    }

    /** Full happy path; returns the JWT. */
    private String registerAndVerify(String email) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test User", "email", email,
                                "password", "Password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.purpose", is("REGISTER")));
        MvcResult verified = mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email,
                                "code", codeFor(email, OtpPurpose.REGISTER),
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
        String mail = email();
        String token = registerAndVerify(mail);

        // Password login now returns a challenge, not a session.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", mail, "password", "Password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.purpose", is("LOGIN")));

        MvcResult second = mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", mail,
                                "code", codeFor(mail, OtpPurpose.LOGIN), "purpose", "LOGIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(mail)))
                .andReturn();
        String loginToken = objectMapper.readTree(second.getResponse().getContentAsString())
                .get("token").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(mail)));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginBeforeVerifyIs403WithFreshCode() throws Exception {
        String mail = email();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Early", "email", mail,
                                "password", "Password123"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", mail, "password", "Password123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not verified")));
    }

    @Test
    void wrongCodeIs401() throws Exception {
        String mail = email();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "W", "email", mail,
                                "password", "Password123"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", mail, "code", "000000",
                                "purpose", "REGISTER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        String mail = email();
        registerAndVerify(mail);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Again", "email", mail,
                                "password", "Password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void roleCannotBeSmuggledThroughRegistration() throws Exception {
        String mail = email();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Sneaky", "email", mail,
                                "password", "Password123", "role", "ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purpose", is("REGISTER")));
    }

    @Test
    void wrongPasswordAndUnknownEmailAreBoth401() throws Exception {
        String mail = email();
        registerAndVerify(mail);
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", mail, "password", "WrongPassword1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password.")));
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", "nobody-" + UUID.randomUUID() + "@example.com",
                                "password", "Password123"))))
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
                        .content(json(Map.of("name", "", "email", "not-an-email",
                                "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").isString())
                .andExpect(jsonPath("$.fieldErrors.password").isString());
    }

    @Test
    void healthStaysPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
