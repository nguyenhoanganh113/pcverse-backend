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
KEYCLOAK_ACTION_CLIENT_ID=pc-verse-frontend
KEYCLOAK_ACTION_REDIRECT_URI=https://oauth.pstmn.io/v1/callback

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

Under **Authentication → Required actions**, enable `UPDATE_EMAIL`. Enable
**Force Email Verification** for the action so Keycloak does not replace the
current email until the new address has been verified. Configure **Maximum Age
of Authentication** there as well; `0` forces reauthentication for every email
change.

The frontend must use Authorization Code Flow with PKCE. It redirects the
browser to Keycloak and never handles the user's password directly.

### Backend resource client

Create the resource client `pc-verse-backend` and add client roles that
represent API permissions. The self-service user endpoints currently require:

- `PROFILE_READ_SELF`
- `PROFILE_UPDATE_SELF`
- `ADDRESS_READ_SELF`
- `ADDRESS_CREATE_SELF`
- `ADDRESS_UPDATE_SELF`
- `ADDRESS_DELETE_SELF`
- `SESSION_READ_SELF`
- `SESSION_TERMINATE_SELF`

Keep `ADMIN` and `CUSTOMER` as realm composite roles. Associate the appropriate
`pc-verse-backend` permission roles with those realm roles; for example,
`CUSTOMER` should inherit the self-service permissions above. After
changing role mappings, obtain a new access token before testing.

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

`KEYCLOAK_ACTION_CLIENT_ID` and `KEYCLOAK_ACTION_REDIRECT_URI` are backend
settings used only when an administrator sends a required-action email through
the Admin API. The redirect URI must also be registered in the selected action
client's **Valid redirect URIs**. They are not used when the frontend starts an
AIA directly. Replace the Postman callback with the exact frontend callback URI
outside local API testing.

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

### Self-service profile API

| Method | Endpoint | Required client role | Operation |
| --- | --- | --- | --- |
| `GET` | `/api/v1/users/me` | `PROFILE_READ_SELF` | Load the current local profile and synchronize identity claims |
| `PATCH` | `/api/v1/users/me` | `PROFILE_UPDATE_SELF` | Partially update the current user's profile |

The `PATCH` request may contain `firstName`, `lastName`, `phoneNumber`, `gender`,
`dateOfBirth`, and `urlAvatar`. Email is deliberately excluded because Keycloak
owns the verified login email and changes it through the AIA flow below.

### Self-service address API

| Method | Endpoint | Required client role | Operation |
| --- | --- | --- | --- |
| `GET` | `/api/v1/users/me/addresses` | `ADDRESS_READ_SELF` | List the current user's addresses |
| `POST` | `/api/v1/users/me/addresses` | `ADDRESS_CREATE_SELF` | Add an address |
| `PATCH` | `/api/v1/users/me/addresses/{addressId}` | `ADDRESS_UPDATE_SELF` | Partially update an owned address |
| `DELETE` | `/api/v1/users/me/addresses/{addressId}` | `ADDRESS_DELETE_SELF` | Delete an owned address |

The API derives the local user from the access token and never accepts a
`userId` from the client. Update and delete queries match both `addressId` and
the authenticated user's ID. A missing address and an address owned by another
user both return `ADDRESS_NOT_FOUND` without revealing whether that ID exists.

Create an address:

```http
POST /api/v1/users/me/addresses
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "recipientName": "Nguyen Hoang Anh",
  "recipientPhone": "0912985895",
  "province": "Ha Noi",
  "district": "Cau Giay",
  "ward": "Dich Vong Hau",
  "streetDetail": "123 Xuan Thuy",
  "isDefault": true
}
```

All fields except `streetDetail` and `isDefault` are required when creating an
address. A `PATCH` request may contain any subset of these fields; sending a
blank `streetDetail` clears it. An empty patch is rejected.

The first address is always made the default. Setting `isDefault` to `true` on
another address removes the default flag from the previous one. Sending
`false` does not unset the current default without a replacement. When the
default address is deleted, the most recently created remaining address is
promoted automatically.

### Self-service email change

The backend intentionally does not expose a
`/api/v1/users/me/email-change` endpoint. When a signed-in user selects
**Change email**, the frontend starts Keycloak's `UPDATE_EMAIL` Application
Initiated Action (AIA) directly:

See the [UPDATE_EMAIL AIA sequence diagram](docs/diagram/update-email-aia.svg)
for the complete interaction between the user, frontend, Keycloak, email
provider, and backend.

```ts
keycloak.login({
  action: "UPDATE_EMAIL",
  redirectUri: `${window.location.origin}/profile/email-change/callback`,
});
```

The callback URI must be registered as an exact **Valid redirect URI** of
`pc-verse-frontend`. Keycloak handles reauthentication, the new-email form,
pending verification, and the verification email. The application never
receives the user's password.

After Keycloak redirects back to the frontend, the frontend must obtain a fresh
access token and reload the local profile:

```ts
await keycloak.updateToken(-1);

await fetch(`${API_BASE_URL}/api/v1/users/me`, {
  headers: {
    Authorization: `Bearer ${keycloak.token}`,
  },
});
```

`updateToken(-1)` forces a token refresh. The subsequent `/me` request updates
the local email only when the token email matches the current email returned by
Keycloak. This prevents an access token issued before the change from restoring
the old email in the application database.

Until a PCVerse frontend exists, test the same behavior through Keycloak's
Account Console, obtain a new `pc-verse-frontend` access token, and call
`GET /api/v1/users/me` manually.

### Self-service password change

The backend intentionally does not accept a user's current or new password.
When a signed-in user selects **Change password**, the frontend starts
Keycloak's `UPDATE_PASSWORD` Application Initiated Action (AIA):

See the
[UPDATE_PASSWORD AIA sequence diagram](docs/diagram/update-password-aia.svg)
for the complete browser flow and the boundary that keeps credentials out of
the PCVerse backend.

```ts
export function startPasswordChange() {
  return keycloak.login({
    action: "UPDATE_PASSWORD",
    redirectUri: `${window.location.origin}/profile/security`,
  });
}
```

The frontend button only calls this function. Keycloak performs the browser
authentication flow, reauthentication, password-policy validation, credential
update, and redirect back to the application. Do not send either password to a
PCVerse backend endpoint and do not use the Keycloak Admin API for this
self-service operation.

Configure the `pc-verse` realm under **Authentication > Required actions >
Update Password** as follows:

- **Enabled**: On
- **Default action**: Off
- **Maximum Authentication Age**: `0`

The zero maximum age makes Keycloak actively reauthenticate the user every time
before changing the password. Keeping the action non-default prevents it from
being forced on every newly registered user. The realm currently keeps the
last three passwords in its password-history policy.

Initialize `keycloak-js` with the standard Authorization Code Flow and PKCE
S256. In **Clients > pc-verse-frontend**, keep **Standard flow** enabled,
disable implicit and direct-access-grant flows, and set **PKCE method** to
`S256` so the server also requires it. The exact redirect URI used above must
be registered under **Valid redirect URIs**. For example, if the frontend runs
at `http://localhost:4200`, register exactly:

```text
http://localhost:4200/profile/security
```

Also register the corresponding exact **Web origin** (`http://localhost:4200`
in this example) so `keycloak-js` can exchange the authorization code from the
browser. Replace these examples with the frontend's real origin and callback.

Do not register a broad wildcard redirect for production. If the user has no
SSO session, Keycloak first shows the login page and then the password form. If
the user already has an SSO session, the configured maximum authentication age
still forces active reauthentication before the password form. Completion or
cancellation returns the browser to the registered frontend URI.

### Self-service session API

| Method | Endpoint | Required client role | Operation |
| --- | --- | --- | --- |
| `GET` | `/api/v1/users/me/sessions` | `SESSION_READ_SELF` | List the current user's active Keycloak sessions |
| `DELETE` | `/api/v1/users/me/sessions/{sessionId}` | `SESSION_TERMINATE_SELF` | Terminate one session owned by the current user |

Both endpoints derive the owner from the access token and never accept a
`userId`. Before deleting a session, the backend verifies that Keycloak lists
the supplied `sessionId` under the authenticated user's `sub`; an unknown or
foreign session returns `USER_SESSION_NOT_FOUND`. After deletion, the backend
also stores the session ID in Redis so access tokens previously issued for
that session are rejected. Deleting the current session allows the delete
response to complete, but later requests using its access token return `401`.

When an authenticated user calls:

```http
GET /api/v1/users/me
Authorization: Bearer <access-token>
```

the backend:

1. Requires non-empty `sub`, `email`, and `preferred_username` claims and
   requires `email_verified=true`.
2. Finds the local user by Keycloak subject (`sub`).
3. Otherwise finds the local user by verified email and links the Keycloak ID.
4. Otherwise creates a local user from the token and assigns the local
   `CUSTOMER` role.
5. For an existing user, synchronizes the username and accepts a changed email
   only when it matches the user's current email in Keycloak.

For users created from an Identity Provider, `phoneNumber`, `gender`, and
`dateOfBirth` may be `null`. Before the first `/me` request, Keycloak must grant
the required self-service client permissions, normally by assigning the
`CUSTOMER` realm role as a default role and making it a composite of those
permissions. The local provisioning code does not add Keycloak roles. Obtain a
new access token after changing any Keycloak role or composite mapping.

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

This is an administrative flow. It is not used by the self-service
**Change email** button, which uses AIA directly from the frontend.

## Admin User API

Administration endpoints authorize the permission roles under
`resource_access.pc-verse-backend.roles`. The `ADMIN` realm role should be a
composite containing these client roles; the backend does not authorize an
endpoint merely because `realm_access.roles` contains `ADMIN`.

| Method | Endpoint | Required client role | Operation |
| --- | --- | --- | --- |
| `GET` | `/api/v1/admin/users/search` | `USER_READ` | Search and paginate local users |
| `GET` | `/api/v1/admin/users/{userId}` | `USER_READ` | Get a local user |
| `POST` | `/api/v1/admin/users` | `USER_CREATE` | Create the Keycloak identity and local profile |
| `PUT` | `/api/v1/admin/users/{userId}` | `USER_UPDATE` | Update the user in Keycloak and the database |
| `DELETE` | `/api/v1/admin/users/{userId}` | `USER_DELETE` | Delete the user from Keycloak and the database |
| `PUT` | `/api/v1/admin/users/{userId}/password` | `USER_PASSWORD_RESET` | Reset the Keycloak password |
| `POST` | `/api/v1/admin/users/{userId}/roles` | `USER_ROLE_MANAGE` | Assign an application realm role in Keycloak and the database |
| `DELETE` | `/api/v1/admin/users/{userId}/roles/{roleName}` | `USER_ROLE_MANAGE` | Remove an application realm role |
| `PATCH` | `/api/v1/admin/users/{userId}/status` | `USER_STATUS_MANAGE` | Enable or disable the Keycloak user |
| `POST` | `/api/v1/admin/users/{userId}/logout` | `USER_SESSION_TERMINATE` | Terminate all Keycloak sessions |
| `GET` | `/api/v1/admin/users/{userId}/sessions` | `USER_SESSION_READ` | List Keycloak sessions |
| `DELETE` | `/api/v1/admin/users/{userId}/sessions/{sessionId}` | `USER_SESSION_TERMINATE` | Terminate one Keycloak session |
| `POST` | `/api/v1/admin/users/{userId}/required-actions-email` | `USER_REQUIRED_ACTION_MANAGE` | Email one or more required actions |
| `PUT` | `/api/v1/admin/users/{userId}/required-actions` | `USER_REQUIRED_ACTION_MANAGE` | Replace pending required actions |

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

The local defaults allow:

```text
https://localhost:8090,http://localhost:8090,http://localhost:4200
```

Override this list with `CORS_ALLOWED_ORIGINS` when the frontend or Keycloak
origin changes. CORS origins must match the browser origin exactly, including
the scheme and port.

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
