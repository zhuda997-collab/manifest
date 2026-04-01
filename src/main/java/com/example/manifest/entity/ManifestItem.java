package com.example.manifest.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * 货单明细实体（每行细项）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "manifest_item")
public class ManifestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 所属货单（级联删除） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manifest_id", nullable = false)
    @JsonIgnore
    private Manifest manifest;

    /** 产品ID */
    @Column(name = "product_id")
    private Integer productId;

    /** 产品名（快照，下单时冻结） */
    @Column(name = "product_name", length = 200)
    private String productName;

    /** 产品号（快照，字符串） */
    @Column(name = "product_no", length = 20)
    private String productNo;

    /** 子型号名（快照） */
    @Column(name = "submodel_name", length = 200)
    private String submodelName;

    /** 子型号（快照，字符串，保留前导零） */
    @Column(name = "submodel_no", length = 20)
    private String submodelNo;

    /** 件数 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    /** 单价-分（快照） */
    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice = 0;

    /** 小计-分 */
    @Column(name = "subtotal", nullable = false)
    private Integer subtotal = 0;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (subtotal == null || subtotal == 0) {
            subtotal = (quantity != null ? quantity : 1) * (unitPrice != null ? unitPrice : 0);
        }
    }

    /**
     * 计算小计
     */
    public void calcSubtotal() {
        this.subtotal = (this.quantity != null ? this.quantity : 0)
                      * (this.unitPrice != null ? this.unitPrice : 0);
    }
}
