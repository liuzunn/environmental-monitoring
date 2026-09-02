-- ============================================================
-- Demo 业务链 · 基础数据脚本（Phase 9）
-- 说明: 幂等可重复执行（INSERT ... ON DUPLICATE KEY UPDATE 或先查后插）
-- 业务链主体（事件/任务/检测/日志）由真实 API 流程驱动，保证状态机与日志真实；
-- 本脚本仅建立基础关联数据：用户/区域/网格/网格成员/设备。
-- 执行方式: mysql -uroot -p < demo_business_chain.sql
-- ============================================================
USE nep;

-- ---------- 1. 用户（角色为语义标记；权限按身份+归属校验） ----------
INSERT INTO users (username, password, nickname, role, status) VALUES
('zhang_san', '123456', '张三', 'PUBLIC_SUPERVISOR', 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), role = VALUES(role), status = 1;

INSERT INTO users (username, password, nickname, role, status) VALUES
('wang_qiang', '123456', '王强', 'GRID_INSPECTOR', 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), role = VALUES(role), status = 1;

-- ---------- 2. 区域：和平区 ----------
INSERT INTO regions (name, parent_id, description) VALUES
('和平区', 0, '演示区域：和平区')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- ---------- 3. 网格 GRID-001（隶属和平区） ----------
INSERT INTO grids (grid_code, grid_name, region_id, description, status)
SELECT 'GRID-001', '和平区一网格', id, '演示网格：和平区一网格', 1 FROM regions WHERE name = '和平区'
ON DUPLICATE KEY UPDATE grid_name = VALUES(grid_name), region_id = VALUES(region_id), status = 1;

-- ---------- 4. 网格成员：王强 -> GRID-001（网格员） ----------
INSERT INTO grid_member (grid_id, user_id, role, status)
SELECT g.id, u.id, 'GRID_USER', 1
FROM grids g, users u
WHERE g.grid_code = 'GRID-001' AND u.username = 'wang_qiang'
ON DUPLICATE KEY UPDATE role = VALUES(role), status = 1;

-- ---------- 5. 设备 DEV-001（空气站，隶属和平区，带坐标） ----------
INSERT INTO devices (device_code, device_name, type, region_id, location, lat, lng, status)
SELECT 'DEV-001', '和平区空气质量站', 'AIR', r.id, '和平区XX路', 31.2304000, 121.4737000, 1
FROM regions r WHERE r.name = '和平区'
ON DUPLICATE KEY UPDATE device_name = VALUES(device_name), region_id = VALUES(region_id), status = 1;
