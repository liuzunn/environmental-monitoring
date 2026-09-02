# NEPM 管理端 · 接口清单（30 个前端封装）

> 身份：`X-Admin-Id`（admins.admin_id）+ Authorization；登录 `/auth/login`。
> 全部数据真实；无 Mock。

| 前端封装 | 方法/路径 | 权限 | 表 | 页面 |
|---|---|---|---|---|
| login/logout | POST /auth/login|logout | 管理员 | admins | 登录/退出 |
| getWorkbenchStats | GET /supervision/admin/stats | 管理员 | event | 工作台 5 统计卡 |
| getPendingEvents | GET /supervision/admin/pending?limit | 管理员 | event | 工作台待处理列表 |
| getUnhandled / getAlertsPage | GET /alerts/unhandled、/alerts/page | 登录 | alerts | 工作台告警/角标 |
| getEventsPage | GET /supervision/admin/list?page&size&status&keyword | 管理员 | event | 监督事件列表 |
| getEventDetail | GET /supervision/{id} | 管理员 | 4 表 | 事件详情（含检测结果卡） |
| approveEvent/rejectEvent | PUT /supervision/{id}/approve|reject | 管理员 | event+log | 审核/驳回 |
| assignEvent | PUT /supervision/{id}/assign | 管理员 | event+task+log | 派单弹窗 |
| getGrids/add/update/delete/changeStatus | GET/POST/PUT/DELETE /grids* | 管理员 | grids | 网格管理 |
| getGridMembers/assign/remove | GET/POST/DELETE /grid-members/* | 管理员 | member+grid+user | 网格员管理 |
| getTasksPage/createTask | GET/POST /tasks* | 管理员 | task | 任务管理 |
| getAssigneeStats | GET /tasks/assignee-stats | 管理员 | task+user | 完成率 |
| getTaskRecords | GET /tasks/{id}/records | 管理员 | record | 详情检测结果 |
| verifyTask/closeTask | PUT /tasks/{id}/verify|close | 管理员 | task+event+log | 核实/关闭 |
| getUsersPage | GET /users/page | 管理员 | users | 网格员分配候选人 |
| getDevicesPage/getQuality/getTrend/getSensors | 共享只读 | 登录 | devices/data/sensors | 详情辅助（AQI/趋势） |

## 请求/响应要点
- 工作台统计 data：`{todayEvents,pendingReview,pendingAssign,processing,todayClosed}`
- 列表分页统一 `{total,pages,records}`；keyword 支持 eventNo/title/description
- 派单 body：`{gridId,assigneeId,priority:LOW|MEDIUM|HIGH,deadline?,remark?}`（仅 APPROVED 可派）
- 检测结果：六项 + aqiValue + images[] + 检测人/坐标/时间（taskId 由事件详情 taskId 字段获取）

## 权限（实测）
- 无 X-Admin-Id → 401；无效 X-Admin-Id（如普通用户 id）→ 403；grid/网格员可写接口均拒绝。