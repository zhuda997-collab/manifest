package com.example.manifest.repository;

import com.example.manifest.entity.Manifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManifestRepository extends JpaRepository<Manifest, Long> {

    /** 按货品编号查找 */
    Optional<Manifest> findByGoodsNo(String goodsNo);

    /** 检查货品编号是否已存在 */
    boolean existsByGoodsNo(String goodsNo);
}
