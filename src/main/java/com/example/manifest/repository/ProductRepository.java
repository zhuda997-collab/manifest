package com.example.manifest.repository;

import com.example.manifest.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    /** 按 GUID 查找 */
    Optional<Product> findByGuid(String guid);

    /** 检查 GUID 是否已存在 */
    boolean existsByGuid(String guid);

    /** 按产品号和子型号查找（联合唯一） */
    Optional<Product> findByProductNoAndSubmodelNo(String productNo, String submodelNo);

    /** 检查产品号+子型号组合是否已存在 */
    boolean existsByProductNoAndSubmodelNo(String productNo, String submodelNo);
}
