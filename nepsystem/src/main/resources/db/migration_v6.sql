-- ============================================================
-- 四端升级 · NEPG 网格员端最小迁移脚本 v6
-- 说明: 仅追加可空列，不修改任何现有数据。
--       ALTER TABLE 在 MySQL 8 不支持 IF NOT EXISTS，本脚本仅需执行一次。
--       执行方式: mysql -uroot -p < migration_v6.sql
-- ============================================================
USE nep;

-- inspection_record 巡检记录表：追加现场检测数据列（六项污染物 + 计算AQI + 检测坐标）
ALTER TABLE inspection_record
  ADD COLUMN pm25 DECIMAL(10,2) DEFAULT NULL COMMENT 'PM2.5检测值(ug/m3)' AFTER content,
  ADD COLUMN pm10 DECIMAL(10,2) DEFAULT NULL COMMENT 'PM10检测值(ug/m3)' AFTER pm25,
  ADD COLUMN so2  DECIMAL(10,2) DEFAULT NULL COMMENT 'SO2检测值(ug/m3)' AFTER pm10,
  ADD COLUMN no2  DECIMAL(10,2) DEFAULT NULL COMMENT 'NO2检测值(ug/m3)' AFTER so2,
  ADD COLUMN co   DECIMAL(10,2) DEFAULT NULL COMMENT 'CO检测值(mg/m3)' AFTER no2,
  ADD COLUMN o3   DECIMAL(10,2) DEFAULT NULL COMMENT 'O3检测值(ug/m3)' AFTER co,
  ADD COLUMN aqi_value INT DEFAULT NULL COMMENT '计算AQI(有值项IAQI最大值)' AFTER o3,
  ADD COLUMN lat DECIMAL(10,7) DEFAULT NULL COMMENT '检测纬度(可空, WGS84)' AFTER aqi_value,
  ADD COLUMN lng DECIMAL(10,7) DEFAULT NULL COMMENT '检测经度(可空, WGS84)' AFTER lat;
