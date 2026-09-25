package com.graminsaathi.repository;

import com.graminsaathi.model.BusinessCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessCategoryRepository extends JpaRepository<BusinessCategory, Long> {

    Optional<BusinessCategory> findByCategoryNameIgnoreCase(String categoryName);

    /** Every category name already stored (lower-cased) - lets the seed initializer skip existing rows (insert-new-only, like {@code VillageService.importCsv}). */
    @Query("select lower(c.categoryName) from BusinessCategory c")
    List<String> findAllCategoryNamesLower();
}
