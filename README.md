# Smart Salon Appointment and Queue Management System

## Project Overview
A college capstone MVP foundation for a salon appointment and queue management system. The system is designed to improve traditional appointment handling by providing a modern backend and frontend skeleton for customer and admin workflows.

## Features
- Project foundation for backend and frontend
- Basic placeholder pages for React frontend
- Spring Boot project structure with layered architecture
- Documentation for architecture, ER diagram, and database schema

## Technology Stack
- Backend: Java 17, Spring Boot 3, Maven, Spring Web, Spring Data JPA, Spring Security, MySQL
- Frontend: React, Vite, JavaScript, React Router, CSS
- Documentation: Markdown

## Architecture
React frontend communicates with a REST API built with Spring Boot. The application is organized with controllers, services, repositories, and entity layers.

## Database
MySQL is used as the relational database. Spring Data JPA and Hibernate will manage persistence.

## Folder Structure
- `backend/` - Spring Boot backend project
- `frontend/` - React + Vite frontend project
- `docs/` - project documentation and diagrams
- `Problem_Statement.md` - capstone problem statement
- `.env.example` - environment variable template

## Prerequisites
- Java 17+
- Maven
- Node.js 18+ and npm
- MySQL

## How to run backend
1. Configure `backend/src/main/resources/application.properties` with your MySQL credentials.
2. Run from `backend/`:
   - `mvn clean test`
   - `mvn spring-boot:run`

## How to run frontend
1. From `frontend/`:
   - `npm install`
   - `npm run build`
   - `npm run dev`

## MySQL setup
1. Create a database named `salon_management`.
2. Update `.env` or `backend/src/main/resources/application.properties` with your connection settings.

## Environment variables
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
