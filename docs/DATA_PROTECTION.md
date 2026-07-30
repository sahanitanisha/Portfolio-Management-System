# Data Protection & PII Handling Policy

**System:** Portfolio Management System · **Classification:** Internal
**Addresses:** review finding **F-8** (PII retention / encryption for audit and customer data).

This policy defines what personal and financial data the system stores, how it is protected at rest and
in transit, who may access it, how long it is retained, and the approach to field-level encryption.

---

## 1. Data inventory (PII & sensitive data)

| Data element | Stored in | Sensitivity | Purpose | Encrypt at rest |
|---|---|---|---|---|
| Customer / advisor name | `customers`, `advisors` | Medium (PII) | Identity, advisory relationship | Recommended |
| Email | `customers`, `advisors` | Medium (PII) | Contact, unique identity | Lookup key — keep searchable; rely on disk/tablespace encryption |
| Phone number | `customers`, `advisors` | Medium (PII) | Contact | Recommended |
| Postal address | `customers`, `advisors` | Medium (PII) | KYC / contact | **Field-level (candidate)** |
| Date of birth | `customers` | High (PII) | KYC / identity | **Field-level (candidate)** |
| Account numbers, balances | `accounts`, `transactions` | High (financial) | Core function | Disk/tablespace + access control |
| Holdings, trades, P&L | `positions`, `trades` | High (financial) | Core function | Disk/tablespace + access control |
| Username | `app_users` | Medium | Authentication | No (identifier) |
| Password | `app_users` | Critical | Authentication | **One-way BCrypt hash** (never reversible) |
| Refresh tokens | `refresh_tokens` | High | Session continuity | **SHA-256 hash only** (raw never stored) |
| Audit principal | `created_by` / `updated_by` on every table | Low/Medium | Accountability | Minimised (username only) |

The application never logs passwords, raw tokens, or signing keys.

---

## 2. Protection controls

### In transit
TLS is terminated at the ingress/load balancer; HSTS should be enabled at the edge. Internal traffic to
PostgreSQL and Redis stays within the cluster network (see the `NetworkPolicy`).

### At rest
- **Baseline:** enable storage/tablespace encryption on the PostgreSQL volume and Redis persistence
  (transparent data encryption at the infrastructure layer).
- **Credentials:** passwords are stored only as **BCrypt** hashes; refresh tokens only as **SHA-256**
  hashes — neither is reversible.
- **Field-level (defence in depth):** for the high-sensitivity, non-searchable fields (`address`,
  `dateOfBirth`), apply application-level **AES-GCM** encryption via a JPA `AttributeConverter` so the
  plaintext never reaches the database. Searchable keys (email, username) are intentionally left
  unencrypted to preserve uniqueness/lookup; protect them via storage encryption instead.

```java
// Sketch: an autoApply=false converter applied with @Convert on chosen fields.
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    private final AeadCipher cipher; // AES-256-GCM, key from a KMS / sealed secret (NOT in source)
    public String convertToDatabaseColumn(String plaintext) {
        return plaintext == null ? null : cipher.encryptToBase64(plaintext);
    }
    public String convertToEntityAttribute(String ciphertext) {
        return ciphertext == null ? null : cipher.decryptFromBase64(ciphertext);
    }
}
// Usage:  @Convert(converter = EncryptedStringConverter.class) private String address;
```

Keys are sourced from a KMS / sealed secret and **rotated** on a schedule; rotation re-encrypts via a
key-id envelope. This is intentionally not enabled by default because it requires the KMS to be
provisioned first and changes the stored representation of those columns (a data migration).

### Access control
- Role-based: read access requires authentication; writes require `ADVISOR`/`ADMIN`. No API path grants
  privileged roles by self-service.
- Every state change is attributed (`created_by`/`updated_by`) for accountability.
- Database credentials and JWT keys are environment/secret-sourced, never committed.

---

## 3. Retention & erasure

| Category | Retention | Notes |
|---|---|---|
| Immutable financial ledgers (transactions, trades, price quotes) | **7 years** (regulatory default) | Append-only; corrections are compensating entries, never edits/deletes. |
| Customer / advisor PII | Life of relationship **+ 7 years** | Aligns with financial record-keeping. |
| Refresh tokens | Until expiry/revocation | A scheduled job should purge expired/revoked rows. |
| Audit metadata | With the parent record | — |
| Application logs (JSON) | 30–90 days | Avoid PII beyond the audit principal. |

**Right to erasure (GDPR Art. 17 / CCPA):** because ledgers are immutable and legally retained, erasure
is handled by **pseudonymisation** — replacing direct identifiers (name, email, phone, address, DOB)
with tokens while preserving the financial records required by law. A documented data-subject-request
(DSAR) workflow should drive this.

---

## 4. Compliance & responsibilities

- **Applicable regimes:** GDPR (EU), CCPA/CPRA (California), and financial record-keeping rules (e.g.
  SEC 17a-4 / FINRA-style retention). This document is a technical control summary, not legal advice.
- **Data subject rights:** access, rectification, erasure (via pseudonymisation), and portability are
  supported through the customer APIs plus the DSAR workflow above.
- **Breach response:** the audit trail, structured logs, and tracing support incident investigation;
  notification timelines follow the applicable regime.
- **Ownership:** the data controller (the operating institution) owns retention schedules and DSAR SLAs;
  engineering owns the technical controls described here.

---

## 5. Status against finding F-8

| Item | Status |
|---|---|
| PII inventory documented | ✅ This document |
| Retention schedule defined | ✅ Section 3 |
| Credentials/tokens irreversibly hashed | ✅ Implemented (BCrypt / SHA-256) |
| Transport & network isolation | ✅ TLS edge + `NetworkPolicy` |
| Field-level encryption | ◻️ Approach defined (Section 2); enable once a KMS is provisioned |
| Erasure via pseudonymisation | ◻️ Workflow defined; implement DSAR tooling when required |
