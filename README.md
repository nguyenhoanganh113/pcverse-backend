# PCVerse Backend

Monolithic Spring Boot backend for PCVerse, an ecommerce platform for PC components, laptops, gaming gear, and ergonomic chairs.

## Tech Stack

- Java 17
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker Compose
- Maven

## Getting Started

```bash
./mvnw spring-boot:run
```

## Keycloak Admin Client

The application uses the official `org.keycloak:keycloak-admin-client` library with the
OAuth 2.0 client-credentials grant. Configure a confidential Keycloak client (the default
client ID is `pc-verse-admin`), enable service accounts, and grant its service-account user
these `realm-management` client roles:

- `manage-users`
- `view-clients`

The target resource client (`pc-verse-api` by default) must contain every client role that
the API will assign, such as `ADMIN` and `CUSTOMER`.

Required environment variables:

```bash
KEYCLOAK_ADMIN_SERVER_URL=http://localhost:8090
KEYCLOAK_ADMIN_REALM=pc-verse
KEYCLOAK_ADMIN_CLIENT_ID=pc-verse-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=replace-with-client-secret
KEYCLOAK_RESOURCE_CLIENT_ID=pc-verse-api
```

### Creating users through the Keycloak Admin REST API

The application does not expose a `POST /api/v1/users/admin` wrapper. Obtain a service-account
token and create the user directly in Keycloak:

```http
POST http://localhost:8090/realms/pc-verse/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=pc-verse-admin&client_secret=<client-secret>
```

```http
POST http://localhost:8090/admin/realms/pc-verse/users
Authorization: Bearer <service-account-access-token>
Content-Type: application/json

{
  "username": "admin",
  "email": "admin@pcverse.com",
  "firstName": "PCVerse",
  "lastName": "Admin",
  "enabled": true,
  "emailVerified": true,
  "attributes": {
    "phoneNumber": ["0900000000"],
    "gender": ["MALE"],
    "birthdate": ["1990-01-01"]
  },
  "credentials": [
    {
      "type": "password",
      "value": "Admin@123456",
      "temporary": false
    }
  ]
}
```

Keycloak returns `201 Created`; the new Keycloak user ID is the final path segment of the
`Location` response header. A user created this way is added to the application database when
they first authenticate and call `GET /api/v1/users/me`.

All administration endpoints require a JWT containing the `ADMIN` client role:

| Method | Endpoint | Operation |
| --- | --- | --- |
| `PUT` | `/api/v1/users/{userId}` | Update the user in Keycloak and the database |
| `DELETE` | `/api/v1/users/{userId}` | Delete the user from Keycloak and the database |
| `PUT` | `/api/v1/users/{userId}/password` | Reset the Keycloak password |
| `POST` | `/api/v1/users/{userId}/roles` | Assign a client role in Keycloak and the database |
| `PATCH` | `/api/v1/users/{userId}/status` | Enable or disable the Keycloak user |
