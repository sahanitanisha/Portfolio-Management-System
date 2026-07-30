package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.Advisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, Long> {

    Optional<Advisor> findByEmail(String email);

    boolean existsByEmail(String email);
}
