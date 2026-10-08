package com.salon.management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.management.entity.OtpCode;
import com.salon.management.entity.OtpPurpose;
import com.salon.management.mail.EmailSender;
import com.salon.management.repository.OtpCodeRepository;
import com.salon.management.service.OtpService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** OTP lifecycle edge cases: resend invalidates, 60s cooldown → 429,
 * attempts exhaust to 410, expiry to 410, bad purpose shape to 400,
 * resend-after-verified to 400. SMTP is mocked; cooldown aging is simulated
 * by backdating created_at via JdbcTemplate (updatable=false blocks JPA). */
@SpringBootTest
@AutoConfigureMockMvc
class OtpApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OtpService otpService;
    @Autowired
    private OtpCodeRepository codes;
    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private EmailSender emailSender;

    private static final AtomicLong EMAIL_SEQ = new AtomicLong();

    private static String email() {
        return "otp" + EMAIL_SEQ.getAndIncrement() + "@example.com";
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private String register(String email) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "O", "email", email,
                                "password", "Password123"))))
                .andExpect(status().isCreated());
        return otpService.lastIssuedCode(email, OtpPurpose.REGISTER);
    }

    private void verify(String email, String code, OtpPurpose purpose, int expected)
            throws Exception {
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "code", code,
                                "purpose", purpose.name()))))
                .andExpect(status().is(expected));
    }

    /** Backdates the latest code past the 60s cooldown (test-only). */
    private void ageLatestCode(String email, OtpPurpose purpose) {
        jdbc.update("UPDATE otp_codes SET created_at = DATEADD('SECOND', -61, NOW())"
                + " WHERE email = ? AND purpose = ?",
                email, purpose.name());
    }

    @Test
    void resendInvalidatesPreviousCode() throws Exception {
        String address = email();
        String first = register(address);
        ageLatestCode(address, OtpPurpose.REGISTER);
        mvc.perform(post("/api/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "purpose", "REGISTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purpose", is("REGISTER")));
        String second = otpService.lastIssuedCode(address, OtpPurpose.REGISTER);
        // Old code is dead even if it differs (single-active-code rule).
        if (!first.equals(second)) {
            verify(address, first, OtpPurpose.REGISTER, 401);
        }
        verify(address, second, OtpPurpose.REGISTER, 200);
    }

    @Test
    void immediateResendIs429() throws Exception {
        String address = email();
        register(address);
        mvc.perform(post("/api/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "purpose", "REGISTER"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message", containsString("wait")));
    }

    @Test
    void fiveWrongAttemptsExhaustTo410() throws Exception {
        String address = email();
        register(address);
        for (int i = 0; i < OtpCode.MAX_ATTEMPTS - 1; i++) {
            verify(address, "000000", OtpPurpose.REGISTER, 401);
        }
        // Fifth wrong try: the code dies and says 410 — request a fresh one.
        verify(address, "000000", OtpPurpose.REGISTER, 410);
    }

    @Test
    void expiredCodeIs410() throws Exception {
        String address = email();
        String code = register(address);
        OtpCode otp = codes
                .findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        address, OtpPurpose.REGISTER)
                .orElseThrow();
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        codes.saveAndFlush(otp);
        verify(address, code, OtpPurpose.REGISTER, 410);
    }

    @Test
    void registerPurposeCodeCannotLogin() throws Exception {
        String address = email();
        String code = register(address);
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "code", code,
                                "purpose", "LOGIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendAfterVerifiedIs400() throws Exception {
        String address = email();
        String code = register(address);
        verify(address, code, OtpPurpose.REGISTER, 200);
        mvc.perform(post("/api/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "purpose", "REGISTER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already verified")));
    }

    @Test
    void malformedCodeIs400() throws Exception {
        String address = email();
        register(address);
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "code", "abc",
                                "purpose", "REGISTER"))))
                .andExpect(status().isBadRequest());
    }
}
