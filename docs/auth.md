# AUTH API SPEC

Notes:
- Email verification required before login
- Username and email must be unique

## Register

Endpoint : POST /api/auth/register

Request Body :

```json
{
  "username": "string (unique, required)",
  "fullname": "string (optional)",
  "email": "string (unique, required)",
  "password": "string (required, min 8, at least 1 uppercase, at least 1 special character)",
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
    "email": ["Email is invalid"],
    "password": ["Password must be at least 8 characters", "...."]
  }
}
```

- Business Error, 409 Conflict
```json
{
  "message": "Request failed",
  "code": "USERNAME_ALREADY_EXISTS",
  "errors": {
    "username": ["Username already exists"]
  }
}
```

### Behavior

- Password will be hashed using bcrypt
# AUTH API SPEC

Notes:
- Email verification required before login
- Username and email must be unique

## Register

### Behavior

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
  "username": "string (unique, required)",
  "fullname": "string (optional)",
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
    "email": ["Email is invalid"],
    "password": ["Password must be at least 8 characters", "...."]
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