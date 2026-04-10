package com.shop.ecommerce.repository;

import com.shop.ecommerce.entities.TicketResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketResponseRepository extends JpaRepository<TicketResponse, Long> {

    List<TicketResponse> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
