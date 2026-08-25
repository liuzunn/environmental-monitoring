# 环境监测保护系统（Environmental Monitoring & Protection System）

多合一通用环境监测平台：覆盖**空气质量 / 水质 / 噪声**三类监测场景，采用模拟数据优先、预留 MQTT 硬件接口的设计，开箱即用。

> 前端视觉遵循苹果风设计规范（源自 emilkowalski/skills 的 apple-design 设计理念），详见 [苹果风设计规范.md](./苹果风设计规范.md)。

---

## 功能清单

| 模块 | 功能 |
|---|---|
| 实时监测大屏 | 统计总览、Gauge 仪表盘、实时曲线（WebSocket 推送）、设备最新值、超标红色高亮、告警滚动 |
| 历史数据 | 多指标趋势图（小时级聚合）、明细分页、CSV 导出 |
| 设备管理 | 设备 CRUD、类型（空气/水质/噪声）、状态管理、在线统计 |
| 告警中心 | 阈值告警（预警 WARN / 报警 ALARM 两级）、30 分钟防重复、处理流程、近 7 天统计 |
| 阈值设置 | 全局/设备级阈值配置、启用开关 |
| 用户管理 | 用户 CRUD、角色、启停 |
| 数据采集 | 内置模拟器（每 5 秒自动上报）、HTTP 上报接口、MQTT 接入预留（见 nepsystem/README-MQTT.md） |

## 技术栈

- **后端**：Spring Boot 2.6.13 · MyBatis-Plus 3.4.1 · MySQL 8.0 · WebSocket
- **前端**：Vue 3 · Vite 5 · Element-Plus · ECharts 5 · Pinia · Vue Router · Axios
- **工具**：JDK 21 · Maven 3.9 · Node 22 · VSCode

## 系统架构

```
┌─ 数据源层 ──────────────────────────────┐
│ 模拟器(定时5s) | 硬件预留(HTTP/MQTT)      │
└──────────────┬──────────────────────────┘
               ▼
┌─ 后端 Spring Boot :8080 ────────────────┐
│ 设备/数据/告警/阈值/统计/用户 API         │
│ 阈值引擎 + WebSocket 广播 (/ws/notify)   │
└──────────────┬──────────────────────────┘
               ▼
┌─ 数据层 MySQL(nep) ─────────────────────┐
│ admins users regions devices sensors    │
│ monitor_data thresholds alerts (8 表)    │
└─────────────────────────────────────────┘
               ▼
┌─ 前端 Vue3 :5173 ───────────────────────┐
│ 登录 → 大屏/历史/设备/告警/阈值/用户      │
│ /api 代理 + /ws 代理                    │
└─────────────────────────────────────────┘
```

## 目录结构

```
drenepsystem/
├── nepsystem/              # Spring Boot 后端
│   ├── src/main/java/org/nep/nepsystem/
│   │   ├── bean/ dao/ service/ ctrl/     # 分层架构
│   │   ├── common/ exception/ config/    # 通用封装与配置
│   │   └── ws/ simulator/               # WebSocket 与模拟器
│   ├── src/main/resources/db/
│   │   ├── init.sql                      # 建库建表 + 示例数据
│   │   └── generate_demo_data.sql        # 3 天演示数据
│   ├── smoke-test.bat                    # 冒烟测试
│   └── README-MQTT.md                    # 硬件接入说明
├── nepsystem-web/          # Vue3 前端
│   └── src/views/                        # 6 大页面
├── 苹果风设计规范.md        # 前端视觉规范
├── 开发流程提示词_环境监测保护系统.md  # 分段式开发总文档
└── docs/                   # 设计文档（数据库/接口/部署/毕设说明）
```

## 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | JAVA_HOME 已配置 |
| Maven | 3.9.x | 阿里云镜像 |
| Node.js | 22 | 前端构建 |
| MySQL | 8.0 | 本地 3306，账号 root |

## 快速启动（约 10 分钟）

### 1. 初始化数据库

```bash
# 首次执行：建库建表 + 示例数据（3 设备 / 8 指标 / 2 阈值）
mysql -uroot -p124102 < nepsystem/src/main/resources/db/init.sql
# 可选：灌入 3 天演示数据（趋势图更饱满）
mysql -uroot -p124102 < nepsystem/src/main/resources/db/generate_demo_data.sql
```

> 若 root 密码不同，请同步修改 `nepsystem/src/main/resources/application.properties` 中 `spring.datasource.password`。

### 2. 启动后端

```bash
cd nepsystem
mvn clean package -DskipTests
java -jar target/nepsystem-0.0.1-SNAPSHOT.jar
# 验证: http://localhost:8080/api/stats/overview
```

### 3. 启动前端

```bash
cd nepsystem-web
npm install --registry=https://registry.npmmirror.com
npm run dev
# 浏览器打开 http://localhost:5173
```

### 4. 登录

- 管理员：`admin / 123456`

## 默认演示数据

| 设备 | 编号 | 类型 |
|---|---|---|
| 教学楼A栋空气质量站 | DEV-AIR-001 | 空气（TEMP/HUMI/PM25/CO2） |
| 人工湖水质监测站 | DEV-WTR-001 | 水质（PH/浊度/溶解氧/TEMP） |
| 操场噪声监测站 | DEV-NSE-001 | 噪声（NOISE） |

阈值：PM25 预警 75 / 报警 150；NOISE 预警 65 / 报警 75。

## 测试

```bash
# 后端单元测试（阈值告警 + 事务回滚）
cd nepsystem && mvn test

# 冒烟测试（后端运行时）
cd nepsystem && smoke-test.bat
```

## 硬件接入

真实硬件（ESP32/Arduino）可通过 HTTP 或 MQTT 接入，详见 [nepsystem/README-MQTT.md](./nepsystem/README-MQTT.md)。
