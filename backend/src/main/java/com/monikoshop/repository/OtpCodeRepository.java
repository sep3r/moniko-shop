package com.monikoshop.repository;

import com.monikoshop.entity.OtpCode;
import com.monikoshop.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByPhoneOrderByCreatedAtDesc(String phone);

    Optional<OtpCode> findFirstByPhoneAndPurposeAndConsumedFalseOrderByCreatedAtDesc(String phone, OtpPurpose purpose);

    long countByPhoneAndCreatedAtAfter(String phone, Instant since);

    long countByCreatedAtAfter(Instant since);

    /** افزایش اتمیک تعداد تلاش؛ اگر به سقف رسیده باشد 0 برمی‌گرداند (جلوی تلاش‌های موازی را هم می‌گیرد). */
    @Transactional
    @Modifying
    @Query("update OtpCode o set o.attempts = o.attempts + 1 where o.id = :id and o.attempts < :max and o.consumed = false")
    int incrementAttempts(@Param("id") Long id, @Param("max") int max);

    @Transactional
    @Modifying
    @Query("update OtpCode o set o.consumed = true where o.id = :id")
    int markConsumed(@Param("id") Long id);

    /** فقط آخرین کد معتبر بماند: کدهای قبلیِ همان شماره و هدف باطل می‌شوند. */
    @Transactional
    @Modifying
    @Query("update OtpCode o set o.consumed = true where o.phone = :phone and o.purpose = :purpose and o.consumed = false")
    int invalidateAll(@Param("phone") String phone, @Param("purpose") OtpPurpose purpose);

    @Transactional
    @Modifying
    @Query("delete from OtpCode o where o.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
