package com.dsu.roomapi;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByNameContainingIgnoreCase(String keyword);
    List<Room> findByCapacityGreaterThanEqualAndNameContainingIgnoreCase(int minCapacity, String keyword);

    // Booking and deletion both lock the room before checking its reservations.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findForUpdate(Long id);
}
