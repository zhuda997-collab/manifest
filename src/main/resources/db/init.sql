-- 货单管理系统数据库初始化脚本
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS manifest_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE manifest_db;

-- ==========================================
-- 客户表
-- ==========================================
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

-- ==========================================
-- 产品表
-- ==========================================
CREATE TABLE IF NOT EXISTS product (
    id            INT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID',
    guid          VARCHAR(36)  NOT NULL COMMENT '全局唯一标识符',
    product_name  VARCHAR(200) NOT NULL COMMENT '产品名',
    product_no    VARCHAR(20)  NOT NULL COMMENT '产品号(字符串)',
    submodel_name VARCHAR(200)          DEFAULT NULL COMMENT '子型号名',
    submodel_no   VARCHAR(20)           DEFAULT NULL COMMENT '子型号(字符串)',
    unit_price    INT          NOT NULL DEFAULT 0 COMMENT '单价(元/件)',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_guid (guid),
    UNIQUE KEY uk_product_submodel (product_no, submodel_no),
    INDEX idx_product_name (product_name),
    INDEX idx_product_no (product_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜籽产品表';

-- ==========================================
-- 货单表（订单头）
-- ==========================================
CREATE TABLE IF NOT EXISTS manifest (
    id                   INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    guid                 VARCHAR(36)  NOT NULL COMMENT '全局唯一标识符',
    customer_id          INT                   DEFAULT NULL COMMENT '客户ID(外键)',
    customer_name        VARCHAR(200)          DEFAULT NULL COMMENT '下单时客户名(快照)',
    customer_phone       VARCHAR(20)           DEFAULT NULL COMMENT '下单时手机号(快照)',
    customer_address     VARCHAR(500)          DEFAULT NULL COMMENT '下单时地址(快照)',
    total_price          INT          NOT NULL DEFAULT 0 COMMENT '总价(分)',
    notes                VARCHAR(1000)         DEFAULT NULL COMMENT '备注',
    order_date           DATE                 DEFAULT NULL COMMENT '货单日期',
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_guid (guid),
    INDEX idx_customer_id (customer_id),
    INDEX idx_order_date (order_date),
    INDEX idx_customer_name (customer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='货单表(订单头)';

-- ==========================================
-- 货单明细表（每行细项）
-- ==========================================
CREATE TABLE IF NOT EXISTS manifest_item (
    id                INT AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID',
    manifest_id        INT          NOT NULL COMMENT '货单ID(外键)',
    product_id        INT                   DEFAULT NULL COMMENT '产品ID',
    product_name      VARCHAR(200)          DEFAULT NULL COMMENT '产品名(快照)',
    product_no        VARCHAR(20)           DEFAULT NULL COMMENT '产品号(快照)',
    submodel_name     VARCHAR(200)          DEFAULT NULL COMMENT '子型号名(快照)',
    submodel_no       VARCHAR(20)           DEFAULT NULL COMMENT '子型号(快照)',
    quantity          INT          NOT NULL DEFAULT 1 COMMENT '件',
    unit_price        INT          NOT NULL DEFAULT 0 COMMENT '单价-分(快照)',
    subtotal          INT          NOT NULL DEFAULT 0 COMMENT '小计-分',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_manifest_id (manifest_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='货单明细表';

-- 添加外键约束
ALTER TABLE manifest_item
    ADD CONSTRAINT fk_manifest_item_manifest
    FOREIGN KEY (manifest_id) REFERENCES manifest(id)
    ON DELETE CASCADE;

-- ==========================================
-- 示例菜籽产品数据
-- ==========================================
INSERT INTO product (guid, product_name, product_no, submodel_name, submodel_no, unit_price, created_at, updated_at) VALUES
(UUID(), '双低油菜籽', '001', '一级', '001', 8500, NOW(), NOW()),
(UUID(), '双低油菜籽', '001', '二级', '002', 7800, NOW(), NOW()),
(UUID(), '双低油菜籽', '001', '三级', '003', 7200, NOW(), NOW()),
(UUID(), '黄籽油菜籽', '002', '精品', '001', 9200, NOW(), NOW()),
(UUID(), '黄籽油菜籽', '002', '普通', '002', 8500, NOW(), NOW()),
(UUID(), '芥菜籽',     '003', '早熟', '001', 6800, NOW(), NOW()),
(UUID(), '芥菜籽',     '003', '晚熟', '002', 7200, NOW(), NOW()),
(UUID(), '白菜籽',     '004', '春季', '001', 5500, NOW(), NOW()),
(UUID(), '白菜籽',     '004', '秋季', '002', 5800, NOW(), NOW()),
(UUID(), '黑籽菜籽',   '005', '精选', '001', 9800, NOW(), NOW());
