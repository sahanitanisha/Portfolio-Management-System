package com.wellsfargo.counselor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Common persistence concerns for every aggregate root: a surrogate identity, an optimistic-lock
 * guard and immutable audit metadata. Centralising these enforces a single, consistent strategy
 * (DRY) and gives every entity lost-update protection and auditability for free.
 *
 * <p>Audit fields are populated by Spring Data JPA auditing (see {@code JpaAuditingConfig}); they
 * are never set by application code, which is why no setters are exposed.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /**
     * Optimistic-lock version. Hibernate increments this on every flush and rejects a write whose
     * in-memory version is stale, converting silent lost updates into an explicit
     * {@code OptimisticLockException}. Essential for concurrent financial state changes.
     */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    /**
     * Identity equality based on the assigned surrogate key, following the Hibernate-recommended
     * pattern. Two not-yet-persisted instances ({@code id == null}) are equal only by reference,
     * which keeps entities safe to place in {@link java.util.Set}s before and after persistence.
     * {@link Hibernate#getClass(Object)} unwraps lazy proxies so a proxy and its target compare equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        BaseEntity other = (BaseEntity) o;
        return id != null && id.equals(other.id);
    }

    /**
     * Constant hash code. Required so an entity's hash does not change when its id transitions from
     * {@code null} to a generated value while it sits inside a hash-based collection.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
