# Architecture Overview

This system uses a layered architecture to separate frontend, backend, service, and persistence concerns.

- React Frontend: user interface for customers and admins.
- REST API: communication layer between frontend and backend.
- Spring Boot Controller: handles HTTP requests and delegates to services.
- Service Layer: application business logic.
- Repository Layer: data access using Spring Data JPA.
- Spring Data JPA / Hibernate: ORM mapping to MySQL.
- MySQL: persistent relational database.

The architecture also includes security flow:
- React
  ↓
- JWT
  ↓
- Spring Security
  ↓
- Protected Spring Boot APIs
