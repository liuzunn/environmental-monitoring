# ENVISION 业务层升级报告

> 升级日期：2026-09-01 ｜ 范围：nepsystem 后端业务层（前端零改动）
> 原则落实：保留技术栈 / 保留现有 API / 保留核心表 / 保留 WebSocket / 保留 Dashboard / 不重构 / 新功能兼容旧功能 / 先分析影响范围 / 每模块跑测试
> 测试基线：升级前 3/3 通过 → 升级后 **27/27 单元/集成测试通过 + 18/18 E2E 接口验证 + WebSocket 实时链路实测通过**

---

## 1. 影响范围分析（改动前）

| 分析项 | 结论 | 应对 |
|---|---|---|
| 现有 API 契约 | 30 个前端调用全部保留原路径/参数/返回结构 | 所有旧端点零修改，新功能全部走新增端点 |
| alerts 表 | 旧字段（level/status/handle_user/handle_time）被前端 Alerts.vue、Dashboard 角标、unhandled/stat 接口依赖 | 仅**追加**新列，不删不改旧列；status 语义保持（0=未处理 1=已处理） |
| devices 表 | last_report_time/status 为健康度与离线检测的数据基础 | 不加列；离线检测只更新 status（与模拟器/上报逻辑兼容） |
| monitor_data 表 | 历史/趋势/导出/评分依赖 | 零修改 |
| WebSocket | Dashboard 消费 data/alert 两种消息 | 广播位置与消息格式保持不变 |
| 模拟器 | @ConditionalOnProperty(simulator.enabled) + @Scheduled + report() 入口 | 类名/注解/配置键全部保留，仅替换内部生成逻辑 |
| 事务边界 | report() 为单事务 | 质量/异常检测写库与上报同事务，回滚一致 |
| 测试影响 | CoreBusinessTests 断言告警级别与事务回滚 | 新增逻辑不影响其断言（回归通过） |

---

## 2. 数据库结构变更（最小迁移，migration_v2.sql）

> 执行：`mysql -uroot -p < nepsystem/src/main/resources/db/migration_v2.sql`（仅需一次；ALTER 不可重复执行）

### 2.1 alerts 表（追加 6 列 + 1 索引）

| 新列 | 类型 | 说明 |
|---|---|---|
| state | VARCHAR(20) NOT NULL DEFAULT 'WARN' | 生命周期状态：WARN/ALARM/ACKNOWLEDGED/PROCESSING/RESOLVED/NORMAL |
| ack_time | DATETIME NULL | 确认时间 |
| ack_user | VARCHAR(50) NULL | 确认人 |
| resolve_time | DATETIME NULL | 解决时间（含自动恢复） |
| resolve_user | VARCHAR(50) NULL | 解决人（SYSTEM=自动恢复） |
| duration_seconds | BIGINT NULL | 告警持续时间（解决/恢复时计算） |
| idx_state | KEY(state) | 状态过滤查询索引 |

存量数据回填：`UPDATE alerts SET state = IF(status=1, 'RESOLVED', level)`（已处理→RESOLVED，未处理→原级别）。

### 2.2 新增 data_quality 表（快照式）

| 字段 | 说明 |
|---|---|
| id BIGINT PK | 主键 |
| device_id / sensor_code | 设备与指标（sensor_code 可空=设备级问题） |
| category | QUALITY（数据质量）/ ANOMALY（统计异常） |
| issue_type | NULL_VALUE / OUT_OF_RANGE / CONSTANT_VALUE / INTERVAL_ABNORMAL / DEVICE_OFFLINE / ZSCORE / CONSECUTIVE_EXCEED / SUDDEN_CHANGE |
| severity | INFO / WARN / CRITICAL（取历史最高） |
| detail / latest_value | 描述 / 最近触发值 |
| first_seen / last_seen / occurrence_count | 首次/最近发现时间、累计次数 |
| 唯一键 (device_id, sensor_code, issue_type) | 每设备每指标每问题类型一行，避免无限膨胀 |

### 2.3 未改动的表

devices / monitor_data / sensors / thresholds / regions / users / admins：**零修改**（健康度全部基于现有字段实时计算）。

---

## 3. 模块一：告警生命周期升级

### 3.1 状态机

```text
                        确认(acknowledge)     处理(process)      解决(resolve)
新建(WARN/ALARM) ───────────────► ACKNOWLEDGED ──────► PROCESSING ──────► RESOLVED
      │                                                                      ▲
      │                    指标回到阈值范围内（超过保持窗口，默认60s）          │
      └──────────────────────────────────────────────────────────────────────┘
                                    NORMAL（自动恢复，resolve_user=SYSTEM）
```

- WARN/ALARM：新建时 state=级别（与 level 字段一致，旧语义不变）
- ACKNOWLEDGED：确认告警（ack_time/ack_user）
- PROCESSING：处理中（handle_time/handle_user，复用旧列）
- RESOLVED：解决（resolve_time/resolve_user/duration_seconds=now-create_time）
- NORMAL：指标恢复正常后自动置为已恢复（有 60s 保持窗口避免抖动，可配 alert.auto-resolve-hold-ms）
- **status 兼容同步**：RESOLVED/NORMAL → status=1；其余 → status=0（unhandled 计数、旧状态过滤完全不变）

### 3.2 新增接口

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| PUT | /api/alerts/{id}/acknowledge | body:{user?} 或 ackUser? | 确认告警 |
| PUT | /api/alerts/{id}/process | body:{user?} 或 handleUser? | 开始处理 |
| PUT | /api/alerts/{id}/resolve | body:{user?} 或 resolveUser? | 解决告警 |
| GET | /api/alerts/page | **新增可选参数 state** | 按生命周期状态过滤（level/status/deviceId 旧参数不变） |

### 3.3 流程对比

| | 修改前 | 修改后 |
|---|---|---|
| 告警产生 | 写 alerts(status=0, level) | 同上 + state=level（等价） |
| 处理 | PUT /handle → status=1 | 保留 /handle（等价 resolve）；新增 ack/process/resolve 细粒度流转 |
| 恢复 | 无（告警永久悬挂） | 指标回正常 → 自动 NORMAL + 持续时间 |
| 查询 | 按 level/status/deviceId | 新增 state 过滤（旧参数全部保留） |

**兼容性**：旧 /handle、unhandled、stat、page 行为完全不变；前端 Alerts.vue 无需修改即可继续工作。

---

## 4. 模块二：数据质量检测

### 4.1 检测项与判定规则

| 检测项 | 规则 | 严重度 | 触发时机 |
|---|---|---|---|
| NULL_VALUE | 上报项缺 sensorCode 或 value | WARN | 上报时（原逻辑是静默跳过，现在记录后跳过） |
| OUT_OF_RANGE | 超出 sensors 字典量程 [min,max] | CRITICAL | 上报时 |
| CONSTANT_VALUE | 连续 N 条数值相同（默认 5，可配 quality.constant-min-repeat） | WARN | 上报时（内存计数） |
| SUDDEN_CHANGE | 与上一条差值 > 量程跨度 50% | CRITICAL | 上报时 |
| INTERVAL_ABNORMAL | 上报间隔 > max(离线阈值/2, 2×interval) | WARN | 周期扫描（60s） |
| DEVICE_OFFLINE | 超过 3×interval（或 quality.offline-threshold-ms）未上报 | CRITICAL | 周期扫描（置 status=0） |

### 4.2 新增接口

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| GET | /api/quality/issues | deviceId?/category?/issueType?/limit? | 质量与异常问题列表（含设备名） |
| GET | /api/quality/status | deviceId | 设备质量状态 GOOD/WARNING/BAD |

**兼容性**：上报接口对缺值项的容忍行为不变（继续跳过不写库），只是额外落一条质量记录。

---

## 5. 模块三：统计异常检测（与阈值引擎并存）

| 检测项 | 方法 | 规则 | 严重度 |
|---|---|---|---|
| ZSCORE | 滚动窗口 30 条，**留一法**（统计窗口不含当前值，避免掩蔽效应） | |z| > 3（可配 anomaly.zscore-threshold），样本 ≥ 10 | CRITICAL |
| CONSECUTIVE_EXCEED | 阈值引擎配置驱动（设备级→全局，与告警同源） | 连续 ≥ 3 条超限（可配 anomaly.consecutive-min）；超报警限 CRITICAL，超预警限 WARN | CRITICAL/WARN |
| SUDDEN_CHANGE | 统计突变 | 差值 > max(3×std, 20%量程) | CRITICAL |

### 新增接口

| 方法 | 路径 | 参数 | 说明 |
|---|---|---|---|
| GET | /api/anomalies | deviceId?/sensorCode?/issueType?/limit? | 统计异常记录列表 |
| GET | /api/anomalies/summary | deviceId? | 按问题类型汇总 |

**并存关系**：阈值引擎（WARN/ALARM 告警 + WS 广播）**完全未动**；异常检测只新增 ANOMALY 类别记录，供健康度与查询使用。

---

## 6. 模块四：设备健康度

### 6.1 指标定义（全部基于现有表实时计算，无新表）

| 指标 | 计算方式 |
|---|---|
| 在线率 onlineRate | 近 7 天有 monitor_data 的天数 / 7 × 100% |
| 最近通信时间 | devices.last_report_time |
| 数据完整率 | 近 24h 已上报指标种类 / sensors 字典中该类型应上报指标种类 × 100% |
| 异常次数 | data_quality(ANOMALY) 近 7 天仍活跃记录累计触发次数 |
| 告警次数 | alerts 近 7 天条数 |
| healthScore | 0.25×在线率 + 0.25×通信分 + 0.20×完整率 + 0.15×异常分 + 0.15×告警分（通信分：3×周期内 100 / 1h 内 70 / 24h 内 40 / 否则 0；异常分=100-20×次数；告警分=100-10×次数，下限 0） |
| healthLevel | ≥80 HEALTHY / ≥60 FAIR / <60 POOR |

### 6.2 新增接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/health/devices | 全部设备健康度 |
| GET | /api/health/devices/{id} | 单台设备健康度 |

---

## 7. 模块五：增强数据模拟器（场景引擎）

### 7.1 场景能力（替代简单 random）

| 场景 | 实现 |
|---|---|
| 正常波动 | 平滑随机游走：向目标值收敛 35% + 小步噪声（非独立随机） |
| 日周期 | 各指标正弦曲线（TEMP 15 点峰值、HUMI 凌晨峰值等） |
| 早晚高峰 | PM25/CO2/NOISE 07-09 与 17-19 梯形增强（最高 +50%） |
| 突发污染 | 周期性事件（平均间隔 40 轮）：PM25/CO2/TURBIDITY/NOISE 抬升至量程 60%-80%，DO 反向滑落；抬升→维持→衰减轨迹 |
| 连续超标 | 污染维持期数值保持超限 ≥3 轮 → 自然触发阈值告警 + CONSECUTIVE_EXCEED 异常 |
| 设备离线/恢复 | 周期性离线窗口（平均 80 轮、持续 15 轮），窗口内不产生数据；恢复后自动重新上报（配合质量扫描置离线/上报置在线闭环） |
| 异常数据 | 每指标每轮 2% 概率注入：超量程 / 大突变 / 缺值 |

### 7.2 实现与兼容性

- 新增纯逻辑类 `SimulationProfile`（无 Spring 依赖，可单测；按设备 ID 种子化随机数，可复现）
- `DataSimulatorService` 类名、@Service、@ConditionalOnProperty(simulator.enabled)、@Scheduled(interval-ms)、report() 调用链全部保留 → **WebSocket 广播与阈值告警链路零改动**（实测 16s 收到 27 条 data 消息）
- 新增配置（全部可省略，有默认值）：simulator.scenario.enabled / pollution-interval-rounds / pollution-duration-rounds / offline-interval-rounds / offline-duration-rounds / abnormal-probability

---

## 8. 修改/新增文件清单

### 新增文件（11 个 Java + 1 个 SQL + 4 个测试）

| 文件 | 说明 |
|---|---|
| src/main/resources/db/migration_v2.sql | 数据库最小迁移脚本 |
| bean/DataQuality.java | data_quality 实体 |
| dao/DataQualityDao.java | data_quality DAO |
| dto/AlertActionDTO.java | 告警流转请求体 |
| dto/QualityIssueDTO.java | 质量/异常记录 DTO |
| dto/QualityStatusDTO.java | 质量状态 DTO |
| dto/AnomalySummaryDTO.java | 异常汇总 DTO |
| dto/DeviceHealthDTO.java | 健康度 DTO |
| service/AlertLifecycleService.java + impl | 告警生命周期状态机 |
| service/DataQualityService.java + impl | 数据质量检测 + 周期扫描 |
| service/AnomalyDetectionService.java + impl | 统计异常检测 |
| service/DeviceHealthService.java + impl | 设备健康度计算 |
| service/simulator/SimulationProfile.java | 模拟场景引擎（纯逻辑） |
| ctrl/DataQualityController.java | /api/quality/* |
| ctrl/AnomalyController.java | /api/anomalies* |
| ctrl/DeviceHealthController.java | /api/health/* |
| test/AlertLifecycleTests.java | 生命周期 5 用例 |
| test/DataQualityTests.java | 质量检测 7 用例 |
| test/AnomalyDetectionTests.java | 异常检测 4 用例 |
| test/DeviceHealthTests.java | 健康度 3 用例 |
| test/SimulationProfileTests.java | 场景引擎 5 用例（纯单测） |

### 修改文件（6 个，全部向后兼容增量修改）

| 文件 | 修改内容 |
|---|---|
| bean/Alerts.java | 追加 6 个生命周期字段（state/ackTime/ackUser/resolveTime/resolveUser/durationSeconds） |
| ctrl/AlertsController.java | page 增加 state 过滤；handle 委托生命周期 resolve；新增 ack/process/resolve 端点 |
| service/impl/MonitorDataServiceImpl.java | ① 告警创建时写 state=level ② 指标回正常→自动恢复 NORMAL ③ NULL 检测 ④ 质量检测 ⑤ 统计异常检测（均为附加调用，主流程不变） |
| service/impl/AlertLifecycleServiceImpl.java | 状态机实现（含时钟回拨防护：负时间差不阻塞恢复） |
| config/SimulatorConfig.java | 追加 6 个场景配置项（带默认值） |
| service/simulator/DataSimulatorService.java | 内部改为场景引擎驱动（对外契约不变） |

---

## 9. 测试结果

### 9.1 自动化测试（mvn test，27/27 通过）

| 测试类 | 用例 | 结果 |
|---|---|---|
| CoreBusinessTests（原有） | 阈值告警触发 / 事务回滚 | ✅ 2/2 |
| NepsystemApplicationTests（原有） | 上下文加载 | ✅ 1/1 |
| AlertLifecycleTests | 完整状态机 / 自动恢复 NORMAL / 旧 handle 兼容 / 新端点 / state 过滤 | ✅ 5/5 |
| DataQualityTests | NULL / 超范围 / 连续相同 / 突变 / 离线置位 / 质量状态 / 接口 | ✅ 7/7 |
| AnomalyDetectionTests | ZSCORE（留一法）/ 连续超标与告警并存 / 统计突变 / 接口 | ✅ 4/4 |
| DeviceHealthTests | 部分数据健康分 / 无数据设备 POOR / 接口 | ✅ 3/3 |
| SimulationProfileTests | 量程内波动 / 早晚高峰 / 污染连续超标 / 离线恢复 / 异常注入 | ✅ 5/5 |

### 9.2 运行时 E2E（后端启动 + 模拟器开启，18/18 通过）

登录 / 上报超标 / 告警 page(level/deviceId/state 过滤) / acknowledge→process→resolve（含 ack_user、duration_seconds） / 旧 handle 兼容 / unhandled / health（含在线率、完整率、异常、告警） / quality issues+status / anomalies list+summary / 旧接口回归（devices/thresholds/sensors/stats/trend/history）。

### 9.3 WebSocket 实时链路实测

连接 /ws/notify 采集 16 秒：**收到 27 条 data 消息**（3 设备 × 4 指标 × ~3 轮，与 5s 周期吻合），消息格式与旧版完全一致 → 现有 Dashboard 无需修改。

---

## 10. 兼容性说明

1. **旧 API 全部保留**：30 个前端调用路径/参数/返回结构零变化；新增接口全部为独立新路径。
2. **旧字段语义不变**：level（WARN/ALARM）、status（0/1）、handle_user/handle_time 继续维护；/handle 等价于 resolve。
3. **WebSocket 不变**：data/alert 消息格式与广播时机不变（仍在上报事务内广播，与审计时一致）。
4. **Dashboard 零改动**：统计接口、WS 消费、空间分布全部不受影响。
5. **模拟器配置兼容**：simulator.enabled / interval-ms 语义不变；新配置均有默认值，缺省即启用场景。
6. **事务一致**：质量/异常/生命周期写入与上报同事务，回滚一致（回归测试验证）。
7. **测试隔离**：新测试全部 @Transactional 回滚；质量/异常类用例使用独立测试设备（E2E-QUALITY-*/E2E-ANOM-*）并按设备过滤断言，避免共享库中历史运行数据（模拟器/扫描任务产物）干扰；模拟器测试为纯单元测试；周期扫描任务带初始延迟 30s，不影响测试。

## 11. 已知限制与后续建议（含预存问题）

1. **alerts.id 为雪花型大整数**（MyBatis-Plus 生成，**预存行为**，本次未引入）：超出 JS 安全整数（2^53），前端直接使用该 id 调 handle/acknowledge 等接口存在精度丢失风险（本次 E2E 已用字符串 id 规避并验证后端正常）。建议后续后端以字符串返回 id 或前端使用 BigInt 处理。
2. **内存态说明**：质量检测的上一值/连续相同计数与异常检测的滚动窗口为进程内状态，重启后重建（需重新积累样本），属流式检测的固有取舍。
3. **告警去重窗口**：30 分钟防重复仍统计全部近期告警（含已恢复），恢复后 30 分钟内再次触发不会生成新告警（旧行为，未改动）。
4. **周期扫描默认阈值**：离线判定默认 3×interval（模拟器 5s 时为 15s），接真实硬件时建议配置 quality.offline-threshold-ms。
5. **模拟器量程常量**：SimulationProfile 内量程/基值与 sensors 字典保持一致（文档已注明），若字典变更需同步修改。

---

*本升级未删除任何原有功能；全部改动经自动化测试 + 运行时 E2E 验证。*
