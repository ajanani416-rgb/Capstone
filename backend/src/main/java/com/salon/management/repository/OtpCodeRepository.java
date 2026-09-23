package com.salon.management.repository;

import com.salon.management.entity.OtpCode;
import com.salon.management.entity.OtpPurpose;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link OtpCode}. */
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String email, OtpPurpose purpose);

    List<OtpCode> findByEmailAndPurposeAndUsedFalse(String email, OtpPurpose purpose);

    /** Latest code of any state — drives the 60-second resend cooldown. */
    Optional<OtpCode> findFirstByEmailAndPurposeOrderByCreatedAtDesc(
            String email, OtpPurpose purpose);
}
