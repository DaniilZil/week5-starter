-- Rooms for 6 or more people, largest first
SELECT *
FROM room
WHERE capacity >= 6
ORDER BY capacity DESC;

-- Every reservation made by Mina
SELECT *
FROM reservation
WHERE reserved_by = 'Mina';

-- Every reservation with its room's name
SELECT *
FROM reservation
JOIN room
ON reservation.room_id = room.id;

-- Seminar A's reservations on 6 October 2026
SELECT *
FROM reservation rs
         JOIN room r
              ON rs.room_id = r.id
WHERE r.name = 'Seminar A' AND DATE(rs.start_time) = '2026-10-06';
