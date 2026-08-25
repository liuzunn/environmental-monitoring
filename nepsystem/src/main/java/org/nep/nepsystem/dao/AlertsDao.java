package org.nep.nepsystem.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.nep.nepsystem.bean.Alerts;

/**
 * Alerts 数据访问层
 */
@Mapper
public interface AlertsDao extends BaseMapper<Alerts> {
}
