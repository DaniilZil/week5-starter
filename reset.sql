-- Run against the lab database after schema.sql and before request 1.
START TRANSACTION;

DELETE FROM reservation;
DELETE FROM room;

INSERT INTO room (id, name, capacity) VALUES
    (1, 'Seminar A', 8),
    (2, 'Study Pod', 4),
    (3, 'Rooftop Room', 12);

INSERT INTO reservation (id, room_id, reserved_by, start_time, end_time) VALUES
    (1, 1, 'Mina',  '2026-10-06 10:00:00', '2026-10-06 11:00:00'),
    (2, 1, 'Omar',  '2026-10-06 13:00:00', '2026-10-06 15:00:00'),
    (3, 2, 'Mina',  '2026-10-06 09:00:00', '2026-10-06 10:00:00'),
    (4, 1, 'Lucas', '2026-10-07 10:00:00', '2026-10-07 12:00:00'),
    (5, 2, 'Aiko',  '2026-10-07 14:00:00', '2026-10-07 15:00:00');

COMMIT;

ALTER TABLE room AUTO_INCREMENT = 4;
ALTER TABLE reservation AUTO_INCREMENT = 6;
