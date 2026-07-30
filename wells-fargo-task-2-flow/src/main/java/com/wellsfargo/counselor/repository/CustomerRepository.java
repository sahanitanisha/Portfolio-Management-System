package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Page<Customer> findByAdvisorId(Long advisorId, Pageable pageable);

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);
}
