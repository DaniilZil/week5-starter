package com.dsu.roomapi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("""
            select r from Reservation r join fetch r.room join fetch r.owner
            where r.room.id = :roomId order by r.startTime, r.id
            """)
    List<Reservation> findByRoomIdOrderByStartTime(Long roomId);

    boolean existsByRoomId(Long roomId);

    @Query("select r.room.id from Reservation r where r.id = :id")
    Optional<Long> findRoomIdById(Long id);

    @Query("""
            select (count(r) > 0) from Reservation r
            where r.room.id = :roomId and r.startTime < :endTime and r.endTime > :startTime
            """)
    boolean overlaps(Long roomId, LocalDateTime startTime, LocalDateTime endTime);

    @Query("""
            select count(r) from Reservation r
            where r.room.id = :roomId and r.id <> :id
            and r.startTime < :endTime and r.endTime > :startTime
            """)
    long countOverlappingExcept(Long roomId, Long id, LocalDateTime startTime, LocalDateTime endTime);

    @Query("""
            select r from Reservation r join fetch r.room join fetch r.owner
            where r.owner.username = :username order by r.startTime, r.id
            """)
    List<Reservation> findMine(String username);
}
