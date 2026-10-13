# room-api — Week 5 starter

This is the Week 4 reference solution: Controller → Service → Repository interface,
wired by constructor injection, with Swagger UI. Use it as your base for Weeks 5 and 6
if your own Assignment 2 code does not have a `RoomRepository` interface.

## Requirements

- JDK 21

## Run

```
./gradlew bootRun          # macOS / Linux
gradlew.bat bootRun        # Windows
```

The server starts on http://localhost:8080. Run the requests in `api.http` from top to bottom.
Swagger UI: http://localhost:8080/swagger-ui.html

## Endpoints

| Method | Path              | Status          | Description                                        |
|--------|-------------------|-----------------|----------------------------------------------------|
| GET    | `/api/rooms`      | 200             | All rooms; optional `minCapacity` and `keyword`    |
| GET    | `/api/rooms/{id}` | 200 / 404       | One room                                           |
| POST   | `/api/rooms`      | 201 / 400       | Create a room; `Location` header and the new room  |
| PUT    | `/api/rooms/{id}` | 200 / 400 / 404 | Replace a room                                     |
| DELETE | `/api/rooms/{id}` | 204 / 404       | Delete a room                                      |
| GET    | `/api/stats`      | 200             | Number of rooms (dependency-injection demo)        |

Capacity must be between 1 and 20. The rule lives in `RoomService`.

## AI use

(Write one line here if you used AI tools.)
