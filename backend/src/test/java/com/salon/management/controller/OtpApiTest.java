package com.salon.management.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salon.management.entity.OtpCode;
import com.salon.management.entity.OtpPurpose;
import com.salon.management.repository.OtpCodeRepository;
import com.salon.management.service.OtpService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** OTP lifecycle edge cases: resend invalidates, attempts exhaust to 410,
 * expiry to 410, bad purpose shape to 400, resend-after-verified to 400. */
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

    @Test
    void resendInvalidatesPreviousCode() throws Exception {
        String mail = "re-" + UUID.randomUUID() + "@example.com";
        String first = register(mail);
        mvc.perform(post("/api/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", mail, "purpose", "REGISTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purpose", is("REGISTER")));
        String second = otpService.lastIssuedCode(mail, OtpPurpose.REGISTER);
        // Old code is dead even if it differs (single-active-code rule).
        if (!first.equals(second)) {
            verify(mail, first, OtpPurpose.REGISTER, 401);
        }
        verify(mail, second, OtpPurpose.REGISTER, 200);
    }

    @Test
    void fiveWrongAttemptsExhaustTo410() throws Exception {
        String mail = "ex-" + UUID.randomUUID() + "@example.com";
        register(mail);
        for (int i = 0; i < OtpCode.MAX_ATTEMPTS - 1; i++) {
            verify(mail, "000000", OtpPurpose.REGISTER, 401);
        }
        // Fifth wrong try: the code dies and says 410 — request a fresh one.
        verify(mail, "000000", OtpPurpose.REGISTER, 410);
    }

    @Test
    void expiredCodeIs410() throws Exception {
        String mail = "old-" + UUID.randomUUID() + "@example.com";
        String code = register(mail);
        OtpCode otp = codes
                .findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        mail, OtpPurpose.REGISTER)
                .orElseThrow();
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        codes.saveAndFlush(otp);
        verify(mail, code, OtpPurpose.REGISTER, 410);
    }

    @Test
    void registerPurposeCodeCannotLogin() throws Exception {
        String mail = "xp-" + UUID.randomUUID() + "@example.com";
        String code = register(mail);
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", mail, "code", code,
                                "purpose", "LOGIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendAfterVerifiedIs400() throws Exception {
        String mail = "done-" + UUID.randomUUID() + "@example.com";
        String code = register(mail);
        verify(mail, code, OtpPurpose.REGISTER, 200);
        mvc.perform(post("/api/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", mail, "purpose", "REGISTER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already verified")));
    }

    @Test
    void malformedCodeIs400() throws Exception {
        String mail = "bad-" + UUID.randomUUID() + "@example.com";
        register(mail);
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", mail, "code", "abc",
                                "purpose", "REGISTER"))))
                .andExpect(status().isBadRequest());
    }
}
