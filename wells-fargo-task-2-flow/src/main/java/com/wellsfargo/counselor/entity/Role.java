package com.wellsfargo.counselor.entity;

/**
 * Authorization role. Mapped to a Spring Security authority {@code ROLE_<name>}. ADMIN and ADVISOR may
 * perform writes; VIEWER is read-only.
 */
public enum Role {
    ADMIN,
    ADVISOR,
    VIEWER
}
