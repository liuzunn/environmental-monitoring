-- ============================================================
-- 四端升级 · NEPM 管理端最小迁移脚本 v5
-- 说明: 仅追加可空/带默认值列，不修改任何现有数据。
--       ALTER TABLE 在 MySQL 8 不支持 IF NOT EXISTS，本脚本仅需执行一次。
--       执行方式: mysql -uroot -p < migration_v5.sql
-- ============================================================
USE nep;

-- 1. grids 网格表：增加逻辑状态（1启用 0停用；删除改用停用，避免外键级联）
ALTER TABLE grids
  ADD COLUMN status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0停用' AFTER description,
  ADD KEY idx_status (status);

-- 2. inspection_task 巡检任务表：增加优先级（管理员派单/创建任务时指定）
ALTER TABLE inspection_task
  ADD COLUMN priority VARCHAR(10) DEFAULT 'MEDIUM' COMMENT '优先级: LOW/MEDIUM/HIGH' AFTER task_type,
  ADD KEY idx_priority (priority);
