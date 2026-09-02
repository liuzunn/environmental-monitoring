# API Contract Matrix · 接口一致性检查

> 方法：后端 Controller 注解/DTO/实体 与 四端 api/index.js 请求/消费字段逐项比对 + 实测响应核对（2026-09-02 审计）。
> 结论：**0 处不一致**（字段名/类型/分页/ID 序列化全链一致）；Long id 统一字符串化。

## 1. 核心契约矩阵

| 接口(URL) | 调用端 | 前端请求字段 | 后端 DTO/参数 | 后端返回 data | 前端读取 | 数据库表 | 一致 |
|---|---|---|---|---|---|---|---|
| POST /auth/login | NEPV/NEPM | {adminCode,password} | Map adminCode/password | {token,adminId,adminCode} | 同字段 | admins | ✅ |
| POST /auth/login-public | NEPS/NEPG | {username,password} | Map | {token,userId,username,nickname} | 同字段 | users | ✅ |
| POST /supervision | NEPS | {eventType,level,title,description,location,deviceId,regionId,lat,lng,attachments[]} | SupervisionCreateDTO 同字段（无 userId/status） | SupervisionEvent(id:String,eventNo,status,...) | created.id 跳详情 | supervision_event+attachment+log | ✅ |
| GET /supervision/mine | NEPS | page,size,status | @RequestParam 同 | PageResult{total,pages,records:[SupervisionEventVO]} | data.total/records[].status | event | ✅ |
| GET /supervision/{id} | NEPS/NEPM | path id | @PathVariable Long | {event:SupervisionEventVO,attachments:[...],statusLogs:[...],taskId,taskNo} | 同字段 | 4 表 | ✅ |
| GET /supervision/admin/list | NEPM | page,size,status,keyword | 同 | PageResult{total,pages,records} | 同 | event | ✅ |
| PUT /supervision/{id}/approve|reject | NEPM | body {remark} | SupervisionReviewDTO | SupervisionEvent | data.status | event+log | ✅ |
| PUT /supervision/{id}/assign | NEPM | {gridId,assigneeId,priority,deadline,remark} | AssignEventDTO | SupervisionEvent | data.status | event+task+log | ✅ |
| GET /tasks/mine | NEPG | page,size,status,keyword | 同 | PageResult{total,pages,records:[TaskVO]} | records[].taskNo/status | task | ✅ |
| PUT /tasks/mine/{id}/accept|start|submit | NEPG | submit:{pm25..o3,content,images[],lat,lng} | DetectSubmitDTO | InspectionTask | data.status | record+task+event+log | ✅ |
| GET /tasks/{id}/records | NEPM | path | Long | [InspectionRecordVO{taskId,pm25..,aqiValue,images[],...}] | 同 | record | ✅ |
| PUT /tasks/{id}/verify|close | NEPM | remark(query) | String | InspectionTask | data.status | task+event+log | ✅ |
| GET /stats/overview | NEPV/NEPS | - | - | {totalDevices,onlineDevices,todayReports,unhandledAlerts,todayEvents,pendingReview,processing,closedEvents,eventHandleRate,totalEvents} | 同 | 多表 | ✅ |
| GET /stats/supervision | NEPV | - | - | {regionDistribution[],typeDistribution[],taskStats{...},gridTasks[],highRiskEvents[],eventTrend[]} | 同 | 4 业务表 | ✅ |
| GET /devices/page | 四端 | page,size,keyword,type,status | 同 | PageResult | data.total/records | devices | ✅ |
| GET /data/trend | NEPM/NEPG/NEPV | deviceId,sensorCode,interval,start,end | 同(Date 格式 yyyy-MM-dd HH:mm:ss) | [{t,avg_value,max_value,min_value}] | r.t/avg_value | monitor_data | ✅ |
| GET /alerts/page | NEPS/NEPM/NEPV | page,size,level,status,deviceId | 同 | PageResult(Alerts.id 字符串) | 同 | alerts | ✅ |

## 2. 命名/类型检查项

| 项 | 结论 |
|---|---|
| camelCase vs snake_case | 全链 camelCase（JSON）↔ DB snake_case（ORM 映射）✅ |
| null 处理 | 前端统一 `?? '-'` / 可选链防御 ✅ |
| 数组 | records/images/statusLogs/attachments 均数组；空返回 [] ✅ |
| 分页 | 请求 page/size(1起)，响应 {total,pages,records}；前端读 total/records ✅（6 接口实测） |
| 时间格式 | 后端 ISO-8601（如 2026-09-02T17:31:00），前端 replace('T',' ') 显示 ✅ |
| 枚举 | eventType(4)/level(2)/priority(3)/status(8/9) 前后端映射表一致 ✅ |
| Long ID | alerts/supervision_event/inspection_task/record/log 的 id 与关联 id 均 ToStringSerializer 字符串输出；前端以字符串传递（路径拼接）✅ 实测一致 |
| 身份头 | X-User-Id/X-Admin-Id 数字字符串，后端解析 Integer ✅ |

## 3. 已知说明项
- 列表接口返回实体扩展字段（如 VO 关联名）为追加式，兼容旧消费 ✅
- 前端死封装 4 个与后端死接口 3 个（NEPV），见《07》BUG-007
- 错误响应字段固定 {code,message,data}，前端拦截器按 code 处理（HTTP 恒 200）——BUG-004 建议改进 HTTP 状态码