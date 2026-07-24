# Frontend Architecture Decision

## Status

Accepted and implemented

## Implementation status

The React and TypeScript foundation was integrated into `develop` on 23 July 2026.

The JWT login flow, protected dashboard route, session persistence, automatic expiry and logout were integrated on 24 July 2026.

The authenticated dashboard now consumes `GET /api/dashboard`, presents real summary data and clears the client session when an authenticated API request returns HTTP `401`.

## Context

The DevFlow Hub backend is implemented with Spring Boot and follows an independent REST API architecture.

The backend currently provides:

- JSON-based REST endpoints
- JWT Bearer authentication
- authorization and security rules
- business logic and validation
- PostgreSQL persistence
- protected endpoints for collaborators, projects, tasks, internal programs and the dashboard

The project requires a frontend technology for building its user interface.

The following alternatives were considered:

1. React with TypeScript
2. Thymeleaf with server-side rendering
3. React and Thymeleaf used together

## Decision

The DevFlow Hub frontend will be developed using React with TypeScript.

Spring Boot will remain responsible exclusively for the backend, including:

- REST API endpoints
- authentication and authorization
- business logic
- validation
- persistence
- database access

Thymeleaf will not be used for the main application interface.

## Architecture

    React and TypeScript frontend
                |
                | HTTP requests and JSON responses
                | JWT Bearer authentication
                v
    Spring Boot REST API
                |
                v
    PostgreSQL database

## Rationale

### Alignment with the existing backend

The Spring Boot application is already structured as a REST API that returns JSON responses.

React can consume these endpoints directly without requiring the backend to render HTML pages.

### Clear separation of responsibilities

React will be responsible for:

- user interface
- navigation
- forms
- state management
- presentation of data
- interaction with the user

Spring Boot will be responsible for:

- authentication
- authorization
- business rules
- data validation
- persistence
- API responses

This separation makes the system easier to understand, maintain and test.

### Reusable frontend components

React supports reusable components for:

- forms
- tables
- navigation menus
- dashboard cards
- task lists
- project views
- authentication pages

This reduces duplicated code and helps maintain a consistent user experience.

### Type safety

TypeScript provides static typing for frontend data structures and API responses.

For example:

    export interface Task {
      id: number
      title: string
      description: string
      status: string
      createdAt: string
      updatedAt: string
    }

This helps identify invalid field names, missing values and incompatible data during development.

### Independent development

The frontend and backend can be developed and tested independently, provided that the API contract remains stable.

### Future integrations

Keeping Spring Boot as an independent API allows the backend to support additional clients in the future, including:

- mobile applications
- desktop applications
- external integrations
- automation services

The backend is therefore not coupled to a specific HTML rendering technology.

## Why Thymeleaf was not selected

Thymeleaf is more appropriate when Spring Boot is also responsible for rendering the HTML interface on the server.

The DevFlow Hub already follows a REST API architecture. Introducing Thymeleaf would create a second presentation model.

Using React and Thymeleaf together could require maintaining:

- two navigation systems
- two sets of layouts
- separate authentication flows
- duplicated validation logic
- different error-handling approaches
- duplicated styles and components
- additional tests

This would increase complexity without providing a clear benefit.

Thymeleaf is also not required for secure cookies. Spring Boot can manage `HttpOnly`, `Secure` and `SameSite` cookies while the interface remains entirely implemented in React.

## Frontend tools

The frontend foundation uses:

- React
- TypeScript
- Vite
- ESLint
- React Router
- native Fetch API

### React

React will provide a component-based structure for building dynamic and reusable user interfaces.

### TypeScript

TypeScript will add static typing to frontend components, application state and API responses.

### Vite

Vite provides a fast development server, TypeScript support, production builds and environment-variable management with minimal configuration.

### ESLint

ESLint will identify code-quality problems and help enforce consistent React and TypeScript practices.

### React Router

React Router will manage navigation and protected routes, including:

    /login
    /dashboard
    /projects
    /tasks
    /collaborators
    /internal-programs

### Fetch API

The native Fetch API will initially be used for communication with the Spring Boot backend.

It supports:

- HTTP requests
- JSON responses
- JWT Bearer headers
- HTTP status handling
- API error processing

An additional HTTP library may be considered later if API communication becomes significantly more complex.

## Authentication

The frontend will initially use the existing JWT Bearer authentication flow:

    React sends credentials to POST /api/auth/login
    Spring Boot validates the credentials
    Spring Boot returns a JWT
    React includes the JWT in the Authorization header
    Spring Boot validates the JWT on protected requests

Example request header:

    Authorization: Bearer <token>

The token-storage strategy will be reviewed separately before production deployment.

## Consequences

### Positive consequences

- clear separation between frontend and backend
- reusable interface components
- independent development and testing
- TypeScript type safety
- compatibility with the existing REST API
- support for future client applications
- reduced coupling between presentation and business logic

### Trade-offs

- frontend and backend run as separate applications
- CORS must be configured
- authentication state must be managed by React
- frontend and backend API contracts must remain coordinated
- deployment requires configuration for both applications

## Final decision

The DevFlow Hub user interface will be developed with React and TypeScript.

Spring Boot will be used exclusively as the REST API backend.

Thymeleaf will not be used for the main application interface.
