package com.supplier.management.infrastructure.persistence.mapper;

import com.supplier.management.domain.model.AnnualTurnover;
import com.supplier.management.domain.model.CountryCode;
import com.supplier.management.domain.model.Duns;
import com.supplier.management.domain.model.SupplierRecord;
import com.supplier.management.domain.model.SupplierStatus;
import com.supplier.management.domain.model.SustainabilityRating;
import com.supplier.management.infrastructure.persistence.entity.SupplierRecordEntity;
import com.supplier.management.infrastructure.persistence.entity.SupplierStatusJpa;
import com.supplier.management.infrastructure.persistence.entity.SustainabilityRatingJpa;
import org.springframework.stereotype.Component;

/**
 * Maps between the {@link SupplierRecord} domain aggregate and {@link SupplierRecordEntity}.
 *
 * <p>{@code toDomain} must use {@code SupplierRecord#reconstitute} (never {@code #apply}) since
 * the entity already represents a persisted, possibly-transitioned state.
 */
@Component
public class SupplierPersistenceMapper {

    public SupplierRecord toDomain(SupplierRecordEntity entity) {
        SustainabilityRatingJpa ratingJpa = entity.getSustainabilityRating();
        return SupplierRecord.reconstitute(
                new Duns(entity.getDuns()),
                entity.getName(),
                new CountryCode(entity.getCountry()),
                new AnnualTurnover(entity.getAnnualTurnover()),
                SupplierStatus.valueOf(entity.getStatus().name()),
                ratingJpa == null ? null : SustainabilityRating.valueOf(ratingJpa.name()));
    }

    public SupplierRecordEntity toEntity(SupplierRecord record) {
        SustainabilityRating rating = record.sustainabilityRating();
        return new SupplierRecordEntity(
                record.duns().value(),
                record.name(),
                record.country().isoCode(),
                record.annualTurnover().value(),
                SupplierStatusJpa.valueOf(record.status().name()),
                rating == null ? null : SustainabilityRatingJpa.valueOf(rating.name()));
    }

    /**
     * Applies the mutable fields of {@code record} onto an already-managed {@code entity}
     * (update path), so JPA dirty-checking flushes only the changed columns instead of a full
     * replace-by-id.
     */
    public void updateEntity(SupplierRecordEntity entity, SupplierRecord record) {
        SustainabilityRating rating = record.sustainabilityRating();
        entity.setDuns(record.duns().value());
        entity.setName(record.name());
        entity.setCountry(record.country().isoCode());
        entity.setAnnualTurnover(record.annualTurnover().value());
        entity.setStatus(SupplierStatusJpa.valueOf(record.status().name()));
        entity.setSustainabilityRating(rating == null ? null : SustainabilityRatingJpa.valueOf(rating.name()));
    }
}
