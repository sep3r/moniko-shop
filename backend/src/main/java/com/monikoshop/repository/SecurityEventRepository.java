package com.monikoshop.repository;

import com.monikoshop.entity.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long>, JpaSpecificationExecutor<SecurityEvent> {

    @Modifying
    @Transactional
    @Query("delete from SecurityEvent e where e.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
