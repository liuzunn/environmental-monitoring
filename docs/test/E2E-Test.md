# E2E Test · 端到端测试记录

> 方式：真实后端(8080)+真实数据库，Node/HTTP 客户端按业务链逐步调用；测试数据用后清理（仅新增后删除自身行，未改动既有数据）。
> 日期：2026-09-02 工程文档阶段复测。结果：✅ 全部通过（P0=0）。

## 1. 链路一：NEPS → NEPM（公众提交→管理员可见）

| 步骤 | 请求 | 响应/数据库 | 结果 |
|---|---|---|---|
| 公众登录 | POST /auth/login-public {zhang_san} | 200 userId=590 | ✅ |
| 提交监督 | POST /supervision（POLLUTION/ALARM/DEV-001/和平区/附件） | 200 event_id=406 eventNo=EV202609021746001871 status=PENDING_REVIEW；DB：supervision_event+attachment(1)+status_log(1) | ✅ |
| NEPM 可见 | GET /supervision/admin/list?keyword=eventNo (X-Admin-Id=1) | total=1，records[0].id=406、status=PENDING_REVIEW、提交人=E2E市民 | ✅ |

## 2. 链路二：NEPM → NEPG（审核/派单→任务→网格员可见）

| 步骤 | 请求 | 响应/数据库 | 结果 |
|---|---|---|---|
| 审核 | PUT /supervision/406/approve | 200 status=APPROVED | ✅ |
| 派单 | PUT /supervision/406/assign {gridId:131,assigneeId:591,HIGH} | 200 status=ASSIGNED；DB：inspection_task id=189(event_id=406,assignee=591,grid=和平区一网格) + status_log | ✅ |
| NEPG 可见 | GET /tasks/mine (X-User-Id=591) | records 命中 id=189 status=ASSIGNED | ✅ |

## 3. 链路三：NEPG → NEPM（接单→检测→提交→管理员可见）

| 步骤 | 请求 | 响应/数据库 | 结果 |
|---|---|---|---|
| 接单 | PUT /tasks/mine/189/accept | ACCEPTED | ✅ |
| 开始 | PUT /tasks/mine/189/start | INSPECTING | ✅ |
| 提交检测 | PUT /tasks/mine/189/submit（六项 88.5/135/12/55/0.9/95 + 照片2 + 坐标） | INSPECTED；DB：inspection_record(task_id=189, AQI=117, recorder=王强)；事件同步 INSPECTED | ✅ |
| NEPM 看结果 | GET /tasks/189/records (X-Admin-Id=1) | 1 条：aqiValue=117、images=2、recorderName=王强 | ✅ |

## 4. 链路四：NEPM → NEPS（核实→关闭→公众可见）

| 步骤 | 请求 | 响应/数据库 | 结果 |
|---|---|---|---|
| 核实 | PUT /tasks/189/verify | VERIFIED（事件同步） | ✅ |
| 关闭 | PUT /tasks/189/close | CLOSED（事件同步；WS 通知提交人） | ✅ |
| NEPS 查看 | GET /supervision/406 (X-User-Id=提交人) | status=CLOSED；taskNo=TK202609021746003255；时间线 8 条完整序列 | ✅ |

## 5. 链路五：四端 → NEPV（统计联动）

| 步骤 | 请求 | 结果 |
|---|---|---|
| 关闭后 | GET /stats/overview | closedEvents 递增、处理率=closed/total（实测 50%） | ✅ |
| 关闭后 | GET /stats/supervision | typeDistribution 含 POLLUTION；taskStats total/closed 递增；eventTrend 当日新增 | ✅ |

## 6. ID Trace 结果（全链一致）

```text
user 662(提交人) → supervision_event 406(eventNo EV202609021746001871, user_id=662)
→ inspection_task 189(event_id=406, assignee_id=591)
→ inspection_record(task_id=189, recorder_id=591)
→ event_status_log ×8(event_id=406，状态序列完整)
→ NEPV 统计（closedEvents 含本事件）
结论：event_id/task_id/record_id/operator_id 逐级一致，无断链。
```

## 7. 异常/边界抽查（同轮审计）

| 场景 | 结果 |
|---|---|
| 重复审核/接单/提交/核实/关闭 | 400 |
| 未审核派单/未接收开始/未核实关闭 | 400 |
| 越权（公众间/网格员间/角色冒充/无身份） | 401/403 |
| 不存在 ID（事件/任务 5 处） | 400 |
| status 注入（body 带 CLOSED） | 后端强制 PENDING_REVIEW |
| WS 通知五链路 + 隔离 | 通过（Phase 7） |
| 孤儿数据（任务/记录/日志） | 0 |

## 8. 结论

完整业务链（公众提交→审核→派单→接单→检测→提交→核实→关闭→查看→统计）真实跑通；ID、状态、时间线、统计全链一致；权限与状态机防护有效；本阶段复测无 P0/P1 阻断项（BUG-001/002 为代码级待修项，见《07》）。