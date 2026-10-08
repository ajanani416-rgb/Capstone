package com.salon.management.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.management.entity.OtpCode;
import com.salon.management.entity.OtpPurpose;
import com.salon.management.entity.Role;
import com.salon.management.entity.User;
import com.salon.management.mail.EmailSender;
import com.salon.management.repository.OtpCodeRepository;
import com.salon.management.repository.UserRepository;
import com.salon.management.security.JwtService;
import com.salon.management.service.OtpService;
import io.jsonwebtoken.Claims;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** PHASE 9: end-to-end OTP verification against the REAL local PostgreSQL
 * (not H2). Boots the full application context with ddl-auto=validate and
 * Flyway over salon_management, so this class also proves startup, Flyway
 * currency, and Hibernate mapping on the production dialect.
 *
 * OTPs travel only in memory (test hook + local variables) — no value is
 * ever printed, logged, or asserted into failure messages. SMTP is mocked;
 * delivery was proven separately (Phase 8).
 *
 * Skipped gracefully where no local PG answers (see condition class). Each
 * test uses a fresh address; rows remain (no user-delete endpoint exists). */
@ExtendWith(RequiresLocalPostgresCondition.class)
@SpringBootTest(properties = {
        "spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/salon_management}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.username=${DB_USERNAME:postgres}",
        "spring.datasource.password=${DB_PASSWORD:password}",
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.otp.expose-last-code=true"})
@AutoConfigureMockMvc
class EmailOtpVerificationFlowTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OtpService otpService;
    @Autowired
    private OtpCodeRepository codes;
    @Autowired
    private UserRepository users;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private EmailSender emailSender;

    private static final AtomicLong SEQ = new AtomicLong();
    // Real PostgreSQL persists across runs: stamp each run uniquely so
    // re-runs never collide with rows left by previous runs.
    private static final long RUN = System.nanoTime();

    private static String email() {
        return "phase9-" + RUN + "-" + SEQ.getAndIncrement() + "@example.com";
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private void register(String address) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Phase Nine", "email", address,
                                "password", "Password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is(address)))
                .andExpect(jsonPath("$.purpose", is("REGISTER")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(emailSender).sendVerificationCode(eq(address), anyString(), anyString());
    }

    private MvcResult submit(String address, String code, OtpPurpose purpose) throws Exception {
        return mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "code", code,
                                "purpose", purpose.name()))))
                .andReturn();
    }

    private static void assertNoToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("\"token\"");
    }

    @Test
    void successFlowVerifiesAndMintsValidJwt() throws Exception {
        String address = email();
        register(address);

        User before = users.findByEmail(address).orElseThrow();
        assertThat(before.getEmailVerified()).isFalse();
        assertThat(before.getRole()).isEqualTo(Role.CUSTOMER);
        OtpCode live = codes
                .findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        address, OtpPurpose.REGISTER)
                .orElseThrow();
        assertThat(live.isLive()).isTrue();

        String code = otpService.lastIssuedCode(address, OtpPurpose.REGISTER);
        MvcResult verified = submit(address, code, OtpPurpose.REGISTER);
        assertThat(verified.getResponse().getStatus()).isEqualTo(200);
        String body = verified.getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("token").asText();
        assertThat(objectMapper.readTree(body).get("role").asText()).isEqualTo("CUSTOMER");

        Claims claims = jwtService.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(before.getId().toString());
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");

        User after = users.findByEmail(address).orElseThrow();
        assertThat(after.getEmailVerified()).isTrue();
        assertThat(codes.findById(live.getId()).orElseThrow().isUsed()).isTrue();

        // Single-use: the same code is dead now.
        MvcResult replay = submit(address, code, OtpPurpose.REGISTER);
        assertThat(replay.getResponse().getStatus()).isEqualTo(401);
        assertNoToken(replay);
    }

    @Test
    void wrongCodeIs401AndCountsAttempt() throws Exception {
        String address = email();
        register(address);

        MvcResult wrong = submit(address, "000000", OtpPurpose.REGISTER);
        assertThat(wrong.getResponse().getStatus()).isEqualTo(401);
        assertNoToken(wrong);

        OtpCode otp = codes
                .findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        address, OtpPurpose.REGISTER)
                .orElseThrow();
        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(users.findByEmail(address).orElseThrow().getEmailVerified()).isFalse();
    }

    @Test
    void fiveWrongAttemptsExhaustTheCode() throws Exception {
        String address = email();
        register(address);

        for (int i = 0; i < OtpCode.MAX_ATTEMPTS - 1; i++) {
            MvcResult attempt = submit(address, "000000", OtpPurpose.REGISTER);
            assertThat(attempt.getResponse().getStatus()).isEqualTo(401);
            assertNoToken(attempt);
        }
        MvcResult fifth = submit(address, "000000", OtpPurpose.REGISTER);
        assertThat(fifth.getResponse().getStatus()).isEqualTo(410);
        assertNoToken(fifth);

        // Even the right code is unusable now.
        String code = otpService.lastIssuedCode(address, OtpPurpose.REGISTER);
        MvcResult late = submit(address, code, OtpPurpose.REGISTER);
        assertThat(late.getResponse().getStatus()).isNotEqualTo(200);
        assertNoToken(late);
        assertThat(users.findByEmail(address).orElseThrow().getEmailVerified()).isFalse();
    }

    @Test
    void expiredCodeIs410WithoutVerifying() throws Exception {
        String address = email();
        register(address);

        OtpCode otp = codes
                .findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        address, OtpPurpose.REGISTER)
                .orElseThrow();
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        codes.saveAndFlush(otp);

        String code = otpService.lastIssuedCode(address, OtpPurpose.REGISTER);
        MvcResult expired = submit(address, code, OtpPurpose.REGISTER);
        assertThat(expired.getResponse().getStatus()).isEqualTo(410);
        assertNoToken(expired);
        assertThat(users.findByEmail(address).orElseThrow().getEmailVerified()).isFalse();
    }

    @Test
    void unverifiedLoginIs403WithFreshCodeAndNoJwt() throws Exception {
        String address = email();
        register(address);

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", address, "password", "Password123"))))
                .andExpect(status().isForbidden())
                .andReturn();
        assertNoToken(login);
        assertThat(users.findByEmail(address).orElseThrow().getEmailVerified()).isFalse();
        // Fresh REGISTER code was issued alongside the 403.
        assertThat(otpService.lastIssuedCode(address, OtpPurpose.REGISTER)).isNotNull();
    }
}
