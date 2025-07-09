# 📱 NFC卡读写工具 - APK生成完整指南

## 🎯 项目概述
这是一个Android NFC卡读写应用，支持：
- ✅ NFC卡检测和读取
- ✅ 密码保护功能
- ✅ 数据加密存储
- ✅ 友好的用户界面
- ✅ 支持英文和数字写入

## 🚀 三种生成APK的方法

### 🥇 方法一：在线构建（最简单，推荐）

#### 使用GitHub Actions
1. **上传代码到GitHub**
   - 创建GitHub账号（如果没有）
   - 创建新仓库
   - 上传整个`NFCCardApp`文件夹

2. **自动构建**
   - GitHub会自动检测到`.github/workflows/build-apk.yml`
   - 自动开始构建APK
   - 大约5-10分钟完成

3. **下载APK**
   - 在仓库的"Actions"页面查看构建状态
   - 构建完成后，在"Artifacts"中下载APK
   - 或者在"Releases"页面下载

**优点**: 无需本地环境，完全自动化，免费
**缺点**: 需要GitHub账号，需要上传代码

### 🥈 方法二：Android Studio（最稳定）

#### 安装和配置
1. **下载Android Studio**
   - 官网: https://developer.android.com/studio
   - 选择适合你系统的版本下载
   - 安装时选择标准安装

2. **导入项目**
   - 启动Android Studio
   - 选择"Open an existing Android Studio project"
   - 浏览并选择`NFCCardApp`文件夹
   - 等待Gradle同步（首次可能需要10-20分钟）

3. **生成APK**
   - 菜单栏: `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 等待构建完成（通常2-5分钟）
   - 点击通知中的"locate"链接找到APK文件

**优点**: 最稳定，功能完整，适合开发
**缺点**: 需要下载大文件（约1GB），占用空间大

### 🥉 方法三：命令行构建（适合开发者）

#### 环境准备
1. **安装Java JDK**
   ```bash
   # 检查是否已安装
   java -version
   
   # 如果没有，下载安装JDK 8或更高版本
   # https://www.oracle.com/java/technologies/downloads/
   ```

2. **下载Gradle Wrapper**
   - 运行项目中的`gradlew.bat`会自动下载
   - 或者参考`下载gradle-wrapper.md`手动下载

#### 构建APK
```bash
# Windows用户 - 双击运行
build_apk.bat

# 或者手动执行
cd NFCCardApp
gradlew.bat clean
gradlew.bat assembleDebug
```

**优点**: 快速，占用空间小，可自动化
**缺点**: 需要配置环境，可能遇到网络问题

## 📱 安装APK到手机

### 准备工作
1. **启用开发者选项**
   - 设置 → 关于手机 → 连续点击"版本号"7次

2. **允许安装未知来源**
   - Android 8.0+: 设置 → 应用和通知 → 特殊应用权限 → 安装未知应用
   - Android 8.0以下: 设置 → 安全 → 未知来源

3. **启用NFC功能**
   - 设置 → 连接 → NFC → 开启

### 安装方法
- **USB传输**: 连接电脑，复制APK文件到手机
- **无线传输**: 通过QQ、微信、邮件等发送APK
- **云存储**: 上传到百度网盘、OneDrive等，手机下载

## 🔧 故障排除

### 常见问题及解决方案

#### 1. Java环境问题
```
错误: 'java' 不是内部或外部命令
解决: 下载安装Java JDK并配置环境变量
```

#### 2. 网络连接问题
```
错误: 无法下载Gradle依赖
解决: 
- 检查网络连接
- 使用VPN（中国大陆用户）
- 尝试手机热点网络
```

#### 3. 构建失败
```
错误: Build failed with an exception
解决:
- 运行 gradlew clean 清理项目
- 检查Android SDK是否正确安装
- 确保有足够的磁盘空间（至少5GB）
```

#### 4. APK安装失败
```
错误: 应用未安装
解决:
- 确保启用了"未知来源"安装
- 检查Android版本（需要5.0+）
- 确保有足够存储空间
- 尝试重启手机后安装
```

## 📋 APK文件信息

### 技术规格
- **文件大小**: 约5-8MB
- **最低Android版本**: 5.0 (API 21)
- **目标Android版本**: 14 (API 34)
- **支持架构**: ARM, ARM64, x86, x86_64

### 权限说明
- **NFC权限**: 读写NFC卡片
- **无网络权限**: 完全离线应用
- **无存储权限**: 不访问用户文件

## 🎉 使用说明

### 首次使用
1. 安装APK到支持NFC的Android设备
2. 打开应用，确保NFC功能已启用
3. 准备一张支持NDEF的NFC卡
4. 按照应用内提示操作

### 基本操作
1. **读取数据**: 将NFC卡靠近手机，输入密码（默认123456）
2. **写入数据**: 在文本框输入内容，点击"写入数据"
3. **加密功能**: 勾选"加密数据"选项可加密存储

## 📞 技术支持

### 获取帮助
- 查看项目中的`使用说明.md`
- 查看`README.md`了解技术细节
- 检查常见问题解决方案

### 反馈问题
请提供以下信息：
- Android设备型号和系统版本
- NFC卡类型和品牌
- 具体错误信息或现象
- 操作步骤

---

## 🎯 推荐方案

**新手用户**: 推荐使用GitHub Actions在线构建
**开发者**: 推荐使用Android Studio
**快速测试**: 推荐使用命令行构建

选择最适合你的方法，开始体验NFC卡读写功能吧！ 🚀
