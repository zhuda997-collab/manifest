package com.example.manifest.service;

import com.example.manifest.entity.Customer;
import com.example.manifest.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Optional<Customer> findById(Integer id) {
        return customerRepository.findById(id);
    }

    @Transactional
    public Customer save(Customer customer) {
        if (customer.getGuid() == null || customer.getGuid().isBlank()) {
            customer.setGuid(UUID.randomUUID().toString());
        }
        return customerRepository.save(customer);
    }

    @Transactional
    public void deleteById(Integer id) {
        customerRepository.deleteById(id);
    }
}
