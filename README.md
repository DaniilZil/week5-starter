# Room API

A Spring Boot API for rooms and reservations. It uses Java 21, MySQL 8 and the Gradle wrapper.

## Run

On a fresh MySQL installation, create the lab database and grant the application account access. Run these statements as a MySQL administrator.

```sql
CREATE DATABASE roomdb;
CREATE USER 'roomapp'@'localhost' IDENTIFIED BY 'roomapp1234';
GRANT ALL PRIVILEGES ON roomdb.* TO 'roomapp'@'localhost';
```

From the repository root, open `mysql -u roomapp -p roomdb`. Create the tables once, then load the lab data.

```sql
SOURCE src/main/resources/schema.sql
SOURCE reset.sql
```

Start the application with `sh gradlew bootRun` on macOS or Linux, or `.\gradlew.bat bootRun` on Windows. The API listens on `http://localhost:8080`. [Swagger UI](http://localhost:8080/swagger-ui.html)

Run [api.http](api.http) from 1 through 43 in order. Before repeating the sequence, run `SOURCE reset.sql` in the lab database. It replaces all rooms and reservations with the three rooms and five bookings used by the requests, and resets the generated IDs.

## Endpoints

| Method | Path | Success |
| --- | --- | --- |
| GET | `/api/rooms` | 200, room list with optional `minCapacity` and `keyword` filters |
| GET | `/api/rooms/{id}` | 200, one room |
| POST | `/api/rooms` | 201, created room and its `Location` |
| PUT | `/api/rooms/{id}` | 200, updated room |
| DELETE | `/api/rooms/{id}` | 204, empty body |
| GET | `/api/stats` | 200, room count |
| GET | `/api/rooms/{roomId}/reservations` | 200, reservations ordered by start time |
| POST | `/api/rooms/{roomId}/reservations` | 201, created reservation and its `Location` |
| GET | `/api/reservations?reservedBy={name}` | 200, matching reservations with room names |
| GET | `/api/reservations/{id}` | 200, one reservation |
| DELETE | `/api/reservations/{id}` | 204, empty body |

Room capacity is between 1 and 20. Bookings for a room may touch at their endpoints. Request 19 starts exactly when Mina's booking ends.

`RoomService.update` loads and edits the room inside a transaction. In the Hibernate SQL log, request 26 produces one `SELECT`. Request 27 changes the room and produces a `SELECT` followed by an `UPDATE`.

## Errors

Every API error uses the `ApiError` record.

| Field | Type | Meaning |
| --- | --- | --- |
| `status` | integer | HTTP status code |
| `error` | string | HTTP reason phrase |
| `message` | string | Explanation of the failure |
| `path` | string | Request path |
| `fieldErrors` | array | Field validation failures, each with a string `field` and `message`. Other errors return `[]`. |

Request 29 sends a room with capacity zero and receives this body.

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "request has invalid fields",
  "path": "/api/rooms",
  "fieldErrors": [
    { "field": "capacity", "message": "capacity must be between 1 and 20" }
  ]
}
```

| Status | Meaning in this API |
| --- | --- |
| 400 Bad Request | An input field is invalid, a required parameter is missing, the body or parameter cannot be read, or the end time is at or before the start time. |
| 404 Not Found | The room, reservation or requested URL does not exist. |
| 405 Method Not Allowed | The path exists, but the requested HTTP method is unsupported. Request 43 uses PATCH on a room. |
| 409 Conflict | A booking overlaps another in the same room, or the room being deleted still has reservations. |
| 500 Internal Server Error | An unexpected server failure occurred. The client receives `unexpected server error` and the server logs the details. |

## AI use

OpenAI Codex helped complete a task.
