# environmental-monitoring · 环境监测保护与公众监督系统

一套后端 + 一个数据库，支撑 **四端** 完整业务闭环：环境数据实时监测（NEPV）→ 公众监督（NEPS）→ 监管处置（NEPM）→ 网格员现场检测（NEPG）。

## 1. 项目简介

系统覆盖三类环境指标（空气 PM2.5/CO2/温湿度、水质 pH/浊度/溶解氧、噪声），并实现“公众发现环境问题 → 管理员审核派单 → 网格员现场检测（六项污染物自动计算 AQI）→ 管理员核实关闭 → 公众查看处理结果”的监督闭环；每次状态变化写入审计时间线，WebSocket 定向通知实时触达，NEPV 大屏聚合业务统计。

## 2. 四端介绍

| 端 | 全称 | 定位 | 登录账号（演示） | 端口 |
|---|---|---|---|---|
| **NEPS** | 公众监督端 | 公众服务平台：环境查询、我要监督、我的监督、消息通知 | zhang_san / 123456 | 5174 |
| **NEPM** | 监管端 | 企业级管理平台：工作台、事件审核/派单、网格/网格员/任务管理 | admin / 123456 | 5175 |
| **NEPG** | 网格员端 | AQI 检测工作台：任务卡片、现场检测（六项→AQI）、位置示意地图 | wang_qiang / 123456 | 5176 |
| **NEPV** | 环境决策大屏 | 决策驾驶舱：实时监测大屏、空间态势、历史/告警/阈值、监管统计区块 | admin / 123456 | 5173 |

## 3. 系统架构

```text
NEPS / NEPM / NEPG / NEPV（Vue3 独立应用）
        │  HTTP /api（Vite 代理或 nginx 反代）    │ WS /ws/notify
        ▼                                        ▼
   Spring Boot 2.6.13 共享后端（8080）
   ctrl → service → dao（MyBatis-Plus）→ MySQL 8（库 nep，17 表）
```

关键设计：统一响应 {code,message,data}；监督事件/任务双状态机（8 态）收敛于 Service（前端不可传状态）；每次流转写 event_status_log（审计时间线）；WebSocket 仅做提醒（真实数据以 REST+DB 为准）；数据库仅追加式迁移（v2-v6）。

## 4. 技术栈

- 后端：Java 8 语法 / Spring Boot 2.6.13 / MyBatis-Plus 3.4.1 / Spring WebSocket
- 前端：Vue 3.5 / Vite 5 / Pinia / Vue Router 4 / Element Plus / ECharts 5 / Sass
- 数据：MySQL 8（utf8mb4/InnoDB）；构建：Maven + npm；运行：JDK 21 / Node 18+

## 5. 数据库

库 `nep` 17 张表：认证（admins/users）、基础（regions/devices/sensors）、监测（monitor_data/alerts/thresholds/data_quality）、监督业务（supervision_event/supervision_attachment/event_status_log/event_evaluation）、网格任务（grids/grid_member/inspection_task/inspection_record），18 个外键。初始化脚本见 `nepsystem/src/main/resources/db/`（init.sql → migration_v2~v6.sql → demo_business_chain.sql）。详见 `docs/02-数据库设计说明书.md`。

## 6. API

80 个后端端点，四端 84 个前端封装（契约一致，Long ID 字符串化防精度丢失）。统一前缀 `/api`，身份头 `X-User-Id`（公众/网格员）/`X-Admin-Id`（管理员）+ `Authorization: Bearer token`（登录签发，24h 有效，登出失效）。接口文档：`docs/03-接口设计与接口文档.md`、`docs/api/*`。

## 7. Demo 运行方法

演示数据已内置（真实业务链：公众张三投诉 → ENV-20260902-001 已闭环、进行中事件、网格/设备/检测记录）。推荐演示路线：NEPS 提交 → NEPM 审核派单 → NEPG 接单检测（自动 AQI）→ NEPM 核实关闭 → NEPS 收到“已处理完成” → NEPV 大屏统计更新。

## 8. 项目启动方法

```bash
# 1) 数据库（首次）
mysql -uroot -p < nepsystem/src/main/resources/db/init.sql
mysql -uroot -p < nepsystem/src/main/resources/db/migration_v2.sql
mysql -uroot -p < nepsystem/src/main/resources/db/migration_v3.sql
mysql -uroot -p < nepsystem/src/main/resources/db/migration_v4.sql
mysql -uroot -p < nepsystem/src/main/resources/db/migration_v5.sql
mysql -uroot -p < nepsystem/src/main/resources/db/migration_v6.sql
mysql -uroot -p < nepsystem/src/main/resources/db/demo_business_chain.sql
# 2) 后端（连接串见 application.properties，密码建议环境变量 DB_PASSWORD）
cd nepsystem && mvn clean package -DskipTests && java -jar target/nepsystem-0.2.0.jar
# 3) 四个前端（各开一个终端）
cd nepsystem-web && npm install && npm run dev        # NEPV :5173
cd nepsystem-web-neps && npm install && npm run dev   # NEPS :5174
cd nepsystem-web-nepm && npm install && npm run dev   # NEPM :5175
cd nepsystem-web-nepg && npm install && npm run dev   # NEPG :5176
```

完整说明见 `docs/08-系统部署与运行说明.md` 与 `docs/VSCode启动指南.md`。

## 9. 测试

后端 `mvn test`（76 用例全绿：业务/权限/越权/状态机/WebSocket/统计/事务回滚）；前端 `npm run build`（四端）。测试与缺陷记录：`docs/06-系统测试报告.md`、`docs/07-Bug缺陷跟踪报告.md`。

## 10. 我的个人贡献

**【项目总负责人】**

- 负责项目总体架构与四端架构设计（NEPS 公众监督 / NEPM 监管 / NEPG 网格员 / NEPV 决策大屏）；
- 前后端架构：共享后端分层（Controller→Service→Mapper）、统一响应契约、四端独立前端与差异化视觉；
- 数据库设计：17 表业务建模、18 外键、最小增量迁移（v2-v6）与演示数据链；
- NEPV 大屏开发：决策驾驶舱 UI/UX 与监管统计联动（基于真实业务数据聚合）；
- 四端接口联调：80 端点契约核对、跨端业务链路（提交→审核→派单→检测→核实→关闭→统计）打通；
- 系统测试与 Bug 定位修复：76 项自动化测试体系、缺陷分级治理（事务/上传链路/令牌校验/异常脱敏等 P1-P2 全部修复）；
- 最终项目验收：工程文档（docs/01-08）、接口矩阵、E2E 报告与 Git 发布前安全整理。