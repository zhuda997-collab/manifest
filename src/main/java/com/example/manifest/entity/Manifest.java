package com.example.manifest.entity;

import jakarta.persistence.*;
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

    /** 客户ID（外键，接收前端传入） */
    @Column(name = "customer_id")
    private Integer customerId;

    /** 客户（动态关联，永远取最新数据） */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    /** 总价（分） */
    @Column(name = "total_price", nullable = false)
    private Integer totalPrice = 0;

    /** 备注 */
    @Column(name = "notes", length = 1000)
    private String notes;

    /** 付款方式：现金/微信/支付宝/银行卡/信用卡/欠款 */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod = "现金";

    /** 出货方式：物流/快递/捎货 */
    @Column(name = "shipping_method", length = 20)
    private String shippingMethod = "物流";

    /** 付款状态：已付/未付 */
    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "未付";

    /** 运费（分） */
    @Column(name = "freight")
    private Integer freight = 0;

    /** 优惠合计（分） */
    @Column(name = "discount")
    private Integer discount = 0;

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
