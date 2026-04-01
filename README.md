# 货单管理系统 (Manifest Management System)

Java 17 + Spring Boot 3.2 + MySQL 8 + 原生 HTML/JS 实现的货单管理 Web 应用。

---

## 技术选型

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 (LTS) | 开发/运行环境 |
| Spring Boot | 3.2.5 | 后端框架 |
| MySQL | 8.0.x | 数据库 |
| 前端 | 原生 HTML/JS | 无框架依赖 |

---

## 功能一览

- ✅ 货单增删改查（CRUD）
- ✅ 货品编号查重
- ✅ 关键字搜索过滤
- ✅ 统计总数量、总金额
- ✅ 分页展示
- ✅ 响应式表格
- ✅ 表单校验

---

## 快速开始

### 1. 环境要求

- **JDK 17+**（[下载地址](https://adoptium.net/)）
- **Maven 3.8+**（[下载地址](https://maven.apache.org/download.cgi)）
- **MySQL 8.0+**（[下载地址](https://dev.mysql.com/downloads/mysql/)）

### 2. 创建数据库

```sql
-- MySQL 命令行执行：
mysql -u root -p < src/main/resources/db/init.sql
```

或手动执行 `src/main/resources/db/init.sql` 中的 SQL。

### 3. 修改数据库连接（可选）

默认配置：
```
用户名: root
密码:   root
端口:   3306
数据库: manifest_db
```

如需修改，编辑 `src/main/resources/application.yml` 中的 datasource 配置。

### 4. 开发模式运行（Mac/Linux）

```bash
cd /path/to/manifest
./src/scripts/build.sh    # 首次构建
./src/scripts/run.sh      # 启动服务
```

### 5. Windows 构建与运行

```bat
# 先双击 build.bat 打包（只需一次）
src\scripts\build.bat

# 以后每次运行：
src\scripts\run.bat
```

### 6. 访问应用

打开浏览器访问：**http://localhost:8080**

---

## 项目结构

```
manifest/
├── pom.xml                           # Maven 配置
├── src/
│   ├── main/
│   │   ├── java/com/example/manifest/
│   │   │   ├── ManifestApplication.java   # 启动入口
│   │   │   ├── entity/
│   │   │   │   └── Manifest.java          # 货单实体
│   │   │   ├── repository/
│   │   │   │   └── ManifestRepository.java
│   │   │   ├── service/
│   │   │   │   └── ManifestService.java
│   │   │   ├── controller/
│   │   │   │   └── ManifestController.java  # REST API
│   │   │   └── config/
│   │   │       └── WebConfig.java           # 跨域配置
│   │   └── resources/
│   │       ├── application.yml              # Spring Boot 配置
│   │       ├── static/                      # 前端静态文件
│   │       │   ├── index.html
│   │       │   ├── css/style.css
│   │       │   └── js/app.js
│   │       └── db/
│   │           └── init.sql                  # 数据库初始化
│   └── scripts/
│       ├── build.sh / build.bat             # 构建脚本
│       └── run.sh / run.bat                 # 启动脚本
```

---

## REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/manifest` | 查询全部货单 |
| GET | `/api/manifest/{id}` | 按 ID 查询 |
| POST | `/api/manifest` | 新增货单 |
| PUT | `/api/manifest/{id}` | 更新货单 |
| DELETE | `/api/manifest/{id}` | 删除货单 |

请求体示例（POST/PUT）：
```json
{
  "goodsNo": "A001",
  "goodsName": "不锈钢螺丝 M8×30",
  "specification": "M8×30mm",
  "unit": "个",
  "quantity": 100,
  "unitPrice": 2.50,
  "remark": "供应商：浙江某某公司"
}
```

---

## 打包为 Windows 一键安装（进阶）

> 完整打包方案需要 jpackage + Inno Setup，较为复杂，如有需要可告诉猛猛帮你配置。

当前阶段打包为可运行 JAR，用户需自行安装 JDK 和 MySQL。

---

## 数据库字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| goods_no | VARCHAR(50) | 货品编号，唯一 |
| goods_name | VARCHAR(200) | 货品名称 |
| specification | VARCHAR(200) | 规格型号 |
| unit | VARCHAR(20) | 单位 |
| quantity | INT | 数量 |
| unit_price | DECIMAL(12,2) | 单价 |
| remark | VARCHAR(500) | 备注 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
