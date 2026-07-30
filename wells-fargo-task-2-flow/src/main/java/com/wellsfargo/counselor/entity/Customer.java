package com.wellsfargo.counselor.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A client managed by an {@link Advisor}. Owns one or more {@link Account}s.
 */
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @NotBlank
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank
    @Email
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Pattern(regexp = "^\\+?[0-9 .()-]{7,20}$", message = "must be a valid phone number")
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Past
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Owning side of the advisor relationship. Non-optional: every client must be managed by an
     * advisor, which is the core invariant the original model was missing.
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advisor_id", nullable = false)
    private Advisor advisor;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Account> accounts = new ArrayList<>();

    /** Required by JPA. */
    protected Customer() {
    }

    public Customer(String firstName, String lastName, String email, String phoneNumber,
                    String address, LocalDate dateOfBirth) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
    }

    /** Adds an account and keeps both sides of the bidirectional association in sync. */
    public void addAccount(Account account) {
        accounts.add(account);
        account.setCustomer(this);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setCustomer(null);
    }

    /** Read-only view; mutate through {@link #addAccount}/{@link #removeAccount}. */
    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Advisor getAdvisor() {
        return advisor;
    }

    /** Package-private: the advisor link is managed by {@link Advisor#addCustomer}/{@link Advisor#removeCustomer}. */
    void setAdvisor(Advisor advisor) {
        this.advisor = advisor;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
