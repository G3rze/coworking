package com.gerson.coworking.repository;

import com.gerson.coworking.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByUsernameWithRole(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
