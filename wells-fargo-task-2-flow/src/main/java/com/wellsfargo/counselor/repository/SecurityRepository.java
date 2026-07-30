package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.Security;
import com.wellsfargo.counselor.entity.SecurityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecurityRepository extends JpaRepository<Security, Long> {

    Optional<Security> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);

    Page<Security> findByType(SecurityType type, Pageable pageable);
}
