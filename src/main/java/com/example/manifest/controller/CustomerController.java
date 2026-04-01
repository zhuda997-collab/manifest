package com.example.manifest.controller;

import com.example.manifest.entity.Customer;
import com.example.manifest.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerService customerService;

    /** 下拉框用：返回所有客户 */
    @GetMapping("/all")
    public ResponseEntity<List<Customer>> all() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping
    public ResponseEntity<List<Customer>> list() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getById(@PathVariable Integer id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody Customer customer,
                                                       BindingResult bindingResult) {
        Map<String, Object> result = new HashMap<>();
        if (bindingResult.hasErrors()) {
            result.put("success", false);
            result.put("message", bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        Customer saved = customerService.save(customer);
        result.put("success", true);
        result.put("message", "添加成功");
        result.put("data", saved);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id,
                                                      @Valid @RequestBody Customer customer,
                                                      BindingResult bindingResult) {
        Map<String, Object> result = new HashMap<>();
        if (bindingResult.hasErrors()) {
            result.put("success", false);
            result.put("message", bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }
        if (customerService.findById(id).isEmpty()) {
            result.put("success", false);
            result.put("message", "客户不存在");
            return ResponseEntity.notFound().build();
        }
        customer.setId(id);
        Customer updated = customerService.save(customer);
        result.put("success", true);
        result.put("message", "更新成功");
        result.put("data", updated);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        if (customerService.findById(id).isEmpty()) {
            result.put("success", false);
            result.put("message", "客户不存在");
            return ResponseEntity.notFound().build();
        }
        customerService.deleteById(id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
