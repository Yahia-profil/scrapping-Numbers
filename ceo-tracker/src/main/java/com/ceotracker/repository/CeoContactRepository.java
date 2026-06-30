package com.ceotracker.repository;

import com.ceotracker.entity.CeoContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CeoContactRepository extends JpaRepository<CeoContact, Long> {

    Optional<CeoContact> findByPhoneNumber(String phoneNumber);

    List<CeoContact> findAllByOrderByViabilityScoreDesc();

    List<CeoContact> findByViabilityScoreGreaterThanEqual(int score);

    List<CeoContact> findByViabilityScoreLessThan(int score);

    @Query("SELECT DISTINCT c.sourceType FROM CeoContact c")
    List<String> findDistinctSourceTypes();

    List<CeoContact> findByVerifiedOnMapsFalse();

    List<CeoContact> findByCeoNameIsNull();

    @Query("SELECT c FROM CeoContact c WHERE c.ceoName IS NULL OR c.ceoName = ''")
    List<CeoContact> findMissingCeoName();

    List<CeoContact> findByCityOrderByViabilityScoreDesc(String city);

    @Query("SELECT DISTINCT c.city FROM CeoContact c WHERE c.city IS NOT NULL ORDER BY c.city")
    List<String> findDistinctCities();
}
