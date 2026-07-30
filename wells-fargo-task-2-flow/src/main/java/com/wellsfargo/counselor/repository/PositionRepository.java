package com.wellsfargo.counselor.repository;

import com.wellsfargo.counselor.entity.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    Page<Position> findByPortfolioId(Long portfolioId, Pageable pageable);

    Optional<Position> findByPortfolioIdAndSecurityId(Long portfolioId, Long securityId);

    /** Valuation read: fetches each position's security in one query to avoid the N+1 problem. */
    @EntityGraph(attributePaths = "security")
    List<Position> findByPortfolioId(Long portfolioId);

    /** Distinct portfolios currently holding the given security (drives live valuation broadcasts). */
    @Query("select distinct p.portfolio.id from Position p where p.security.id = :securityId")
    List<Long> findPortfolioIdsHoldingSecurity(@Param("securityId") Long securityId);
}
