package com.dsu.roomapi;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final AppUserRepository userRepository;

    public ReservationService(RoomRepository roomRepository, ReservationRepository reservationRepository,
                              AppUserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    public List<ReservationResponse> findByRoom(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException("room " + roomId + " not found");
        }
        return reservationRepository.findByRoomIdOrderByStartTime(roomId).stream()
                .map(ReservationResponse::from).toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse create(Long roomId, String username, LocalDateTime startTime, LocalDateTime endTime) {
        checkTime(startTime, endTime);
        Room room = roomRepository.findForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException("room " + roomId + " not found"));
        AppUser owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("user " + username + " not found"));
        if (reservationRepository.overlaps(roomId, startTime, endTime)) {
            throw new ConflictException("room " + roomId + " is already booked at that time");
        }
        return ReservationResponse.from(reservationRepository.save(new Reservation(room, owner, startTime, endTime)));
    }

    public List<ReservationResponse> findMine(String username) {
        return reservationRepository.findMine(username).stream().map(ReservationResponse::from).toList();
    }

    public ReservationResponse get(Long id, String username, boolean admin) {
        Reservation reservation = findReservation(id);
        requireOwnerOrAdmin(reservation, username, admin);
        return ReservationResponse.from(reservation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse update(Long id, String username, LocalDateTime startTime, LocalDateTime endTime) {
        checkTime(startTime, endTime);
        Reservation reservation = findForChange(id);
        requireOwnerOrAdmin(reservation, username, false);
        Long roomId = reservation.getRoom().getId();
        if (reservationRepository.countOverlappingExcept(roomId, id, startTime, endTime) > 0) {
            throw new ConflictException("room " + roomId + " is already booked at that time");
        }
        reservation.reschedule(startTime, endTime);
        return ReservationResponse.from(reservation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(Long id, String username, boolean admin) {
        Reservation reservation = findForChange(id);
        requireOwnerOrAdmin(reservation, username, admin);
        reservationRepository.delete(reservation);
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("reservation " + id + " not found"));
    }

    private Reservation findForChange(Long id) {
        Long roomId = reservationRepository.findRoomIdById(id)
                .orElseThrow(() -> new NotFoundException("reservation " + id + " not found"));
        // All booking writes lock their room first. Read the entity after the lock
        // so a simultaneous move, cancellation or creation cannot use stale times.
        roomRepository.findForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException("reservation " + id + " not found"));
        return findReservation(id);
    }

    private void requireOwnerOrAdmin(Reservation reservation, String username, boolean admin) {
        if (!admin && !reservation.isOwnedBy(username)) {
            throw new ForbiddenException("reservation " + reservation.getId() + " belongs to another user");
        }
    }

    private void checkTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
    }
}
