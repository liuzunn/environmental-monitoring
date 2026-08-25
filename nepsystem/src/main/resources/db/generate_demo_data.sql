-- ============================================================
-- 演示数据生成：为每台设备生成 3 天历史数据（每 10 分钟一条）
-- 执行方式: mysql -uroot -p < generate_demo_data.sql
-- 说明：可重复执行（先清空 monitor_data 再插入）
-- ============================================================
USE nep;

DROP PROCEDURE IF EXISTS generate_demo_data;
DELIMITER $$
CREATE PROCEDURE generate_demo_data()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE dev_id INT;
    DECLARE dev_type VARCHAR(20);
    DECLARE dev_code VARCHAR(50);
    DECLARE base_time DATETIME;
    DECLARE i INT DEFAULT 0;
    DECLARE total_points INT;
    DECLARE rnd DOUBLE;
    DECLARE cur CURSOR FOR SELECT id, type, device_code FROM devices WHERE status != 2;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- 清空旧数据（保留真实上报数据）
    DELETE FROM monitor_data;
    DELETE FROM alerts;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO dev_id, dev_type, dev_code;
        IF done = 1 THEN
            LEAVE read_loop;
        END IF;

        -- 3 天，每 10 分钟一条 = 432 个时间点
        SET base_time = DATE_SUB(NOW(), INTERVAL 3 DAY);
        SET total_points = 432;
        SET i = 0;

        WHILE i < total_points DO
            SET rnd = RAND();

            IF dev_type = 'AIR' THEN
                INSERT INTO monitor_data (device_id, sensor_code, value, report_time, create_time) VALUES
                (dev_id, 'TEMP', ROUND(18 + RAND() * 17, 2), DATE_ADD(base_time, INTERVAL i * 10 MINUTE), NOW()),
                (dev_id, 'HUMI', ROUND(30 + RAND() * 50, 2), DATE_ADD(base_time, INTERVAL i * 10 MINUTE), NOW()),
                (dev_id, 'PM25', ROUND(IF(RAND() < 0.05, 150 + RAND() * 150, RAND() * 120), 2), DATE_ADD(base_time, INTERVAL i * 10 MINUTE), NOW()),
                (dev_id, 'CO2', ROUND(350 + RAND() * 850, 2), DATE_ADD(base_time, INTERVAL i * 10 MINUTE), NOW());
            ELSEIF dev_type = 'WATER' THEN
                INSERT INTO monitor_data (device_id, sensor_code, value, report_time, create_time) VALUES
                (dev_id, 'PH', ROUND(6.5 + RAND() * 2.5, 2), DATE_ADD(base_time, INTERVAL i * 10 MINUTE), NOW()),
                (dev_id, 'TURBIDITY', ROUND(RAND() * 20, 2), DATE_ADD(base_time, INTERVAL i * 10 MINUTE), NOW()),
                (dev_id, 'DO', ROUND(4 + RAND() * 8, 2), DATE_ADD(base_time, INTERVAL i * 10 MINUTE), NOW()),
                (dev_id, 'TEMP', ROUND(15 + RAND() * 15, 2), DATE_ADD(base_time, INTERVAL i * 10 MINUTE), NOW());
            ELSEIF dev_type = 'NOISE' THEN
                INSERT INTO monitor_data (device_id, sensor_code, value, report_time, create_time) VALUES
                (dev_id, 'NOISE', ROUND(30 + RAND() * 65, 2), DATE_ADD(base_time, INTERVAL i * 10 MINUTE), NOW());
            END IF;

            SET i = i + 1;
        END WHILE;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL generate_demo_data();
DROP PROCEDURE IF EXISTS generate_demo_data;

-- 验证
SELECT COUNT(*) AS demo_total FROM monitor_data;
SELECT device_id, COUNT(*) AS cnt FROM monitor_data GROUP BY device_id;
