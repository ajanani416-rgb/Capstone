package com.salon.management.repository;

import com.salon.management.entity.Barber;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Barber}. Customers see active barbers only.
 */
public interface BarberRepository extends JpaRepository<Barber, Long> {

    List<Barber> findByActiveTrue();
}
