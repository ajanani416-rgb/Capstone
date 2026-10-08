package com.salon.management.repository;

import com.salon.management.entity.Service;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Service}. Customers see active services only;
 * admins see all (enforced in the service layer, TASK-006).
 */
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByActiveTrue();
}
