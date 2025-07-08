# Gradle Wrapper 下载说明

## 重要提示
为了完成APK构建，你需要下载Gradle Wrapper的jar文件。

## 下载步骤

### 方法1: 自动下载（推荐）
运行以下命令，Gradle会自动下载所需文件：
```bash
# Windows
gradlew.bat --version

# Linux/Mac  
./gradlew --version
```

### 方法2: 手动下载
如果自动下载失败，请手动下载：

1. **创建目录结构**
   ```
   NFCCardApp/
   └── gradle/
       └── wrapper/
           ├── gradle-wrapper.properties
           └── gradle-wrapper.jar  ← 需要下载这个文件
   ```

2. **下载gradle-wrapper.jar**
   - 下载地址: https://github.com/gradle/gradle/raw/v8.4.0/gradle/wrapper/gradle-wrapper.jar
   - 将文件保存到: `gradle/wrapper/gradle-wrapper.jar`

3. **验证文件**
   - 文件大小约: 60KB
   - 确保文件路径正确

### 方法3: 使用Android Studio
1. 用Android Studio打开项目
2. Android Studio会自动下载所需的Gradle文件
3. 等待同步完成

## 验证安装
运行以下命令验证Gradle Wrapper是否正常工作：
```bash
gradlew.bat --version
```

如果看到Gradle版本信息，说明安装成功。

## 如果仍然有问题
1. 确保网络连接正常
2. 可能需要使用VPN（在中国大陆）
3. 检查防火墙设置
4. 尝试使用Android Studio自动处理

## 完整的构建流程
1. 下载gradle-wrapper.jar文件
2. 运行 `gradlew.bat assembleDebug`
3. 等待构建完成
4. 在 `app/build/outputs/apk/debug/` 目录找到APK文件
