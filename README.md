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

### Creating users through the backend Admin API

Create users through the backend so identity data is created in Keycloak while application
profile data is stored in the local database:

```http
POST http://localhost:8083/api/v1/admin/users
Authorization: Bearer <admin-user-access-token>
Content-Type: application/json

{
  "username": "new-user",
  "email": "new-user@pcverse.com",
  "password": "Password@123",
  "firstName": "PCVerse",
  "lastName": "User",
  "phoneNumber": "0900000000",
  "gender": "MALE",
  "dateOfBirth": "1990-01-01",
  "urlAvatar": null
}
```

`phoneNumber`, `gender`, `dateOfBirth`, and `urlAvatar` are application-owned fields. They are
stored only in the local database and are not synchronized to or from Keycloak.

All administration endpoints require a JWT containing the `ADMIN` client role:

| Method | Endpoint | Operation |
| --- | --- | --- |
| `PUT` | `/api/v1/users/{userId}` | Update the user in Keycloak and the database |
| `DELETE` | `/api/v1/users/{userId}` | Delete the user from Keycloak and the database |
| `PUT` | `/api/v1/users/{userId}/password` | Reset the Keycloak password |
| `POST` | `/api/v1/admin/users/{userId}/roles` | Assign a client role in Keycloak and the database |
| `PATCH` | `/api/v1/users/{userId}/status` | Enable or disable the Keycloak user |
