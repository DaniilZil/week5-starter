package com.dsu.roomapi;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/rooms/{roomId}/reservations")
    public List<ReservationResponse> listForRoom(@PathVariable Long roomId) {
        return reservationService.findByRoom(roomId);
    }

    @PostMapping("/rooms/{roomId}/reservations")
    public ResponseEntity<ReservationResponse> create(@PathVariable Long roomId,
            @Valid @RequestBody ReservationCreateRequest request, Authentication authentication) {
        var reservation = reservationService.create(roomId, authentication.getName(), request.startTime(), request.endTime());
        return ResponseEntity.created(URI.create("/api/reservations/" + reservation.id())).body(reservation);
    }

    @GetMapping("/reservations/mine")
    public List<ReservationResponse> mine(Authentication authentication) {
        return reservationService.findMine(authentication.getName());
    }

    @GetMapping("/reservations/{id}")
    public ReservationResponse findOne(@PathVariable Long id, Authentication authentication) {
        return reservationService.get(id, authentication.getName(), isAdmin(authentication));
    }

    @PutMapping("/reservations/{id}")
    public ReservationResponse update(@PathVariable Long id, @Valid @RequestBody ReservationCreateRequest request,
                                       Authentication authentication) {
        return reservationService.update(id, authentication.getName(), request.startTime(), request.endTime());
    }

    @DeleteMapping("/reservations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        reservationService.delete(id, authentication.getName(), isAdmin(authentication));
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
