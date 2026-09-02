-- ============================================================
-- ENVISION 空间态势升级 · 最小迁移脚本
-- 说明：仅追加可空列，不修改任何现有数据（含演示设备）。
-- 坐标为空(NULL)时，前端自动回退到"类型分组抽象布局"（即原空间分布逻辑）。
-- ============================================================

ALTER TABLE devices
  ADD COLUMN lat DECIMAL(10,7) DEFAULT NULL COMMENT '纬度(可空, WGS84)' AFTER location,
  ADD COLUMN lng DECIMAL(10,7) DEFAULT NULL COMMENT '经度(可空, WGS84)' AFTER lat,
  ADD KEY idx_lat_lng (lat, lng);

-- ------------------------------------------------------------
-- 可选（演示用途，按需自行执行）：为 3 台演示设备填充示意坐标。
-- 注意：这是对现有数据行的修改，默认不执行；需要时取消注释后运行。
-- UPDATE devices SET lat=31.2304000, lng=121.4737000 WHERE device_code='DEV-AIR-001';
-- UPDATE devices SET lat=31.2312000, lng=121.4745000 WHERE device_code='DEV-WTR-001';
-- UPDATE devices SET lat=31.2289000, lng=121.4718000 WHERE device_code='DEV-NSE-001';
-- ------------------------------------------------------------
