INSERT INTO app_user (id, username, password_hash, role) VALUES
    (1, 'mina', '$2a$10$bCGPRhtYDHqsC.6KIkAmdedI/GmIKw8cUx3KHH7nCskBg12ddfgvC', 'USER'),
    (2, 'omar', '$2a$10$wBwPhGfCDiX2ArwePJvIDuRJR6yf74EGEGbyqIXcr6JKNX5.rX0Gi', 'USER'),
    (3, 'admin', '$2a$10$pb8.wlIvce3mXNy4fHXAM.f8yz60fgTrlftZDZt82Ckr6RFYmF6rq', 'ADMIN');

INSERT INTO room (id, name, capacity) VALUES
    (1, 'Seminar A', 8),
    (2, 'Study Pod', 4),
    (3, 'Rooftop Room', 12);

INSERT INTO reservation (id, room_id, owner_id, start_time, end_time) VALUES
    (1, 1, 1, '2026-10-06 10:00:00', '2026-10-06 11:00:00'),
    (2, 1, 2, '2026-10-06 13:00:00', '2026-10-06 15:00:00'),
    (3, 2, 1, '2026-10-06 09:00:00', '2026-10-06 10:00:00'),
    (4, 1, 2, '2026-10-07 10:00:00', '2026-10-07 12:00:00'),
    (5, 2, 2, '2026-10-07 14:00:00', '2026-10-07 15:00:00');
