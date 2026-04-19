# Coop Loan Approval

A solution for loan application submission, payment schedule generation, and loan review.
The project includes a Spring Boot backend, a PostgreSQL database, and a React/Vite user interface.

## Quick Overview

The solution covers the mandatory assignment requirements:

- loan application submission and persistence to the database;
- Estonian personal code validation;
- one active application rule per customer;
- age check with a configurable limit;
- annuity schedule generation and persistence;
- process states `STARTED` -> `IN_REVIEW` -> `APPROVED` / `REJECTED`;
- manual review, approval, and rejection with reason;
- OpenAPI / Swagger documentation;
- Dockerized stack that starts with one `docker-compose.yaml` file.

## Technologies

- **Java:** 25
- **Spring Boot:** 4.0.5
- **Database:** PostgreSQL 16
- **Migrations:** Liquibase
- **API documentation:** SpringDoc OpenAPI 3
- **Backend tests:** JUnit 5 + Mockito
- **Frontend:** React 19 + Vite
- **Containerization:** Docker + Docker Compose

## Project Structure

```text
coop-loan-approval/
|- backend/      # Spring Boot backend, Liquibase migrations, Dockerfile
|- frontend/     # React/Vite UI, Nginx config, Dockerfile
|- docker-compose.yaml
`- README.md
```

## How To Run
Run in the project root (coop-loan-approval):

```
docker-compose up -d
```

This starts:

- PostgreSQL database;
- backend service on port `8080`;
- frontend service on port `3000`.

### URLs After Startup

- **Frontend UI:** http://localhost:3000
- **Backend API:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html


## Backend API

Main endpoints:

- `GET /api/loan-applications` - get all applications
- `GET /api/loan-applications/{id}` - get application with payment schedule
- `GET /api/loan-applications/in-review` - get applications in review
- `POST /api/loan-applications` - create a new loan application
- `POST /api/loan-applications/{id}/approve` - approve an application
- `POST /api/loan-applications/{id}/reject` - reject an application with reason
- `PUT /api/loan-applications/{id}/regenerate-schedule` - regenerate schedule (only in `IN_REVIEW` status)

## Implemented Business Rules

### Loan Application Submission

- First name: up to 32 characters
- Last name: up to 32 characters
- Personal code: validated against Estonian standard
- Loan period: 6-360 months
- Interest margin: `>= 0`
- Base interest rate: `>= 0`
- Loan amount: `>= 5000`

On submission:

- all input data is persisted;
- a unique ID is generated;
- the system checks that the customer does not already have another active application.

### Age Check

- If applicant age exceeds the configured threshold, the application is automatically rejected.
- Status is set to `REJECTED`.
- Rejection reason is set to `CUSTOMER_TOO_OLD`.
- Age limit is configurable via `loan.customer.max-age` in `application.properties`.

### Payment Schedule Generation

- The system generates monthly annuity payments.
- The schedule is saved to the database.
- First payment date defaults to the current date.
- After successful schedule generation, status moves to `IN_REVIEW`.

### Loan Review

- `IN_REVIEW` applications can be viewed with applicant data and payment schedule.
- Application can be approved to `APPROVED`.
- Application can be rejected with a reason to `REJECTED`.
- Application can be `regenerated` with updated parameters.

## Database And Migrations

Database schema is managed with Liquibase migrations.

Main tables:

- `loan_application`
- `loan_payment_schedule`

Database data validation and integrity rules:

- personal code format check;
- uniqueness for active applications per personal code;
- loan period, interest, and amount constraints;
- foreign key between payment schedule and application.

## Validation And Transaction Handling

- Frontend UI includes client-side checks (for example, disabled actions for empty lookup ID / reject reason and numeric input types for loan parameters).
- API request payloads are validated with `@Valid` in controller endpoints.
- DTOs and entities use Bean Validation annotations such as `@NotBlank`, `@Size`, `@Min`, `@Max`, and `@DecimalMin`.
- Database-level constraints are enforced via Liquibase migrations, providing a second validation and integrity layer.
- Core write operations in `LoanApplicationService` use `@Transactional` to keep multi-step updates atomic.
- Read operations use transactional read boundaries with `@Transactional(readOnly = true)`.
- Backend and database validation are the source of truth for business-rule enforcement.

## Configuration

Application configuration is in `backend/src/main/resources/application.properties`.

Important parameter:

- `loan.customer.max-age=70` - age threshold for automatic rejection.

Database environment variables used in Docker Compose:

- `DB_HOST`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`

## Optional Tasks Implemented

The following optional tasks are implemented in this solution:

- **Error handling** - global `@RestControllerAdvice` for validation, business, and technical errors.
- **Testing** - Mockito-based unit tests for business logic.
- **Schedule regeneration** - schedule can be updated and regenerated in `IN_REVIEW` status.
- **User interface (UI)** - simple React/Vite frontend included in Docker Compose.

