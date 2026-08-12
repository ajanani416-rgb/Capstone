# ER Diagram

Entities and relationships for the salon management system.

- USER (PK: id)
- SERVICE (PK: id)
- BARBER (PK: id)
- APPOINTMENT (PK: id, FK: userId, barberId, serviceId)

Relationships:
- USER 1 --- MANY APPOINTMENT
- BARBER 1 --- MANY APPOINTMENT
- SERVICE 1 --- MANY APPOINTMENT
