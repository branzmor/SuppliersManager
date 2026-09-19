package com.supplier.management.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * JPA entity backing the {@code SupplierRecord} aggregate. Deliberately separate from the domain
 * class (never reused as the domain object — see {@code domain} package-info and
 * {@code SOLUTION.md}) so JPA lifecycle/proxying concerns never leak into business logic.
 *
 * <p>{@code duns} carries the {@code UNIQUE} constraint that makes the aggregate's integrity
 * rules (README §"Integrity Rules") automatic — see proposed schema in
 * {@code V1__create_supplier_record_table.sql}.
 *
 * <p>{@code version} ({@link Version}, added in {@code V2__add_supplier_record_version.sql})
 * enables JPA optimistic locking: two concurrent transactions that both load the same row and
 * then both try to update it will have the second one's flush fail with
 * {@code ObjectOptimisticLockingFailureException} instead of silently overwriting the first
 * transaction's change (see {@code GlobalExceptionHandler}, which maps that to {@code 409}).
 * Field-only, deliberately with no setter — the value is managed exclusively by Hibernate.
 */
@Entity
@Table(name = "supplier_record", uniqueConstraints = @UniqueConstraint(name = "uk_supplier_record_duns", columnNames = "duns"))
public class SupplierRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer duns;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false)
    private Long annualTurnover;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SupplierStatusJpa status;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private SustainabilityRatingJpa sustainabilityRating;

    @Version
    @Column(nullable = false)
    private Long version;

    protected SupplierRecordEntity() {
        // JPA
    }

    public SupplierRecordEntity(Integer duns, String name, String country, Long annualTurnover,
                                 SupplierStatusJpa status, SustainabilityRatingJpa sustainabilityRating) {
        this.duns = duns;
        this.name = name;
        this.country = country;
        this.annualTurnover = annualTurnover;
        this.status = status;
        this.sustainabilityRating = sustainabilityRating;
    }

    public Long getId() {
        return id;
    }

    public Integer getDuns() {
        return duns;
    }

    public void setDuns(Integer duns) {
        this.duns = duns;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Long getAnnualTurnover() {
        return annualTurnover;
    }

    public void setAnnualTurnover(Long annualTurnover) {
        this.annualTurnover = annualTurnover;
    }

    public SupplierStatusJpa getStatus() {
        return status;
    }

    public void setStatus(SupplierStatusJpa status) {
        this.status = status;
    }

    public SustainabilityRatingJpa getSustainabilityRating() {
        return sustainabilityRating;
    }

    public void setSustainabilityRating(SustainabilityRatingJpa sustainabilityRating) {
        this.sustainabilityRating = sustainabilityRating;
    }

    public Long getVersion() {
        return version;
    }
}
