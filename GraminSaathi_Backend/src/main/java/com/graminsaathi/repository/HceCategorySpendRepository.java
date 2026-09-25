package com.graminsaathi.repository;

import com.graminsaathi.model.HceCategorySpend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HceCategorySpendRepository extends JpaRepository<HceCategorySpend, Long> {

    Optional<HceCategorySpend> findByStateIgnoreCaseAndCategoryNameIgnoreCase(String state, String categoryName);
}
