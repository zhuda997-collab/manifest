package com.example.manifest.service;

import com.example.manifest.entity.Product;
import com.example.manifest.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /** 查询全部产品 */
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /** 根据 ID 查询 */
    public Optional<Product> findById(Integer id) {
        return productRepository.findById(id);
    }

    /** 根据 GUID 查询 */
    public Optional<Product> findByGuid(String guid) {
        return productRepository.findByGuid(guid);
    }

    /** 新增或更新（自动生成 GUID） */
    @Transactional
    public Product save(Product product) {
        if (product.getGuid() == null || product.getGuid().isBlank()) {
            product.setGuid(UUID.randomUUID().toString());
        }
        return productRepository.save(product);
    }

    /** 删除 */
    @Transactional
    public void deleteById(Integer id) {
        productRepository.deleteById(id);
    }

    /** 检查产品号+子型号组合是否已存在 */
    public boolean existsByProductNoAndSubmodelNo(String productNo, String submodelNo) {
        return productRepository.existsByProductNoAndSubmodelNo(productNo, submodelNo);
    }
}
