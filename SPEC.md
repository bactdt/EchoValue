# Vault — Android 密码保管库 设计约束

> 基于 Stitch 项目 "Android Password Vault" 设计稿（项目 ID: 11340323869675097283）

---

## 1. 设计系统

| Token | 值 | 用途 |
|-------|-----|------|
| Primary | `#000666` | 按钮、FAB、标题、强调色 |
| PrimaryContainer | `#1A237E` | 安全得分卡背景、渐变基调 |
| OnPrimary | `#FFFFFF` | 按钮文字、图标 |
| Background | `#F7F9FB` | 所有屏幕背景 |
| Surface | `#F7F9FB` | AppBar、卡片 |
| SurfaceContainerLow | `#F2F4F6` | 登录卡片、次要背景 |
| SurfaceVariant | `#E0E3E5` | 悬停态、hint |
| OnSurface | `#191C1E` | 主要文字 |
| OnSurfaceVariant | `#454652` | 次要文字、label |
| Outline | `#767683` | 描边 |
| OutlineVariant | `#C6C5D4` | 弱描边、分隔线 |
| Error | `#BA1A1A` | 错误态、弱密码指示 |
| ErrorContainer | `#FFDAD6` | |
| Success / Good | `#2E7D32` | |

- 圆角: 12dp (卡片/输入框), 24dp (pill 按钮), 16dp (FAB)
- 字体: Roboto Flex (系统 sans-serif 回退)
- 间距: 8dp 节奏, 16dp 移动端边距

## 2. 屏幕流程

```
LoginActivity → (RegisterActivity) → UnlockActivity → MainActivity (Fragment 容器)
                                                       ├── VaultFragment (tab 1)
                                                       ├── GeneratorFragment (tab 2)
                                                       ├── SecurityFragment (tab 3)
                                                       └── SettingsFragment (tab 4)
                                                       ├── PasswordGeneratorActivity（外部启动，非 tab）
                                                       ├── PasswordDetailActivity
                                                       └── AddCredentialActivity
```

- `LoginActivity` 是 Launcher
- `SessionManager` 控制登录态 + 解锁态
- `VaultApp` 持有主密码（仅内存，char[]）
- MainActivity 为单 Activity 宿主，4 个 tab 使用 FragmentTransaction.hide/show 切换
- 底部导航栏只存在一份（activity_main.xml），永不重建

## 3. 数据层

- **存储**: SQLite (`VaultDbHelper`)
- **表**: `users` (name, email, password_hash, password_salt, pin_hash) + `credentials` (name, username, password_encrypted, website, notes, favorite, timestamps)
- **凭据密码加密**: AES-256-GCM via `CryptoUtil`
- **主密码哈希**: PBKDF2WithHmacSHA256, 60000 迭代
- **PIN 验证**: SHA-256(email + ":" + pin) — 演示级，非安全
- **仓库**: `CredentialRepository` 封装加解密

## 4. 包结构

```
cn.it.cast.keshe/
├── adapter/CredentialAdapter.java
├── data/
│   ├── CredentialRepository.java
│   └── VaultDbHelper.java
├── model/
│   ├── Credential.java
│   └── UserAccount.java
├── util/
│   ├── ClipboardUtil.java
│   ├── CryptoUtil.java
│   ├── PasswordGenerator.java
│   ├── PinVerifier.java
│   ├── SessionManager.java
│   └── StrengthEvaluator.java
├── VaultApp.java
├── MainActivityCallback.java       ← Fragment 与 Activity 通信接口
├── MainActivity.java               ← 单 Activity 宿主，管理 4 个 tab 切换
├── VaultFragment.java              ← tab 1: 密码列表主页
├── GeneratorFragment.java          ← tab 2: 密码生成器
├── SecurityFragment.java           ← tab 3: 安全概览
├── SettingsFragment.java           ← tab 4: 设置/注销
├── LoginActivity.java
├── RegisterActivity.java
├── UnlockActivity.java
├── PasswordDetailActivity.java
└── AddCredentialActivity.java
```

## 5. 每个 Activity 的关键属性

| Activity | 传入 Extra | 返回 | 布局文件 | 备注 |
|----------|-----------|------|---------|------|
| LoginActivity | — | — | `activity_login.xml` | Launcher |
| RegisterActivity | `prefill_email` (可选) | — | `activity_register.xml` | |
| UnlockActivity | — | — | `activity_unlock.xml` | |
| MainActivity | — | — | `activity_main.xml` | 宿主，包含 FragmentContainerView |
| PasswordDetailActivity | `cred_id: long` | RESULT_OK 删除后 | `activity_password_detail.xml` | |
| AddCredentialActivity | `cred_id: long` (编辑模式) | RESULT_OK | `activity_add_credential.xml` | |

| Fragment | 对应 tab | 布局文件 | 
|----------|----------|---------|
| VaultFragment | 1 — Vault | `fragment_vault.xml` |
| GeneratorFragment | 2 — 生成器 | `fragment_generator.xml` |
| SecurityFragment | 3 — 安全 | `fragment_security.xml` |
| SettingsFragment | 4 — 设置 | `fragment_settings.xml` |

## 6. 守卫（进入条件）

所有除 Login/Register 外的 Activity 以及 MainActivity 中的 Fragment 必须满足：
1. `SessionManager.isLoggedIn()` == true
2. `SessionManager.isUnlocked()` == true
3. `VaultApp.hasMasterPassword()` == true

不满足时跳转回 LoginActivity 或 UnlockActivity。守卫在 MainActivity.onCreate() 和 Fragment.onViewCreated() 中各自执行。

## 7. 启动器优先级

1. `Login → Unlock → Main` 是标准流程（App 被杀后重建从此路线恢复）
2. 如 `isLoggedIn=false` → 跳 Login
3. 如 `isUnlocked=false` → 跳 Unlock
4. 否则显示主页

## 8. 依赖（build.gradle）

```groovy
implementation 'androidx.appcompat:appcompat:1.7.1'
implementation 'com.google.android.material:material:1.14.0'
implementation 'androidx.activity:activity:1.13.0'
implementation 'androidx.constraintlayout:constraintlayout:2.2.1'
implementation 'androidx.recyclerview:recyclerview:1.4.0'
implementation 'androidx.cardview:cardview:1.0.0'
implementation 'androidx.coordinatorlayout:coordinatorlayout:1.2.0'
implementation 'androidx.biometric:biometric:1.2.0-alpha05'
```

## 9. 权限

```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.VIBRATE" />
```

## 10. 导航栏

底部 4 tab 固定顺序：**Vault | 生成器 | 安全 | 设置**

- 底栏只存在于 activity_main.xml 中，4 个 Fragment 不包含底栏
- 切换方式：`FragmentTransaction.hide(current).show(next)`，保留 Fragment 实例状态，零延迟
- 主 tab 用 `MainActivityCallback` 接口通知宿主切换 toolbar 标题、搜索按钮和头像显隐
- FAB 只在 Vault tab 显示，其余 tab 隐藏
- 设置 tab：点击注销按钮通过回调通知 MainActivity 清空 session 并跳转 Login

## 11. 样式约定

- 所有按钮用 `@drawable/bg_btn_pill_primary`（primary pill）或 `bg_btn_pill_outline`
- 输入框用 `bg_input_rounded`（12dp 圆角 + outline-variant 描边）
- 卡片用 `bg_card_rounded` / `bg_card_elevated` / `bg_card_high`（12dp 圆角 + surface-container 底色）
- Chip 活动用 `bg_chip_active`（secondary-container）、非活动用 `bg_chip_inactive`（surface-container-high）
- 强度条用 `bg_strength_error` / `bg_strength_secondary` / `bg_strength_primary`
- 底部导航用 `BottomNavItem` / `BottomNavItemActive` 样式

## 12. 默认数据

首次启动时（`repository.count() == 0`），在 MainActivity.onCreate 中播种 4 条示例凭据：
Google / Bank of America / Netflix / Adobe CC，包含示例用户名和密码。
