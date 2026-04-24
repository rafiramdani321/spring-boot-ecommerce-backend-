# AUTH API SPEC

Notes:
- Email verification required before login

## Register

### Behavior

- Username and email must be unique
- System will validate all request fields
- confirmPassword must match password
- System will check for existing username and email before creating user
- Password will be hashed using bcrypt
- Default role assigned: USER (fetched from roles table)
- isVerified will be set to false
- authProvider will be set to LOCAL
- Verification email will be sent to user
- Verification email contains a token to activate the account

Endpoint : POST /api/auth/register

Request Body :

```json
{
  "username": "string (unique, required, min = 3, max = 50)",
  "fullname": "string (optional, max = 100)",
  "email": "string (unique, required)",
  "password": "string (required, min 8, at least 1 uppercase, at least 1 special character",
  "confirmPassword": "string (must be same with password)"
}
```

Response Body :

- Success, 201 - Created
```json
{
  "data": {
    "id": "uuid",
    "username": "testusername",
    "fullname": "test fullname",
    "email": "test@example.com",
    "isVerified": false
  },
  "message": "Registration Success. Please check your email for activation."
}
```

- Validation Failed, 400 Bad Request
```json
{
  "message": "Request failed",
  "code": "VALIDATION_FAILED",
  "errors": {
    "username": ["Username is required", "...."],
    "fullname": ["Fullname must not exceed 100 characters"],
    "email": ["Email is invalid"],
    "password": ["Password must be between 8 and 100 characters", "...."]
  }
}
```

- Business Error, 409 Conflict
```json
{
  "message": "Request failed",
  "code": "USERNAME_ALREADY_EXIST",
  "errors": {
    "username": ["Username already exist"]
  }
}
```

## Verify Email Activation

### Behavior

- Query param : token (mandatory)
- System will check token
- token expired (15 minute)
- if checking success, isVerified will be set to true

Endpoint : GET /api/auth/verify-email?token=

Request Body :

Response Body :

- Success, 200 - OK
```json
{
  "data": "success",
  "message": "Email verification successful"
}
```

- Failed (Token not found), 404 Not Found
```json
{
  "message": "Request failed",
  "code": "TOKEN_NOT_FOUND",
  "errors": {
    "token": [
      "Invalid token"
    ]
  }
}
```

- Failed (Token Expired), 400 Bad Request
```json
{
  "message": "Request failed",
  "code": "TOKEN_EXPIRED",
  "errors": {
    "email": [
      "testuser@example.com"
    ],
    "token": [
      "This link has expired. Please request a new verification link."
    ]
  }
}
```

## Resend Verify Email Activation

### Behavior

- Request body email mandatory.
- System will check whether email is registered.
- System will check whether is verified is true.
- Change all old tokens to expired. 
- set and create token expired (15 minute)
- if checking success. send verification email.

Endpoint : GET /api/auth/resend-email-verification

Request Body :

```json
{
  "email": "example@mail.com"
}
```

Response Body :

- Success, 201 - CREATED
```json
{
  "data": "success",
  "message": "Resend email verification success"
}
```

- Failed (Email not found), 404 Not Found
```json
{
  "message": "Request failed",
  "code": "USER_NOT_FOUND",
  "errors": {
    "global": [
      "User not found"
    ]
  }
}
```

## Login

### Behavior

- Request body (email, password) mandatory.
- System will check whether email is registered.
- System will check password.
- System will check is verified.
- Create session.
- Generate JWT access token and refresh token.
- set cookies.

Endpoint : GET /api/auth/login

Request Body :

```json
{
  "username": "example@mail.com",
  "password": "123123"
}
```

Response Body :

- Success, 200 - OK
```json
{
  "data": {
    "username": "testuser",
    "email": "example@mail.com",
    "accessToken": "accestokensecret",
    "refreshToken": "refreshtokensecret"
  },
  "message": "Resend email verification success"
}
```

- Failed (Email or Password incorrect), 400 Bad Request
```json
{
  "message": "Request failed",
  "code": "LOGIN_FAILED",
  "errors": {
    "global": [
      "Email or Password incorrect"
    ]
  }
}
```