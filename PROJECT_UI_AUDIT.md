# PROJECT_UI_AUDIT — 环境监测管理系统前端审计

> 生成时间：2026-08-25
> 目的：为「环境数据驾驶舱」高级化视觉重构提供完整基线。**所有业务逻辑、API、数据结构、核心功能与用户流程在重构中保持不变。**

---

## 1. 当前技术栈

| 层 | 技术 | 版本 | 说明 |
|---|---|---|---|
| 前端框架 | Vue 3（Composition API, `<script setup>`） | 3.5.x | 全部页面用 setup 语法 |
| 构建 | Vite | 5.4.x | 别名 `@` → `src` |
| UI 组件库 | Element Plus | 2.8.x | 全局注册 + zh-cn locale |
| 图标 | @element-plus/icons-vue | 2.3.x | 全局注册为组件 |
| 图表 | ECharts | 5.5.x | Dashboard 大屏 + History 趋势 |
| 状态管理 | Pinia | 2.2.x | 仅 user store |
| 路由 | Vue Router 4 | 4.4.x | createWebHistory + 登录守卫 |
| HTTP | Axios | 1.7.x | baseURL `/api`，拦截器统一处理 |
| 实时通信 | 原生 WebSocket | — | `/ws/notify`，3s 断线重连 |
| 样式 | SCSS + 原生 CSS | sass 1.80 | tokens.css + element-override.scss |
| 后端 | Spring Boot 2.6.13 + MyBatis-Plus 3.4.1 | — | :8080，MySQL 8（库 nep） |

## 2. 前端目录结构

```
nepsystem-web/src/
├── api/index.js          # 全部 API 封装（auth/devices/data/alerts/thresholds/stats/users/sensors）
├── layout/MainLayout.vue # 布局：固定侧边栏(230px) + 顶栏 + 主区
├── router/index.js       # /login + 6 个子路由 + 登录守卫
├── store/user.js         # token/adminCode/adminId（localStorage 持久化）
├── styles/tokens.css     # 设计令牌（苹果风 × shadcn 混合体系，浅色为主）
├── styles/element-override.scss  # Element Plus 主题覆盖
├── utils/request.js      # axios 实例 + 拦截器（401 跳登录）
├── utils/ws.js           # WebSocket 封装（onWSMessage 订阅）
└── views/
    ├── Login.vue         # 登录页
    ├── Dashboard.vue     # 实时监测大屏（深色）
    ├── History.vue       # 历史数据（趋势 + 明细 + CSV 导出）
    ├── Devices.vue       # 设备管理 CRUD
    ├── Alerts.vue        # 告警中心（级别/状态/处理）
    ├── Thresholds.vue    # 阈值设置
    └── Users.vue         # 用户管理
```

## 3. 路由表

| 路径 | 页面 | meta.title | 说明 |
|---|---|---|---|
| /login | Login | 登录 | 无守卫放行 |
| / (MainLayout) | — | — | 登录后布局容器 |
| /dashboard | 实时监测 | 实时监测 | 默认首页（redirect） |
| /history | 历史数据 | 历史数据 | 支持 `?deviceId=&sensorCodes=` 预筛选 |
| /devices | 设备管理 | 设备管理 | 支持 `?status=` 预筛选 |
| /alerts | 告警中心 | 告警中心 | 支持 `?level=&status=&deviceId=` 预筛选 |
| /thresholds | 阈值设置 | 阈值设置 | — |
| /users | 用户管理 | 用户管理 | — |
| * | 重定向 | — | → /dashboard |

守卫逻辑：无 `nep_token` 访问非 /login → 跳 /login；有 token 访问 /login → 跳 /dashboard。

## 4. 核心组件与功能

| 组件/页面 | 功能 | 数据来源 |
|---|---|---|
| MainLayout | 侧边栏菜单、顶栏标题、未处理告警徽标(30s轮询)、退出登录 | /alerts/unhandled |
| Dashboard | 4 统计卡(30s轮询)、设备切换、Gauge 仪表盘×N、实时趋势(40点/平滑)、设备最新值卡、最新告警、近7天告警柱状图(60s轮询)、WebSocket 实时刷新、组件点击跳转 | /stats/overview、/devices/page、/sensors、/devices/{id}/latest、/data/history、/alerts/unhandled、/alerts/stat、WS |
| History | 设备/多指标/时间筛选、小时聚合趋势图、明细分页表格、CSV 导出 | /devices/page、/sensors、/data/trend、/data/history、/data/export |
| Devices | 关键字/类型/状态筛选、CRUD 对话框 | /devices/page、/devices(POST/PUT/DELETE) |
| Alerts | 级别/状态/设备筛选、处理告警、未处理数 | /alerts/page、/alerts/{id}/handle、/alerts/unhandled |
| Thresholds | 全局/设备级阈值 CRUD、启用开关 | /thresholds(GET/POST/PUT/DELETE) |
| Users | 关键字筛选、CRUD、启停 | /users/page、/users(POST/PUT/DELETE)、/users/{id}/status |
| Login | 账号密码登录、表单校验 | /auth/login |

## 5. 数据模型（后端 bean，重构不可触碰）

- **Admins**：adminId/adminCode/password/…（登录）
- **Users**：id/username/password/nickname/role/status
- **Devices**：id/deviceCode/deviceName/type(AIR|WATER|NOISE)/regionId/location/status(0离线|1在线|2停用)
- **Sensors**：id/sensorCode/sensorName/unit/deviceType/minRange/maxRange/standardMax
- **MonitorData**：id/deviceId/sensorCode/value/reportTime
- **Alerts**：id/deviceId/sensorCode/level(WARN|ALARM)/alertValue/message/status(0未处理|1已处理)/handleUser/handleTime/createTime
- **Thresholds**：id/deviceId(null=全局)/sensorCode/warnMin/warnMax/alarmMin/alarmMax/enabled
- **Regions**：id/regionName/…（设备分区，前端未直接使用）

## 6. API 清单（baseURL /api）

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | /auth/login、/auth/logout | 认证 |
| GET | /devices/page、/devices/{id}/latest、/devices/online/count | 设备 |
| POST/PUT/DELETE | /devices | 设备 CRUD |
| POST | /data/report | 数据上报（模拟器） |
| GET | /data/latest、/data/history、/data/trend、/data/export | 数据查询 |
| GET | /alerts/page、/alerts/unhandled、/alerts/stat | 告警查询 |
| PUT | /alerts/{id}/handle?handleUser= | 处理告警 |
| GET/POST/PUT/DELETE | /thresholds | 阈值 |
| GET | /stats/overview、/stats/quality、/stats/device-ranking | 统计 |
| GET | /users/page、/sensors | 用户/指标字典 |
| POST/PUT/DELETE | /users、/users/{id}/status | 用户管理 |
| WS | /ws/notify | data/alert 两类消息实时推送 |

## 7. 状态管理与数据流

- Pinia user store：token + adminCode + adminId（localStorage 键 `nep_token` / `nep_adminCode` / `nep_adminId`）
- axios 请求拦截：`Authorization: Bearer <token>`；响应拦截：code!==200 弹 ElMessage，401 清 token 跳登录
- WebSocket：`connectWS()` 全局单例，`onWSMessage(handler)` 订阅；Dashboard 收到 data 消息即时更新 gauge+趋势，收到 alert 消息刷新未处理告警
- 页面间跳转：Dashboard 点击组件 → router.push(query) → 目标页 onMounted 读 query 预筛选

## 8. 可复用资产

- 设计令牌：tokens.css（字体层级/8pt 间距/圆角阴影/系统色/灰阶/深色变量）
- 组件覆盖：element-override.scss（按钮/输入/表格/对话框/下拉/分页/开关/分段控件）
- 通用类：`.apple-card`、`.page-container`、`.page-title`、`.page-subtitle`、`.tabular-nums`、`.focus-ring`
- 工具：request.js（统一请求）、ws.js（WS 订阅）、api/index.js（全接口封装）
- ECharts 封装模式：init → setOption → resize → dispose（每个页面独立管理）

## 9. 当前 UI 问题清单（重构目标）

1. **传统后台骨架**：固定 236px 侧边栏 + 60px 顶栏，占内容空间、视觉平庸
2. **浅色 Admin 感**：管理页浅灰底白卡堆叠，与「驾驶舱」定位不符
3. **卡片同质化**：设备/告警/用户/阈值页都是「标题+筛选+表格」同构卡片
4. **数据层级扁平**：所有数字同字号，无 Primary/Secondary/Tertiary 区分
5. **图表样式默认**：ECharts 默认坐标轴/网格/图例，与整体风格脱节（History 页较明显）
6. **无空间叙事**：无 Hero、无滚动叙事、无 section 节奏、无 reveal 动效
7. **无地图/空间视图**：传感器分布无空间化表达（Regions 表存在但未用）
8. **告警表达传统**：红色大卡片/表格行，缺少 typography 驱动的告警语言
9. **登录页与系统风格割裂**：浅色渐变卡片 vs 深色大屏
10. **深色/浅色双体系并存**：大屏深色、管理页浅色，缺乏统一设计语言

## 10. 改造风险与约束（红线）

| 风险 | 等级 | 对策 |
|---|---|---|
| 破坏登录/token/守卫 | 高 | 不动 router/store/request.js 逻辑，只改视觉 |
| 破坏 API 调用 | 高 | 不改 api/index.js 与任何接口参数 |
| 破坏 WS 数据流 | 高 | 只改渲染层，保持 connectWS/onWSMessage 模式 |
| 破坏 CRUD 流程 | 高 | 保留全部 el-form/el-dialog/el-table 交互语义，仅换肤 |
| ECharts 实例生命周期 | 中 | 保持 init/resize/dispose 模式，重写 option |
| 页面结构大改导致 query 预筛选失效 | 中 | 保留 useRoute().query 读取逻辑 |
| 组件库深色适配 | 中 | element-override.scss 全面覆盖深色变量 |
| 无地图库依赖 | 中 | 不引入新依赖（网络受限），用 ECharts scatter/自定义 SVG 实现传感器空间图 |
| 构建验证 | 低 | 每阶段 npm run build + 浏览器冒烟 |

## 11. 改造阶段计划

- [ ] P1 设计令牌：深色工业风 Design Token（bg/text/border/env-green 语义层）+ Element 深色覆盖
- [ ] P2 导航：MainLayout → Floating HUD（fixed 顶部漂浮胶囊导航，毛玻璃，不占内容区）
- [ ] P3 首页/实时监测：Hero(80-100vh) + Editorial 布局 + 数据三级层级 + 实时状态 + SCROLL 提示
- [ ] P4 传感器网络空间视图（ECharts scatter 抽象空间图，Region 分组，状态脉冲）
- [ ] P5 管理页适配：History/Devices/Alerts/Thresholds/Users 深色 Editorial 化
- [ ] P6 图表重设计：极简坐标轴/低对比网格/渐变填充/动画 reveal
- [ ] P7 滚动叙事：reveal/fade/scale 交互（克制），Login 页深色化
- [ ] P8 构建验证 + 全量提交
