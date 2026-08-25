package org.nep.nepsystem.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.nep.nepsystem.bean.Devices;

/**
 * Devices 数据访问层
 */
@Mapper
public interface DevicesDao extends BaseMapper<Devices> {
}
