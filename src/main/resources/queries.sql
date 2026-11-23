-- Rooms for 6 or more people, largest first
SELECT * FROM room WHERE capacity >= 6 ORDER BY capacity DESC;

-- Every reservation owned by mina
SELECT r.* FROM reservation r JOIN app_user u ON u.id = r.owner_id
WHERE u.username = 'mina' ORDER BY r.start_time;

-- Reservations with room and owner names
SELECT r.id, room.name AS room_name, u.username AS owner, r.start_time, r.end_time
FROM reservation r JOIN room ON room.id = r.room_id JOIN app_user u ON u.id = r.owner_id;

-- Seminar A's reservations on 6 October 2026
SELECT r.* FROM reservation r JOIN room ON room.id = r.room_id
WHERE room.name = 'Seminar A' AND DATE(r.start_time) = '2026-10-06';
