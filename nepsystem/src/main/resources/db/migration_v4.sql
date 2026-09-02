-- ============================================================
-- 四端升级 · 网格与监督业务层数据库迁移脚本 v4
-- 库: nep  字符集: utf8mb4  引擎: InnoDB
-- 说明: 可重复执行（全部使用 IF NOT EXISTS / 无 ALTER）
--       执行方式: mysql -uroot -p < migration_v4.sql  或 Navicat 中直接运行
-- 原则: 不删除/不修改任何现有表，仅新增业务表（网格/监督事件/巡检）
-- 业务关系:
--   regions ↓ grids ↓ grid_member ↓ users
--   regions ↓ devices ↓ monitor_data      devices ↓ alerts
--   users ↓ supervision_event ↓ inspection_task ↓ inspection_records
--   supervision_event ↓ supervision_attachment
--   supervision_event ↓ event_status_log
--   supervision_event ↓ event_evaluation
-- 核心状态机(事件与任务共用): PENDING_REVIEW/APPROVED/REJECTED/
--   ASSIGNED/ACCEPTED/INSPECTING/INSPECTED/VERIFIED/CLOSED
-- ============================================================
USE nep;

-- ------------------------------------------------------------
-- 1. grids 网格表（隶属区域: regions ↓ grids）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grids (
  id          INT AUTO_INCREMENT COMMENT '网格ID(主键)',
  grid_code   VARCHAR(50)  NOT NULL COMMENT '网格编号(唯一)',
  grid_name   VARCHAR(100) NOT NULL COMMENT '网格名称',
  region_id   INT DEFAULT NULL COMMENT '所属区域ID',
  description VARCHAR(255) DEFAULT NULL COMMENT '网格描述',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_grid_code (grid_code),
  KEY idx_region (region_id),
  CONSTRAINT fk_grid_region FOREIGN KEY (region_id) REFERENCES regions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格表';

-- ------------------------------------------------------------
-- 2. grid_member 网格成员表（grids ↓ grid_member ↓ users）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS grid_member (
  id          INT AUTO_INCREMENT COMMENT '成员ID(主键)',
  grid_id     INT NOT NULL COMMENT '网格ID',
  user_id     INT NOT NULL COMMENT '用户ID(网格员)',
  role        VARCHAR(20) DEFAULT 'GRID_USER' COMMENT '网格内角色: GRID_USER网格员/GRID_LEADER网格长',
  status      TINYINT DEFAULT 1 COMMENT '状态: 1在职 0离职',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_grid_user (grid_id, user_id),
  KEY idx_user (user_id),
  CONSTRAINT fk_gm_grid FOREIGN KEY (grid_id) REFERENCES grids (id),
  CONSTRAINT fk_gm_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网格成员表';

-- ------------------------------------------------------------
-- 3. supervision_event 监督事件表（公众监督核心表）
--    状态机: PENDING_REVIEW/APPROVED/REJECTED/ASSIGNED/ACCEPTED/
--            INSPECTING/INSPECTED/VERIFIED/CLOSED
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS supervision_event (
  id          BIGINT AUTO_INCREMENT COMMENT '事件ID(主键)',
  event_no    VARCHAR(50)  NOT NULL COMMENT '事件编号(唯一, 如 EV20260901001)',
  user_id     INT DEFAULT NULL COMMENT '提交人ID, NULL=匿名提交',
  event_type  VARCHAR(30)  NOT NULL COMMENT '事件类型: POLLUTION污染/NOISE噪声/DEVICE_FAULT设备故障/OTHER其他',
  title       VARCHAR(100) NOT NULL COMMENT '事件标题',
  description VARCHAR(500) DEFAULT NULL COMMENT '事件描述',
  device_id   INT DEFAULT NULL COMMENT '关联设备ID(可空)',
  region_id   INT DEFAULT NULL COMMENT '关联区域ID(可空)',
  location    VARCHAR(255) DEFAULT NULL COMMENT '事发位置描述',
  lat         DECIMAL(10,7) DEFAULT NULL COMMENT '纬度(可空, WGS84)',
  lng         DECIMAL(10,7) DEFAULT NULL COMMENT '经度(可空, WGS84)',
  level       VARCHAR(10)  DEFAULT 'WARN' COMMENT '严重程度: WARN预警/ALARM报警',
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '状态机: PENDING_REVIEW/APPROVED/REJECTED/ASSIGNED/ACCEPTED/INSPECTING/INSPECTED/VERIFIED/CLOSED',
  assignee_id INT DEFAULT NULL COMMENT '当前处理人ID(网格员)',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_event_no (event_no),
  KEY idx_status (status),
  KEY idx_user (user_id),
  KEY idx_device (device_id),
  KEY idx_region (region_id),
  KEY idx_assignee (assignee_id),
  CONSTRAINT fk_se_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_se_device FOREIGN KEY (device_id) REFERENCES devices (id),
  CONSTRAINT fk_se_region FOREIGN KEY (region_id) REFERENCES regions (id),
  CONSTRAINT fk_se_assignee FOREIGN KEY (assignee_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监督事件表(核心)';

-- ------------------------------------------------------------
-- 4. supervision_attachment 监督事件附件表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS supervision_attachment (
  id             BIGINT AUTO_INCREMENT COMMENT '附件ID(主键)',
  event_id       BIGINT NOT NULL COMMENT '监督事件ID',
  file_name      VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  file_path      VARCHAR(500) DEFAULT NULL COMMENT '存储路径',
  file_size      BIGINT DEFAULT NULL COMMENT '文件大小(字节)',
  content_type   VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
  upload_user_id INT DEFAULT NULL COMMENT '上传人ID(可空)',
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (id),
  KEY idx_event (event_id),
  CONSTRAINT fk_sa_event FOREIGN KEY (event_id) REFERENCES supervision_event (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监督事件附件表';

-- ------------------------------------------------------------
-- 5. inspection_task 巡检任务表（由监督事件派生或独立创建）
--    状态机与监督事件共用
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inspection_task (
  id          BIGINT AUTO_INCREMENT COMMENT '任务ID(主键)',
  task_no     VARCHAR(50)  NOT NULL COMMENT '任务编号(唯一, 如 TK20260901001)',
  event_id    BIGINT DEFAULT NULL COMMENT '关联监督事件ID(可空=独立巡检)',
  device_id   INT DEFAULT NULL COMMENT '巡检设备ID(可空)',
  grid_id     INT DEFAULT NULL COMMENT '所属网格ID(可空)',
  assignee_id INT DEFAULT NULL COMMENT '执行网格员ID(可空)',
  task_type   VARCHAR(20)  DEFAULT 'INSPECTION' COMMENT '任务类型: INSPECTION巡检/VERIFY核实',
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '状态机: PENDING_REVIEW/APPROVED/REJECTED/ASSIGNED/ACCEPTED/INSPECTING/INSPECTED/VERIFIED/CLOSED',
  deadline    DATETIME DEFAULT NULL COMMENT '截止时间',
  result      VARCHAR(500) DEFAULT NULL COMMENT '巡检结论',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_status (status),
  KEY idx_assignee (assignee_id),
  KEY idx_event (event_id),
  KEY idx_device (device_id),
  KEY idx_grid (grid_id),
  CONSTRAINT fk_it_event FOREIGN KEY (event_id) REFERENCES supervision_event (id),
  CONSTRAINT fk_it_device FOREIGN KEY (device_id) REFERENCES devices (id),
  CONSTRAINT fk_it_grid FOREIGN KEY (grid_id) REFERENCES grids (id),
  CONSTRAINT fk_it_assignee FOREIGN KEY (assignee_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检任务表(核心)';

-- ------------------------------------------------------------
-- 6. inspection_record 巡检记录表（任务执行明细）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inspection_record (
  id          BIGINT AUTO_INCREMENT COMMENT '记录ID(主键)',
  task_id     BIGINT NOT NULL COMMENT '巡检任务ID',
  record_type VARCHAR(20) DEFAULT 'INSPECT' COMMENT '记录类型: INSPECT巡检/VERIFY核实',
  content     VARCHAR(500) DEFAULT NULL COMMENT '记录内容',
  images      VARCHAR(1000) DEFAULT NULL COMMENT '现场图片路径(逗号分隔)',
  recorder_id INT DEFAULT NULL COMMENT '记录人ID(可空)',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (id),
  KEY idx_task (task_id),
  KEY idx_recorder (recorder_id),
  CONSTRAINT fk_ir_task FOREIGN KEY (task_id) REFERENCES inspection_task (id),
  CONSTRAINT fk_ir_recorder FOREIGN KEY (recorder_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='巡检记录表';

-- ------------------------------------------------------------
-- 7. event_status_log 监督事件状态流转日志表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_status_log (
  id          BIGINT AUTO_INCREMENT COMMENT '日志ID(主键)',
  event_id    BIGINT NOT NULL COMMENT '监督事件ID',
  from_status VARCHAR(20) DEFAULT NULL COMMENT '原状态(创建时为NULL)',
  to_status   VARCHAR(20) NOT NULL COMMENT '新状态',
  operator_id INT DEFAULT NULL COMMENT '操作人ID(可空=系统)',
  remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_event (event_id),
  KEY idx_operator (operator_id),
  CONSTRAINT fk_esl_event FOREIGN KEY (event_id) REFERENCES supervision_event (id),
  CONSTRAINT fk_esl_operator FOREIGN KEY (operator_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件状态流转日志表';

-- ------------------------------------------------------------
-- 8. event_evaluation 监督事件评价表（事件闭环后评价）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_evaluation (
  id          BIGINT AUTO_INCREMENT COMMENT '评价ID(主键)',
  event_id    BIGINT NOT NULL COMMENT '监督事件ID',
  user_id     INT DEFAULT NULL COMMENT '评价人ID(可空=匿名)',
  score       TINYINT DEFAULT NULL COMMENT '评分 1-5',
  content     VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_event_user (event_id, user_id),
  KEY idx_user (user_id),
  CONSTRAINT fk_ee_event FOREIGN KEY (event_id) REFERENCES supervision_event (id),
  CONSTRAINT fk_ee_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件评价表';
