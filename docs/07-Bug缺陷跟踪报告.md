# 07 · Bug 缺陷跟踪报告（修复状态更新于 Bug 修复阶段）

> 来源：代码静态扫描（TODO/FIXME/console.error/异常处理/死代码）+ 真实动态审计（越权/状态注入/错误处理/事务/一致性）。
> 分级：P0 业务不可运行 ｜ P1 核心业务错误 ｜ P2 功能异常 ｜ P3 体验 ｜ P4 优化建议。

## 修复记录（Bug 修复阶段，2026-09-02）

| Bug | 根因 | 修改文件 | 修改内容 | 测试 | 结果 |
|---|---|---|---|---|---|
| BUG-001 | InspectionTaskServiceImpl 6 个写方法无事务 | service/impl/InspectionTaskServiceImpl.java；test/GridWorkerTests.java | 补 @Transactional(rollbackFor=Exception.class) ×6 + import；新增回滚一致性用例 | mvn test 71→76 | ✅（超长 images 触发异常后记录/任务/事件/日志全部回滚实测） |
| BUG-002 | 无上传接口/存储/URL | 新增 ctrl/UploadController.java、config/StaticResourceConfig.java；GlobalExceptionHandler 超限 400；application.properties multipart 5MB；NEPS Supervise/详情、NEPG TaskDetail、NEPM 详情；三端 vite 代理 /uploads | POST /api/upload（图片 5MB 校验+UUID 存储）；/uploads/** 静态映射；前端选图即上传并写入 filePath/images（URL）；详情按 URL 展示 | 实测：上传→静态 200→非图 400→超限 400→落库 filePath 一致 | ✅ PASS |
| BUG-003 | token 无校验 | 新增 common/TokenStore.java、config/AuthInterceptor.java；AuthController/PublicAuthController 签发/登出移除 | TokenStore 24h 有效期；拦截器校验 Authorization（无效 401，HTTP200+code 与契约一致）；登录签发入库、登出移除 | AuthTokenTests×4 + E2E | ✅ 乱 token 401 / 登出失效 / 有效放行 |
| BUG-004 | 兜底返回 e.getMessage() | common/GlobalExceptionHandler.java | 兜底脱敏为固定文案 + 完整日志；上传超限转 400 | 脱敏用例 + E2E | ✅ 500 不再泄露堆栈（HTTP 状态码统一因前端/测试依赖 code+HTTP200 契约，保留现状并记录） |

## 1. 扫描结果（静态）

| 扫描项 | 结果 |
|---|---|
| TODO/FIXME/XXX/HACK | 无业务标记（仅测试字符串 "HACK"） |
| 前端 console.error | 无 |
| 前端 Mock/随机/写死数据 | 无（grep Math.random/mock 零命中） |
| 未实现按钮/空接口 | 无（所有按钮均有真实实现） |

## 2. Bug 清单

| 编号 | 级别 | 端/模块 | 问题描述 | 根因 | 修复方案 | 状态 |
|---|---|---|---|---|---|---|
| BUG-001 | P1 | 后端任务域 | 写操作无事务：submit(插记录+改任务+改事件+插日志 4 步)/verify/close/accept/start 各 3 步，中途异常会部分提交 | InspectionTaskServiceImpl 6 个写方法缺 @Transactional（事件域同款操作已有） | 6 方法补 @Transactional(rollbackFor=Exception.class) + 回滚测试 | 待修 |
| BUG-002 | P1 | 全端 | 图片无真实上传：附件/检测照仅登记文件名；file_path 恒空；Blob URL 刷新失效 | 无 POST /api/upload、无文件存储与静态映射 | 新增上传接口+uploads 目录+静态映射；NEPS/NEPG 先传后写 URL；详情按 URL 展示 | 待修 |
| BUG-003 | P2 | 后端认证 | Token 形同虚设：乱 Authorization 仍 200（身份由 X-Admin-Id/X-User-Id 头决定，可伪造） | 无拦截器/token 校验（UUID 装饰 token） | AuthInterceptor + 内存 TokenStore（过期）+ 写接口校验；AdminsCtrl 一并下线 | 待修 |
| BUG-004 | P2 | 后端异常 | 兜底异常返回 e.getMessage()（实测 ClassCastException 细节泄露），HTTP 状态恒 200 | GlobalExceptionHandler 兜底未脱敏 | 固定文案+全量日志；错误响应带真实 HTTP 状态码 | 待修 |
| BUG-005 | P3 | 后端鉴权 | 鉴权状态码跨域不一致：事件域无凭证=401，网格/任务域=403 | requireAdmin 实现不一 | 统一“无凭证401/无效凭证403” | 待修 |
| BUG-006 | P4 | 后端/前端 | 教学残留：/admins/insert|update|delete 无鉴权可匿名改管理员；demos/* 匿名可访问 | AdminsCtrl、demos 包、static/index.html 未清理 | 随 BUG-003 一并下线/加鉴权；删除 demos | 待修 |
| BUG-007 | P4 | 前端 NEPV | 死封装 4 个：getOnlineCount/getLatestData/getDeviceRanking/reportData（后端 3 死接口） | 历史预留 | 删除或注释说明 | 待修 |
| BUG-008 | P4 | WebSocket | 无心跳保活；断线由前端 3s 重连兜底 | 设计取舍 | 可选 ping/pong | 待修 |
| BUG-009 | P4 | 数据库 | 密码明文存储（admins/users）；阈值表无唯一约束（可重复配置，引擎 limit 1 随机命中） | 课程设计定位 | 加密改造/唯一键或业务查重（P4） | 待修 |
| BUG-010 | P4 | 历史遗留 | monitor_data 无保留期清理（模拟器 8.6 万行/天量级）；历史/趋势接口无默认时间范围；质量评分 DO/PH 方向缺陷（v0.1 审计遗留） | 数据治理缺口 | 清理任务/前端默认范围/评分特判（P4） | 待修 |
| BUG-011 | P4 | 历史遗留 | 设备无离线检测任务（在线状态依赖上报置 1）；阈值重复配置 | 早期审计 H4/H11 遗留 | DeviceStatusJob/查重（P4） | 待修 |
| BUG-012 | P3 | 前端 | 页面首次打开依赖 Vite 预构建，构建期 504 曾致黑屏（已修：手动 optimize + 文档提示）；曾修复 onUnmounted 缺 import 导致登录后黑屏（已修） | 历史缺陷，均已修复 | 无（已闭环） | ✅已修 |
| BUG-013 | P3 | 数据 | 历史脚本（PowerShell 中文）写入的乱码数据（0x3F） | 客户端编码非 UTF-8 | fix_demo_data.sql 已修复，残留 0 | ✅已修 |
| BUG-014 | P4 | 前端 | 各页存在同类工具函数重复（typeName/fmtTime/colorOf 等 4-6 处） | 早期审计 H15 | 抽 utils/format（P4） | 待修 |

## 3. 分级统计

| 级别 | 数量 | 编号 |
|---|---|---|
| P0 | 0 | - |
| P1 | 2 → **0（已修复）** | BUG-001 ✅ / BUG-002 ✅ |
| P2 | 2 → **0（已修复）** | BUG-003 ✅ / BUG-004 ✅（HTTP 状态码部分因契约约束保留 HTTP200+code） |
| P3 | 3 | BUG-005、BUG-012（已修）、BUG-013（已修） |
| P4 | 9 | BUG-006~011、BUG-014 等 |

## 4. 建议修复顺序
1. BUG-001 事务注解（半小时）→ 2. BUG-002 上传链路（半天）→ 3. BUG-003+006 认证拦截（半天）→ 4. BUG-004 异常脱敏（半小时）→ 5. BUG-005/007/008/009/010/011/014 顺带（P3/P4 可分批）。