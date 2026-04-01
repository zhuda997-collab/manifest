package com.example.manifest.controller;

import com.example.manifest.entity.Product;
import com.example.manifest.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    /** 下拉框用：返回所有产品（含子型号） */
    @GetMapping("/all")
    public ResponseEntity<List<Product>> all() {
        return ResponseEntity.ok(productService.findAll());
    }

    /** GET /api/product — 查询全部产品 */
    @GetMapping
    public ResponseEntity<List<Product>> list() {
        return ResponseEntity.ok(productService.findAll());
    }

    /** GET /api/product/{id} — 根据 ID 查询 */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Integer id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/product — 新增产品 */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody Product product,
                                                       BindingResult bindingResult) {
        Map<String, Object> result = new HashMap<>();

        if (bindingResult.hasErrors()) {
            result.put("success", false);
            result.put("message", bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }

        if (productService.existsByProductNoAndSubmodelNo(product.getProductNo(), product.getSubmodelNo())) {
            result.put("success", false);
            result.put("message", "产品号+子型号组合已存在，请勿重复添加");
            return ResponseEntity.badRequest().body(result);
        }

        Product saved = productService.save(product);
        result.put("success", true);
        result.put("message", "添加成功");
        result.put("data", saved);
        return ResponseEntity.ok(result);
    }

    /** PUT /api/product/{id} — 更新产品 */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Integer id,
                                                        @Valid @RequestBody Product product,
                                                        BindingResult bindingResult) {
        Map<String, Object> result = new HashMap<>();

        if (bindingResult.hasErrors()) {
            result.put("success", false);
            result.put("message", bindingResult.getFieldErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(result);
        }

        if (productService.findById(id).isEmpty()) {
            result.put("success", false);
            result.put("message", "产品不存在");
            return ResponseEntity.notFound().build();
        }

        product.setId(id);
        Product updated = productService.save(product);
        result.put("success", true);
        result.put("message", "更新成功");
        result.put("data", updated);
        return ResponseEntity.ok(result);
    }

    /** DELETE /api/product/{id} — 删除产品 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();

        if (productService.findById(id).isEmpty()) {
            result.put("success", false);
            result.put("message", "产品不存在");
            return ResponseEntity.notFound().build();
        }

        productService.deleteById(id);
        result.put("success", true);
        result.put("message", "删除成功");
        return ResponseEntity.ok(result);
    }
}
