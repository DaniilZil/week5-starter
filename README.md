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

Run [api.http](api.http) from 1 through 58 in order. Before repeating the sequence, run `SOURCE reset.sql` in the lab database. It replaces all rooms and reservations with the three rooms and five bookings used by the requests, resets the generated IDs and seeds the three sample accounts. Run it before the first start after upgrading from Assignment 6, because it also creates the app_user table.

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

`RoomService.update` loads and edits the room inside a transaction. In the Hibernate SQL log, request 26 selects the room without updating it. Request 27 selects and updates the room. HTTP Basic also adds an account lookup to both requests.

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
| 401 Unauthorized | Login is missing, credentials are wrong, or a Bearer token is invalid or expired. All responses use ApiError. |
| 404 Not Found | The room, reservation or requested URL does not exist. |
| 405 Method Not Allowed | The path exists, but the requested HTTP method is unsupported. Request 58 uses PATCH on a room. |
| 409 Conflict | A booking overlaps another in the same room, or the room being deleted still has reservations. |
| 500 Internal Server Error | An unexpected server failure occurred. The client receives `unexpected server error` and the server logs the details. |

## Authentication

Public endpoints are POST /api/auth/signup, POST /api/auth/login, GET /api/rooms, GET /api/rooms/{id}, GET /api/stats and the Swagger/OpenAPI pages. Following the Lab F rule, GET /api/rooms/{roomId}/reservations is also public. GET /api/reservations and GET /api/reservations/** require authentication, as does GET /api/auth/me. Every other POST, PUT, DELETE and PATCH requires authentication.

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
GET http://localhost:8080/api/reservations?reservedBy=Mina
Authorization: Bearer <accessToken>
```

HTTP Basic also works with the sample accounts. api.http uses Mina's Basic header in requests 1–41, then registers lee / lee12345 in request 43. Request 50 saves its accessToken in the IntelliJ HTTP Client variable token for requests 52–53. When using another HTTP client, copy the token into the Bearer header. Run 1–56 in order, then the two additional failure cases 57–58.

The three 401 messages are login required, wrong username or password, and token is invalid or expired. A supplied invalid token is rejected even on public routes. /api/auth/me returns username and authorities. Basic includes ROLE_USER or ROLE_ADMIN. JWT authorities are empty in this assignment, following slide 28.

app.jwt.secret reads JWT_SECRET from the environment. The checked-in fallback is for the classroom only. For deployment, set a private, random secret containing at least 32 UTF-8 bytes before starting the application. app.jwt.ttl-minutes sets the token lifetime and defaults to 30.

```powershell
$env:JWT_SECRET = "<your-private-random-secret-at-least-32-bytes>"
.\gradlew.bat bootRun
```

After configuring MySQL and running reset.sql, run .\gradlew.bat test on Windows or sh gradlew test on macOS/Linux. The tests cover the ApiError body for 401, expired and malformed tokens, BCrypt storage and its UTF-8 limit.

## AI use

OpenAI Codex helped implement and test Lab E and Lab F, including validation, authentication and JWT.
