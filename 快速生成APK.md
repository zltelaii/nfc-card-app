# 🚀 快速生成APK安装包

## 🎯 最简单的方法（推荐）

### 方法1: 使用在线构建服务

#### GitHub Actions（免费，自动化）
1. **上传代码到GitHub**
   ```bash
   # 创建GitHub仓库，然后上传代码
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/你的用户名/nfc-card-app.git
   git push -u origin main
   ```

2. **自动构建**
   - 代码上传后，GitHub Actions会自动构建APK
   - 在仓库的"Actions"标签页查看构建进度
   - 构建完成后在"Artifacts"中下载APK

3. **下载APK**
   - 进入GitHub仓库的"Releases"页面
   - 下载最新的APK文件

#### 其他在线构建平台
- **AppCenter**: https://appcenter.ms/
- **Bitrise**: https://www.bitrise.io/
- **CircleCI**: https://circleci.com/

### 方法2: 使用Android Studio（本地构建）

#### 快速步骤
1. **下载Android Studio**
   - 官网: https://developer.android.com/studio
   - 安装并启动

2. **导入项目**
   - 打开Android Studio
   - 选择"Open an existing project"
   - 选择`NFCCardApp`文件夹

3. **生成APK**
   - 菜单: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 等待构建完成
   - 点击通知中的"locate"找到APK文件

## 🛠️ 命令行构建（适合开发者）

### Windows用户
```batch
# 双击运行构建脚本
build_apk.bat
```

### 手动命令行
```bash
# 1. 确保Java环境
java -version

# 2. 进入项目目录
cd NFCCardApp

# 3. 构建APK
gradlew.bat assembleDebug  # Windows
./gradlew assembleDebug    # Linux/Mac

# 4. 查找APK文件
# 位置: app/build/outputs/apk/debug/app-debug.apk
```

## 📱 安装APK到手机

### 准备工作
1. **启用开发者选项**
   - 设置 → 关于手机 → 连续点击"版本号"7次

2. **允许安装未知来源应用**
   - Android 8.0+: 设置 → 应用和通知 → 特殊应用权限 → 安装未知应用
   - Android 8.0以下: 设置 → 安全 → 未知来源

### 安装方法

#### 方法A: 文件传输
1. 将APK文件复制到手机
2. 使用文件管理器找到APK文件
3. 点击安装

#### 方法B: ADB安装（需要USB调试）
```bash
# 启用USB调试后
adb install NFCCardApp.apk
```

#### 方法C: 无线传输
- 通过QQ、微信、邮件等发送APK文件
- 在手机上下载并安装

## 🔧 环境要求

### 最低要求
- **Java**: JDK 8或更高版本
- **Android SDK**: API 21+
- **内存**: 至少4GB RAM
- **存储**: 至少10GB可用空间

### 推荐配置
- **Java**: JDK 17
- **Android Studio**: 最新版本
- **内存**: 8GB+ RAM
- **存储**: 20GB+ 可用空间

## 🚨 常见问题解决

### Q1: Java环境问题
```bash
# 检查Java版本
java -version

# 如果没有Java，下载安装：
# https://www.oracle.com/java/technologies/downloads/
```

### Q2: Android SDK问题
```bash
# 设置环境变量（Windows）
set ANDROID_HOME=C:\Users\你的用户名\AppData\Local\Android\Sdk

# 或者安装Android Studio会自动配置
```

### Q3: 构建失败
```bash
# 清理项目
gradlew clean

# 重新构建
gradlew assembleDebug

# 检查网络连接（可能需要VPN）
```

### Q4: APK安装失败
- 确保启用了"未知来源"安装
- 检查Android版本兼容性（需要Android 5.0+）
- 确保有足够的存储空间

## 📦 APK文件说明

### 文件信息
- **文件名**: `app-debug.apk` 或 `NFCCardApp.apk`
- **大小**: 约5-10MB
- **最低Android版本**: 5.0 (API 21)
- **目标Android版本**: 14 (API 34)

### 权限说明
- **NFC权限**: 读写NFC卡
- **网络权限**: 无（离线应用）
- **存储权限**: 无

## 🎉 构建成功后

### 测试APK
1. 安装到支持NFC的Android设备
2. 启用NFC功能
3. 准备一张NFC卡进行测试
4. 打开应用，按照使用说明操作

### 分享APK
- 可以直接分享APK文件给其他用户
- 建议通过安全渠道传输
- 提醒用户注意安全，只安装可信来源的APK

## 📞 技术支持

如果遇到问题：
1. 检查本文档的常见问题部分
2. 查看详细的构建日志
3. 确保网络连接正常
4. 尝试使用VPN（如果在中国大陆）

---

**提示**: 推荐使用GitHub Actions进行在线构建，这是最简单且不需要本地环境配置的方法！
