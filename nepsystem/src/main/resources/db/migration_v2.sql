-- ============================================================
-- ENVISION 业务层升级 · 数据库最小迁移脚本（增量，可重复执行需谨慎）
-- 1) alerts 表：增加生命周期字段（state/ack/resolve/duration）
-- 2) 新增 data_quality 表：数据质量 + 统计异常检测记录（快照式）
-- 说明：ALTER TABLE 在 MySQL 8 不支持 IF NOT EXISTS，本脚本仅需执行一次；
--       执行方式参考 docs/BUSINESS_UPGRADE.md。
-- ============================================================

-- ---------- 1. alerts 告警生命周期 ----------
ALTER TABLE alerts
  ADD COLUMN state VARCHAR(20) NOT NULL DEFAULT 'WARN'
    COMMENT '生命周期状态: WARN/ALARM/ACKNOWLEDGED/PROCESSING/RESOLVED/NORMAL' AFTER level,
  ADD COLUMN ack_time DATETIME DEFAULT NULL COMMENT '确认时间' AFTER handle_time,
  ADD COLUMN ack_user VARCHAR(50) DEFAULT NULL COMMENT '确认人' AFTER ack_time,
  ADD COLUMN resolve_time DATETIME DEFAULT NULL COMMENT '解决时间(含自动恢复NORMAL)' AFTER ack_user,
  ADD COLUMN resolve_user VARCHAR(50) DEFAULT NULL COMMENT '解决人(SYSTEM=自动恢复)' AFTER resolve_time,
  ADD COLUMN duration_seconds BIGINT DEFAULT NULL COMMENT '告警持续时间(秒)' AFTER resolve_user,
  ADD KEY idx_state (state);

-- 存量数据回填：status=1(已处理) -> RESOLVED，否则保持原级别(WARN/ALARM)
UPDATE alerts SET state = IF(status = 1, 'RESOLVED', level) WHERE state IS NULL OR state = '';

-- ---------- 2. data_quality 数据质量/异常检测记录表 ----------
CREATE TABLE IF NOT EXISTS data_quality (
  id BIGINT AUTO_INCREMENT COMMENT '记录ID',
  device_id INT NOT NULL COMMENT '设备ID',
  sensor_code VARCHAR(30) DEFAULT NULL COMMENT '指标编码(NULL=设备级问题)',
  category VARCHAR(20) NOT NULL COMMENT '类别: QUALITY数据质量 / ANOMALY统计异常',
  issue_type VARCHAR(40) NOT NULL COMMENT '问题类型: NULL_VALUE/OUT_OF_RANGE/CONSTANT_VALUE/INTERVAL_ABNORMAL/DEVICE_OFFLINE/ZSCORE/CONSECUTIVE_EXCEED/SUDDEN_CHANGE',
  severity VARCHAR(10) NOT NULL COMMENT '级别: INFO/WARN/CRITICAL',
  detail VARCHAR(255) DEFAULT NULL COMMENT '描述',
  latest_value DECIMAL(10,2) DEFAULT NULL COMMENT '最近一次触发值',
  first_seen DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次发现时间',
  last_seen DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近发现时间',
  occurrence_count INT DEFAULT 1 COMMENT '累计发生次数',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dev_sensor_type (device_id, sensor_code, issue_type),
  KEY idx_category_seen (category, last_seen)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量与统计异常记录(快照式,每设备每指标每问题类型一行)';

-- ---------- 3. 设备健康度辅助视图(可选,查询用) ----------
-- 不建视图：健康度由 DeviceHealthService 实时计算，避免维护冗余。
