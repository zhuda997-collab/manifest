#!/bin/bash
# =============================================
# Mac / Linux 构建脚本
# 用法: ./build.sh
# =============================================

set -e

echo "========================================"
echo "  货单管理系统 - macOS / Linux 构建"
echo "========================================"

# 1. 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ 错误：未找到 Maven，请先安装"
    echo "  macOS: brew install maven"
    exit 1
fi

# 2. 清理并打包
echo ""
echo ">>> 正在打包..."
mvn clean package -DskipTests

# 3. 检查产物
JAR_FILE=$(find target -name "manifest-*.jar" ! -name "*-sources.jar" 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ 打包失败，未找到 JAR 文件"
    exit 1
fi

echo ""
echo "✅ 构建成功！"
echo "    JAR 文件：$JAR_FILE"
echo ""
echo ">>> 运行方式："
echo "    java -jar $JAR_FILE"
echo ""
echo ">>> 访问地址：http://localhost:8080"
echo "========================================"
