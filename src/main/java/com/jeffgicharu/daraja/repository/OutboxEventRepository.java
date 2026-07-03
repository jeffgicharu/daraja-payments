package com.jeffgicharu.daraja.repository;

import com.jeffgicharu.daraja.domain.OutboxEvent;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByPublishedAtIsNullOrderByIdAsc(Limit limit);
}
