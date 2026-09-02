-- ============================================================
-- 乱码数据修复 + 真实感数据补充（Phase 10 收尾）
-- 说明: 0x3F(?) 为历史 PowerShell 中文请求写入的不可恢复乱码，
--       按行精确 UPDATE 重写；删除一条联动验证测试残留链；
--       补充高新区基础数据。仅在当前演示库执行一次。
-- ============================================================
USE nep;

-- ---------- 1. 网格（修复） ----------
UPDATE grids SET grid_name = '演示网格-东区', description = '用于功能演示的东区网格' WHERE id = 22;

-- ---------- 2. 监督事件（修复标题/描述/位置） ----------
UPDATE supervision_event SET
  title = '路面扬尘监督事件',
  description = '东区道路施工产生明显扬尘，影响周边居民出行与空气质量。',
  location = '东区主干道施工段'
WHERE id = 95;

UPDATE supervision_event SET
  title = 'ENV-001 路面扬尘',
  description = '东区道路施工扬尘，颗粒物浓度超标，影响周边环境。',
  location = '东区工地入口'
WHERE id = 205;

UPDATE supervision_event SET
  title = '和平区空气异味投诉',
  description = '和平区XX路附近出现明显异味，怀疑存在空气污染。',
  location = '和平区XX路'
WHERE id = 360;

-- ---------- 3. 删除联动验证测试残留链（事件359 -> 任务162 -> 记录69 + 日志/附件） ----------
DELETE sa FROM supervision_attachment sa JOIN supervision_event e ON sa.event_id = e.id WHERE e.id = 359;
DELETE FROM event_status_log WHERE event_id = 359;
DELETE ir FROM inspection_record ir JOIN inspection_task t ON ir.task_id = t.id WHERE t.event_id = 359;
DELETE FROM inspection_task WHERE event_id = 359;
DELETE FROM supervision_event WHERE id = 359;

-- ---------- 4. 巡检任务（修复 result） ----------
UPDATE inspection_task SET result = '完成现场巡检与数据采集，扬尘明显，PM2.5偏高' WHERE id = 14;
UPDATE inspection_task SET result = '现场检测：PM2.5超标，颗粒物偏高，已记录取证' WHERE id = 76;
UPDATE inspection_task SET result = '现场检测：发现明显颗粒物污染，PM2.5超标，已拍照取证' WHERE id = 163;

-- ---------- 5. 检测记录（修复 content） ----------
UPDATE inspection_record SET content = '现场巡检完成：扬尘明显，PM2.5偏高，已取证' WHERE id = 9;
UPDATE inspection_record SET content = '现场检测：颗粒物偏高，超标风险' WHERE id = 23;
UPDATE inspection_record SET content = '现场检测：发现明显颗粒物污染，PM2.5超标，已拍照取证' WHERE id = 70;

-- ---------- 6. 状态日志（含问号的 remark 按流转语义重写，仅限修复行） ----------
UPDATE event_status_log SET remark = CASE to_status
  WHEN 'APPROVED' THEN '管理员(admin): 审核通过'
  WHEN 'ASSIGNED' THEN '管理员(admin): 派单'
  WHEN 'ACCEPTED' THEN '网格员接收任务'
  WHEN 'INSPECTING' THEN '网格员开始检测'
  WHEN 'INSPECTED' THEN '网格员提交检测'
  WHEN 'VERIFIED' THEN '管理员(admin): 核实通过'
  WHEN 'CLOSED' THEN '管理员(admin): 关闭事件'
  WHEN 'REJECTED' THEN '管理员(admin): 驳回'
  ELSE remark END
WHERE remark LIKE '%?%' AND event_id IN (95, 205, 360);

-- ---------- 7. 补充真实感基础数据：高新区 / GRID-002 / DEV-002 ----------
INSERT INTO regions (name, parent_id, description) VALUES ('高新区', 0, '高新技术产业园区')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO grids (grid_code, grid_name, region_id, description, status)
SELECT 'GRID-002', '高新区一网格', id, '高新区综合监管网格', 1 FROM regions WHERE name = '高新区'
ON DUPLICATE KEY UPDATE grid_name = VALUES(grid_name), region_id = VALUES(region_id), status = 1;

INSERT INTO devices (device_code, device_name, type, region_id, location, lat, lng, status)
SELECT 'DEV-002', '高新区空气监测站', 'AIR', r.id, '高新区科技园东门', 31.2350000, 121.4800000, 1
FROM regions r WHERE r.name = '高新区'
ON DUPLICATE KEY UPDATE device_name = VALUES(device_name), region_id = VALUES(region_id), status = 1;
