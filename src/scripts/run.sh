#!/bin/bash
# =============================================
#  Mac / Linux 启动脚本
#  用法: ./run.sh
# =============================================

JAR_FILE=$(find . -name "manifest-*.jar" ! -name "*-sources.jar" 2>/dev/null | head -1)

if [ -z "$JAR_FILE" ]; then
    echo "❌ 未找到 JAR 文件，请先运行 ./build.sh"
    exit 1
fi

echo "🚀 启动货单管理系统..."
echo "   JAR: $JAR_FILE"
echo "   访问: http://localhost:8080"
echo "   按 Ctrl+C 停止"
echo ""

java -jar "$JAR_FILE"
