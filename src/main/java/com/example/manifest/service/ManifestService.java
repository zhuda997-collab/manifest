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
        return manifestRepository.findAllWithCustomerAndItems();
    }

    public Manifest findById(Integer id) {
        return manifestRepository.findByIdWithCustomerAndItems(id).orElse(null);
    }

    /**
     * 新增/更新货单：
     * 1. 动态关联客户（通过 customer_id 查询最新客户数据）
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

        // 动态关联客户（永远取最新数据，不快照）
        if (manifest.getCustomerId() != null) {
            Customer c = customerRepository.findById(manifest.getCustomerId()).orElse(null);
            if (c == null) {
                throw new RuntimeException("客户不存在: ID=" + manifest.getCustomerId());
            }
            manifest.setCustomer(c);
        }

        // 处理明细行：冻结产品快照 + 计算小计 + 关联父实体
        for (ManifestItem item : manifest.getItems()) {
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
            // 强制使用产品表中的当前单价
            item.setUnitPrice(p.getUnitPrice());
            // 计算小计
            item.calcSubtotal();
            // 手动关联父实体
            item.setManifest(manifest);
        }

        // 计算总价
        manifest.calcTotalPrice();

        return manifestRepository.save(manifest);
    }

    /**
     * 真删除货单（orphanRemoval 级联删除所有明细行）
     */
    @Transactional
    public void deleteById(Integer id) {
        manifestRepository.deleteById(id);
    }

    /**
     * 按客户ID查询货单
     */
    public List<Manifest> findByCustomerId(Integer customerId) {
        return manifestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * 按关键字搜索货单（客户名或手机号模糊匹配）
     */
    public List<Manifest> searchByKeyword(String keyword) {
        return manifestRepository.searchByCustomer(keyword);
    }
}
