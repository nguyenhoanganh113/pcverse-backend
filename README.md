# PCVerse Backend

Monolithic Spring Boot backend for PCVerse, an ecommerce platform for PC
components, laptops, gaming gear, and ergonomic chairs.

## Tech stack

- Java 17
- Spring Boot 4.1
- Spring Web MVC and Spring Security
- OAuth 2.0 Resource Server and OpenID Connect
- Spring Data JPA and PostgreSQL
- Spring Data Redis
- Keycloak 26
- Docker Compose
- Maven Wrapper

Flyway dependencies are present, but Flyway is currently disabled while local
development uses Hibernate `ddl-auto: update`. This is a development-only
schema strategy. Re-enable Flyway and use versioned migrations before deploying
to production.

## Prerequisites

- Java 17
- Docker with Docker Compose
- OpenSSL
- `curl`

## Local environment

Create a `.env` file in the repository root. The values below are development
examples only and must not be reused in production:

```dotenv
APP_PORT=8083
SERVER_PORT=8085

POSTGRES_DB=pc_verse
POSTGRES_USER=postgres
POSTGRES_PASSWORD=123456
POSTGRES_PORT=5433

KEYCLOAK_PORT=8090
KEYCLOAK_DB_NAME=keycloak
KEYCLOAK_DB_USERNAME=keycloak
KEYCLOAK_DB_PASSWORD=keycloak123
KEYCLOAK_ADMIN_USERNAME=admin
KEYCLOAK_ADMIN_PASSWORD=admin

KEYCLOAK_ADMIN_CLIENT_ID=pc-verse-admin
KEYCLOAK_ADMIN_CLIENT_SECRET=replace-after-creating-the-admin-client
KEYCLOAK_RESOURCE_CLIENT_ID=pc-verse-backend

MAIL_USERNAME=
MAIL_PASSWORD=
```

These values match the current Docker Compose topology. Keep
`POSTGRES_DB=pc_verse`, `POSTGRES_USER=postgres`, and
`POSTGRES_PASSWORD=123456` unless the hard-coded PostgreSQL container
credentials in `compose.yaml` are changed at the same time. Keep
`KEYCLOAK_PORT=8090` while `KC_HOSTNAME` is configured as
`https://localhost:8090`.

The `.env` file, local TLS keys, and local Keycloak database directory are
ignored by Git. Never commit real client secrets, SMTP passwords, or private
keys.

## Local Keycloak HTTPS

Keycloak is exposed to the host at `https://localhost:8090`. Inside the Docker
network, the backend communicates with Keycloak over
`http://keycloak:8180`; this internal HTTP port is not published to the host.

Generate the local certificate and private key from the repository root:

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

Verify both files:

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

Start the infrastructure services:

```bash
docker compose up -d postgres redis keycloak-db keycloak
docker logs --tail 100 pc-verse-keycloak
```

Because this is a self-signed development certificate, browsers do not trust it
automatically. On macOS, either accept the browser warning for local development
or add `infrastructure/keycloak/certs/tls.crt` to Keychain Access and set it to
**Always Trust**. Restart the browser after changing the trust setting.

## Keycloak realm setup

Open the Keycloak Admin Console:

```text
https://localhost:8090/admin/master/console/
```

Sign in with the bootstrap administrator and create the `pc-verse` realm.
Because the realm is stored in a local bind-mounted database and is not imported
from a versioned realm file, the following settings must currently be created
manually after a clean Keycloak database.

### Realm settings

Configure:

- `Forgot password`: enabled.
- `Login theme`: `pcverse`.
- `Email theme`: `pcverse`.
- Supported locales: `en` and `vi`, when localization is enabled.
- Password policy: `Not recently used = 3`.
- SMTP under **Realm settings → Email**.

With the current Docker Compose topology, use `Require SSL = External requests`.
Public browser traffic uses HTTPS, while the backend uses private Docker-network
HTTP. Selecting `All requests` also requires changing the backend JWKS and Admin
Client URLs to internal HTTPS.

The reset-credentials UI intentionally returns the same message for existing and
non-existing accounts:

```text
Nếu tài khoản tồn tại, bạn sẽ sớm nhận được email chứa hướng dẫn tiếp theo.
```

The real reason, such as `user_not_found`, remains available in Keycloak event
logs. This behavior prevents account enumeration.

Keycloak SMTP is separate from the Spring `MAIL_*` configuration. Password
reset, verify-email, and required-action emails initiated by Keycloak require
SMTP to be configured in the realm.

### Frontend client

Create an OpenID Connect client named `pc-verse-frontend`:

- Client authentication: Off, because a browser SPA cannot safely store a
  client secret.
- Standard flow: On.
- Direct access grants: Off.
- PKCE method: `S256`.
- Valid redirect URIs: use exact FE callback URLs.
- Web origins: use exact FE origins.

For Postman development, the callback is:

```text
https://oauth.pstmn.io/v1/callback
```

The frontend must use Authorization Code Flow with PKCE. It redirects the
browser to Keycloak and never handles the user's password directly.

### Backend resource client

Create the resource client `pc-verse-backend` and add the client roles used by
the API, including:

- `ADMIN`
- `CUSTOMER`

Configure the frontend client/client scopes so issued access tokens contain:

- `aud` including `pc-verse-backend`.
- Client roles under `resource_access.pc-verse-backend.roles`.

### Backend Admin Client

Create a confidential client named `pc-verse-admin`:

- Client authentication: On.
- Service account roles: On.

Grant its service-account user these `realm-management` client roles:

- `manage-users`
- `view-clients`

Copy the generated client secret to
`KEYCLOAK_ADMIN_CLIENT_SECRET` in `.env`.

The application uses the official `org.keycloak:keycloak-admin-client` library
with the OAuth 2.0 client-credentials grant. The Admin Client is used for
administrative operations such as creating users, assigning roles, and
terminating sessions. It is not used for end-user login.

When the backend runs through Docker Compose, it uses
`http://keycloak:8180` for Admin API and JWKS requests. A backend started
directly on the host must explicitly use the public HTTPS URLs:

```dotenv
KEYCLOAK_ISSUER_URI=https://localhost:8090/realms/pc-verse
KEYCLOAK_JWK_SET_URI=https://localhost:8090/realms/pc-verse/protocol/openid-connect/certs
KEYCLOAK_ADMIN_SERVER_URL=https://localhost:8090
```

The host JVM must trust the local certificate.

### Google Identity Provider

Create a Google Identity Provider in the `pc-verse` realm. Register this exact
redirect URI in the Google OAuth client:

```text
https://localhost:8090/realms/pc-verse/broker/google/endpoint
```

This is the Google-to-Keycloak callback. It is different from the
Keycloak-to-frontend callback configured in `Valid redirect URIs`. Keep the
Google client secret outside Git.

Use an opaque technical username for Google users instead of deriving it from
their email address:

1. Keep **Realm settings → Login → Email as username** disabled.
2. Open **Identity providers → Google → Mappers** and add a mapper.
3. Set **Mapper type** to `Username Template Importer`.
4. Set **Sync mode override** to `Import`.
5. Set **Template** to `pcv-${UUID}`.
6. Set **Target** to `LOCAL`.

For example, a newly imported user receives a username similar to
`pcv-550e8400-e29b-41d4-a716-446655440000`. Keycloak currently generates the
`${UUID}` value using UUID v4. This username is an internal, collision-resistant
login name; the application must use the immutable OIDC `sub` claim as the
identity key and must not use `preferred_username` as a primary key.

The mapper only affects users imported after it is configured. It does not
rename existing Keycloak users and does not create an account when an unknown
user submits an email and password to the normal Keycloak login form.

### Safe First Broker Login flow

Do not automatically link a Google identity to an existing Keycloak account
solely because both accounts contain the same email address. A malicious or
misconfigured Identity Provider could otherwise cause account takeover.

Create an editable copy of Keycloak's built-in safe flow:

1. Open **Authentication → Flows**.
2. Open the built-in `first broker login` flow.
3. Select **Action → Duplicate**.
4. Set the name to `google safe first broker login`.
5. Use a description such as
   `Safely creates or verifies and links users on their first Google login`.

Keep the duplicated flow's security-relevant structure:

```text
Review Profile                                      Required
User creation or linking                            Required
├── Create User If Unique                           Alternative
└── Handle Existing Account                         Alternative
    ├── Confirm Link Existing Account               Required
    └── Account verification options                Required
        ├── Verify Existing Account By Email         Alternative
        └── Verify Existing Account By Re-authentication
                                                     Alternative
```

`Create User If Unique` creates and links a new local Keycloak user when no
account conflicts. When an account with the same username or email already
exists, the verification branch requires the user to prove ownership before
linking. Keep SMTP configured if email verification is allowed; otherwise the
re-authentication alternative must remain available.

If the Google profile always supplies every required user attribute and the
extra review page is not wanted, open the `Review Profile` execution settings
and set **Update Profile On First Login** to `Off`. Do not replace the
verification branch with automatic account linking.

Finally, open **Identity providers → Google → Settings**, select
`google safe first broker login` as **First login flow**, and save. Existing
federated users are unaffected because this flow runs only when a Google
identity has not yet been linked.

## Verify OpenID Connect

After creating the realm, verify its discovery document:

```bash
curl -k \
  https://localhost:8090/realms/pc-verse/.well-known/openid-configuration
```

The response must contain:

```json
{
  "issuer": "https://localhost:8090/realms/pc-verse"
}
```

## Run the backend

After configuring the realm and setting the Admin Client secret:

```bash
docker compose up -d --build app
docker logs --tail 100 pc-verse-backend-app
```

Local service URLs:

| Service | Host URL |
| --- | --- |
| Backend API | `http://localhost:8083` |
| Keycloak | `https://localhost:8090` |
| PostgreSQL | `localhost:5433` |
| Redis | `localhost:6379` |

To run the backend directly on the host instead:

```bash
./mvnw spring-boot:run
```

The direct host process listens on port `8085` by default and requires
PostgreSQL, Redis, and Keycloak to already be running. It also requires the
host-side HTTPS Keycloak environment variables shown above.

## Authentication and local-user provisioning

The browser login flow is:

```text
FE
→ Keycloak Authorization Endpoint
→ username/password or Google
→ FE callback with authorization code
→ code exchange with PKCE
→ FE sends the access token to the backend
→ backend validates the JWT
```

When an authenticated user calls:

```http
GET /api/v1/users/me
Authorization: Bearer <access-token>
```

the backend:

1. Finds the local user by Keycloak subject (`sub`).
2. Otherwise finds the local user by verified email and links the Keycloak ID.
3. Otherwise creates a local user from the token and assigns `CUSTOMER`.

For users created from an Identity Provider, `phoneNumber`, `gender`, and
`dateOfBirth` may be `null`. The access token used for the first `/me` request
was issued before the `CUSTOMER` role was assigned, so refresh the token or sign
in again before calling an endpoint that requires that new role.

## Creating users through the backend Admin API

Create users through the backend so identity data is created in Keycloak while
application profile data is stored in the local database:

```http
POST http://localhost:8083/api/v1/admin/users
Authorization: Bearer <admin-user-access-token>
Content-Type: application/json

{
  "username": "new-user",
  "password": "replace-with-a-development-password",
  "email": "new-user@pcverse.com",
  "firstName": "PCVerse",
  "lastName": "User",
  "phoneNumber": "0900000000",
  "gender": "MALE",
  "dateOfBirth": "1990-01-01",
  "urlAvatar": null
}
```

The user is created enabled in Keycloak with a non-temporary password and an
unverified email. The local profile starts in `PENDING_VERIFICATION`, and the
backend asynchronously requests a verification email from Keycloak.

The following fields are application-owned and are not synchronized to or from
Keycloak:

- `phoneNumber`
- `gender`
- `dateOfBirth`
- `urlAvatar`

`phoneNumber`, `gender`, and `dateOfBirth` are required by the explicit
registration/admin-create request; `urlAvatar` is optional. All four fields may
remain `null` for an Identity Provider user provisioned from an access token.

An administrator can send or resend required actions separately:

```http
POST http://localhost:8083/api/v1/admin/users/{userId}/required-actions-email
Authorization: Bearer <admin-user-access-token>
Content-Type: application/json

{
  "actions": ["VERIFY_EMAIL", "UPDATE_PASSWORD"],
  "lifespanSeconds": 43200
}
```

## Admin User API

All administration endpoints require a JWT containing the `ADMIN` role for the
`pc-verse-backend` client:

| Method | Endpoint | Operation |
| --- | --- | --- |
| `GET` | `/api/v1/admin/users/search` | Search and paginate local users |
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
| `POST` | `/api/v1/admin/users/{userId}/required-actions-email` | Email one or more required actions |
| `PUT` | `/api/v1/admin/users/{userId}/required-actions` | Replace pending required actions |

## Local themes and registration CORS

Keycloak mounts local themes from:

```text
infrastructure/keycloak/themes
```

Restart Keycloak after editing theme messages or CSS:

```bash
docker compose restart keycloak
```

The Keycloak-hosted registration page has origin
`https://localhost:8090` and calls the backend registration endpoint. The
backend CORS allow-list must therefore contain this exact HTTPS origin.

The current `application.yaml` default only contains
`http://localhost:8090,http://localhost:4200`, and `compose.yaml` does not yet
pass `CORS_ALLOWED_ORIGINS` to the app container. Consequently, the
Keycloak-hosted registration form will be blocked by CORS until one of those
configurations is updated to include `https://localhost:8090`.

The current registration API URL is `http://localhost:8083`; use backend HTTPS
or a same-origin reverse proxy before using this flow outside local
development.

## Local data and schema notes

Current development schema settings:

```yaml
spring:
  flyway:
    enabled: false
  jpa:
    hibernate:
      ddl-auto: update
```

Do not use this combination as the production migration strategy.

Docker Compose stores:

- Application PostgreSQL data in the named volume `postgres_data`.
- Redis data in the named volume `redis_data`, mounted at Redis's persistence
  directory `/data`.
- Keycloak PostgreSQL data in the bind-mounted directory
  `./keycloak_db_data`.

`docker compose down -v` removes the PostgreSQL and Redis named volumes, but it
does not remove the bind-mounted Keycloak database directory. Treat any manual
removal of `keycloak_db_data` as a destructive operation that deletes realms,
clients, users, sessions, and Identity Provider configuration.
