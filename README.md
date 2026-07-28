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

## Local Keycloak HTTPS

Keycloak is exposed to the host at `https://localhost:8090`. Inside the Docker
network, the backend still communicates with Keycloak over
`http://keycloak:8180`; this internal HTTP port is not published to the host.

The local TLS certificate and private key are intentionally ignored by Git.
Generate them after cloning the repository by running the following commands
from the repository root:

```bash
mkdir -p infrastructure/keycloak/certs

openssl req \
  -x509 \
  -newkey rsa:2048 \
  -sha256 \
  -nodes \
  -days 365 \
  -keyout infrastructure/keycloak/certs/tls.key \
  -out infrastructure/keycloak/certs/tls.crt \
  -subj "/CN=localhost" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" \
  -addext "keyUsage=digitalSignature,keyEncipherment" \
  -addext "extendedKeyUsage=serverAuth"
```

The generated files have different purposes:

- `tls.crt` is the public certificate presented by Keycloak.
- `tls.key` is the private key and must never be committed or shared.

Verify the generated certificate and private key:

```bash
openssl x509 \
  -in infrastructure/keycloak/certs/tls.crt \
  -noout \
  -subject \
  -issuer \
  -dates

openssl pkey \
  -in infrastructure/keycloak/certs/tls.key \
  -check \
  -noout
```

Start Keycloak and verify its OpenID Connect issuer:

```bash
docker compose up -d keycloak-db keycloak

curl -k \
  https://localhost:8090/realms/pc-verse/.well-known/openid-configuration
```

The discovery response must contain:

```json
{
  "issuer": "https://localhost:8090/realms/pc-verse"
}
```

Because this is a self-signed development certificate, browsers do not trust it
automatically. On macOS, either accept the browser warning for local development
or add `infrastructure/keycloak/certs/tls.crt` to Keychain Access and set it to
**Always Trust**. Restart the browser after changing the trust setting.

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
KEYCLOAK_ADMIN_SERVER_URL=https://localhost:8090
KEYCLOAK_ADMIN_REALM=pc-verse
KEYCLOAK_ADMIN_CLIENT_ID=pc-verse-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=replace-with-client-secret
KEYCLOAK_RESOURCE_CLIENT_ID=pc-verse-backend
```

When the backend runs through Docker Compose, the service overrides
`KEYCLOAK_ADMIN_SERVER_URL` with `http://keycloak:8180` so administrative
requests stay inside the Docker network. A backend started directly on the host
uses the public HTTPS URL and its JVM must trust the local certificate.

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
  "firstName": "PCVerse",
  "lastName": "User",
  "phoneNumber": "0900000000",
  "gender": "MALE",
  "dateOfBirth": "1990-01-01",
  "urlAvatar": null
}
```

The response contains the local user `id` and `email`. The user is enabled but has no password,
and the email is initially unverified. Send the invitation separately so the operation can be
retried if email delivery fails:

```http
POST http://localhost:8083/api/v1/admin/users/{userId}/required-actions-email
Authorization: Bearer <admin-user-access-token>
Content-Type: application/json

{
  "actions": ["VERIFY_EMAIL", "UPDATE_PASSWORD"],
  "lifespanSeconds": 43200
}
```

`phoneNumber`, `gender`, `dateOfBirth`, and `urlAvatar` are application-owned fields. They are
stored only in the local database and are not synchronized to or from Keycloak.

All administration endpoints require a JWT containing the `ADMIN` client role:

| Method | Endpoint | Operation |
| --- | --- | --- |
| `GET` | `/api/v1/admin/users` | List local users |
| `GET` | `/api/v1/admin/users/{userId}` | Get a local user |
| `POST` | `/api/v1/admin/users` | Create the Keycloak identity and local profile |
| `PUT` | `/api/v1/admin/users/{userId}` | Update the user in Keycloak and the database |
| `DELETE` | `/api/v1/admin/users/{userId}` | Delete the user from Keycloak and the database |
| `PUT` | `/api/v1/admin/users/{userId}/password` | Reset the Keycloak password |
| `POST` | `/api/v1/admin/users/{userId}/roles` | Assign a client role in Keycloak and the database |
| `DELETE` | `/api/v1/admin/users/{userId}/roles/{roleName}` | Remove a client role |
| `PATCH` | `/api/v1/admin/users/{userId}/status` | Enable or disable the Keycloak user |
| `POST` | `/api/v1/admin/users/{userId}/logout` | Terminate all Keycloak sessions |
| `GET` | `/api/v1/admin/users/{userId}/sessions` | List Keycloak sessions |
| `DELETE` | `/api/v1/admin/users/{userId}/sessions/{sessionId}` | Terminate one Keycloak session |
| `GET` | `/api/v1/admin/users/{userId}/credentials` | List safe credential metadata |
| `DELETE` | `/api/v1/admin/users/{userId}/credentials/{credentialId}` | Delete an OTP or WebAuthn credential |
| `POST` | `/api/v1/admin/users/{userId}/required-actions-email` | Email one or more required actions |
| `PUT` | `/api/v1/admin/users/{userId}/required-actions` | Replace the user's pending required actions |
