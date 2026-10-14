# CampusFlea · 校园二手交易平台 Android 客户端

> 面向高校场景的二手物品交易 App。学生可以发布闲置、按分类浏览、搜索比价、实时聊天议价、线上下单与评价；卖家可管理在售商品与订单。
>
> 本科毕业设计作品，需与同项目的后端服务、管理端配套使用。

---

## 功能概览

| 模块 | 说明 |
| --- | --- |
| 账号体系 | 密码登录、短信验证码登录、注册、重置密码、登出；AccessToken 过期自动刷新 |
| 首页与分类 | 商品瀑布流、分类导航、关键词搜索与结果筛选、浏览历史 |
| 商品 | 发布 / 编辑 / 下架、多图上传、商品详情、收藏、举报 |
| 交易 | 下单、地址管理、订单列表与详情、订单状态流转、交易评价 |
| 即时通讯 | 基于 WebSocket 的一对一会话，按「商品 + 对方」建立会话；聊天记录本地落库可离线查看 |
| 消息通知 | 会话列表、系统通知、未读提示 |
| 个人中心 | 资料编辑、修改密码、更换手机号、我的商品、我的订单、意见反馈、关于 |
| 个性化 | 浅色 / 深色 / 跟随系统主题切换 |
| 性能监控 | 内置 APM：启动耗时、帧率卡顿、网络耗时、崩溃采集与端内可视化面板（见 [APM](#apm-性能监控)） |

---

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 / 构建 | Kotlin 2.2.10、Gradle Kotlin DSL、AGP 9.1.1、KSP |
| UI | Jetpack Compose（BOM 2026.02.01）+ Material 3、Material Icons Extended |
| 架构 | MVVM + Repository，UI / 领域 / 数据分层，单向数据流 |
| 导航 | Navigation Compose（35 条路由，含参数化详情页） |
| 异步 | Kotlin Coroutines + Flow |
| 网络 | Retrofit 2.9 + OkHttp 4.12（自定义鉴权与 401 自动刷新拦截器）+ Gson |
| 实时通讯 | OkHttp WebSocket，带心跳与指数退避重连 |
| 本地存储 | Room 2.8（聊天记录）、DataStore Preferences（登录态与主题偏好） |
| 图片加载 | Coil 2.5（自定义 `NetworkImage` 统一处理占位与错误态） |
| 其他 | SplashScreen API、Coil 图片缓存、`CacheManager` 统一缓存策略 |
| 兼容性 | minSdk 26（Android 8.0）/ targetSdk 35 |

---

## 项目结构

```
CampusFlea/
├── app/src/main/java/com/example/campusflea/
│   ├── apm/                    # 自研性能监控：启动 / 帧率 / 网络 / 崩溃采集与上报
│   ├── data/
│   │   ├── local/              # Room 数据库、DataStore、Token 管理、缓存
│   │   ├── model/              # 网络数据模型（商品 / 订单 / 消息 / 用户）
│   │   ├── remote/             # RetrofitClient、ApiService、WebSocketManager
│   │   └── repository/         # 各业务域数据仓库，对上层屏蔽数据来源
│   ├── notification/           # 本地通知
│   └── ui/
│       ├── components/         # 通用组件：骨架屏、毛玻璃、动效、图标
│       ├── navigation/         # Screen 路由定义与 NavGraph
│       ├── screen/             # 33 个页面
│       ├── theme/              # Material 3 设计系统与主题偏好
│       └── viewmodel/          # 各页面 ViewModel
├── docs/
│   └── UI_AUDIT_REPORT.md      # UI/UX 审计报告（33 个屏幕、35 条路由）
├── gradle/libs.versions.toml   # 依赖版本集中管理
└── gradle.properties           # 服务端地址、APM 开关等环境配置
```

---

## 快速开始

### 1. 环境要求

- JDK 11 及以上
- Android Studio（需支持 AGP 9.1.1）与 Android SDK 35
- Android 8.0（API 26）及以上设备或模拟器

### 2. 配置本地 SDK 路径

在项目根目录新建 `local.properties`（该文件不入库）：

```properties
sdk.dir=你的 Android SDK 路径
```

### 3. 配置服务端地址

App 依赖同项目的后端服务。地址统一由 `gradle.properties` 注入，**切换环境只需改配置，不用改代码**：

```properties
# 后端 HTTP 基地址
api.baseUrl=http://你的服务器地址

# WebSocket 地址；留空则按 api.baseUrl 自动推导（http -> ws、https -> wss，末尾补 /ws）
# api.wsUrl=ws://你的服务器地址/ws
```

这两项会编译进 `BuildConfig.API_BASE_URL` / `BuildConfig.WS_BASE_URL`，分别被 `RetrofitClient` 与 `WebSocketManager` 读取。

> 图片资源默认从与后端同主机的 MinIO 服务（9000 端口）加载，由 `RetrofitClient.minioBaseUrl` 拼接。

### 4. 构建与运行

```bash
# 调试包，产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# 直接安装到已连接的设备
./gradlew :app:installDebug
```

### 5. 打包发布

项目**未配置 `signingConfig`**，因此 `./gradlew :app:assembleRelease` 产出的是 `app-release-unsigned.apk`，需要自行签名：

- 图形化：Android Studio → Build → Generate Signed Bundle / APK
- 命令行：`apksigner sign --ks your.jks --out app-release.apk app-release-unsigned.apk`

签名后的安装包请不要提交到仓库（`.gitignore` 已忽略 `app/release/`），需要分发时通过 GitHub Releases 上传。

---

## APM 性能监控

`apm/` 是一个不依赖第三方 SDK 的轻量性能采集模块，配置项同样在 `gradle.properties`：

```properties
# 总开关。false 时所有采集入口短路，不产生任何可观测开销
apm.enabled=true
apm.appId=campus-flea
# 上报地址。留空表示只在端上采集（仍可在 App「设置 - 性能监控」查看），不发起任何外发请求
apm.uploadUrl=
```

采集内容涵盖：应用启动耗时、帧率与卡顿、网络请求耗时与失败率、未捕获异常。数据的采集策略集中在 `ApmConfig`，端内可视化面板位于「设置 → 性能监控」。

---

## 说明

- 本项目为本科毕业设计作品，用于学习与交流。
- 后端服务与管理端为独立仓库，需配套部署后才能完整跑通业务流程。
