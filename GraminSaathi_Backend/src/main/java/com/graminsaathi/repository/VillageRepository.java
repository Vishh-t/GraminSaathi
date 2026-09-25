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

    /** id + lgdCode for every row with an LGD code - lets a backfill match existing rows without a query per row. */
    @Query("select v.id, v.lgdCode from Village v where v.lgdCode is not null")
    List<Object[]> findIdsByLgdCode();

    /** id + the three parts of the name+district+state dedup key, for every row - same purpose as above for rows with no LGD code. */
    @Query("select v.id, v.nameNormalized, v.district, v.state from Village v")
    List<Object[]> findIdNameDistrictState();

    /** id + the four PC11 code parts, for every row that has them - lets a coordinates backfill match on the precise Census key first. */
    @Query("select v.id, v.pc11StateCode, v.pc11DistrictCode, v.pc11SubdistrictCode, v.pc11VillageCode from Village v where v.pc11VillageCode is not null")
    List<Object[]> findIdsByPc11Codes();

    /**
     * Keyset (seek) pagination: the next up-to-{@code pageable}'s-limit rows with id greater than
     * {@code lastId}, ordered by id. Used instead of {@code findAll(Pageable)} (OFFSET-based) for any
     * full-table scan over the whole village table - OFFSET gets slower every page since Postgres has to
     * skip that many rows first; this stays fast at any point in the table because {@code id > :lastId}
     * seeks straight there via the primary key index. Pass {@code PageRequest.of(0, batchSize)} for
     * {@code pageable} - only its page size is used, not its page number.
     */
    List<Village> findByIdGreaterThanOrderByIdAsc(Long lastId, Pageable pageable);

    /**
     * Exact single-village lookup for callers that already have a specific village name (e.g.
     * DiscoveryService), as opposed to {@link #search} which is fuzzy/multi-result for autocomplete.
     * {@code nameNormalized} isn't unique table-wide (same village name can exist in different
     * districts/states), so this returns the first match by id - callers needing a specific
     * district/state should filter further themselves.
     */
    java.util.Optional<Village> findFirstByNameNormalized(String nameNormalized);
}
