package com.dsu.roomapi;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReservationService(RoomRepository roomRepository, ReservationRepository reservationRepository){
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public Optional<List<Reservation>> findByRoom(Long roomId){
        if(!roomRepository.existsById(roomId))
            return Optional.empty();
        return Optional.of(reservationRepository.findByRoomIdOrderByStartTime(roomId));
    }

    private void checkRequest(String reservedBy, LocalDateTime startTime, LocalDateTime endtime)
    {
        if(reservedBy == null || reservedBy.isBlank())
            throw new IllegalArgumentException("reservedBy is required");
        if(startTime == null || endtime == null)
            throw new IllegalArgumentException("startTime and(or) endTime are required");
        if(startTime.isBefore(endtime))
            throw new IllegalArgumentException("starTime must be before endTime");
    }

    public Optional<Reservation> create(Long roomId, String reservedBy, LocalDateTime startTime, LocalDateTime endtime)
    {
        checkRequest(reservedBy, startTime, endtime);
        Optional<Room> room = roomRepository.findById(roomId);
        if(room.isEmpty())
            return Optional.empty();
        return Optional.of(reservationRepository.save(new Reservation(room.get(), reservedBy, startTime, endtime)));

    }

    public List<Reservation> findByReservedBy(String name){
        return reservationRepository.findByReservedByWithRoom(name);
    }





}
