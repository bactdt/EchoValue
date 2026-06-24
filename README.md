# Vault — Android 密码保管库

基于 Android 的本地密码管理应用，数据存储在 SQLite 中，凭据密码使用 AES-256-GCM 加密。

## 项目结构

```
app/src/main/java/cn/ntit/passwordmanager/
├── adapter/          RecyclerView 适配器
├── data/             数据库 + 仓库层
├── model/            数据模型
├── service/          自动填充服务（AutofillService）
├── util/             工具类（加密、密码生成、会话管理等）
├── MainActivity.java 单一宿主 Activity（Fragment 容器）
├── VaultFragment.java       密码列表主页
├── GeneratorFragment.java   密码生成器
├── SecurityFragment.java    安全概览
├── SettingsFragment.java    设置/注销
├── LoginActivity.java       登录
├── RegisterActivity.java    注册
├── UnlockActivity.java      PIN/生物识别解锁
├── AddCredentialActivity.java  添加/编辑凭据
├── PasswordDetailActivity.java 凭据详情
└── VaultApp.java            全局 Application
```

## 如何构建

```bash
# 编译 Debug APK
./gradlew assembleDebug

# 安装到已连接的设备
./gradlew installDebug
```

**不要使用系统安装的 `gradle` 命令**，一律用项目根目录的 `./gradlew`（Gradle Wrapper）。

## 团队协作 — Gradle 版本不一致怎么办？

项目已启用 **Gradle Wrapper**，它自动解决版本不一致问题。

每次运行 `./gradlew` 时，Wrapper 会：

1. 读取 `gradle/wrapper/gradle-wrapper.properties` 中的 `distributionUrl`
2. 检查本地 `~/.gradle/wrapper/` 下是否已有对应版本的 Gradle
3. 没有则自动下载，有则直接使用

**只要你和同伴都运行 `./gradlew`**，Gradle 版本就完全一致（本项目使用 8.9），不需要各自去装特定版本的 Gradle。

### 升级 Gradle 版本

```bash
# 修改 gradle-wrapper.properties 中的 distributionUrl
# 然后运行
./gradlew wrapper --gradle-version 新版本号
```

提交后所有同伴自动使用新版本。

### 需要提交到 Git 的文件

```
gradle/wrapper/gradle-wrapper.jar       # 包装器 JAR
gradle/wrapper/gradle-wrapper.properties # 版本配置
gradlew                                  # Linux/macOS 启动脚本
gradlew.bat                              # Windows 启动脚本
```

这四个文件已提交，同伴 clone 后直接 `./gradlew assembleDebug` 即可。

## 设计规范

详见 `SPEC.md`。

## 依赖

- AndroidX AppCompat / Material 3 / ConstraintLayout
- RecyclerView / CardView / CoordinatorLayout
- ViewPager2
- AndroidX Biometric（指纹解锁）
- AndroidX Lifecycle（ProcessLifecycleOwner 前后台监听）
- AndroidX Security crypto
- AES-256-GCM 加密（`javax.crypto`）
- PBKDF2WithHmacSHA256（主密码哈希）
