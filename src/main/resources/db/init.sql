-- 货单管理系统数据库初始化脚本
-- 手动执行方式：mysql -u root -p < init.sql

CREATE DATABASE IF NOT EXISTS manifest_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE manifest_db;

-- 建表语句（如果 JPA ddl-auto=update 有问题，手动执行这个）
CREATE TABLE IF NOT EXISTS manifest (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    goods_no      VARCHAR(50)  NOT NULL UNIQUE COMMENT '货品编号',
    goods_name    VARCHAR(200) NOT NULL COMMENT '货品名称',
    specification VARCHAR(200)          DEFAULT NULL COMMENT '规格型号',
    unit          VARCHAR(20)  NOT NULL COMMENT '单位',
    quantity      INT          NOT NULL DEFAULT 0 COMMENT '数量',
    unit_price    DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
    remark        VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_goods_no (goods_no),
    INDEX idx_goods_name (goods_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='货单表';

-- 产品表（Product）
CREATE TABLE IF NOT EXISTS product (
    id            INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    guid          VARCHAR(36)  NOT NULL COMMENT '全局唯一标识符',
    product_name  VARCHAR(200) NOT NULL COMMENT '产品名',
    product_no    INT          NOT NULL COMMENT '产品号',
    submodel_name VARCHAR(200)          DEFAULT NULL COMMENT '子型号名',
    submodel_no   INT                   DEFAULT NULL COMMENT '子型号int',
    unit_price    INT          NOT NULL DEFAULT 0 COMMENT '单价(分人民币)',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_guid (guid),
    UNIQUE KEY uk_product_submodel (product_no, submodel_no),
    INDEX idx_product_name (product_name),
    INDEX idx_product_no (product_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品表';

-- 客户表（Customer）
CREATE TABLE IF NOT EXISTS customer (
    id            INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    guid          VARCHAR(36)  NOT NULL COMMENT '全局唯一标识符',
    customer_name VARCHAR(200) NOT NULL COMMENT '客户名',
    phone         VARCHAR(20)           DEFAULT NULL COMMENT '客户手机号',
    address       VARCHAR(500)          DEFAULT NULL COMMENT '客户地址',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_guid (guid),
    INDEX idx_customer_name (customer_name),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户表';
