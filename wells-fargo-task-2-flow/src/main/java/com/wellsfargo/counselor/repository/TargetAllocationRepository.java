package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.TargetAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TargetAllocationRepository extends JpaRepository<TargetAllocation, Long> {

    List<TargetAllocation> findByPortfolioId(Long portfolioId);

    Optional<TargetAllocation> findByPortfolioIdAndSecurityId(Long portfolioId, Long securityId);

    void deleteByPortfolioId(Long portfolioId);
}
