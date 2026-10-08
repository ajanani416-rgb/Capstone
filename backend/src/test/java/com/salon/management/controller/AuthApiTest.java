package com.salon.management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.salon.management.entity.OtpPurpose;
import com.salon.management.exception.EmailSendFailedException;
import com.salon.management.mail.EmailSender;
import com.salon.management.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Email + password contract tests: register → challenge (no token, email
 * sent), verify → JWT, password login → JWT, unverified login → 403, wrong
 * password → 401, duplicates → 409, SMTP failure → 502, validation → 400.
 * SMTP is mocked — no test reads a real inbox; issued codes come from the
 * test-only OtpService hook. Passwords and OTPs never appear in responses. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OtpService otpService;

    @MockBean
    private EmailSender emailSender;

    private static final AtomicLong EMAIL_SEQ = new AtomicLong();

    private static String email() {
        return "user" + EMAIL_SEQ.getAndIncrement() + "@example.com";
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
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.email", is(email)))
                .andExpect(jsonPath("$.purpose", is("REGISTER")));
        verify(emailSender).sendVerificationCode(eq(email), anyString(), anyString());
        MvcResult verified = mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email,
                                "code", codeFor(email, OtpPurpose.REGISTER),
                                "purpose", "REGISTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andReturn();
        return objectMapper.readTree(verified.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    void registerVerifyLoginMe() throws Exception {
        String address = email();
        registerAndVerify(address);

        // Verified users log in with email+password and get the JWT directly.
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "password", "Password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email", is(address)))
                .andReturn();
        String loginToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("token").asText();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + loginToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(address)));
    }

    @Test
    void loginBeforeVerifyIs403WithFreshCode() throws Exception {
        String address = email();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Early", "email", address,
                                "password", "Password123"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "password", "Password123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("not verified")));
    }

    @Test
    void wrongPasswordIs401WithoutLeakage() throws Exception {
        String address = email();
        registerAndVerify(address);
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "password", "WrongPass1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password.")));
    }

    @Test
    void unknownEmailIs401WithSameMessage() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email(), "password", "Password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password.")));
    }

    @Test
    void wrongCodeIs401() throws Exception {
        String address = email();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "W", "email", address,
                                "password", "Password123"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "code", "000000",
                                "purpose", "REGISTER"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailIsConflict() throws Exception {
        String address = email();
        registerAndVerify(address);
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Again", "email", address,
                                "password", "Password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void roleCannotBeSmuggledThroughRegistration() throws Exception {
        String address = email();
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Sneaky", "email", address,
                                "password", "Password123", "role", "ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purpose", is("REGISTER")));
        MvcResult verified = mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address,
                                "code", codeFor(address, OtpPurpose.REGISTER),
                                "purpose", "REGISTER"))))
                .andExpect(status().isOk())
                .andReturn();
        // Unknown JSON fields are ignored: the account is still CUSTOMER.
        objectMapper.readTree(verified.getResponse().getContentAsString());
    }

    @Test
    void smtpFailureIs502AndLeavesNoUser() throws Exception {
        String address = email();
        // First send fails; the retry succeeds (stub chains throw→no-op).
        doThrow(new EmailSendFailedException()).doNothing().when(emailSender)
                .sendVerificationCode(eq(address), anyString(), anyString());
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "No Mail", "email", address,
                                "password", "Password123"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message", containsString("verification email")));
        // Rolled back: retrying registration is not a duplicate.
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "No Mail", "email", address,
                                "password", "Password123"))))
                .andExpect(status().isCreated());
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
                .andExpect(jsonPath("$.fieldErrors.password").isString())
                .andExpect(jsonPath("$.fieldErrors.name").isString());
    }

    @Test
    void healthStaysPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
