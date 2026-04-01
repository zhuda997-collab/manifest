package com.example.manifest.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 货单实体（订单头）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "manifest")
public class Manifest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 全局唯一标识符 */
    @Column(name = "guid", unique = true, length = 36, nullable = false)
    private String guid;

    /** 客户ID（外键） */
    @Column(name = "customer_id")
    private Integer customerId;

    /** 下单时客户名（快照） */
    @Column(name = "customer_name", length = 200)
    private String customerName;

    /** 下单时手机号（快照） */
    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    /** 下单时地址（快照） */
    @Column(name = "customer_address", length = 500)
    private String customerAddress;

    /** 总价（分） */
    @Column(name = "total_price", nullable = false)
    private Integer totalPrice = 0;

    /** 备注 */
    @Column(name = "notes", length = 1000)
    private String notes;

    /** 货单日期 */
    @Column(name = "order_date")
    private LocalDate orderDate;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 货单明细（一对多） */
    @OneToMany(mappedBy = "manifest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<ManifestItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderDate == null) {
            orderDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 添加明细行，自动关联到当前货单
     */
    public void addItem(ManifestItem item) {
        items.add(item);
        item.setManifest(this);
    }

    /**
     * 清除所有明细行
     */
    public void clearItems() {
        for (ManifestItem item : items) {
            item.setManifest(null);
        }
        items.clear();
    }

    /**
     * 计算总价 = 所有明细小计之和
     */
    public void calcTotalPrice() {
        this.totalPrice = items.stream()
                .mapToInt(item -> {
                    item.calcSubtotal();
                    return item.getSubtotal();
                })
                .sum();
    }
}
