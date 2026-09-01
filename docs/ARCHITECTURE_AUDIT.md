# ENVISION 环境监测保护系统 · 架构审计报告

> 审计日期：2026-08-25 ｜ 审计范围：nepsystem（后端）、nepsystem-web（前端）、docs、db/init.sql、db/generate_demo_data.sql
> 审计方式：全量静态代码走读（未启动运行时环境，未修改任何代码/数据库）
> 结论摘要：**架构总体为"课程设计级三层结构"，功能闭环完整、代码量小、可运行；但存在认证形同虚设、危险裸写端点、CSV 导出前端断链、设备在线状态失真、数据量无治理等高风险问题。在保持现有架构的前提下，P0 级 3 个改动（约半天工作量）即可消除全部"功能损坏"与"数据可被破坏"风险。**

---

## A. 当前架构图

```text
┌────────────────────────────────────────────────────────────────────────────┐
│                              展示层 nepsystem-web                          │
│  Vue 3 + Vite 5 + Element Plus + ECharts 5 + Pinia + Vue Router + Axios    │
│  ├─ 页面：Login / Dashboard / History / Devices / Alerts / Thresholds /    │
│  │        Users（MainLayout 框架，路由守卫仅查 localStorage）              │
│  ├─ api/index.js（Axios 统一封装，baseURL=/api，携带 Bearer token）         │
│  └─ utils/ws.js（WebSocket 封装，3s 断线重连）                              │
└───────────────┬──────────────────────────────────┬────────────────────────┘
                │ HTTP /api（Vite 代理 / nginx 反代）│ WS /ws/notify
┌───────────────▼──────────────────────────────────▼────────────────────────┐
│                         服务层 nepsystem（Spring Boot 2.6.13, :8080）       │
│  Controller 层（9 个 API Controller + 1 个残留 AdminsCtrl + 2 个教学 Demo）  │
│      ├─ 大多数 Controller 直接注入 DAO（绕过 Service 层）                   │
│      └─ 唯一核心业务：DataController → MonitorDataService.report()          │
│  Service 层：7 个接口 + 7 个 Impl（除 report 外均为 MyBatis-Plus 样板转发）  │
│  DAO 层：8 个接口 extends BaseMapper<T>（MyBatis-Plus 3.4.1，无自定义 SQL）  │
│  支撑组件：WebSocket（/ws/notify 广播）、阈值引擎（report 内联）、           │
│            模拟器（@Scheduled 5s）、全局异常处理、Result/PageResult 封装     │
└───────────────┬────────────────────────────────────────────────────────────┘
                │ JDBC（HikariCP）
┌───────────────▼────────────────────────────────────────────────────────────┐
│             MySQL 8（库 nep，utf8mb4，8 张表）                               │
│  admins / users / regions / devices / sensors / monitor_data /             │
│  thresholds / alerts                                                        │
└────────────────────────────────────────────────────────────────────────────┘
数据源：
  ├─ 数据模拟器（后端 @Scheduled，simulator.enabled=true，5s/轮）
  ├─ HTTP 上报 POST /api/data/report（预留真实硬件通道）
  └─ MQTT（仅预留 MqttProperties 配置，未实现）
```

**架构特征**：前后端分离 + 经典三层（Controller/Service/DAO）+ 数据库驱动。WebSocket 为"服务端主动广播、前端全量接收"模式，无订阅粒度。

---

## B. 前端模块图

```text
nepsystem-web/src
├── main.js                 # 入口：Pinia + Router + ElementPlus(zh-cn) + 全量图标注册
├── App.vue                 # 根组件（<router-view/> + 全局过渡样式）
├── router/index.js         # 路由 + 守卫（仅检查 localStorage nep_token 是否存在）
├── store/user.js           # Pinia 会话（token/adminCode/adminId，localStorage 持久化）
├── layout/MainLayout.vue   # 主框架：悬浮导航 6 菜单、未处理告警角标(30s轮询)、时钟、登出
├── api/index.js            # 全部 30 个 API 封装（唯一 API 入口）
├── utils/
│   ├── request.js          # Axios 实例：token 注入、{code,message,data} 解包、401 跳登录
│   └── ws.js               # WebSocket：连接/订阅/3s 自动重连
├── styles/                 # tokens.css + element-override.scss（深色驾驶舱主题）
└── views/
    ├── Login.vue           # 登录（默认账号 admin/123456 预填）
    ├── Dashboard.vue       # 实时监测大屏（核心页面，1,211 行）
    │                       #   - 统计卡(overview 30s轮询) / Gauge 仪表 / 实时曲线 /
    │                       #   - 告警趋势(stat 60s) / 空间分布(effectScatter) / WS 实时更新
    ├── History.vue         # 历史数据：分页表格 + 趋势图(interval=hour) + CSV 导出
    ├── Devices.vue         # 设备 CRUD（分页/关键字/类型/状态过滤）
    ├── Alerts.vue          # 告警中心：分页/筛选/处理/未处理数
    ├── Thresholds.vue      # 阈值设置：全局/设备级，仅开放 warnMax/alarmMax
    └── Users.vue           # 用户 CRUD + 启停（角色字段仅展示，无权限效果）
```

**页面 → API 调用链**（调用关系核对，详见 §5 专项）：

| 页面 | 调用的 API | 周期 |
|---|---|---|
| Login.vue | login, logout | 一次性 |
| MainLayout.vue | getUnhandled, logout | 30s 轮询 |
| Dashboard.vue | getDevicesPage, getDeviceLatest(每设备), getOverview, getUnhandled, getSensors, getHistory(每WS消息), getAlertsStat, getQuality | 30s/60s 轮询 + WS |
| History.vue | getDevicesPage, getSensors, getHistory, getTrend, exportCsv | 交互触发 |
| Devices.vue | getDevicesPage, addDevice, updateDevice, deleteDevice | 交互触发 |
| Alerts.vue | getAlertsPage, handleAlert, getUnhandled, getDevicesPage, getSensors | 交互+挂载 |
| Thresholds.vue | getThresholds, addThreshold, updateThreshold, deleteThreshold, getDevicesPage, getSensors | 交互触发 |
| Users.vue | getUsersPage, addUser, updateUser, deleteUser, changeUserStatus | 交互触发 |
| **无人调用** | **getOnlineCount, reportData, getLatestData, getDeviceRanking** | —（死封装，对应后端 3 个死接口） |

---

## C. 后端模块图

```text
nepsystem/src/main/java/org/nep/nepsystem
├── NepsystemApplication.java      # @SpringBootApplication + @EnableScheduling
├── ctrl/                          # 控制器（@RestController /api/...）
│   ├── AuthController             # /api/auth/login、logout（登录查 admins 表）
│   ├── DevicesController          # /api/devices/page、POST/PUT、DELETE、online/count、{id}/latest
│   ├── DataController             # /api/data/report、latest、history、trend、export
│   ├── AlertsController           # /api/alerts/page、{id}/handle、unhandled、stat
│   ├── ThresholdsController       # /api/thresholds CRUD
│   ├── StatsController            # /api/stats/overview、quality、device-ranking
│   ├── UsersController            # /api/users CRUD + {id}/status
│   ├── SensorsController          # /api/sensors 列表
│   ├── AdminsCtrl                 # ⚠ 教学残留：/admins/insert|update|delete（无鉴权硬编码写库）
│   └── demos/web/                 # ⚠ Spring 官方教程 Demo（/hello、/user、/save_user、/html、/javabeat）
├── service/
│   ├── MonitorDataService/Impl    # ★ 唯一真实业务：report()=写数据+置在线+阈值告警+WS 广播（@Transactional）
│   └── Alerts/Devices/Sensors/Thresholds/Users/Regions Service/Impl
│                                  # 全部为 MyBatis-Plus 样板转发（Controller 实际未使用）
├── service/simulator/DataSimulatorService  # 模拟器：@Scheduled 5s/轮，按设备类型生成随机指标
├── ws/NotifyWebSocketHandler      # 会话 Map + broadcast()（data/alert 两种消息）
├── config/
│   ├── WebSocketConfig            # 注册 /ws/notify，allowedOrigins("*")
│   ├── SimulatorConfig            # simulator.enabled / interval-ms
│   ├── MqttProperties             # MQTT 预留配置（未实现）
│   └── MybatisPlusConfig          # 分页插件
├── dao/                           # 8 个 BaseMapper 接口（无自定义 SQL）
├── bean/                          # 8 个实体（@TableName 映射，字段与表一致）
├── common/                        # Result<T>、PageResult<T>、GlobalExceptionHandler
└── exception/BizException         # 业务异常（code+message）
```

**Controller → Service → DAO → DB 调用链（§4 专项）**：

| 链路 | 走向 | 说明 |
|---|---|---|
| 上报链路（核心） | DataController.report → **MonitorDataServiceImpl.report()** → MonitorDataDao / DevicesDao / ThresholdsDao / AlertsDao + NotifyWebSocketHandler.broadcast | @Transactional 单事务；模拟器与 HTTP 共用 |
| 设备链路 | DevicesController → DevicesDao / MonitorDataDao（**绕过** DevicesService） | 仅 {id}/latest 与 delete 校验用 MonitorDataDao |
| 告警链路 | AlertsController → AlertsDao（**绕过** AlertsService） | 分页/处理/统计 |
| 统计链路 | StatsController → DevicesDao / MonitorDataDao / AlertsDao / SensorsDao | overview 4 次 COUNT；quality 评分；ranking N+1 |
| 阈值链路 | ThresholdsController → ThresholdsDao（绕过 Service） | CRUD，无查重 |
| 用户链路 | UsersController → UsersDao | CRUD + 状态 |
| 认证链路 | AuthController → AdminsDao | 明文密码比对，UUID token 无存储无校验 |
| 字典链路 | SensorsController → SensorsDao | 按 deviceType 过滤 |
| 危险残留 | AdminsCtrl → AdminsDao | 无参即可写/删 admin id=2 |

---

## D. 数据库 ER 关系

```text
admins（管理员，登录用）                users（普通用户，独立 CRUD，登录不用）
┌──────────────┐                       ┌──────────────┐
│ admin_id PK  │                       │ id PK        │
│ admin_code U │                       │ username U   │
│ password     │                       │ password     │
│ remarks      │                       │ nickname     │
│ create_time  │                       │ role/status  │
└──────────────┘                       │ create_time  │
                                       └──────────────┘

regions ──1:N── devices ──1:N── monitor_data
│ id PK         │ id PK                 │ id PK (BIGINT)
│ name          │ device_code U         │ device_id FK→devices.id
│ parent_id(0)  │ device_name           │ sensor_code (逻辑关联 sensors)
│ description   │ type AIR/WATER/NOISE  │ value DECIMAL(10,2)
│               │ region_id FK→regions  │ report_time (idx: device+time, sensor+time)
│               │ location              │ create_time
│               │ status 0/1/2          │
│               │ last_report_time      │
│               │ create_time           │
│               └──────┬───────────────┘
│               1:N    │                1:N
│       ┌──────────────▼──────┐        ┌──────────────┐
│       │ thresholds          │        │ alerts       │
│       │ id PK               │        │ id PK BIGINT │
│       │ device_id NULL=全局 │        │ device_id FK │
│       │ sensor_code         │        │ sensor_code  │
│       │ warn_min/max        │        │ level WARN/ALARM
│       │ alarm_min/max       │        │ alert_value  │
│       │ enabled,update_time │        │ message      │
│       │ idx(device,sensor)  │        │ status 0/1   │
│       └─────────────────────┘        │ handle_user  │
│                                       │ handle_time  │
│                                       │ create_time  │
│                                       │ idx(device,time)/status/level
│                                       └──────────────┘

sensors（指标字典，逻辑关联，无物理外键）
│ id PK / sensor_code U / sensor_name / unit
│ device_type（NULL=通用）/ min_range / max_range / standard_max
└────────────────────────────────────────────
※ 全部为逻辑外键（无 CONSTRAINT FOREIGN KEY），由应用层保证一致性
※ monitor_data 与 alerts 无外键到 devices → 设备删除时靠"有数据则拒绝删除"兜底
```

**ER 要点**：核心关联为 devices → monitor_data / alerts / thresholds 三张子表；sensors 为字典表（sensor_code 软关联）；regions 已有表结构与种子数据（校区/教学楼/人工湖），但**没有任何代码使用**（休眠表）；admins 与 users 为两套并行的"用户体系"，互不相通。

---

## E. WebSocket 数据流

```text
[数据产生]                               [后端]                              [前端]
模拟器 @Scheduled 5s/轮 ──┐
                          ├─► MonitorDataServiceImpl.report()
POST /api/data/report ────┘        │ ① 写 monitor_data
                                   │ ② devices.status=1 + last_report_time
                                   │ ③ checkThreshold → 写 alerts（去重后）
                                   │ ④ 每指标广播 data 消息（★事务提交前）
                                   ▼
                        NotifyWebSocketHandler.broadcast(json)
                                   │ SESSIONS（ConcurrentHashMap，全量广播）
                                   ▼
                        /ws/notify（WebSocketConfig 注册，allowedOrigins=*）
                                   │ Vite dev 代理 /ws → 8080；生产 nginx 反代
                                   ▼
                        utils/ws.js connectWS()（3s 断线重连，无心跳）
                                   │
                                   ▼
                        Dashboard.vue onWSMessage 分发
                          ├─ type=data  → latestMap 增量更新 + Gauge 刷新
                          │             + loadTrend()（★每消息重拉历史） + renderSpatial()
                          └─ type=alert → loadUnhandled()（角标刷新）
```

**消息格式**（后端手拼 JSON 字符串，无转义）：
- `{"type":"data","deviceId":1,"deviceCode":"...","sensorCode":"PM25","value":165,"reportTime":"yyyy-MM-dd HH:mm:ss"}`
- `{"type":"alert","alertId":...,"level":"ALARM","sensorCode":"PM25","alertValue":165,"message":"..."}`

**流量估算**：3 台设备 × 4 指标/轮 × 12 轮/分 ≈ 144 条 data 消息/分钟广播；每条 data 消息在 Dashboard 触发 1 次 loadTrend()（≈4 个 HTTP 历史请求）与 1 次空间图全量重绘。

---

---

## F. 告警数据流

```text
上报（模拟器 / POST /api/data/report）
  │
  ▼
MonitorDataServiceImpl.report()  @Transactional
  │ ① 按 device_code 查设备（不存在 → BizException → 整体回滚）
  │ ② 逐指标写 monitor_data
  │ ③ devices.status=1、last_report_time=now
  │ ④ 逐指标 checkThreshold()：
  │      ├─ 取阈值：设备级(device_id=该设备) 优先 → 全局(device_id IS NULL)，enabled=1，limit 1
  │      ├─ 判级顺序：alarm_max 超 → ALARM
  │      │            alarm_min 低 → ALARM
  │      │            warn_max 超  → WARN
  │      │            warn_min 低  → WARN
  │      ├─ 30 分钟去重：同设备+同指标+同级别 且 create_time > now-30min → 跳过
  │      └─ 写 alerts(status=0) + 广播 {"type":"alert",...}
  │
  ▼
前端消费：
  ├─ Dashboard：WS alert 消息 → loadUnhandled() 角标
  ├─ MainLayout：30s 轮询 getUnhandled 角标
  ├─ Alerts.vue：分页查询 / 按级别/状态/设备筛选 / 处理（PUT /alerts/{id}/handle →
  │             status=1 + handle_user + handle_time）
  └─ Dashboard 告警趋势：getAlertsStat（近 7 天按日按级别 COUNT）
```

**阈值引擎要点**：引擎内联在 report() 事务中，无独立模块；同一次上报同指标同时超 warn 与 alarm 时只记 ALARM（报警优先）；阈值表允许重复配置（无唯一约束），命中规则为"limit 1 不排序"——重复时结果不确定；告警去重依赖 alerts.create_time（等于上报时间，若回填历史时间则去重窗口失真）。

---

## G. 当前功能清单

| # | 功能 | 前端实现 | 后端实现 | 数据来源 | 状态 |
|---|---|---|---|---|---|
| 1 | 登录/登出 | Login.vue / MainLayout.vue | AuthController + AdminsDao | admins 表（明文密码） | ✅ 可用（仅形式校验） |
| 2 | 实时监测大屏 | Dashboard.vue（Gauge/曲线/空间分布/统计卡） | DevicesController/StatsController/DataController + WS | devices、monitor_data、alerts + WS 推送 | ✅ 可用 |
| 3 | 空气/水质/噪声分型监测 | Dashboard activeSensors 按 deviceType 过滤 | sensors 字典 device_type + 模拟器按类型生成 | sensors、monitor_data | ✅ 可用 |
| 4 | WebSocket 实时推送 | utils/ws.js + Dashboard 订阅 | WebSocketConfig + NotifyWebSocketHandler | 上报事务内广播 | ✅ 可用（见 E/H） |
| 5 | 数据模拟器 | —（纯后端） | DataSimulatorService（@Scheduled 5s） | 随机数 → report() | ✅ 可用 |
| 6 | 阈值引擎 | Thresholds.vue 配置 | report() 内联 checkThreshold | thresholds 表 | ✅ 可用（UI 仅支持上限） |
| 7 | WARN/ALARM 两级告警 | Alerts.vue 列表/处理/角标 | checkThreshold + AlertsDao | alerts 表 | ✅ 可用 |
| 8 | 历史数据查询 | History.vue 分页表格 | DataController.history | monitor_data | ✅ 可用 |
| 9 | 趋势分析 | History.vue 曲线（按小时聚合） | DataController.trend（AVG/MAX/MIN） | monitor_data | ✅ 可用 |
| 10 | CSV 导出 | History.vue onExport（blob 下载） | DataController.export（limit 10000） | monitor_data | ❌ **前端断链**（见 I-3） |
| 11 | 设备 CRUD | Devices.vue | DevicesController | devices | ✅ 可用（删除受数据限制） |
| 12 | 设备空间分布 | Dashboard effectScatter 抽象布局 | 无独立接口（复用 devices+latest） | 内存计算 | ✅ 可用（非真实地理） |
| 13 | 用户管理 | Users.vue | UsersController | users 表 | ✅ 可用（与登录无关） |
| 14 | 统计 Dashboard | Dashboard 统计卡/质量分/告警趋势 | StatsController | 多表 COUNT/聚合 | ✅ 可用 |
| 15 | 环境质量评分 | Dashboard qualityOverall | StatsController.quality | sensors.standard_max | ✅ 可用（模型有缺陷，见 J-12） |
| 16 | 在线设备统计 | 统计卡"在线设备" | devices.status 计数 | devices | ⚠️ 失真（无离线检测，见 I-4） |
| 17 | 区域管理 | —（无页面） | —（无 Controller） | regions（种子数据） | ⛔ 休眠功能 |
| 18 | MQTT 硬件接入 | — | MqttProperties 仅配置 | — | ⛔ 预留未实现 |

**Dashboard 数据来源明细（§10 专项）**：

| 大屏区域 | 数据来源 API | 刷新机制 |
|---|---|---|
| 统计卡（设备总数/在线/今日上报/未处理告警） | GET /api/stats/overview | 挂载 + 30s 轮询 |
| 主仪表 Gauge（首指标）+ 超标判断 | GET /api/devices/{id}/latest（挂载时每设备一次） | WS data 消息增量更新 |
| 实时趋势曲线 | GET /api/data/history（每设备每指标 size=40） | **每 WS data 消息重拉** |
| 告警趋势柱状图（近 7 天） | GET /api/alerts/stat | 挂载 + 60s 轮询 |
| 环境质量分 | GET /api/stats/quality（无 deviceId） | 挂载 + 60s 轮询 |
| 空间分布图 | 无接口（devices + latestMap 内存渲染） | WS 消息触发全量重绘 |
| 未处理告警角标 | GET /api/alerts/unhandled | 挂载 + 30s 轮询 + WS alert |
| 顶部时钟 | 本地 Date | 1s |

---

## H. 当前问题清单（总表）

> 严重度：🔴 高（损坏功能/数据安全/必然劣化）｜🟠 中（功能缺陷/性能/一致性）｜🟡 低（整洁度/维护性）

| 编号 | 问题 | 严重度 | 位置 |
|---|---|---|---|
| H1 | 认证形同虚设：UUID token 无服务端存储与校验，全部 API 可匿名调用 | 🔴 | AuthController / 无拦截器 |
| H2 | 危险裸写端点：/admins/insert|update|delete 无鉴权直接改删管理员 | 🔴 | AdminsCtrl |
| H3 | CSV 导出前端断链：axios 拦截器把 Blob 当 JSON 判 code | 🔴 | request.js × History.vue |
| H4 | 设备在线状态失真：report 只置 1，无离线检测任务 | 🔴 | MonitorDataServiceImpl |
| H5 | monitor_data 无治理：模拟器约 8.6 万行/天，查询与导出持续劣化 | 🔴 | 模拟器/DataController |
| H6 | WS 广播在事务提交前发送 + 手拼 JSON 无转义 | 🔴 | MonitorDataServiceImpl |
| H7 | users 用户体系与登录脱节，role 无任何权限效果 | 🟠 | UsersController/AuthController |
| H8 | regions 区域功能休眠（表/DAO/Service 有，无 Controller/页面） | 🟠 | 全链路缺失 |
| H9 | Service 层空转：7 组 Service/Impl 未被 Controller 使用 | 🟠 | service/impl/* |
| H10 | Dashboard WS 消息风暴：每 data 消息重拉历史+全量重绘 | 🟠 | Dashboard.vue |
| H11 | thresholds 可重复配置（无唯一约束），引擎 limit 1 行为不确定 | 🟠 | ThresholdsController/Impl |
| H12 | 环境质量评分模型缺陷：一律"越高越差"，DO/PH 反向指标算错 | 🟠 | StatsController.quality |
| H13 | 全局异常兜底泄漏 e.getMessage()，HTTP 状态恒 200 | 🟠 | GlobalExceptionHandler |
| H14 | 密码明文存储 + application.properties 含 DB 密码入库 | 🟠 | init.sql/AuthController/配置文件 |
| H15 | 前端工具函数大量重复（typeName/colorOf/fmtTime 等 4~6 处） | 🟡 | views/* |
| H16 | History 多指标筛选缺陷：≥2 指标时表格/导出不过滤 | 🟡 | History.vue |
| H17 | 演示数据脚本 DELETE 全表（注释称"保留真实数据"），误执行清库 | 🟡 | generate_demo_data.sql |
| H18 | 无 CORS 配置；WS allowedOrigins="*" | 🟡 | WebSocketConfig |
| H19 | 告警无通知渠道（仅站内 WS），无邮件/钉钉/短信 | 🟡 | 架构缺口 |
| H20 | 死代码双向：/data/latest、/stats/device-ranking、/devices/online/count 无页面调用 | 🟡 | api/index.js + 后端 |
| H21 | 教学残留：demos/web、static/index.html、AdminsCtrl（见 H2） | 🟡 | 多个 |
| H22 | JDK 21 运行 Spring Boot 2.6.13（Spring 5.3.x）非官方组合；pom 仍 target 1.8 | 🟡 | pom.xml |
| H23 | N+1 查询：devices/{id}/latest 每指标 1 查；device-ranking 逐行查设备 | 🟡 | DevicesController/StatsController |
| H24 | 文档与仓库不一致：部署手册引用 docker-compose.yml/nginx.conf 但仓库无此文件 | 🟡 | docs/部署手册.md |
| H25 | 设备删除死胡同：有数据即拒删，无"清理数据"入口，演示设备永不可删 | 🟡 | DevicesController |
| H26 | 告警处理人前端可伪造（handleUser 由请求参数传入） | 🟡 | AlertsController |

---

## I. 高风险问题（🔴 必须优先处理）

### I-1 认证形同虚设（H1）
- **现状**：`POST /api/auth/login` 比对 admins 表明文密码后返回随机 UUID；该 token **不落库、不校验**。全项目无拦截器/过滤器/AOP，所有 `/api/**`（含写操作）均可匿名直接调用；`logout` 仅返回成功。前端路由守卫只检查 localStorage 是否存在 nep_token（伪造即可进入）。
- **风险**：任意人可读写全部监测数据、删改设备/用户/阈值/告警。课程设计定位可理解，但属于"会破坏功能与数据"的最高优先级债务。
- **修复方向（不改架构）**：Spring HandlerInterceptor + 内存 token 表（登录时写入，登出/过期删除），拦截 /api/** 放行 /api/auth/login；对 /api/admins 单独禁用。

### I-2 危险裸写端点（H2）
- **现状**：`AdminsCtrl`（@Controller + @RequestMapping("/admins")）三个端点**无需任何参数**即执行：insert（硬编码插入 id=2 管理员）、update（把 id=2 密码改为 123）、delete（删除 id=2）。配合 I-1 无鉴权 → 任何人 GET /admins/delete 即删除默认管理员，导致系统无法登录。
- **修复方向**：加鉴权拦截后该组端点自然被保护；或 @RequestMapping 值改为不冲突路径并加校验。

### I-3 CSV 导出前端断链（H3）
- **现状**：`exportCsv` 使用 `responseType:'blob'`，但 `utils/request.js` 响应拦截器对**所有**响应执行 `res.code === 200` 判断——Blob 对象没有 code 属性 → 恒走 `ElMessage.error(res.message || '请求失败')` 并 reject → History.vue 导出按钮实际永远弹"请求失败"、无文件下载。smoke/e2e 测试均只覆盖后端 API（Invoke-RestMethod 直连），未覆盖该前端链路，故未被发现。
- **修复方向（一行级）**：拦截器中 `response.config.responseType === 'blob'` 时直接返回 response.data。

### I-4 设备在线状态失真（H4）
- **现状**：report() 无条件把设备置 status=1 并刷新 last_report_time；**没有任何机制把超时未上报的设备置回 0（离线）**。模拟器会把新建的 status=0 设备在 5s 内全部置为在线；后端重启/设备故障后，Dashboard"在线设备"、统计卡永远虚高。
- **修复方向**：新增 @Scheduled 任务（如每 60s）扫描 last_report_time < now - N 分钟（N 可配，如 3×interval）且 status=1 的设备置 0；顺带将置离线事件 WS 广播。

### I-5 数据量增长无治理（H5）
- **现状**：模拟器 3 台设备 × 4 指标 × 12 轮/分 ≈ **8.6 万行/天**（24h≈207k 行）；/api/data/history 与 /trend 无默认时间范围（全表扫描聚合）；export limit 10000 静默截断。项目说明中已规划"时序数据库"，但当前 MySQL 会在 1~2 个月内明显变慢。
- **修复方向（不改库）**：新增 @Scheduled 清理任务按保留天数（可配，如 7 天）删除 monitor_data 与已处理 alerts；前端历史查询默认带 7 天范围；导出加条数提示。

### I-6 WebSocket 事务内广播 + 手拼 JSON（H6）
- **现状**：broadcast() 在 @Transactional 方法**提交前**执行——事务回滚时（如后续指标解析失败）客户端已收到"幽灵数据"；消息为字符串拼接（`"message":"..."`），一旦 message/sensorCode 含引号或换行即产生非法 JSON，前端 JSON.parse 静默丢弃，实时链路中断且无日志。
- **修复方向**：事务提交后（TransactionSynchronization.afterCommit）再广播；用 Jackson ObjectMapper 序列化消息对象。

---

## J. 中风险问题（🟠 建议近期处理）

### J-1 users 体系与登录脱节（H7）
users 表 CRUD、role（ADMIN/USER）、status 启停均有实现与页面，但登录只查 admins 表；role 与 status 不参与任何鉴权。用户管理页实际是"独立演示功能"。建议二选一：a) 明确 users 为演示模块；b) 登录改为 users 表（或双表），role 接入拦截器做菜单级控制——改动可控。

### J-2 regions 休眠（H8）
regions 表 + RegionsDao/RegionsService/RegionsServiceImpl 已存在且 init.sql 有种子数据（校区/教学楼A栋/人工湖），但**无 Controller、无前端页面、设备表单无 region_id 字段**。新建设备 region_id 恒为 NULL；"设备空间分布"用设备类型抽象坐标而非区域。激活成本低：一个 RegionsController（list）+ 设备表单加区域选择 + 空间分布按区域分组。

### J-3 Service 层空转（H9）
除 MonitorDataService.report 外，其余 6 组 Service/Impl 与 DAO 完全相同（save/update/deleteById/getById/page 样板），Controller 全部直接注入 DAO。分层名存实亡。**不建议本轮重构**（违反"不重构"约束），仅在报告中标注；后续新业务应落到 Service，旧代码保持不动。

### J-4 Dashboard WS 消息风暴（H10）
每条 data 消息触发 loadTrend()（活动设备每指标 1 次 getHistory，约 4 个请求）+ renderSpatial() 全量重绘。按当前模拟频率 = 每 5s 约 4~12 个历史请求 + 2 次图表 setOption。设备增多后前端请求量线性爆炸，且与 30s 轮询叠加。修复方向（纯前端）：WS 消息 1~2s 节流合并（debounce）后再刷新趋势与空间图；趋势图改为只对活动设备刷新。

### J-5 阈值配置无唯一约束（H11）
thresholds 无 (device_id, sensor_code) 唯一键（仅普通索引）；checkThreshold 用 limit 1 **无排序**取一行——重复配置时命中行不确定，且 UI 保存"全局 PM25"后再次保存会静默产生第二行（新行优先被命中，旧行成为死配置）。修复方向（不改库）：ThresholdsController.add 前查重（同 device 作用域 + sensor_code 已存在则拒绝或提示覆盖）。

### J-6 环境质量评分模型缺陷（H12）
`得分 = value / standard_max × 100` 一律"越高越差"：
- **DO 溶解氧**（standard_max=5）：DO 越高越好，score 逻辑反向（DO=8 得 160 分被判"超标"）；
- **PH**（6.5~9.0 目标区间）：区间型指标完全无法表达；
- **TEMP/HUMI** 无下限概念（低温/低湿不是污染），但当前模型把"低值"也算高分——恰好结果正确但语义错误；
- 无 deviceId 时混用**全库**最新值（limit 100 内取每个 sensor_code 第一条，跨设备混淆）。
修复方向：后端按 sensor_code 特判（DO 反向、PH 区间、其余正向），quality 接口在无 deviceId 时改为按设备聚合后再平均（或前端强制传设备）。

### J-7 异常处理粗糙（H13）
GlobalExceptionHandler 兜底返回 `"系统异常: " + e.getMessage()`——SQL 语法错误、类名、堆栈信息直接暴露给客户端；所有错误 HTTP 状态码恒为 200（前端仅靠 code 字段，且 blob 请求连 code 都没有——见 I-3）。修复：兜底改为固定文案（"系统繁忙，请稍后重试"）+ 详细日志；错误响应带合理 HTTP 状态（400/401/500）。

### J-8 密码与凭据明文（H14）
admins/users 表 password 明文（种子为 "123456"）；application.properties 含真实 MySQL 密码（root/124102）且已随仓库分发（部署手册 docker-compose 中也明文）。修复方向：BCrypt（spring-security-crypto 单依赖即可，无需引入完整 Security）+ 登录兼容双校验；DB 密码改环境变量注入。

### J-9 告警无通知渠道（H19）
告警仅落库 + WS 站内推送；无人盯着页面时告警即被淹没。低成本增强：告警写入后调用钉钉/企业微信/Server酱 Webhook（一个配置项 + 一个方法），属"最小改动最大提升"候选。

---

## K. 低风险问题（🟡 顺手清理）

1. **前端重复代码（H15）**：`typeName/typeTag/statusName/fmtTime/sensorName/colorOf/smoothValues` 等函数在 Login 外的 4~6 个页面重复定义；Dashboard 与 History 各有一套颜色 palette。建议抽 `utils/format.js`、`utils/color.js`（纯新增文件，不改业务）。
2. **History 多指标筛选缺陷（H16）**：选中 ≥2 个指标时表格与导出均不传 sensorCode → 返回全部指标数据，页面标签与实际数据不符。
3. **演示数据脚本风险（H17）**：generate_demo_data.sql 开头 `DELETE FROM monitor_data; DELETE FROM alerts;` 与注释"保留真实上报数据"矛盾——按部署手册执行会清空真实数据。建议加提示或改为按表前缀清理。
4. **CORS（H18）**：后端无 CORS 配置；若前端不通过 nginx 同域反代而直接跨域调用将被浏览器拦截（当前 dev 走 Vite 代理所以正常）。
5. **死代码（H20）**：前端 getOnlineCount/reportData/getLatestData/getDeviceRanking 无页面使用；后端 /api/data/latest、/api/stats/device-ranking、/api/devices/online/count 无调用方（可保留作扩展，但应写注释）。
6. **教学残留（H21）**：demos/web 的 /hello、/user、/save_user、/html、/javabeat 端点与 static/index.html（"hello word"）可被匿名访问（无害但暴露项目是课程作业）；建议删除或加开关（属"清理"而非"改功能"）。
7. **JDK 组合（H22）**：pom target 1.8 + 运行在 JDK 21 + Spring Boot 2.6.13（官方支持至 JDK 17）；实测可运行（app.log 无异常），但属于非官方组合，Spring 升级或启用新特性时有兼容风险。当前**不建议动**。
8. **N+1 查询（H23）**：devices/{id}/latest 按"先查 DISTINCT sensor_code 再逐码查最新"（1+N）；StatsController.deviceRanking 逐行 selectById（N+1）。数据量小时无感，量大后明显。
9. **文档与仓库不一致（H24）**：部署手册给出 nginx.conf / docker-compose.yml 示例但仓库根目录并无这两个文件（Docker 方式需自行创建）。
10. **设备删除死胡同（H25）**：删除被"存在监测数据"阻止且无清理入口；演示数据灌入后 3 台演示设备永不可删。建议提供"删除并清理数据"二次确认选项（后端加一个接口或在删除接口增加 force 参数）。
11. **告警处理人可伪造（H26）**：handleUser 由请求参数传入，前端写死 "admin"；接入真实鉴权后应取会话用户。
12. **细节杂项**：Dashboard 无 deviceId 时 quality 全站混合（并入 J-6）；devices 表单无 region_id（并入 J-2）；登录页预填默认账号密码（演示便利 vs 安全，建议保留但加提示）；前端请求失败大量静默 catch（拦截器已提示，但部分场景无反馈）。

---

## L. 推荐改造顺序

> 原则：不升级技术栈、不重构、不删功能、不动数据库结构；全部为**新增/修改单个文件**级别的小改动。

| 阶段 | 目标 | 改动项 | 涉及文件 | 工作量 |
|---|---|---|---|---|
| **P0 止血**（先做） | 恢复损坏功能、堵住数据破坏口 | ① CSV 导出：拦截器对 blob 直接放行<br>② 登录拦截器：内存 token 校验 /api/**（放行 login）<br>③ 停用/隔离 AdminsCtrl 写端点与 demos 教学端点 | utils/request.js、新增 AuthInterceptor、AuthController、AdminsCtrl | 0.5~1 天 |
| **P1 数据可信** | 让"在线数/告警/评分"反映真实 | ④ 离线检测定时任务（超时置 0 + WS 广播）<br>⑤ 阈值新增查重（同设备+同指标拒绝重复）<br>⑥ 质量评分修正（DO 反向、PH 区间特判）<br>⑦ Dashboard WS 消息节流（1~2s debounce） | 新增 DeviceStatusJob、ThresholdsController、StatsController、Dashboard.vue | 1~2 天 |
| **P2 容量与健壮** | 长期运行不劣化 | ⑧ monitor_data/alerts 保留期清理任务（可配）<br>⑨ 异常兜底脱敏 + 错误 HTTP 状态<br>⑩ 历史查询默认时间范围（前端） | 新增 DataRetentionJob、GlobalExceptionHandler、History.vue/Dashboard.vue | 1 天 |
| **P3 功能增值** | 最小改动获得最大功能提升（见下） | ⑪~⑮ | 见下 | 2~4 天 |

---

## 附：保持现有架构情况下，最小改动获得最大功能提升的改造方案

> 目标：**不动表结构、不动技术栈、不动现有代码路径**，以"新增文件 + 单点修改"为主，按投入产出排序。

### 方案一（0.5 天，修复优先——先恢复"应有"的功能）
1. **修复 CSV 导出**：request.js 拦截器增加 `if (response.config.responseType === 'blob') return response.data` —— 一行恢复现有功能（当前导出按钮是坏的）。
2. **最小鉴权**：新增 `AuthInterceptor`（Spring HandlerInterceptor，登录时 token 写入 ConcurrentHashMap 并带过期时间），注册拦截 /api/**、放行 /api/auth/login；同时把 /admins、/hello、/user 等残留端点排除在放行外。**收益：全部写接口从"裸奔"变为"必须登录"，消除数据被任意破坏的最大风险。**

### 方案二（1 天，数据可信度——让大屏与统计说真话）
3. **离线检测**：新增 `DeviceStatusJob`（@Scheduled 60s）：`last_report_time < now - 3×interval` 且 status=1 → 置 0 并 WS 广播设备状态消息。**收益：在线数、空间分布颜色、统计卡立即恢复真实。**
4. **阈值查重 + 评分修正**：ThresholdsController.add 前查同作用域同指标是否已存在（提示覆盖）；StatsController.quality 对 DO（越低越差反向）与 PH（区间）特判。**收益：告警配置不再出现"双配置随机命中"，质量分不再把好水质判为超标。**
5. **WS 节流**：Dashboard 中 WS data 处理改为 1.5s debounce 合并后统一刷新趋势/空间图。**收益：请求量下降约 90%，大屏更流畅，后端压力大减。**

### 方案三（1~1.5 天，容量治理——保证长期可运行）
6. **数据保留策略**：新增 `DataRetentionJob`（@Scheduled 每日一次，保留天数可配，默认 7 天）：删除过期 monitor_data 与已处理 alerts。**收益：数据表体积封顶，历史/趋势查询保持毫秒级，规避"1 个月后变慢"的必然劣化。**
7. **异常脱敏**：GlobalExceptionHandler 兜底改为固定文案 + 详细日志，错误附带合理 HTTP 状态码。**收益：不再向客户端泄漏 SQL/类信息。**

### 方案四（2~3 天，功能增值——用现有数据资产换新能力）
8. **激活区域管理（复用既有表/DAO/Service）**：新增 RegionsController（列表/树），Devices 表单加区域下拉（region_id），Dashboard 空间分布改为按区域分组展示。**收益：休眠的 regions 资产被激活，"设备空间分布"从抽象坐标升级为真实区域视图——代码量约 200 行。**
9. **告警通知（新增独立小模块）**：alert 写入后调用可配置 Webhook（钉钉/Server酱），未处理告警超过 N 分钟可重复提醒。**收益：告警从"站内可见"升级为"主动触达"，这是环境监测系统的核心价值点。**
10. **登录打通 users 表 + 角色菜单**：登录改为兼容 admins/users 双表（或迁至 users），role 控制菜单显隐（前端 router meta + 后端拦截器）。**收益：用户管理页从"摆设"变为真实权限体系，与现有 UI 零冲突。**

### 不建议做（本阶段）
- ❌ Service 层重构（现有 6 组样板 Service 维持现状，新业务再走 Service）
- ❌ 引入 Redis/消息队列/JWT/Spring Security 全家桶（超出"最小改动"边界，且与课程设计定位不符）
- ❌ 修改表结构（外键/唯一约束/时序分区）——如确需唯一约束，可先用业务层查重替代
- ❌ 升级 Spring Boot 3 / Vite 6（违反约束）

---

## 各审计专项结论速览（对应任务 1~16）

| 专项 | 结论 |
|---|---|
| 1 前端目录结构 | 标准 Vite+src 布局，单 api/index.js 聚合层，views 7 页面，结构清晰（§B） |
| 2 后端目录结构 | ctrl/service(impl)/dao/bean/common/config/ws/exception 分层齐全，混入教学残留包（§C） |
| 3 数据库表结构 | 8 表字段与实体一一对应，索引基本合理（§D） |
| 4 Controller→Service→DAO→DB | 除 report 链路外全部 Controller 直连 DAO，Service 层空转（§C） |
| 5 前端 API→页面调用链 | 30 个封装全部与后端匹配；4 个封装无页面使用（§B） |
| 6 WebSocket 数据流 | 单端点全量广播，事务内发送，前端 3s 重连无心跳（§E） |
| 7 数据模拟器 | @Scheduled 5s/轮，按类型生成，PM25 5% 峰值，排除停用设备（§F/G） |
| 8 阈值与告警机制 | 设备级→全局两级，WARN/ALARM，30 分钟去重，内联于上报事务（§F） |
| 9 设备状态机制 | 仅"置在线"，无离线检测，状态不可逆失真（I-4） |
| 10 Dashboard 数据来源 | 4 个统计接口 + 2 个数据接口 + WS，轮询与推送并存（§G 明细表） |
| 11 前后端接口一致性 | **无路径/参数不一致**（30 个调用全部命中）；行为不一致 1 处：CSV 导出（I-3） |
| 12 数据库字段冗余 | 无明显冗余；last_report_time 为可推导缓存字段（合理）；regions 表整体休眠（J-2） |
| 13 重复代码 | 后端：6 组 Service/Impl 样板雷同、latest 逻辑两处实现；前端：工具函数 4~6 处重复（H15） |
| 14 异常处理 | 有全局处理器但兜底泄漏内部信息、HTTP 状态恒 200（J-7） |
| 15 权限与登录 | 形式登录 + 零鉴权 + 明文密码 + 双用户体系（I-1/I-2/J-1/J-8） |
| 16 技术债务 | 认证缺失、数据无治理、WS 事务内广播、JDK/Spring 非官方组合、文档与仓库不一致等（H 总表） |

---

*本报告基于 2026-08-25 全量静态走读生成；审计过程未修改任何业务代码、未修改数据库。CSV 导出断链（I-3）为静态推导结论（后端当前未运行，未做运行时复现），修复方案已给出。*
