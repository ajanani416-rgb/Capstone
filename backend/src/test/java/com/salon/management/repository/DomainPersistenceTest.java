package com.salon.management.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.salon.management.entity.Appointment;
import com.salon.management.entity.AppointmentStatus;
import com.salon.management.entity.Barber;
import com.salon.management.entity.Role;
import com.salon.management.entity.Service;
import com.salon.management.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * TASK-002 slice tests: proves the JPA domain maps and persists per
 * docs/database-schema.md.
 */
@DataJpaTest
class DomainPersistenceTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository users;

    @Autowired
    private ServiceRepository services;

    @Autowired
    private BarberRepository barbers;

    @Autowired
    private AppointmentRepository appointments;

    private User customer() {
        return new User("Asha", "asha-" + System.nanoTime() + "@example.com",
                "$2a$10$testhashplaceholder0000000000000000000000", Role.CUSTOMER);
    }

    private Service haircut() {
        return new Service("Haircut", "Basic cut", 30,
                new BigDecimal("299.00"), true);
    }

    private Barber barber() {
        return new Barber("Ravi", "Fade", true);
    }

    @Test
    void persistsFullAppointmentGraph() {
        User user = em.persistAndFlush(customer());
        Service service = em.persistAndFlush(haircut());
        Barber barber = em.persistAndFlush(barber());

        Appointment appt = new Appointment(user, barber, service,
                LocalDate.of(2026, 10, 1), LocalTime.of(10, 0), AppointmentStatus.QUEUED);
        appt.setQueueNumber(1);
        Appointment saved = appointments.saveAndFlush(appt);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        List<Appointment> mine = appointments
                .findByUserIdOrderByAppointmentDateAscAppointmentTimeAsc(user.getId());
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getQueueNumber()).isEqualTo(1);
    }

    @Test
    void emailIsUnique() {
        String email = "dup-" + System.nanoTime() + "@example.com";
        users.saveAndFlush(new User("One", email, "hash", Role.CUSTOMER));

        assertThatThrownBy(() -> users.saveAndFlush(
                new User("Two", email, "hash", Role.CUSTOMER)))
                .isInstanceOf(Exception.class);
    }

    @Test
    void activeFiltersHideInactiveCatalog() {
        services.saveAndFlush(haircut());
        Service archived = new Service("Old", null, 15,
                new BigDecimal("99.00"), false);
        services.saveAndFlush(archived);
        assertThat(services.findByActiveTrue())
                .extracting(Service::getName).contains("Haircut").doesNotContain("Old");

        barbers.saveAndFlush(barber());
        barbers.saveAndFlush(new Barber("Gone", null, false));
        assertThat(barbers.findByActiveTrue())
                .extracting(Barber::getName).contains("Ravi").doesNotContain("Gone");
    }

    @Test
    void queueQueryOrdersByDateTimeQueueNumber() {
        User user = em.persistAndFlush(customer());
        Service service = em.persistAndFlush(haircut());
        Barber barber = em.persistAndFlush(barber());
        LocalDate day = LocalDate.of(2026, 10, 2);

        Appointment late = new Appointment(user, barber, service,
                day, LocalTime.of(11, 0), AppointmentStatus.QUEUED);
        late.setQueueNumber(2);
        Appointment early = new Appointment(user, barber, service,
                day, LocalTime.of(9, 0), AppointmentStatus.QUEUED);
        early.setQueueNumber(1);
        appointments.saveAllAndFlush(List.of(late, early));

        List<Appointment> ordered = appointments
                .findByAppointmentDateOrderByAppointmentTimeAscQueueNumberAsc(day);
        assertThat(ordered).hasSize(2);
        assertThat(ordered.get(0).getAppointmentTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(ordered.get(1).getAppointmentTime()).isEqualTo(LocalTime.of(11, 0));
    }
}
