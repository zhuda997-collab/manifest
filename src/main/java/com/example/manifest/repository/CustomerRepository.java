package com.example.manifest.repository;

import com.example.manifest.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByGuid(String guid);

    boolean existsByGuid(String guid);

    Optional<Customer> findByPhone(String phone);
}
