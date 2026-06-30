package com.ceotracker.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ceo_contacts", indexes = {
    @Index(name = "idx_phone", columnList = "phone_number"),
    @Index(name = "idx_score", columnList = "viability_score")
})
public class CeoContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "ceo_name")
    private String ceoName;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "city")
    private String city;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "viability_score")
    private int viabilityScore;

    @Column(name = "verified_by_website")
    private boolean verifiedByWebsite;

    @Column(name = "verified_on_maps")
    private boolean verifiedOnMaps;

    @Column(name = "num_sources")
    private int numberOfSources = 1;

    @Column(name = "cross_referenced")
    private boolean crossReferenced;

    @Column(name = "status", nullable = false)
    private String status = "pending";

    @Column(name = "is_valid")
    private boolean isValid;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCeoName() { return ceoName; }
    public void setCeoName(String ceoName) { this.ceoName = ceoName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public int getViabilityScore() { return viabilityScore; }
    public void setViabilityScore(int viabilityScore) { this.viabilityScore = viabilityScore; }

    public boolean isVerifiedByWebsite() { return verifiedByWebsite; }
    public void setVerifiedByWebsite(boolean verifiedByWebsite) { this.verifiedByWebsite = verifiedByWebsite; }

    public boolean isVerifiedOnMaps() { return verifiedOnMaps; }
    public void setVerifiedOnMaps(boolean verifiedOnMaps) { this.verifiedOnMaps = verifiedOnMaps; }

    public int getNumberOfSources() { return numberOfSources; }
    public void setNumberOfSources(int numberOfSources) { this.numberOfSources = numberOfSources; }

    public boolean isCrossReferenced() { return crossReferenced; }
    public void setCrossReferenced(boolean crossReferenced) { this.crossReferenced = crossReferenced; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }

    public LocalDateTime getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(LocalDateTime lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof CeoContact other) {
            return Objects.equals(phoneNumber, other.phoneNumber);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(phoneNumber);
    }
}
