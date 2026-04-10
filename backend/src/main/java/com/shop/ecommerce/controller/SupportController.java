package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.support.*;
import com.shop.ecommerce.services.JwtService;
import com.shop.ecommerce.services.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<TicketDTO> createTicket(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TicketCreateDTO dto) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(supportService.createTicket(userId, dto));
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<List<TicketDTO>> getMyTickets(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        return ResponseEntity.ok(supportService.getUserTickets(userId));
    }

    @GetMapping("/my-tickets/{ticketId}")
    public ResponseEntity<TicketDTO> getMyTicket(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(supportService.getTicketById(ticketId));
    }

    @GetMapping("/admin/tickets")
    public ResponseEntity<List<TicketDTO>> getAllTickets() {
        return ResponseEntity.ok(supportService.getAllTickets());
    }

    @GetMapping("/admin/tickets/{ticketId}")
    public ResponseEntity<TicketDTO> getTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(supportService.getTicketById(ticketId));
    }

    @PostMapping("/admin/tickets/{ticketId}/respond")
    public ResponseEntity<TicketResponseDTO> respondToTicket(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long ticketId,
            @RequestBody TicketResponseCreateDTO dto) {
        Long adminId = extractUserId(authHeader);
        return ResponseEntity.ok(supportService.addResponse(ticketId, adminId, dto));
    }

    @PatchMapping("/admin/tickets/{ticketId}/status")
    public ResponseEntity<TicketDTO> updateStatus(
            @PathVariable Long ticketId,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(supportService.updateTicketStatus(ticketId, body.get("status")));
    }

    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtService.extractUserId(token);
    }
}
