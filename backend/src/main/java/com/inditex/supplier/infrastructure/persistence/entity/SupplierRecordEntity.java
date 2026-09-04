package com.inditex.supplier.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA entity backing the {@code SupplierRecord} aggregate. Deliberately separate from the domain
 * class (never reused as the domain object — see {@code domain} package-info and
 * {@code SOLUTION.md}) so JPA lifecycle/proxying concerns never leak into business logic.
 *
 * <p>{@code duns} carries the {@code UNIQUE} constraint that makes the aggregate's integrity
 * rules (README §"Integrity Rules") automatic — see proposed schema in
 * {@code V1__create_supplier_record_table.sql}.
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

    protected SupplierRecordEntity() {
        // JPA
    }

    // TODO: add an all-args constructor (excluding generated id) once the persistence mapper is implemented.

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
}
