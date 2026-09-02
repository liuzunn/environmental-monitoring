# NEPG 网格员端 · 接口清单（12 个前端封装）

> 身份：`X-User-Id`（users.id）+ Authorization；登录复用 `/auth/login-public`（users 表）。
> 任务归属校验：仅本人（assignee_id）任务可见可操作。

| 前端封装 | 方法/路径 | 权限 | 表 | 页面 |
|---|---|---|---|---|
| login/logout | POST /auth/login-public|logout-public | 匿名/本人 | users | 登录 |
| getMyTasks | GET /tasks/mine?page&size&status&keyword | 本人 | task | 首页任务列表 |
| getMyTaskStats | GET /tasks/mine/stats | 本人 | task | 首页统计卡（待接收/进行中/今日完成/超时） |
| getMyTaskDetail | GET /tasks/mine/{id} | 本人 | task+event+attachment | 任务详情（公众反馈） |
| acceptTask | PUT /tasks/mine/{id}/accept | 本人 | task+event+log | 接收按钮 |
| startTask | PUT /tasks/mine/{id}/start | 本人 | task+event+log | 开始检测 |
| submitDetect | PUT /tasks/mine/{id}/submit | 本人 | record+task+event+log | 检测提交（六项+AQI） |
| getDevicesPage/getQuality/getTrend/getSensors | 共享只读 | 登录 | devices/data/sensors | 详情 AQI/地图辅助 |

## 请求/响应要点
- mine 响应 records：[{id,taskNo,eventTitle,priority,status,deadline,gridName,assigneeName,...}]
- submit body：`{pm25,pm10,so2,no2,co,o3,content,images:[文件名],lat,lng}`；AQI 后端计算入 record.aqiValue
- 状态按钮驱动：ASSIGNED→接收；ACCEPTED→开始；INSPECTING→提交；INSPECTED→完成（不可再操作）

## 权限（实测）
- 非本人任务：详情/接单/开始/提交均 403；未登录 401；状态乱序 400（如未接收就开始）。