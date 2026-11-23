package com.dsu.roomapi;

import java.time.LocalDateTime;

public record ReservationResponse(Long id, Long roomId, String roomName,
                                  String owner, LocalDateTime startTime, LocalDateTime endTime) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(reservation.getId(), reservation.getRoom().getId(),
                reservation.getRoom().getName(), reservation.getOwner().getUsername(),
                reservation.getStartTime(), reservation.getEndTime());
    }
}
