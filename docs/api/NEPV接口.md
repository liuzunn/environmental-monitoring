# NEPV 决策可视化端 · 接口清单（33 个前端封装）

> 身份：管理员登录（admins）+ Authorization；只读展示为主（设备/用户/阈值管理页为管理员操作）。
> 数据全部真实：监测数据来自模拟器上报，监管统计来自业务表。

| 前端封装 | 方法/路径 | 说明/页面 |
|---|---|---|
| login/logout | POST /auth/login|logout | 登录 |
| getDevicesPage/addDevice/updateDevice/deleteDevice | /devices* | 设备管理页 |
| getOnlineCount | GET /devices/online/count | ⚠️死封装（无页面调用） |
| getDeviceLatest | GET /devices/{id}/latest | Dashboard 每设备最新值 |
| reportData | POST /data/report | ⚠️死封装（模拟器走后端直调） |
| getLatestData | GET /data/latest | ⚠️死封装 |
| getHistory | GET /data/history | 历史页表格 |
| getTrend | GET /data/trend | 历史趋势图 + 详情 AQI |
| exportCsv | GET /data/export(blob) | 历史页导出 |
| getAlertsPage/handleAlert/getUnhandled/getAlertsStat | /alerts* | 告警中心/角标/Dashboard 趋势 |
| getThresholds/add/update/delete | /thresholds* | 阈值设置页 |
| getOverview | GET /stats/overview | Dashboard 统计卡（含监管字段） |
| getQuality | GET /stats/quality | 首页 AQI/评分 |
| getDeviceRanking | GET /stats/device-ranking | ⚠️死封装 |
| getUsersPage/add/update/delete/changeUserStatus | /users* | 用户管理页 |
| getSensors | GET /sensors | 指标字典 |
| getSupervisionStats | GET /stats/supervision | Dashboard 监管区块（Phase8） |
| getSituationOverview/getSituationDevices | /situation/* | 空间态势页 |
| （未封装但后端提供） | /health/*、/quality/*、/anomalies/* | 大屏/态势内部数据源（页面经 situation 间接消费） |

## 数据真实性声明
- 无 Mock/随机/写死：Dashboard 图表均来自上述 API；监管统计 100% 来自 supervision_event/inspection_task/inspection_record/event_status_log
- 监测数据源为后端模拟器（系统设计，5s/轮场景引擎）；接真实硬件时由 POST /data/report 替代
- 死封装 4 个（online-count/latest/device-ranking/reportData）为历史预留，无页面调用（P4 项）