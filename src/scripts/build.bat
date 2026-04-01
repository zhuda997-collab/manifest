@echo off
rem =============================================
rem  货单管理系统 - Windows 构建脚本
rem  用法: build.bat
rem =============================================
setlocal enabledelayedexpansion

echo ========================================
echo   货单管理系统 - Windows 构建
echo ========================================

rem 1. 检查 Maven
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Maven，请先安装并配置 PATH
    echo   下载地址: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

rem 2. 清理并打包
echo.
echo [1/2] 正在打包...
call mvn clean package -DskipTests

rem 3. 定位 JAR 文件
set "JAR_FILE="
for /r "target" %%f in ("manifest-*.jar") do (
    if not "%%~nxf"=="%%f" (
        set "JAR_FILE=%%f"
    )
)

if not defined JAR_FILE (
    echo [错误] 打包失败，未找到 JAR 文件
    pause
    exit /b 1
)

echo.
echo [2/2] 构建成功！
echo     JAR 文件: %JAR_FILE%
echo.
echo [运行方式]
echo     java -jar "%JAR_FILE%"
echo.
echo [访问地址] http://localhost:8080
echo ========================================
pause
