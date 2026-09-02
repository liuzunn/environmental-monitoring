# NEPS 公众监督端 · 接口清单（9 个前端封装）

> 身份：`X-User-Id`（users.id）+ Authorization；登录接口 `/auth/login-public`。
> 全部数据来自真实后端；无 Mock。

| 前端封装 | 方法 | 路径 | 权限 | 数据库表 | 页面 |
|---|---|---|---|---|---|
| loginPublic / logoutPublic | POST | /auth/login-public / logout-public | 匿名/本人 | users | 登录页/退出 |
| getQuality | GET | /stats/quality | 登录 | monitor_data+sensors | 首页 AQI 卡 |
| getDevicesPage | GET | /devices/page?page=1&size=100 | 登录 | devices | 首页附近监测点 |
| getAlertsPage | GET | /alerts/page?page=1&size=5 | 登录 | alerts | 首页近期环境问题 |
| getOverview | GET | /stats/overview | 登录 | 多表 | 首页统计行 |
| createSupervision | POST | /supervision | 公众 | supervision_event+attachment+log | 我要监督 |
| getSupervisionMine | GET | /supervision/mine?page&size&status | 公众(本人) | supervision_event | 我的监督 |
| getSupervisionDetail | GET | /supervision/{id} | 本人或管理员 | 4 表 | 监督详情(时间线) |

## 请求/响应要点
- 创建 body：`{eventType,level,title,description,location,deviceId?,regionId?,lat?,lng?,attachments:[{fileName,fileSize,contentType}]}`（无 userId/status 字段——身份取自请求头、状态后端强制）
- mine 响应：`{total,pages,records:[{id,eventNo,title,eventType,status,location,createTime,...}]}`（status 过滤：PENDING_REVIEW/APPROVED/.../CLOSED）
- 详情响应：`{event,attachments[],statusLogs[]}`——时间线驱动“待审核/处理中/已完成/已驳回”展示与消息中心；CLOSED 文案“已完成/事件已处理”
- 消息中心：基于 mine 事件动态聚合（真实 API）；EVENT_CLOSED WebSocket 通知仅提醒

## 错误场景（实测）
- 401 未登录/用户不存在/密码错误；403 账号禁用/查看他人事件；400 参数或状态非法；统一 {code,message,data}。