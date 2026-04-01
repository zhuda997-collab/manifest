package com.example.manifest.service;

import com.example.manifest.entity.Manifest;
import com.example.manifest.repository.ManifestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManifestService {

    private final ManifestRepository manifestRepository;

    /** 查询全部 */
    public List<Manifest> findAll() {
        return manifestRepository.findAll();
    }

    /** 根据 ID 查询 */
    public Optional<Manifest> findById(Long id) {
        return manifestRepository.findById(id);
    }

    /** 根据货品编号查询 */
    public Optional<Manifest> findByGoodsNo(String goodsNo) {
        return manifestRepository.findByGoodsNo(goodsNo);
    }

    /** 新增或更新 */
    @Transactional
    public Manifest save(Manifest manifest) {
        return manifestRepository.save(manifest);
    }

    /** 删除 */
    @Transactional
    public void deleteById(Long id) {
        manifestRepository.deleteById(id);
    }

    /** 检查货品编号是否存在 */
    public boolean existsByGoodsNo(String goodsNo) {
        return manifestRepository.existsByGoodsNo(goodsNo);
    }
}
