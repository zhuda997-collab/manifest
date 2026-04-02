package com.example.manifest.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * 客户实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer")
@Where(clause = "is_del = false")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 全局唯一标识符（自动生成） */
    @Column(name = "guid", unique = true, length = 36, nullable = false)
    private String guid;

    /** 客户名 */
    @NotBlank(message = "客户名不能为空")
    @Column(name = "customer_name", length = 200, nullable = false)
    private String customerName;

    /** 客户手机号 */
    @Column(name = "phone", length = 20)
    private String phone;

    /** 客户地址 */
    @Column(name = "address", length = 500)
    private String address;

    /** 创建时间 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 软删除标记（true=已删除，默认false） */
    @Column(name = "is_del")
    private Boolean isDel = false;

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
