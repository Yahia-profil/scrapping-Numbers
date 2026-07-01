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

    List<CeoContact> findByCityOrderByViabilityScoreDesc(String city);

    @Query("SELECT DISTINCT c.city FROM CeoContact c WHERE c.city IS NOT NULL ORDER BY c.city")
    List<String> findDistinctCities();

    @Query("SELECT DISTINCT c.activity FROM CeoContact c WHERE c.activity IS NOT NULL AND c.activity <> '' ORDER BY c.activity")
    List<String> findDistinctActivities();

    List<CeoContact> findByActivityOrderByViabilityScoreDesc(String activity);

    List<CeoContact> findByCityAndActivityOrderByViabilityScoreDesc(String city, String activity);
}
