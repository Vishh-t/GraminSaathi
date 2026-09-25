package com.graminsaathi.repository;

import com.graminsaathi.model.VillageFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * One row per {@link com.graminsaathi.model.Village}, keyed by the same id (see
 * Project_Docs/RECOMMENDATION_ENGINE_BUILD_LOG.md, step 2). Standard CRUD is all the ETL needs so far -
 * paging through the village table itself (not this one) drives the population-projection batch job.
 */
@Repository
public interface VillageFeaturesRepository extends JpaRepository<VillageFeatures, Long> {
}
