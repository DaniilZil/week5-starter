package com.dsu.roomapi;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationServiceTest {
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final ReservationService service = new ReservationService(rooms, reservations, users);

    private final Room room = new Room(1L, "Seminar A", 8);
    private final AppUser mina = new AppUser("mina", "hash", "USER");
    private final LocalDateTime ten = LocalDateTime.of(2026, 12, 1, 10, 0);
    private final LocalDateTime eleven = ten.plusHours(1);

    @Test
    void createRefusesEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () -> service.create(1L, "mina", eleven, ten));
        verifyNoInteractions(rooms, users, reservations);
    }

    @Test
    void createRefusesMissingRoom() {
        when(rooms.findForUpdate(99L)).thenReturn(Optional.empty());

        var error = assertThrows(NotFoundException.class, () -> service.create(99L, "mina", ten, eleven));
        assertEquals("room 99 not found", error.getMessage());
        verify(reservations, never()).save(any());
    }

    @Test
    void createRefusesOverlap() {
        when(rooms.findForUpdate(1L)).thenReturn(Optional.of(room));
        when(users.findByUsername("mina")).thenReturn(Optional.of(mina));
        when(reservations.overlaps(1L, ten, eleven)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(1L, "mina", ten, eleven));
        verify(reservations, never()).save(any());
    }

    @Test
    void deleteRefusesAnotherUser() {
        givenMinasReservation();

        assertThrows(ForbiddenException.class, () -> service.delete(1L, "omar", false));
        verify(reservations, never()).delete(any());
    }

    @Test
    void createSavesWithTheLoggedInOwner() {
        when(rooms.findForUpdate(1L)).thenReturn(Optional.of(room));
        when(users.findByUsername("mina")).thenReturn(Optional.of(mina));
        when(reservations.save(any(Reservation.class))).thenAnswer(call -> call.getArgument(0));

        var result = service.create(1L, "mina", ten, eleven);

        verify(reservations).save(argThat(booking -> booking.getOwner() == mina));
        assertEquals("mina", result.owner());
    }

    @Test
    void ownerMayDelete() {
        Reservation booking = givenMinasReservation();

        service.delete(1L, "mina", false);

        verify(reservations).delete(booking);
    }

    @Test
    void adminMayDeleteAnotherUsersReservation() {
        Reservation booking = givenMinasReservation();

        service.delete(1L, "admin", true);

        verify(reservations).delete(booking);
    }

    @Test
    void adminMayNotReschedule() {
        Reservation booking = givenMinasReservation();

        assertThrows(ForbiddenException.class, () -> service.update(1L, "admin", eleven, eleven.plusHours(1)));
        assertEquals(ten, booking.getStartTime());
        assertEquals(eleven, booking.getEndTime());
    }

    private Reservation givenMinasReservation() {
        Reservation booking = new Reservation(room, mina, ten, eleven);
        when(reservations.findRoomIdById(1L)).thenReturn(Optional.of(1L));
        when(rooms.findForUpdate(1L)).thenReturn(Optional.of(room));
        when(reservations.findById(1L)).thenReturn(Optional.of(booking));
        return booking;
    }
}
