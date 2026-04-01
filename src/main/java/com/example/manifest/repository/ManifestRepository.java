package com.example.manifest.repository;

import com.example.manifest.entity.Manifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ManifestRepository extends JpaRepository<Manifest, Integer> {

    Optional<Manifest> findByGuid(String guid);

    boolean existsByGuid(String guid);

    List<Manifest> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    List<Manifest> findByOrderDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT m FROM Manifest m WHERE m.customerName LIKE %:kw% OR m.customerPhone LIKE %:kw% ORDER BY m.createdAt DESC")
    List<Manifest> searchByCustomer(@Param("kw") String keyword);
}
