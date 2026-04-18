package com.shop.ecommerce.services;

import com.shop.ecommerce.dto.support.*;
import com.shop.ecommerce.entities.*;
import com.shop.ecommerce.enums.TicketStatus;
import com.shop.ecommerce.enums.TicketType;
import com.shop.ecommerce.repository.*;
import com.shop.ecommerce.config.SqlInjectionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final TicketResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final SqlInjectionValidator sqlInjectionValidator;

    public TicketDTO createTicket(Long userId, TicketCreateDTO dto) {
        // SQL injection validation on user-submitted text
        sqlInjectionValidator.validate("ticket subject", dto.getSubject());
        sqlInjectionValidator.validate("ticket message", dto.getMessage());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setSubject(dto.getSubject());
        ticket.setMessage(dto.getMessage());
        ticket.setType(TicketType.valueOf(dto.getType()));
        ticket.setStatus(TicketStatus.OPEN);

        if (dto.getProductId() != null) {
            Product product = productRepository.findById(dto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            ticket.setProduct(product);
        }

        if (dto.getReviewId() != null) {
            Review review = reviewRepository.findById(dto.getReviewId())
                    .orElseThrow(() -> new RuntimeException("Review not found"));
            ticket.setReview(review);
        }

        SupportTicket saved = ticketRepository.save(ticket);
        return toDTO(saved);
    }

    public List<TicketDTO> getUserTickets(Long userId) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<TicketDTO> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public TicketDTO getTicketById(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        return toDTO(ticket);
    }

    public TicketResponseDTO addResponse(Long ticketId, Long adminId, TicketResponseCreateDTO dto) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        TicketResponse response = new TicketResponse();
        response.setTicket(ticket);
        response.setAdmin(admin);
        // SQL injection validation on admin response text
        sqlInjectionValidator.validate("ticket response", dto.getMessage());
        response.setMessage(dto.getMessage());

        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            ticket.setStatus(TicketStatus.valueOf(dto.getStatus()));
            ticketRepository.save(ticket);
        }

        TicketResponse saved = responseRepository.save(response);
        return toResponseDTO(saved);
    }

    public TicketDTO updateTicketStatus(Long ticketId, String status) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(TicketStatus.valueOf(status));
        ticketRepository.save(ticket);
        return toDTO(ticket);
    }

    private TicketDTO toDTO(SupportTicket ticket) {
        TicketDTO dto = new TicketDTO();
        dto.setId(ticket.getId());
        dto.setUserId(ticket.getUser().getId());
        dto.setUserName(ticket.getUser().getName() + " " + ticket.getUser().getSurname());
        dto.setSubject(ticket.getSubject());
        dto.setMessage(ticket.getMessage());
        dto.setType(ticket.getType().name());
        dto.setStatus(ticket.getStatus().name());
        dto.setCreatedAt(ticket.getCreatedAt());

        if (ticket.getProduct() != null) {
            dto.setProductId(ticket.getProduct().getId());
            dto.setProductName(ticket.getProduct().getName());
        }
        if (ticket.getReview() != null) {
            dto.setReviewId(ticket.getReview().getId());
        }

        List<TicketResponseDTO> responses = responseRepository
                .findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
        dto.setResponses(responses);

        return dto;
    }

    private TicketResponseDTO toResponseDTO(TicketResponse response) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(response.getId());
        dto.setAdminName(response.getAdmin().getName() + " " + response.getAdmin().getSurname());
        dto.setMessage(response.getMessage());
        dto.setCreatedAt(response.getCreatedAt());
        return dto;
    }
}
