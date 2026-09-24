package com.graminsaathi.repository;

import com.graminsaathi.model.Village;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VillageRepository extends JpaRepository<Village, Long> {

    /**
     * Autocomplete search. {@code q} MUST already be normalized (see VillageService#normalize) - that
     * strips LIKE wildcards (%, _) so user input can't inject them. {@code state} is an empty string
     * for "any state" (a null param here trips Postgres type inference, hence the '' convention).
     * Names that START with the query rank ahead of names that merely contain it.
     */
    @Query("""
            select v from Village v
            where v.nameNormalized like concat('%', :q, '%')
              and (:state = '' or lower(v.state) = lower(:state))
            order by case when v.nameNormalized like concat(:q, '%') then 0 else 1 end, v.name, v.district
            """)
    List<Village> search(@Param("q") String q, @Param("state") String state, Pageable pageable);

    /** Every LGD code already stored - lets the importer skip existing rows without a query per row. */
    @Query("select v.lgdCode from Village v where v.lgdCode is not null")
    List<String> findAllLgdCodes();

    /** name|district|state keys (all lower-case) for rows with no LGD code, for the same skip-existing check. */
    @Query("select concat(v.nameNormalized, '|', lower(v.district), '|', lower(v.state)) from Village v")
    List<String> findAllNameKeys();
}
