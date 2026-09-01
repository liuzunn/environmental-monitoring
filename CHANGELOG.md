# 更新日志

## v0.2.0（2026-09-01）业务层升级 + 预存问题修复
- 告警生命周期：WARN/ALARM -> ACKNOWLEDGED -> PROCESSING -> RESOLVED，指标恢复正常自动 NORMAL（含持续时间/确认/解决字段）
- 设备健康度：在线率/最近通信/数据完整率/异常次数/告警次数 -> healthScore（/api/health/*）
- 数据质量检测：NULL/超范围/突变/长时间不变化/间隔异常/设备离线（/api/quality/*）
- 统计异常检测：Z-Score(留一法)/连续超标/统计突变，与阈值引擎并存（/api/anomalies*）
- 增强数据模拟器：日周期/早晚高峰/突发污染/连续超标/离线恢复/异常注入（场景引擎 SimulationProfile）
- 数据库最小迁移：alerts 追加生命周期字段；新增 data_quality 表（migration_v2.sql）
- 预存问题修复：alerts/monitor_data 自增计数器重置；告警 id 以字符串序列化（解决 JS 精度丢失）；CSV 导出 blob 拦截修复
- 测试：27/27 通过 + E2E 18/18 + WebSocket 实测

## v0.1.0（初始基线）
- 原有功能：实时监测/空气水质噪声/WebSocket 推送/数据模拟器/阈值引擎/WARN-ALARM 告警/历史数据/CSV 导出/设备 CRUD/空间分布/用户管理/统计 Dashboard
