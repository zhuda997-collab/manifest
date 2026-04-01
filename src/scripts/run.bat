@echo off
rem =============================================
rem  货单管理系统 - Windows 一键启动脚本
rem  用法: 双击 run.bat
rem =============================================
setlocal enabledelayedexpansion

echo ========================================
echo   货单管理系统 - 启动中
echo ========================================
echo.

rem 查找 JAR 文件
set "JAR_FILE="
for /r "%~dp0" %%f in ("manifest-*.jar") do (
    if not "%%~nxf"=="%%f" (
        set "JAR_FILE=%%f"
    )
)

if not defined JAR_FILE (
    echo [错误] 未找到 JAR 文件
    echo   请先运行 build.bat 进行打包
    pause
    exit /b 1
)

echo [信息] JAR 文件: %JAR_FILE%
echo [信息] 数据库: MySQL 需已运行 (localhost:3306)
echo [信息] 数据库用户: root  密码: root
echo [信息] 数据库名: manifest_db
echo.
echo 按 Ctrl+C 可停止服务
echo.
echo 启动中，请稍候...
echo ========================================

java -jar "%JAR_FILE%"

pause
