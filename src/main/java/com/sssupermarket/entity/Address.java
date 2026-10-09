package com.sssupermarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false, length = 100) private String fullName;
    @Column(nullable = false, length = 10) private String mobileNumber;
    @Column(nullable = false, length = 250) private String addressLine1;
    @Column(length = 250) private String addressLine2;
    @Column(nullable = false, length = 100) private String city;
    @Column(nullable = false, length = 100) private String state;
    @Column(nullable = false, length = 10) private String pincode;
    @Column(nullable = false, length = 80) private String country;
    @Column(name = "is_default", nullable = false) private boolean defaultAddress;
    @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist protected void create() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate protected void update() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public User getUser() { return user; } public void setUser(User user) { this.user = user; }
    public String getFullName() { return fullName; } public void setFullName(String v) { fullName = v; }
    public String getMobileNumber() { return mobileNumber; } public void setMobileNumber(String v) { mobileNumber = v; }
    public String getAddressLine1() { return addressLine1; } public void setAddressLine1(String v) { addressLine1 = v; }
    public String getAddressLine2() { return addressLine2; } public void setAddressLine2(String v) { addressLine2 = v; }
    public String getCity() { return city; } public void setCity(String v) { city = v; }
    public String getState() { return state; } public void setState(String v) { state = v; }
    public String getPincode() { return pincode; } public void setPincode(String v) { pincode = v; }
    public String getCountry() { return country; } public void setCountry(String v) { country = v; }
    public boolean isDefaultAddress() { return defaultAddress; } public void setDefaultAddress(boolean v) { defaultAddress = v; }
}
