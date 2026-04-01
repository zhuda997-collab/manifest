package com.example.manifest.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 产品实体
 * 单价以"分"为单位存储（int），避免浮点精度问题
 * 例如：unitPrice = 100 表示 1.00 元
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 全局唯一标识符（自动生成） */
    @Column(name = "guid", unique = true, length = 36, nullable = false)
    private String guid;

    /** 产品名 */
    @NotBlank(message = "产品名不能为空")
    @Column(name = "product_name", length = 200, nullable = false)
    private String productName;

    /** 产品号（字符串，保留前导零，如 001） */
    @NotBlank(message = "产品号不能为空")
    @Column(name = "product_no", nullable = false, length = 20)
    private String productNo;

    /** 子型号名 */
    @Column(name = "submodel_name", length = 200)
    private String submodelName;

    /** 子型号（字符串，保留前导零，如 001001） */
    @Column(name = "submodel_no", length = 20)
    private String submodelNo;

    /** 单价（分人民币） */
    @NotNull(message = "单价不能为空")
    @PositiveOrZero(message = "单价不能为负数")
    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
