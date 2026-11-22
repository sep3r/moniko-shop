package com.monikoshop.repository;

import com.monikoshop.entity.VisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VisitLogRepository extends JpaRepository<VisitLog, Long>, JpaSpecificationExecutor<VisitLog> {

    Optional<VisitLog> findByVisitDateAndVisitorIdAndIp(LocalDate visitDate, String visitorId, String ip);

    @Modifying
    @Transactional
    @Query("delete from VisitLog v where v.visitDate < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDate cutoff);
}
