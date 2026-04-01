package com.example.manifest.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 货单实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "manifest")
public class Manifest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 货品编号 */
    @NotBlank(message = "货品编号不能为空")
    @Column(name = "goods_no", unique = true, length = 50)
    private String goodsNo;

    /** 货品名称 */
    @NotBlank(message = "货品名称不能为空")
    @Column(name = "goods_name", length = 200)
    private String goodsName;

    /** 规格型号 */
    @Column(name = "specification", length = 200)
    private String specification;

    /** 单位 */
    @NotBlank(message = "单位不能为空")
    @Column(name = "unit", length = 20)
    private String unit;

    /** 数量 */
    @NotNull(message = "数量不能为空")
    @PositiveOrZero(message = "数量不能为负数")
    @Column(name = "quantity")
    private Integer quantity;

    /** 单价 */
    @NotNull(message = "单价不能为空")
    @PositiveOrZero(message = "单价不能为负数")
    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** 备注 */
    @Column(name = "remark", length = 500)
    private String remark;

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
