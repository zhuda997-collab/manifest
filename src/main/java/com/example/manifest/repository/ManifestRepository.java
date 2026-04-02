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

    @Query("SELECT m FROM Manifest m LEFT JOIN FETCH m.customer LEFT JOIN FETCH m.items WHERE m.id = :id")
    Optional<Manifest> findByIdWithCustomerAndItems(@Param("id") Integer id);

    @Query("SELECT DISTINCT m FROM Manifest m LEFT JOIN FETCH m.customer LEFT JOIN FETCH m.items ORDER BY m.createdAt DESC")
    List<Manifest> findAllWithCustomerAndItems();

    @Query("SELECT m FROM Manifest m LEFT JOIN FETCH m.customer WHERE m.customer.id = :customerId ORDER BY m.createdAt DESC")
    List<Manifest> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") Integer customerId);

    List<Manifest> findByOrderDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT DISTINCT m FROM Manifest m LEFT JOIN FETCH m.customer LEFT JOIN FETCH m.items WHERE m.customer.customerName LIKE %:kw% OR m.customer.phone LIKE %:kw% ORDER BY m.createdAt DESC")
    List<Manifest> searchByCustomer(@Param("kw") String keyword);

    boolean existsByGuid(String guid);
}
