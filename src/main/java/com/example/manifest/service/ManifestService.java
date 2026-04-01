package com.example.manifest.service;

import com.example.manifest.entity.Customer;
import com.example.manifest.entity.Manifest;
import com.example.manifest.entity.ManifestItem;
import com.example.manifest.entity.Product;
import com.example.manifest.repository.CustomerRepository;
import com.example.manifest.repository.ManifestRepository;
import com.example.manifest.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManifestService {

    private final ManifestRepository manifestRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public List<Manifest> findAll() {
        return manifestRepository.findAll();
    }

    public Manifest findById(Integer id) {
        return manifestRepository.findById(id).orElse(null);
    }

    /**
     * 新增货单：
     * 1. 冻结客户快照信息
     * 2. 计算每个明细的小计
     * 3. 计算总价
     * 4. 保存
     */
    @Transactional
    public Manifest save(Manifest manifest) {
        // 自动生成 GUID
        if (manifest.getGuid() == null || manifest.getGuid().isBlank()) {
            manifest.setGuid(UUID.randomUUID().toString());
        }

        // 冻结客户快照信息
        if (manifest.getCustomerId() != null) {
            Customer c = customerRepository.findById(manifest.getCustomerId()).orElse(null);
            if (c != null) {
                manifest.setCustomerName(c.getCustomerName());
                manifest.setCustomerPhone(c.getPhone());
                manifest.setCustomerAddress(c.getAddress());
            }
        }

        // 处理明细行：冻结产品快照 + 计算小计 + 关联父实体
        for (ManifestItem item : manifest.getItems()) {
            // 必须选择产品（productId 不能为空）
            if (item.getProductId() == null) {
                throw new RuntimeException("每条明细必须选择产品");
            }
            Product p = productRepository.findById(item.getProductId()).orElse(null);
            if (p == null) {
                throw new RuntimeException("产品不存在: ID=" + item.getProductId());
            }
            // 冻结产品快照信息
            item.setProductName(p.getProductName());
            item.setProductNo(p.getProductNo());
            item.setSubmodelName(p.getSubmodelName());
            item.setSubmodelNo(p.getSubmodelNo());
            // 强制使用产品表中的当前单价（不接受前端传入的值）
            item.setUnitPrice(p.getUnitPrice());
            // 计算小计
            item.calcSubtotal();
            // 手动关联父实体（确保外键不为 null）
            item.setManifest(manifest);
        }

        // 计算总价
        manifest.calcTotalPrice();

        return manifestRepository.save(manifest);
    }

    @Transactional
    public void deleteById(Integer id) {
        manifestRepository.deleteById(id);
    }
}
