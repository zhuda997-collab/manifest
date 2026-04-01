package com.example.manifest.repository;

import com.example.manifest.entity.ManifestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManifestItemRepository extends JpaRepository<ManifestItem, Integer> {

    List<ManifestItem> findByManifestIdOrderByIdAsc(Integer manifestId);

    void deleteByManifestId(Integer manifestId);
}
