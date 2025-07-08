@echo off
echo ========================================
echo NFC卡读写工具 - APK构建脚本
echo ========================================
echo.

:: 检查Java环境
echo [1/6] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到Java环境
    echo 请安装Java JDK 8或更高版本
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)
echo ✅ Java环境检查通过

:: 检查Android SDK
echo.
echo [2/6] 检查Android SDK...
if not defined ANDROID_HOME (
    echo ⚠️  警告: 未设置ANDROID_HOME环境变量
    echo 尝试使用默认路径...
    set ANDROID_HOME=%USERPROFILE%\AppData\Local\Android\Sdk
)

if not exist "%ANDROID_HOME%" (
    echo ❌ 错误: 未找到Android SDK
    echo 请安装Android Studio或单独安装Android SDK
    echo 并设置ANDROID_HOME环境变量
    pause
    exit /b 1
)
echo ✅ Android SDK检查通过: %ANDROID_HOME%

:: 检查项目文件
echo.
echo [3/6] 检查项目文件...
if not exist "gradlew.bat" (
    echo ❌ 错误: 未找到gradlew.bat文件
    echo 请确保在NFCCardApp项目根目录下运行此脚本
    pause
    exit /b 1
)
echo ✅ 项目文件检查通过

:: 清理项目
echo.
echo [4/6] 清理项目...
call gradlew.bat clean
if %errorlevel% neq 0 (
    echo ❌ 项目清理失败
    pause
    exit /b 1
)
echo ✅ 项目清理完成

:: 构建APK
echo.
echo [5/6] 构建APK文件...
echo 这可能需要几分钟时间，请耐心等待...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo ❌ APK构建失败
    echo 请检查错误信息并解决问题
    pause
    exit /b 1
)
echo ✅ APK构建完成

:: 查找并复制APK文件
echo.
echo [6/6] 处理APK文件...
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if exist "%APK_PATH%" (
    echo ✅ APK文件生成成功: %APK_PATH%
    
    :: 复制APK到当前目录
    copy "%APK_PATH%" "NFCCardApp.apk" >nul
    if exist "NFCCardApp.apk" (
        echo ✅ APK文件已复制到: NFCCardApp.apk
    )
    
    :: 显示文件信息
    echo.
    echo 📱 APK文件信息:
    for %%A in ("%APK_PATH%") do (
        echo    文件大小: %%~zA 字节
        echo    修改时间: %%~tA
    )
    
    echo.
    echo 🎉 构建完成！
    echo.
    echo 📋 安装说明:
    echo 1. 将 NFCCardApp.apk 文件传输到Android设备
    echo 2. 在设备上启用"未知来源"安装权限
    echo 3. 点击APK文件进行安装
    echo.
    echo 💡 提示:
    echo - 可以通过USB、蓝牙、邮件等方式传输APK
    echo - 首次安装需要允许安装未知来源应用
    echo - 确保设备支持NFC功能
    
) else (
    echo ❌ 错误: 未找到生成的APK文件
    echo 请检查构建过程是否有错误
)

echo.
echo ========================================
pause
