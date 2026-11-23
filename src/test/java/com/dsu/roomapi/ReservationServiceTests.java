package com.dsu.roomapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationServiceTests {
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final ReservationService service = new ReservationService(rooms, reservations, users);
    private final LocalDateTime start = LocalDateTime.of(2026, 11, 3, 10, 0);
    private final LocalDateTime end = start.plusHours(1);
    private Reservation booking;

    @BeforeEach
    void setUp() {
        Room room = new Room(5L, "Library", 6);
        booking = spy(new Reservation(room, new AppUser("mina", "unused", "USER"), start, end));
        when(booking.getId()).thenReturn(9L);
        when(reservations.findById(9L)).thenReturn(Optional.of(booking));
        when(reservations.findRoomIdById(9L)).thenReturn(Optional.of(5L));
        when(rooms.findForUpdate(5L)).thenReturn(Optional.of(room));
    }

    @Test
    void readAndDeleteRequireOwnershipOrAdmin() {
        assertThrows(ForbiddenException.class, () -> service.get(9L, "omar", false));
        assertThrows(ForbiddenException.class, () -> service.delete(9L, "omar", false));
        verify(reservations, never()).delete(any());
        assertEquals("mina", service.get(9L, "admin", true).owner());
        service.delete(9L, "admin", true);
        verify(reservations).delete(booking);
    }

    @Test
    void onlyOwnerMayRescheduleEvenForAnAdminAccount() {
        for (String username : new String[]{"omar", "admin"}) {
            var failure = assertThrows(ForbiddenException.class,
                    () -> service.update(9L, username, start.plusHours(4), end.plusHours(4)));
            assertEquals("reservation 9 belongs to another user", failure.getMessage());
        }
        verify(reservations, never()).countOverlappingExcept(any(), any(), any(), any());
        assertEquals(start, booking.getStartTime());
    }

    @Test
    void sameTimeExcludesTheBookingItselfAndUsesDirtyChecking() {
        var response = service.update(9L, "mina", start, end);
        assertEquals(start, response.startTime());
        verify(reservations).countOverlappingExcept(5L, 9L, start, end);
        verify(reservations, never()).save(any());
    }

    @Test
    void conflictingMoveLeavesExistingTimesIntact() {
        LocalDateTime newStart = start.plusHours(4);
        LocalDateTime newEnd = end.plusHours(4);
        when(reservations.countOverlappingExcept(5L, 9L, newStart, newEnd)).thenReturn(1L);
        assertThrows(ConflictException.class, () -> service.update(9L, "mina", newStart, newEnd));
        assertEquals(start, booking.getStartTime());
        assertEquals(end, booking.getEndTime());
    }

    @Test
    void invalidTimeAndMissingBookingHaveConsistentPrecedence() {
        assertThrows(IllegalArgumentException.class, () -> service.update(99L, "omar", end, start));
        assertThrows(NotFoundException.class, () -> service.update(99L, "omar", start, end));
    }
}

