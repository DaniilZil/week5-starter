-- Week 11 lab reset. Recreates the three tables and removes their existing data.
-- Stop the application, connect to roomdb, and run this file before request 1.
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS app_user;

CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT uq_app_user_username UNIQUE (username)
);

CREATE TABLE room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    CONSTRAINT chk_room_capacity CHECK (capacity BETWEEN 1 AND 20)
);

CREATE TABLE reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    CONSTRAINT fk_reservation_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT fk_reservation_owner FOREIGN KEY (owner_id) REFERENCES app_user (id)
);

START TRANSACTION;

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

COMMIT;
