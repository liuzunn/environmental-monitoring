# ENVISION · 环境监测保护系统

> 多合一环境监测平台 —— 空气 / 水质 / 噪声 三类监测场景开箱即用
> 前端采用「环境数据驾驶舱」设计语言：深色 · 极简 · 空间感 · Editorial 排版

[![Vue 3](https://img.shields.io/badge/Vue-3.5-42b883?logo=vuedotjs&logoColor=white)](https://vuejs.org)
[![Vite](https://img.shields.io/badge/Vite-5-646cff?logo=vite&logoColor=white)](https://vitejs.dev)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.8-409eff?logo=element&logoColor=white)](https://element-plus.org)
[![ECharts](https://img.shields.io/badge/ECharts-5.5-aa344d)](https://echarts.apache.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6-6db33f?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479a1?logo=mysql&logoColor=white)](https://www.mysql.com)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

## 📌 项目简介

**ENVISION** 是一套完整的物联网级环境监测系统，覆盖 **空气质量 / 水质 / 噪声** 三类场景：

- **数据采集**：内置数据模拟器（5 秒自动上报），预留 HTTP / MQTT 硬件接入接口
- **实时监测**：WebSocket 推送 + Gauge 仪表盘 + 平滑实时趋势曲线
- **智能告警**：预警（WARN）/ 报警（ALARM）两级阈值引擎，30 分钟防重复
- **数据分析**：小时级趋势聚合、历史查询、CSV 导出
- **设备管理**：设备 CRUD、在线状态、空间分布可视化
- **用户体系**：登录认证、角色权限、用户启停管理

> 前端采用自研 **ENVISION 设计系统**：深色工业风 Design Token、Floating HUD 漂浮导航、Hero 首屏叙事、滚动 reveal 动效，让系统呈现「环境数据驾驶舱」而非传统后台。

## ✨ 功能特性

| 模块 | 功能 |
|---|---|
| 🛰️ 实时监测 | 100vh Hero 首屏、空气质量指数、Gauge 仪表盘、40 点平滑趋势曲线（WebSocket 实时刷新） |
| 🗺️ 传感器网络 | 空间分布视图（在线脉冲 / 超标高亮 / 选中聚焦）、设备最新数据、点击节点切换 |
| 📈 历史数据 | 多指标趋势图（小时级聚合）、明细分页、CSV 导出、空态自适应 |
| 🚨 告警中心 | WARN / ALARM 两级告警、未处理计数（HUD 角标）、一键处理 |
| ⚙️ 阈值设置 | 全局默认 / 设备级阈值、启用开关 |
| 🖥️ 设备管理 | CRUD、类型（空气/水质/噪声）、状态管理、安装位置 |
| 👥 用户管理 | CRUD、角色（USER/ADMIN）、启停 |
| 🔔 实时推送 | WebSocket 断线自动重连（3s），告警实时通知 |

## 🛠️ 技术栈

**前端** Vue 3 · Vite 5 · Element Plus · ECharts 5 · Pinia · Vue Router · Axios · WebSocket

**后端** Spring Boot 2.6 · MyBatis-Plus 3.4 · MySQL 8 · WebSocket（阈值引擎 + 消息广播）

**工具** JDK 21 · Maven 3.9 · Node 22

## 🚀 快速开始

### 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21+ | 已配置 JAVA_HOME |
| Maven | 3.9+ | 建议阿里云镜像 |
| Node.js | 22+ | 前端构建 |
| MySQL | 8.0 | 本地 3306 |

### 1. 初始化数据库

```bash
# 建库建表 + 示例数据（3 设备 / 8 指标 / 2 阈值）
mysql -uroot -p < nepsystem/src/main/resources/db/init.sql

# 可选：灌入 3 天演示数据（趋势图更饱满）
mysql -uroot -p < nepsystem/src/main/resources/db/generate_demo_data.sql
```

### 2. 配置后端

```bash
cd nepsystem/src/main/resources
cp application.example.properties application.properties
# 修改 application.properties 中的数据库密码
```

> `application.properties` 已加入 .gitignore，不会提交到仓库。

### 3. 启动后端

```bash
cd nepsystem
mvn clean package -DskipTests
java -jar target/nepsystem-0.0.1-SNAPSHOT.jar
# 验证: http://localhost:8080/api/stats/overview
```

### 4. 启动前端

```bash
cd nepsystem-web
npm install
npm run dev
# 浏览器打开 http://localhost:5173
```

### 5. 登录

| 账号 | 密码 | 角色 |
|---|---|---|
| admin | 123456 | 管理员 |

## 📁 目录结构

```
drenepsystem/
├── nepsystem/                  # Spring Boot 后端
│   ├── src/main/java/org/nep/nepsystem/
│   │   ├── bean/               # 数据模型（8 表实体）
│   │   ├── dao/                # MyBatis-Plus 数据访问
│   │   ├── service/            # 业务层（阈值引擎/告警）
│   │   ├── ctrl/               # REST API 控制器
│   │   ├── config/             # WebSocket/MQTT/模拟器配置
│   │   ├── ws/                 # WebSocket 消息广播
│   │   └── simulator/          # 数据模拟器（5s 上报）
│   └── src/main/resources/
│       ├── db/init.sql         # 建库建表 + 示例数据
│       └── application.example.properties
├── nepsystem-web/              # Vue 3 前端
│   └── src/
│       ├── views/              # 6 大页面（Dashboard/History/Devices/...）
│       ├── layout/             # Floating HUD 漂浮导航
│       ├── styles/             # ENVISION Design Token + Element 覆盖
│       ├── api/                # 全部 API 封装
│       ├── utils/              # axios / WebSocket 封装
│       └── router/ store/      # 路由守卫 / Pinia 状态
├── docs/                       # 设计文档（接口/数据库/部署）
├── PROJECT_UI_AUDIT.md         # UI 重构审计（技术栈/API/风险/阶段）
└── ENVISION-DESIGN.md          # 设计体系（设计令牌/布局/组件/动效规范）
```

## 📡 API 概览（baseURL /api）

| 分组 | 端点 | 说明 |
|---|---|---|
| 认证 | POST `/auth/login` `/auth/logout` | Bearer Token |
| 设备 | GET/POST/PUT/DELETE `/devices`、`/devices/{id}/latest` | 设备管理 + 最新数据 |
| 数据 | GET `/data/history` `/data/trend` `/data/export` | 查询 / 聚合 / CSV |
| 告警 | GET `/alerts/page` `/alerts/unhandled` `/alerts/stat`、PUT `/alerts/{id}/handle` | 查询 / 处理 / 统计 |
| 阈值 | GET/POST/PUT/DELETE `/thresholds` | 全局与设备级阈值 |
| 统计 | GET `/stats/overview` `/stats/quality` `/stats/device-ranking` | 总览 / 质量评分 / 排行 |
| 实时 | WS `/ws/notify` | data / alert 消息推送 |

## 🧪 测试

```bash
# 后端单元测试（阈值告警 + 事务回滚）
cd nepsystem && mvn test

# 端到端冒烟（10 模块 36 项断言）
cd nepsystem && powershell -ExecutionPolicy Bypass -File e2e-test.ps1
```

## 📚 文档

- [接口文档](./docs/接口文档.md)
- [数据库设计](./docs/数据库设计.md)
- [部署手册](./docs/部署手册.md)
- [项目说明](./docs/项目说明.md)
- [ENVISION 设计体系](./ENVISION-DESIGN.md)
- [UI 重构审计](./PROJECT_UI_AUDIT.md)
- [MQTT 硬件接入](./nepsystem/README-MQTT.md)

## 🔑 安全说明

- 数据库密码、令牌等敏感配置一律放入本地 `application.properties`（已被 gitignore 忽略），仓库仅提供 `application.example.properties` 模板
- 登录令牌存储于 localStorage（键 `nep_token`），请求头 `Authorization: Bearer <token>`

## 📄 License

MIT License — 仅限学习交流使用
