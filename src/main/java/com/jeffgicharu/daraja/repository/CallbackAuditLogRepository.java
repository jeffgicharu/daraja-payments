package com.jeffgicharu.daraja.repository;

import com.jeffgicharu.daraja.domain.CallbackAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallbackAuditLogRepository extends JpaRepository<CallbackAuditLog, Long> {
}
