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
import java.util.concurrent.atomic.AtomicLong;
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

    private static final AtomicLong PHONE_SEQ = new AtomicLong(9192000000L);

    private static String phone() {
        return "+" + PHONE_SEQ.getAndIncrement();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private String register(String phone) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "O", "phone", phone,
                                "password", "Password123"))))
                .andExpect(status().isCreated());
        return otpService.lastIssuedCode(phone, OtpPurpose.REGISTER);
    }

    private void verify(String phone, String code, OtpPurpose purpose, int expected)
            throws Exception {
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", phone, "code", code,
                                "purpose", purpose.name()))))
                .andExpect(status().is(expected));
    }

    @Test
    void resendInvalidatesPreviousCode() throws Exception {
        String number = phone();
        String first = register(number);
        mvc.perform(post("/api/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", number, "purpose", "REGISTER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purpose", is("REGISTER")));
        String second = otpService.lastIssuedCode(number, OtpPurpose.REGISTER);
        // Old code is dead even if it differs (single-active-code rule).
        if (!first.equals(second)) {
            verify(number, first, OtpPurpose.REGISTER, 401);
        }
        verify(number, second, OtpPurpose.REGISTER, 200);
    }

    @Test
    void fiveWrongAttemptsExhaustTo410() throws Exception {
        String number = phone();
        register(number);
        for (int i = 0; i < OtpCode.MAX_ATTEMPTS - 1; i++) {
            verify(number, "000000", OtpPurpose.REGISTER, 401);
        }
        // Fifth wrong try: the code dies and says 410 — request a fresh one.
        verify(number, "000000", OtpPurpose.REGISTER, 410);
    }

    @Test
    void expiredCodeIs410() throws Exception {
        String number = phone();
        String code = register(number);
        OtpCode otp = codes
                .findFirstByPhoneAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        number, OtpPurpose.REGISTER)
                .orElseThrow();
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        codes.saveAndFlush(otp);
        verify(number, code, OtpPurpose.REGISTER, 410);
    }

    @Test
    void registerPurposeCodeCannotLogin() throws Exception {
        String number = phone();
        String code = register(number);
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", number, "code", code,
                                "purpose", "LOGIN"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resendAfterVerifiedIs400() throws Exception {
        String number = phone();
        String code = register(number);
        verify(number, code, OtpPurpose.REGISTER, 200);
        mvc.perform(post("/api/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", number, "purpose", "REGISTER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already verified")));
    }

    @Test
    void malformedCodeIs400() throws Exception {
        String number = phone();
        register(number);
        mvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("phone", number, "code", "abc",
                                "purpose", "REGISTER"))))
                .andExpect(status().isBadRequest());
    }
}
