# 生成APK安装包指南

## 方法一：使用Android Studio（推荐）

### 1. 安装Android Studio
1. 下载Android Studio：https://developer.android.com/studio
2. 安装并启动Android Studio
3. 配置Android SDK

### 2. 导入项目
1. 打开Android Studio
2. 选择 "Open an existing Android Studio project"
3. 选择 `NFCCardApp` 文件夹
4. 等待Gradle同步完成

### 3. 生成APK
1. 在菜单栏选择 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. 等待编译完成
3. 点击通知中的 "locate" 链接找到APK文件
4. APK文件位置：`app/build/outputs/apk/debug/app-debug.apk`

## 方法二：使用命令行

### 1. 环境准备
确保已安装：
- Java JDK 8 或更高版本
- Android SDK
- 设置环境变量 ANDROID_HOME

### 2. 使用Gradle构建
```bash
# 进入项目目录
cd NFCCardApp

# Windows
gradlew.bat assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

### 3. 查找APK文件
生成的APK文件位于：
`app/build/outputs/apk/debug/app-debug.apk`

## 方法三：在线构建（简单快捷）

### 使用GitHub Actions或其他CI/CD服务
1. 将代码上传到GitHub
2. 配置GitHub Actions自动构建
3. 下载构建好的APK

## 安装APK到手机

### 1. 启用未知来源安装
- Android 8.0+：设置 → 应用和通知 → 特殊应用权限 → 安装未知应用
- Android 8.0以下：设置 → 安全 → 未知来源

### 2. 安装方法
- **USB连接**：将APK复制到手机，使用文件管理器安装
- **ADB安装**：`adb install app-debug.apk`
- **无线传输**：通过QQ、微信等发送APK文件

## 签名APK（发布版本）

### 1. 生成签名密钥
```bash
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 配置签名
在 `app/build.gradle` 中添加：
```gradle
android {
    signingConfigs {
        release {
            storeFile file('my-release-key.keystore')
            storePassword 'your-store-password'
            keyAlias 'my-key-alias'
            keyPassword 'your-key-password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 3. 构建发布版本
```bash
gradlew assembleRelease
```

## 故障排除

### 常见问题
1. **Gradle同步失败**：检查网络连接，尝试使用VPN
2. **SDK版本问题**：更新Android SDK到最新版本
3. **内存不足**：增加Gradle内存设置
4. **权限问题**：确保有写入权限

### 解决方案
- 清理项目：`gradlew clean`
- 重新构建：`gradlew build`
- 更新Gradle：修改 `gradle/wrapper/gradle-wrapper.properties`

## 快速构建脚本

我已经为你准备了自动化构建脚本，请查看 `build_apk.bat` 文件。
