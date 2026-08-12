# Smart Salon Appointment and Queue Management System

## 1. Introduction
Traditional salons and barbershops often manage appointments manually through phone calls, walk-ins, notebooks, or messaging applications. This results in poor queue visibility, missed bookings, and a frustrating customer experience.

## 2. Background
Salon businesses depend on appointments and walk-ins to serve customers efficiently. Without a digital system, staff spend excessive time tracking schedules and managing service capacity.

## 3. Existing System
Many small salons still use paper appointment books, spreadsheets, or messaging apps to manage bookings. These systems are prone to human error and provide limited visibility into the queue.

## 4. Problems in Existing System
- Double booking
- Long waiting times
- Poor queue visibility
- Difficulty managing appointments
- Difficulty tracking appointment status
- Poor customer experience

## 5. Proposed System
The proposed solution is a web-based salon appointment and queue management system that allows customers to register, book services, receive queue numbers, and track appointment status. Admins can manage the queue, services, barbers, and appointment updates.

## 6. Objectives
- Create a reliable salon scheduling platform
- Reduce appointment conflicts and waiting times
- Improve visibility into queue and appointment status
- Provide a user-friendly experience for customers and staff
- Establish a foundation for future enhancement and analytics

## 7. Scope
- Customer registration and login
- Service browsing and barber selection
- Appointment booking and queue tracking
- Admin appointment and queue management
- Core backend and frontend foundations only for Day 1

## 8. Functional Requirements
- Customer can register and login
- Customer can view salon services
- Customer can view barbers
- Customer can book appointments
- Customer can receive queue numbers
- Customer can track appointment status
- Customer can view their appointments
- Admin can view appointments
- Admin can manage the queue
- Admin can update appointment status
- Admin can manage services
- Admin can manage barbers

## 9. Non-Functional Requirements
- Secure authentication and authorization
- Responsive frontend UI
- Fast API responses
- Scalable service architecture
- Reliable MySQL persistence
- Clear documentation for development

## 10. User Roles
- Customer: browse services, book appointments, track status
- Admin: manage appointments, queue, services, and barbers

## 11. Technology Stack
- Backend: Java 17, Spring Boot 3, Spring Web, Spring Data JPA, Hibernate, Spring Security, MySQL
- Frontend: React, Vite, JavaScript, React Router, CSS

## 12. Expected Outcome
A working foundation for a salon appointment system with documentation, a backend skeleton, and a frontend skeleton for future implementation.

## 13. Future Enhancements
- JWT authentication and role-based access
- Real appointment booking workflows
- Queue management and status updates
- Customer notifications and email reminders
- Analytics and prediction service
- Payment integration
- WebSocket live queue updates
