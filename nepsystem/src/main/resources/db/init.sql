-- ============================================================
-- 环境监测保护系统 · 数据库初始化脚本
-- 库: nep  字符集: utf8mb4  引擎: InnoDB
-- 说明: 可重复执行（全部使用 IF NOT EXISTS）
-- 执行方式: mysql -uroot -p < init.sql  或 Navicat 中直接运行
-- ============================================================
CREATE DATABASE IF NOT EXISTS nep DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE nep;

-- ------------------------------------------------------------
-- 1. admins 管理员表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admins (
  admin_id   INT AUTO_INCREMENT COMMENT '管理员ID(主键)',
  admin_code VARCHAR(50)  NOT NULL COMMENT '管理员账号(登录名)',
  password   VARCHAR(100) NOT NULL COMMENT '登录密码',
  remarks    VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (admin_id),
  UNIQUE KEY uk_admin_code (admin_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ------------------------------------------------------------
-- 2. users 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id          INT AUTO_INCREMENT COMMENT '用户ID(主键)',
  username    VARCHAR(50)  NOT NULL COMMENT '用户名(登录名)',
  password    VARCHAR(100) NOT NULL COMMENT '密码',
  nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  role        VARCHAR(20)  DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
  status      TINYINT      DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='普通用户表';

-- ------------------------------------------------------------
-- 3. regions 区域表（监测点位归属区域）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS regions (
  id          INT AUTO_INCREMENT COMMENT '区域ID(主键)',
  name        VARCHAR(50)  NOT NULL COMMENT '区域名称',
  parent_id   INT DEFAULT 0 COMMENT '父区域ID, 0为顶级',
  description VARCHAR(255) DEFAULT NULL COMMENT '区域描述',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域表';

-- ------------------------------------------------------------
-- 4. devices 监测设备/站点表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS devices (
  id               INT AUTO_INCREMENT COMMENT '设备ID(主键)',
  device_code      VARCHAR(50)  NOT NULL COMMENT '设备编号(唯一, 上报时使用)',
  device_name      VARCHAR(100) NOT NULL COMMENT '设备名称',
  type             VARCHAR(20)  NOT NULL COMMENT '设备类型: AIR空气/WATER水质/NOISE噪声',
  region_id        INT DEFAULT NULL COMMENT '所属区域ID',
  location         VARCHAR(255) DEFAULT NULL COMMENT '安装位置描述',
  status           TINYINT      DEFAULT 0 COMMENT '状态: 0离线 1在线 2停用',
  last_report_time DATETIME DEFAULT NULL COMMENT '最近上报时间',
  create_time      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_code (device_code),
  KEY idx_type (type),
  KEY idx_region (region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测设备表';

-- ------------------------------------------------------------
-- 5. sensors 指标字典表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sensors (
  id           INT AUTO_INCREMENT COMMENT '指标ID(主键)',
  sensor_code  VARCHAR(30)  NOT NULL COMMENT '指标编码(唯一): PM25/CO2/TEMP/HUMI/PH/TURBIDITY/DO/NOISE',
  sensor_name  VARCHAR(50)  NOT NULL COMMENT '指标名称',
  unit         VARCHAR(20)  DEFAULT NULL COMMENT '单位: ug/m3, ppm, ℃, %, dB 等',
  device_type  VARCHAR(20)  DEFAULT NULL COMMENT '适用设备类型: AIR/WATER/NOISE, NULL=通用',
  min_range    DECIMAL(10,2) DEFAULT NULL COMMENT '量程下限',
  max_range    DECIMAL(10,2) DEFAULT NULL COMMENT '量程上限',
  standard_max DECIMAL(10,2) DEFAULT NULL COMMENT '标准限值(超标判定)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sensor_code (sensor_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测指标字典表';

-- ------------------------------------------------------------
-- 6. monitor_data 监测数据表（核心表）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS monitor_data (
  id          BIGINT AUTO_INCREMENT COMMENT '数据ID(主键)',
  device_id   INT          NOT NULL COMMENT '设备ID',
  sensor_code VARCHAR(30)  NOT NULL COMMENT '指标编码',
  value       DECIMAL(10,2) NOT NULL COMMENT '监测数值',
  report_time DATETIME     NOT NULL COMMENT '上报时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (id),
  KEY idx_device_time (device_id, report_time),
  KEY idx_sensor_time (sensor_code, report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测数据表(核心)';

-- ------------------------------------------------------------
-- 7. thresholds 告警阈值表（device_id 为空 = 全局默认）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS thresholds (
  id          INT AUTO_INCREMENT COMMENT '阈值ID(主键)',
  device_id   INT DEFAULT NULL COMMENT '设备ID, NULL=全局默认',
  sensor_code VARCHAR(30) NOT NULL COMMENT '指标编码',
  warn_min    DECIMAL(10,2) DEFAULT NULL COMMENT '预警下限',
  warn_max    DECIMAL(10,2) DEFAULT NULL COMMENT '预警上限',
  alarm_min   DECIMAL(10,2) DEFAULT NULL COMMENT '报警下限',
  alarm_max   DECIMAL(10,2) DEFAULT NULL COMMENT '报警上限',
  enabled     TINYINT  DEFAULT 1 COMMENT '是否启用: 1启用 0停用',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_device_sensor (device_id, sensor_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警阈值表';

-- ------------------------------------------------------------
-- 8. alerts 告警记录表（核心表）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alerts (
  id           BIGINT AUTO_INCREMENT COMMENT '告警ID(主键)',
  device_id    INT          NOT NULL COMMENT '设备ID',
  sensor_code  VARCHAR(30)  NOT NULL COMMENT '指标编码',
  level        VARCHAR(10)  NOT NULL COMMENT '级别: WARN预警/ALARM报警',
  alert_value  DECIMAL(10,2) NOT NULL COMMENT '触发告警的数值',
  message      VARCHAR(255) DEFAULT NULL COMMENT '告警描述',
  status       TINYINT      DEFAULT 0 COMMENT '状态: 0未处理 1已处理',
  handle_user  VARCHAR(50)  DEFAULT NULL COMMENT '处理人',
  handle_time  DATETIME DEFAULT NULL COMMENT '处理时间',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
  PRIMARY KEY (id),
  KEY idx_device_time (device_id, create_time),
  KEY idx_status (status),
  KEY idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表(核心)';

-- ============================================================
-- 示例数据
-- ============================================================
INSERT INTO admins (admin_code, password, remarks) VALUES
('admin', '123456', '系统管理员(默认账号)')
ON DUPLICATE KEY UPDATE remarks = remarks;

INSERT INTO regions (name, parent_id, description) VALUES
('校区', 0, '校园监测区域'),
('教学楼A栋', 1, '教学楼A栋监测点'),
('人工湖', 1, '校园人工湖水质监测点')
ON DUPLICATE KEY UPDATE description = description;

-- 3 台演示设备（空气/水质/噪声）
INSERT INTO devices (device_code, device_name, type, region_id, location, status) VALUES
('DEV-AIR-001', '教学楼A栋空气质量站', 'AIR', 2, '教学楼A栋3层走廊', 1),
('DEV-WTR-001', '人工湖水质监测站', 'WATER', 3, '人工湖东岸', 1),
('DEV-NSE-001', '操场噪声监测站', 'NOISE', 1, '操场东南角', 1)
ON DUPLICATE KEY UPDATE device_name = device_name;

-- 5 个核心指标
INSERT INTO sensors (sensor_code, sensor_name, unit, device_type, min_range, max_range, standard_max) VALUES
('TEMP', '温度', '℃', NULL, -10, 60, 35),
('HUMI', '湿度', '%', NULL, 0, 100, 80),
('PM25', 'PM2.5', 'ug/m3', 'AIR', 0, 500, 75),
('CO2', '二氧化碳', 'ppm', 'AIR', 300, 5000, 1000),
('PH', '酸碱度', 'pH', 'WATER', 0, 14, 8.5),
('TURBIDITY', '浊度', 'NTU', 'WATER', 0, 100, 5),
('DO', '溶解氧', 'mg/L', 'WATER', 0, 20, 5),
('NOISE', '噪声', 'dB', 'NOISE', 20, 130, 70)
ON DUPLICATE KEY UPDATE sensor_name = sensor_name;

-- 2 条全局阈值（示例）：PM25 预警75 报警150；噪声 预警65 报警75
INSERT INTO thresholds (device_id, sensor_code, warn_min, warn_max, alarm_min, alarm_max, enabled) VALUES
(NULL, 'PM25', NULL, 75, NULL, 150, 1),
(NULL, 'NOISE', NULL, 65, NULL, 75, 1)
ON DUPLICATE KEY UPDATE enabled = enabled;
