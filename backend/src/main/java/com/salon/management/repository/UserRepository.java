package com.salon.management.repository;

import com.salon.management.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link User}. Query methods here are derived (no JPQL) so
 * behavior stays obvious for capstone review.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(com.salon.management.entity.Role role);

    List<User> findAllByOrderByIdAsc();
}
