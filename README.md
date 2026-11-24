# Room API

A Spring Boot API for rooms and reservations. It uses Java 21, MySQL 8 and the Gradle wrapper.

## Run

On a fresh MySQL installation, create the lab database and grant the application account access. Run these statements as a MySQL administrator.

```sql
CREATE DATABASE roomdb;
CREATE USER 'roomapp'@'localhost' IDENTIFIED BY 'roomapp1234';
GRANT ALL PRIVILEGES ON roomdb.* TO 'roomapp'@'localhost';
```

From the repository root, open `mysql -u roomapp -p roomdb`. Stop the application and run the Week 11 reset. It recreates the tables, replacing the previous schema and data.

```sql
SOURCE reset.sql
```

Start the application with `sh gradlew bootRun` on macOS or Linux, or `.\gradlew.bat bootRun` on Windows. The API listens on `http://localhost:8080`. [Swagger UI](http://localhost:8080/swagger-ui.html)

Run [api.http](api.http) from 1 through 82 in order. Before repeating the sequence, run `SOURCE reset.sql` in the lab database. It replaces all rooms and reservations with the three rooms and five bookings used by the requests, resets the generated IDs and seeds the three sample accounts. Run it before the first start after upgrading, because reservation now requires owner_id instead of reserved_by.

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
| GET | `/api/reservations/mine` | 200, current account's reservations with room names |
| GET | `/api/reservations/{id}` | 200, one reservation |
| PUT | `/api/reservations/{id}` | 200, rescheduled reservation |
| DELETE | `/api/reservations/{id}` | 204, empty body |

Room capacity is between 1 and 20. Bookings for a room may touch at their endpoints. Request 19 starts exactly when Mina's booking ends. A reservation may be moved to its existing times without overlapping itself.

`RoomService.update` loads and edits the room inside a transaction. In the Hibernate SQL log, request 26 selects the room without updating it. Request 27 selects and updates the room. HTTP Basic also adds an account lookup to both requests.

## Errors

Authentication, authorization, validation and application errors use the `ApiError` record. Spring's CORS rejection in request 82 returns 403 with plain text `Invalid CORS request`, as specified in the slides.

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
| 401 Unauthorized | Login is missing, credentials are wrong, or a Bearer token is invalid or expired. All responses use ApiError. |
| 403 Forbidden | A logged-in caller lacks the ADMIN role or does not own the requested reservation. A disallowed CORS origin also receives 403. |
| 404 Not Found | The room, reservation or requested URL does not exist. |
| 405 Method Not Allowed | The path exists, but the requested HTTP method is unsupported. |
| 409 Conflict | A booking overlaps another in the same room, or the room being deleted still has reservations. |
| 500 Internal Server Error | An unexpected server failure occurred. The client receives `unexpected server error` and the server logs the details. |

## Authentication

POST /api/auth/signup and POST /api/auth/login are public. Reading rooms, room reservation lists and stats is public, as are Swagger/OpenAPI pages. The Authorization section below lists the protected routes.

Run reset.sql to create app_user and restore these classroom accounts.

| Username | Password for the classroom client | Role |
| --- | --- | --- |
| mina | mina1234 | USER |
| omar | omar1234 | USER |
| admin | admin1234 | ADMIN |

The SQL file stores BCrypt hashes in password_hash. Signup always assigns USER and returns id, username and role. Passwords must have 8–72 characters and fit within 72 UTF-8 bytes.

Send a login request to obtain a token.

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{ "username": "mina", "password": "mina1234" }
```

The response contains accessToken, tokenType (Bearer), and expiresIn (1800 seconds). Send the accessToken value in the Authorization header on later requests.

```http
GET http://localhost:8080/api/reservations/mine
Authorization: Bearer <accessToken>
```

HTTP Basic also works with the sample accounts. Room changes in requests 1–41 use admin, and bookings use mina. Request 43 registers lee / lee12345. Requests 50 and 58 save the access tokens as token and adminToken in the IntelliJ HTTP Client. Other clients can copy the returned token into the Bearer header. Run all 82 requests in order. The Week 11 HTTP and reset files were reconstructed from the supplied slides because separate course files were unavailable.

The three 401 messages are login required, wrong username or password, and token is invalid or expired. A supplied invalid token is rejected even on public routes. /api/auth/me returns username and authorities. Both Basic and JWT expose ROLE_USER or ROLE_ADMIN. The JWT role claim supplies the authority.

app.jwt.secret reads JWT_SECRET from the environment. The checked-in fallback is for the classroom only. For deployment, set a private, random secret containing at least 32 UTF-8 bytes before starting the application. app.jwt.ttl-minutes sets the token lifetime and defaults to 30.

```powershell
$env:JWT_SECRET = "<your-private-random-secret-at-least-32-bytes>"
.\gradlew.bat bootRun
```

DB_PASSWORD supplies the MySQL password with a classroom default of roomapp1234. CORS_ORIGINS is a comma-separated list of allowed browser origins, defaulting to http://localhost:5500 and http://127.0.0.1:5500. The CORS policy allows GET, POST, PUT and DELETE, accepts Authorization and Content-Type, and exposes Location. Request 81 tests an allowed preflight and request 82 tests a rejected origin.

## Authorization

| Endpoint | Who may call it |
| --- | --- |
| POST /api/auth/signup and /api/auth/login | Anyone |
| GET /api/rooms, /api/rooms/{id}, /api/rooms/{id}/reservations and /api/stats | Anyone |
| GET /api/auth/me | Any logged-in account |
| POST /api/rooms | ADMIN |
| PUT or DELETE /api/rooms/{id} | ADMIN |
| POST /api/rooms/{id}/reservations | Any logged-in account, which becomes the owner |
| GET /api/reservations/mine | Any logged-in account, returning only its own bookings |
| GET or DELETE /api/reservations/{id} | Owner or ADMIN |
| PUT /api/reservations/{id} | Owner only, including when the caller is an admin |

The database links reservation.owner_id to app_user.id. Request bodies contain startTime and endTime. Any submitted owner, ownerId or reservedBy value is ignored. Reservation responses contain the owner's username in owner. The old search by reservedBy has been removed.

A missing login gets 401. Room changes by a USER get 403 with admin role required. Reading, cancelling or moving another account's reservation gets 403 with reservation {id} belongs to another user. An admin may read and cancel another owner's booking, but only the owner may move it.

For rescheduling, field and time validation runs first (400), followed by lookup (404), ownership (403), and overlap checking (409). The overlap query excludes the reservation being moved. Room and reservation updates use transactional dirty checking.

## Tests

The full suite requires Java 21 and a running MySQL 8 lab database. From the repository root, connect to `roomdb` as described under **Run** and execute `SOURCE reset.sql` before testing. This replaces the lab data. The API tests expect Mina to own reservation 1 in room 1 on October 6, 2026.

Run `./gradlew test` on macOS or Linux, or `.\gradlew.bat test` on Windows. Open `build/reports/tests/test/index.html` for the results.

`ReservationServiceTest` uses JUnit and Mockito and can run without MySQL using `./gradlew test --tests '*ReservationServiceTest'`. `ApiFailureTest` exercises the security filters, controllers, and services through MockMvc against MySQL. Each test transaction rolls back, including the room created by the admin success case.

## AI use

OpenAI Codex helped implement and test Labs E–G and wrote the Assignment 8 service tests, API tests, and test instructions.
