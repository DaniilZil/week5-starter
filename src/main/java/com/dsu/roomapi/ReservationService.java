package com.dsu.roomapi;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public List<ReservationResponse> findByRoom(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException("room " + roomId + " not found");
        }
        return reservationRepository.findByRoomIdOrderByStartTime(roomId).stream()
                .map(ReservationResponse::from).toList();
    }

    @Transactional
    public ReservationResponse create(Long roomId, String reservedBy, LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        Room room = roomRepository.findForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException("room " + roomId + " not found"));
        if (reservationRepository.overlaps(roomId, startTime, endTime)) {
            throw new ConflictException("room " + roomId + " is already booked at that time");
        }
        var reservation = new Reservation(room, reservedBy, startTime, endTime);
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    public List<ReservationResponse> findByReservedBy(String name) {
        return reservationRepository.findByReservedByWithRoom(name).stream()
                .map(ReservationResponse::from).toList();
    }

    public ReservationResponse get(Long id) {
        return ReservationResponse.from(findReservation(id));
    }

    @Transactional
    public void delete(Long id) {
        reservationRepository.delete(findReservation(id));
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("reservation " + id + " not found"));
    }
}
