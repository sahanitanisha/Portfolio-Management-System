package com.wellsfargo.counselor.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A financial advisor and the root of the client hierarchy. Inherits identity, optimistic locking
 * and audit metadata from {@link BaseEntity}.
 */
@Entity
@Table(name = "advisors")
public class Advisor extends BaseEntity {

    @NotBlank
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank
    @Column(name = "address", nullable = false)
    private String address;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9 .()-]{7,20}$", message = "must be a valid phone number")
    @Column(name = "phone", nullable = false)
    private String phone;

    @NotBlank
    @Email
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * Clients managed by this advisor. {@code orphanRemoval} ties a client's lifecycle to its
     * advisor link; the collection is owned by {@link Customer#advisor} (the {@code mappedBy} side).
     */
    @OneToMany(mappedBy = "advisor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Customer> customers = new ArrayList<>();

    /** Required by JPA. */
    protected Advisor() {
    }

    public Advisor(String firstName, String lastName, String address, String phone, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.phone = phone;
        this.email = email;
    }

    /** Adds a client and keeps both sides of the bidirectional association in sync. */
    public void addCustomer(Customer customer) {
        customers.add(customer);
        customer.setAdvisor(this);
    }

    public void removeCustomer(Customer customer) {
        customers.remove(customer);
        customer.setAdvisor(null);
    }

    /** Read-only view; mutate through {@link #addCustomer}/{@link #removeCustomer} to preserve the back-reference. */
    public List<Customer> getCustomers() {
        return Collections.unmodifiableList(customers);
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
